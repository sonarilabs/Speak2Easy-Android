package com.sonari.speak2easy.ui.chart

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.lessons.LessonRepository
import com.sonari.speak2easy.data.remote.dto.ContentItem
import com.sonari.speak2easy.service.TtsPlayer
import com.sonari.speak2easy.ui.lessons.JapaneseScript
import kotlinx.coroutines.launch

data class ChartUiState(
    val script: JapaneseScript = JapaneseScript.HIRAGANA,
    val hiragana: List<ContentItem> = emptyList(),
    val katakana: List<ContentItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val items: List<ContentItem> get() = if (script == JapaneseScript.HIRAGANA) hiragana else katakana
}

class ChartViewModel(
    private val repo: LessonRepository,
    appContext: Context,
) : ViewModel() {

    var state by mutableStateOf(ChartUiState())
        private set

    private val tts = TtsPlayer(appContext)

    init {
        load()
    }

    fun selectScript(script: JapaneseScript) {
        state = state.copy(script = script)
    }

    /** Speaks the kana itself (matches iOS: passes contentText, not romaji). */
    fun speak(text: String) {
        tts.speak(text)
    }

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val hira = repo.getHiraganaChart()
                val kata = repo.getKatakanaChart()
                state = state.copy(hiragana = hira, katakana = kata, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Couldn't load the chart")
            }
        }
    }

    override fun onCleared() {
        tts.shutdown()
        super.onCleared()
    }

    class Factory(
        private val repo: LessonRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ChartViewModel(repo, appContext) as T
    }
}
