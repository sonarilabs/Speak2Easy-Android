package com.sonari.speak2easy.ui.lessons

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.lessons.LessonRepository
import com.sonari.speak2easy.data.prefs.SonariPreferences
import com.sonari.speak2easy.data.remote.dto.LessonProgress
import com.sonari.speak2easy.model.LessonCategory
import kotlinx.coroutines.launch

data class LessonsUiState(
    val category: LessonCategory = LessonCategory.HIRAGANA,
    val isLoading: Boolean = false,
    val hiraganaProgress: Map<String, Float> = emptyMap(),
    val hiraganaUnlocked: Map<String, Boolean> = emptyMap(),
    val katakanaProgress: Map<String, Float> = emptyMap(),
    val katakanaUnlocked: Map<String, Boolean> = emptyMap(),
    val wordSections: List<WordGroupSection> = emptyList(),
    val wordsAccessible: Boolean = true,
    // Per-group unlock state from /lessons?charset=words → wordGroups[*].isUnlocked.
    // Matches iOS behavior: a group is locked only when the API explicitly returns
    // false for it; missing key = treat as unlocked (the backend exhaustively lists
    // every group the user can see, so missing in practice means no data yet).
    val wordGroupUnlocked: Map<String, Boolean> = emptyMap(),
    // Per-group progress as a 0..1 fraction (completed_items / total_items). Drives
    // the progress bar on each WordGroupCard, same treatment as LessonCard.
    val wordGroupProgress: Map<String, Float> = emptyMap(),
    // Sentences track: numbered lessons loaded straight from /lessons?charset=sentences.
    val sentenceLessons: List<LessonProgress> = emptyList(),
    // Topics track: thematic vocab groups. Unlock/progress reuse wordGroup* maps above.
    val topicSections: List<WordGroupSection> = emptyList(),
    val error: String? = null,
)

/**
 * Pulls lesson progress + unlock state from the API and exposes it to [LessonsScreen]. Unlock
 * logic lives entirely server-side now (mirrors iOS); [SonariPreferences] is no longer threaded
 * through because the local level/completion overrides were removed.
 */
class LessonsViewModel(private val repo: LessonRepository) : ViewModel() {

    var state by mutableStateOf(LessonsUiState())
        private set

    private val loaded = mutableSetOf<LessonCategory>()

    init {
        selectCategory(LessonCategory.HIRAGANA)
    }

    fun selectCategory(category: LessonCategory) {
        state = state.copy(category = category)
        if (category !in loaded) load(category)
    }

    /** Re-fetch the current category (e.g. after a session, or when the network returns). */
    fun reloadCurrent() {
        loaded.remove(state.category)
        load(state.category)
    }

    private fun load(category: LessonCategory) {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                when (category) {
                    LessonCategory.HIRAGANA -> loadKana("hiragana", LessonData.hiraganaLessons) { p, u ->
                        state = state.copy(hiraganaProgress = p, hiraganaUnlocked = u)
                    }
                    LessonCategory.KATAKANA -> loadKana("katakana", LessonData.katakanaLessons) { p, u ->
                        state = state.copy(katakanaProgress = p, katakanaUnlocked = u)
                    }
                    LessonCategory.WORDS -> loadWords()
                    LessonCategory.SENTENCES -> loadSentences()
                    LessonCategory.TOPICS -> loadTopics()
                }
                loaded.add(category)
            } catch (e: Exception) {
                state = state.copy(error = e.message ?: "Failed to load lessons")
            } finally {
                state = state.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadKana(
        charset: String,
        groups: List<LessonGroup>,
        apply: (Map<String, Float>, Map<String, Boolean>) -> Unit,
    ) {
        val resp = repo.getLessons(charset)
        val progress = resp.lessons.associate { it.progressKey to it.progress }
        val apiUnlocked = resp.lessons.associate { it.progressKey to it.isUnlocked }
        apply(progress, computeUnlocked(groups, progress, apiUnlocked))
    }

    private suspend fun loadWords() {
        val groupsResp = repo.getContentGroups("word")
        val lessonsResp = runCatching { repo.getLessons("words") }.getOrNull()
        val perGroupUnlocked = lessonsResp?.wordGroups
            ?.associate { it.groupLabel to it.isUnlocked }
            .orEmpty()
        val perGroupProgress = lessonsResp?.wordGroups
            ?.associate { wg ->
                wg.groupLabel to (
                    if (wg.totalItems > 0) wg.completedItems.toFloat() / wg.totalItems else 0f
                )
            }
            .orEmpty()
        state = state.copy(
            wordSections = buildWordSections(groupsResp.groups),
            wordsAccessible = lessonsResp?.categoryAccessible ?: true,
            wordGroupUnlocked = perGroupUnlocked,
            wordGroupProgress = perGroupProgress,
        )
    }

    /** Sentences are charset-agnostic numbered lessons; the backend is the source of truth for unlock. */
    private suspend fun loadSentences() {
        val resp = repo.getLessons("sentences")
        state = state.copy(sentenceLessons = resp.lessons)
    }

    /** Topics: thematic vocab groups with an independent unlock chain (per-group from /lessons?charset=topics). */
    private suspend fun loadTopics() {
        val groupsResp = repo.getContentGroups("word", group = "topics")
        val lessonsResp = runCatching { repo.getLessons("topics") }.getOrNull()
        val unlocked = lessonsResp?.wordGroups?.associate { it.groupLabel to it.isUnlocked }.orEmpty()
        val progress = lessonsResp?.wordGroups
            ?.associate { wg -> wg.groupLabel to (if (wg.totalItems > 0) wg.completedItems.toFloat() / wg.totalItems else 0f) }
            .orEmpty()
        state = state.copy(
            topicSections = buildTopicSections(groupsResp.groups),
            wordGroupUnlocked = state.wordGroupUnlocked + unlocked,
            wordGroupProgress = state.wordGroupProgress + progress,
        )
    }

    class Factory(private val repo: LessonRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LessonsViewModel(repo) as T
    }
}
