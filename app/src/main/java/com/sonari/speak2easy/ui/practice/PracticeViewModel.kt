package com.sonari.speak2easy.ui.practice

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.lessons.LessonRepository
import com.sonari.speak2easy.data.practice.PracticeRepository
import com.sonari.speak2easy.data.remote.dto.ContentItem
import com.sonari.speak2easy.data.remote.dto.StartSessionRequest
import com.sonari.speak2easy.service.AudioRecorder
import com.sonari.speak2easy.service.HapticsManager
import com.sonari.speak2easy.service.TtsPlayer
import com.sonari.speak2easy.ui.lessons.PracticeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

data class PracticeItemUi(
    val contentId: String,
    val character: String,
    val romanization: String,
    val guide: String,
    val englishTranslation: String?,
    val hiraganaReading: String?,
) {
    val isWord: Boolean get() = englishTranslation != null
}

data class PracticeResultUi(val isCorrect: Boolean, val transcribed: String?, val feedback: String?)

enum class PracticePhase { LOADING, ACTIVE, COMPLETED, ERROR }

data class PracticeUiState(
    val phase: PracticePhase = PracticePhase.LOADING,
    val errorMessage: String? = null,
    val items: List<PracticeItemUi> = emptyList(),
    val currentIndex: Int = 0,
    val isRecording: Boolean = false,
    val isSubmitting: Boolean = false,
    val isPlaying: Boolean = false,
    val showResult: Boolean = false,
    val lastResult: PracticeResultUi? = null,
    val hintRevealed: Boolean = false,
    val focusMode: Boolean = false,
    val firstTryCorrect: Int = 0,
    val needsPractice: List<PracticeItemUi> = emptyList(),
) {
    val currentItem: PracticeItemUi? get() = items.getOrNull(currentIndex)
    val total: Int get() = items.size
    val accuracyPercent: Int get() = if (total == 0) 0 else firstTryCorrect * 100 / total
}

/**
 * Drives a practice session, mirroring iOS PracticeView: start session → fetch content →
 * per item (optional TTS → record → multipart submit → result → advance), with hands-free,
 * focus mode, skip, and a completion summary. Accuracy is first-try-correct ÷ total.
 */
class PracticeViewModel(
    private val plan: PracticePlan,
    private val practiceRepo: PracticeRepository,
    private val lessonRepo: LessonRepository,
    private val recorder: AudioRecorder,
    private val tts: TtsPlayer,
    private val haptics: HapticsManager,
) : ViewModel() {

    var state by mutableStateOf(PracticeUiState(focusMode = plan.options.isFocusMode))
        private set

    private var sessionId: String? = null
    private val firstAttempt = mutableMapOf<String, Boolean>()
    private val skipped = mutableListOf<String>()
    private var recordJob: Job? = null
    private var advanceJob: Job? = null
    private var hasMicPermission = false

    /**
     * Hands-free auto-record is paused after the user manually taps the speaker icon. They need
     * to press the mic to opt back in for the current item. Reset on each `advance()`.
     */
    private var handsFreePaused = false

    init {
        setup()
    }

    fun onPermissionResult(granted: Boolean) {
        hasMicPermission = granted
    }

    fun dismissError() {
        state = state.copy(errorMessage = null)
    }

    // MARK: Setup

    private fun setup() {
        viewModelScope.launch {
            state = state.copy(phase = PracticePhase.LOADING)
            try {
                val (sid, items) = loadItems()
                sessionId = sid
                val ordered = if (plan.options.isSequential) items else items.shuffled()
                if (ordered.isEmpty()) {
                    state = state.copy(phase = PracticePhase.ERROR, errorMessage = "No items to practice yet.")
                    return@launch
                }
                state = state.copy(phase = PracticePhase.ACTIVE, items = ordered, currentIndex = 0)
                maybeAutoStart()
            } catch (e: Exception) {
                state = state.copy(phase = PracticePhase.ERROR, errorMessage = e.message ?: "Couldn't start practice.")
            }
        }
    }

    private suspend fun loadItems(): Pair<String, List<PracticeItemUi>> = when (val src = plan.source) {
        is PracticeSource.Lesson -> {
            val charset = src.characterSet.value
            val lessonNumber = src.info.apiLessonNumber ?: src.info.number
            val contentType = if (src.info.number >= 16) "double_character" else "single_character"
            val session = practiceRepo.startSession(
                StartSessionRequest(
                    sessionType = "lesson",
                    isHandsFree = plan.options.isHandsFree,
                    lessonNumber = lessonNumber,
                    unitNumber = src.info.unitNumber,
                    charset = charset,
                ),
            )
            val content = lessonRepo.getLessonContent(lessonNumber, charset, contentType, src.info.unitNumber)
            session.sessionId to content.items.map { it.toUi() }
        }
        is PracticeSource.WordGroup -> {
            val charset = src.group.charset?.value
            val session = practiceRepo.startSession(
                StartSessionRequest(
                    sessionType = "drill",
                    isHandsFree = plan.options.isHandsFree,
                    groupLabel = src.group.groupLabel,
                    charset = charset,
                ),
            )
            val content = lessonRepo.getContentItems("word", charset, src.group.groupLabel, limit = 200)
            session.sessionId to content.items.map { it.toUi() }
        }
    }

    // MARK: Audio playback / recording

    fun playCurrent(then: () -> Unit = {}) {
        val item = state.currentItem ?: return then()
        state = state.copy(isPlaying = true)
        tts.speak(item.character) {
            state = state.copy(isPlaying = false)
            then()
        }
    }

    /**
     * Called by the UI when the user taps the speaker icon. Plays the pronunciation, and in
     * hands-free mode pauses the auto-record / auto-advance loop until they tap the mic to
     * resume — otherwise the user would hear the prompt and instantly get cut off by an auto
     * recording starting underneath them.
     */
    fun onManualSpeakerTap() {
        if (plan.options.isHandsFree) {
            handsFreePaused = true
            recordJob?.cancel()
            advanceJob?.cancel()
        }
        playCurrent()
    }

    fun startRecording() {
        if (!hasMicPermission) {
            state = state.copy(errorMessage = "Microphone permission is needed to practice speaking.")
            return
        }
        if (state.isRecording || state.isSubmitting || state.showResult) return
        // User explicitly asked to record — opt back into hands-free auto-flow for this item.
        handsFreePaused = false
        try {
            recorder.start()
        } catch (_: Exception) {
            state = state.copy(errorMessage = "Couldn't start recording.")
            return
        }
        state = state.copy(isRecording = true, errorMessage = null)
        recordJob = viewModelScope.launch {
            delay(RECORD_LIMIT_MS)
            if (state.isRecording) stopRecordingAndSubmit()
        }
    }

    fun stopRecordingAndSubmit() {
        if (!state.isRecording) return
        recordJob?.cancel()
        val file = recorder.stop()
        state = state.copy(isRecording = false)
        if (file == null) {
            state = state.copy(errorMessage = "No audio recorded — try again.")
            return
        }
        submit(file)
    }

    private fun submit(file: File) {
        val item = state.currentItem ?: return
        val sid = sessionId ?: return
        state = state.copy(isSubmitting = true)
        viewModelScope.launch {
            try {
                val resp = practiceRepo.submitAttempt(sid, item.contentId, file)
                handleResult(item, resp.isCorrect, resp.transcribedText, resp.feedback)
            } catch (e: Exception) {
                state = state.copy(isSubmitting = false, errorMessage = e.message ?: "Couldn't score that attempt.")
            } finally {
                file.delete()
            }
        }
    }

    private fun handleResult(item: PracticeItemUi, isCorrect: Boolean, transcribed: String?, feedback: String?) {
        if (isCorrect) haptics.playCorrect() else haptics.playIncorrect()
        if (item.contentId !in firstAttempt) firstAttempt[item.contentId] = isCorrect
        state = state.copy(
            isSubmitting = false,
            showResult = true,
            lastResult = PracticeResultUi(isCorrect, transcribed, feedback),
        )
        advanceJob = viewModelScope.launch {
            delay(if (isCorrect) CORRECT_DELAY_MS else INCORRECT_DELAY_MS)
            state = state.copy(showResult = false, lastResult = null)
            if (isCorrect) {
                advance()
            } else {
                // Don't advance on incorrect — stay on this item so the user can retry.
                // In hands-free, replay the prompt + auto-record; otherwise wait for the user.
                maybeAutoStart()
            }
        }
    }

    // MARK: Navigation within session

    fun onSkip() {
        if (state.isRecording) {
            recordJob?.cancel()
            recorder.cancel()
            state = state.copy(isRecording = false)
        }
        advanceJob?.cancel()
        state.currentItem?.let { skipped.add(it.contentId) }
        state = state.copy(showResult = false, lastResult = null)
        advance()
    }

    fun revealHint() {
        state = state.copy(hintRevealed = true)
    }

    /** Back-step to the previous item — cancels any in-flight recording or pending advance. */
    fun goPrev() {
        if (state.currentIndex <= 0) return
        advanceJob?.cancel()
        recordJob?.cancel()
        if (state.isRecording) runCatching { recorder.cancel() }
        state = state.copy(
            currentIndex = state.currentIndex - 1,
            hintRevealed = false,
            showResult = false,
            lastResult = null,
            isRecording = false,
            isSubmitting = false,
        )
        // Don't auto-start on a back-step — user pressed back deliberately.
    }

    private fun advance() {
        val next = state.currentIndex + 1
        if (next >= state.items.size) {
            complete()
            return
        }
        // Fresh item = fresh auto-flow opportunity, even if the previous one was paused.
        handsFreePaused = false
        state = state.copy(currentIndex = next, hintRevealed = false)
        maybeAutoStart()
    }

    private fun maybeAutoStart() {
        // iOS parity: autoplay is only meaningful inside hands-free. Outside hands-free,
        // the user drives playback + recording manually.
        if (!plan.options.isHandsFree) return
        if (handsFreePaused) return  // user took manual control on this item
        if (plan.options.autoPlayAudio) {
            playCurrent(then = { scheduleAutoRecord() })
        } else {
            scheduleAutoRecord()
        }
    }

    private fun scheduleAutoRecord() {
        viewModelScope.launch {
            delay(AUTO_RECORD_DELAY_MS)
            if (state.phase == PracticePhase.ACTIVE && !state.isRecording && !state.isSubmitting && !state.showResult && !handsFreePaused) {
                startRecording()
            }
        }
    }

    private fun complete() {
        val firstTry = firstAttempt.values.count { it }
        val needs = state.items.filter { firstAttempt[it.contentId] == false }
        state = state.copy(phase = PracticePhase.COMPLETED, firstTryCorrect = firstTry, needsPractice = needs)
        val sid = sessionId ?: return
        viewModelScope.launch { runCatching { practiceRepo.completeSession(sid, skipped.toList()) } }
    }

    override fun onCleared() {
        recordJob?.cancel()
        advanceJob?.cancel()
        recorder.cancel()
        tts.shutdown()
    }

    private fun ContentItem.toUi() = PracticeItemUi(
        contentId = contentId,
        character = contentText,
        romanization = romanization,
        guide = pronunciationGuide ?: "Pronounce as \"$romanization\"",
        englishTranslation = englishTranslation,
        hiraganaReading = hiraganaReading,
    )

    class Factory(
        private val plan: PracticePlan,
        private val practiceRepo: PracticeRepository,
        private val lessonRepo: LessonRepository,
        private val context: Context,
        private val haptics: HapticsManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PracticeViewModel(plan, practiceRepo, lessonRepo, AudioRecorder(context), TtsPlayer(context), haptics) as T
    }

    private companion object {
        const val RECORD_LIMIT_MS = 3_000L
        const val CORRECT_DELAY_MS = 1_500L
        const val INCORRECT_DELAY_MS = 2_500L
        const val AUTO_RECORD_DELAY_MS = 500L
    }
}
