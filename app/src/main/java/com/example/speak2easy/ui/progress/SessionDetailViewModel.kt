package com.example.speak2easy.ui.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.speak2easy.data.practice.ProgressRepository
import com.example.speak2easy.data.remote.dto.PracticeAttemptDetail
import com.example.speak2easy.data.remote.dto.SkippedItem
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val correct: List<PracticeAttemptDetail> = emptyList(),
    val incorrect: List<PracticeAttemptDetail> = emptyList(),
    val skipped: List<SkippedItem> = emptyList(),
) {
    val total: Int get() = correct.size + incorrect.size
    val accuracyPercent: Int
        get() = if (total == 0) 0 else (correct.size * 100) / total
}

class SessionDetailViewModel(
    private val repo: ProgressRepository,
    private val sessionId: String,
) : ViewModel() {

    var state by mutableStateOf(SessionDetailUiState())
        private set

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null)
            try {
                val response = repo.getSessionAttempts(sessionId)
                // iOS parity (Views/Progress/SessionDetailView.swift:18-35): dedup by contentId,
                // first attempt wins. Retries don't reclassify.
                val firstByContent = LinkedHashMap<String, PracticeAttemptDetail>()
                response.attempts.forEach { a ->
                    val key = a.contentId ?: return@forEach
                    if (!firstByContent.containsKey(key)) firstByContent[key] = a
                }
                val first = firstByContent.values.toList()
                state = SessionDetailUiState(
                    isLoading = false,
                    correct = first.filter { it.correctness == true },
                    incorrect = first.filter { it.correctness == false },
                    skipped = response.skipped.orEmpty(),
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Couldn't load the session details")
            }
        }
    }

    class Factory(
        private val repo: ProgressRepository,
        private val sessionId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionDetailViewModel(repo, sessionId) as T
    }
}
