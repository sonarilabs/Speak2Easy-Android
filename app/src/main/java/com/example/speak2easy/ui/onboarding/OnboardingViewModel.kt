package com.example.speak2easy.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.speak2easy.data.auth.AuthRepository
import com.example.speak2easy.data.auth.AuthResult
import kotlinx.coroutines.launch

/**
 * Holds the onboarding form and submits it. On success, [AuthRepository] flips auth state to
 * Authenticated + onboardingCompleted, which routes the app into the main scaffold.
 */
class OnboardingViewModel(private val repo: AuthRepository) : ViewModel() {

    var form by mutableStateOf(OnboardingForm())
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun update(transform: (OnboardingForm) -> OnboardingForm) {
        form = transform(form)
    }

    fun submit() {
        if (isSubmitting) return
        isSubmitting = true
        errorMessage = null
        viewModelScope.launch {
            when (val result = repo.completeOnboarding(form.toRequest())) {
                is AuthResult.Success -> Unit // auth state change navigates away
                is AuthResult.Error -> errorMessage = result.message
            }
            isSubmitting = false
        }
    }

    class Factory(private val repo: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = OnboardingViewModel(repo) as T
    }
}
