package com.example.speak2easy.ui.writing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.speak2easy.data.lessons.LessonRepository
import com.example.speak2easy.data.remote.dto.ContentItem
import com.example.speak2easy.ui.lessons.JapaneseScript
import kotlinx.coroutines.launch

data class WritingHomeUiState(
    val script: JapaneseScript = JapaneseScript.HIRAGANA,
    val hiragana: List<ContentItem> = emptyList(),
    val katakana: List<ContentItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    val items: List<ContentItem>
        get() = if (script == JapaneseScript.HIRAGANA) hiragana else katakana
}

/**
 * Reuses [LessonRepository.getHiraganaChart] / [LessonRepository.getKatakanaChart] — same
 * data the Chart tab uses, so the in-memory cache means this loads instantly after Chart
 * (and vice versa).
 */
class WritingHomeViewModel(private val repo: LessonRepository) : ViewModel() {

    var state by mutableStateOf(WritingHomeUiState())
        private set

    init { load() }

    fun selectScript(script: JapaneseScript) {
        state = state.copy(script = script)
    }

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                // Restrict writing to the 46 basic kana (no dakuten/contracted) — the only ones
                // we have stroke counts for and the only ones iOS originally shipped.
                val hira = repo.getHiraganaChart().filter { KanaStrokeCounts.hiragana.containsKey(it.contentText) }
                val kata = repo.getKatakanaChart().filter { KanaStrokeCounts.katakana.containsKey(it.contentText) }
                state = state.copy(hiragana = hira, katakana = kata, isLoading = false)
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Couldn't load characters")
            }
        }
    }

    class Factory(private val repo: LessonRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WritingHomeViewModel(repo) as T
    }
}
