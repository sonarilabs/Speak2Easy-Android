package com.example.speak2easy.ui.lessons

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.speak2easy.data.lessons.LessonRepository
import com.example.speak2easy.model.LessonCategory
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
    val error: String? = null,
)

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
        state = state.copy(
            wordSections = buildWordSections(groupsResp.groups),
            wordsAccessible = lessonsResp?.categoryAccessible ?: true,
        )
    }

    class Factory(private val repo: LessonRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LessonsViewModel(repo) as T
    }
}
