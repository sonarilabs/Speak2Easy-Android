package com.sonari.speak2easy.ui.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.auth.AuthRepository
import com.sonari.speak2easy.data.practice.ProgressRepository
import com.sonari.speak2easy.data.remote.dto.PracticeSessionSummary
import com.sonari.speak2easy.data.remote.dto.UserProgressResponse
import kotlinx.coroutines.launch

data class ProgressUiState(
    val isLoading: Boolean = true,
    val progress: UserProgressResponse? = null,
    val sessions: List<PracticeSessionSummary> = emptyList(),
)

class ProgressViewModel(
    private val repo: ProgressRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    var state by mutableStateOf(ProgressUiState())
        private set

    init {
        load()
    }

    fun load() {
        val userId = auth.currentUser?.userId
        if (userId == null) {
            state = state.copy(isLoading = false)
            return
        }
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            val progress = runCatching { repo.getProgress(userId) }.getOrNull()
            val sessions = runCatching { repo.getSessions(userId, limit = 20) }.getOrNull().orEmpty()
            state = ProgressUiState(
                isLoading = false,
                progress = progress,
                sessions = sessions.filter { it.endedAt != null && (it.totalItems ?: 0) > 0 },
            )
        }
    }

    class Factory(
        private val repo: ProgressRepository,
        private val auth: AuthRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ProgressViewModel(repo, auth) as T
    }
}
