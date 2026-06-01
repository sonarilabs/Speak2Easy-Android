package com.sonari.speak2easy.ui.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.auth.AuthRepository
import com.sonari.speak2easy.data.auth.AuthResult
import com.sonari.speak2easy.data.prefs.SonariPreferences
import kotlinx.coroutines.launch

/**
 * Holds the onboarding form and submits it. On success, [AuthRepository] flips auth state to
 * Authenticated + onboardingCompleted, which routes the app into the main scaffold.
 *
 * Also mirrors the chosen Japanese level into [SonariPreferences] so the Lessons screen can
 * gate initial unlocks without re-fetching the user.
 */
class OnboardingViewModel(
    private val repo: AuthRepository,
    private val preferences: SonariPreferences,
) : ViewModel() {

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
                is AuthResult.Success -> {
                    // Persist locally before auth state flips and the screen unmounts.
                    preferences.setJapaneseLevel(form.japaneseLevel.value)
                }
                is AuthResult.Error -> errorMessage = result.message
            }
            isSubmitting = false
        }
    }

    class Factory(
        private val repo: AuthRepository,
        private val preferences: SonariPreferences,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OnboardingViewModel(repo, preferences) as T
    }
}
