package com.sonari.speak2easy.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.auth.AuthRepository
import com.sonari.speak2easy.data.auth.AuthResult
import com.sonari.speak2easy.data.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

/**
 * Screen-facing wrapper over [AuthRepository]: exposes the app-wide auth state plus
 * per-screen loading/error/info messages, and validates input before hitting the network.
 */
class AuthViewModel(private val repo: AuthRepository) : ViewModel() {

    val authState: StateFlow<AuthState> = repo.authState

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    fun clearMessages() {
        _ui.value = _ui.value.copy(errorMessage = null, infoMessage = null)
    }

    /** Surface an error raised outside the repository (e.g. Google credential retrieval). */
    fun reportError(message: String) {
        _ui.value = AuthUiState(errorMessage = message)
    }

    fun signInWithEmail(email: String, password: String) =
        submit(validate = { emailError(email) ?: passwordError(password) }) {
            repo.signInWithEmail(email, password)
        }

    fun signUpWithEmail(email: String, password: String, displayName: String?) =
        submit(validate = { emailError(email) ?: passwordError(password) }) {
            repo.signUpWithEmail(email, password, displayName)
        }

    fun onGoogleCredential(idToken: String, email: String?, displayName: String?) =
        submit { repo.signInWithGoogle(idToken, email, displayName) }

    fun resendVerification() = submit { repo.resendVerificationEmail() }

    fun forgotPassword(email: String) =
        submit(validate = { emailError(email) }) { repo.forgotPassword(email) }

    fun resetPassword(token: String, newPassword: String) =
        submit(validate = { passwordError(newPassword) }) { repo.resetPassword(token, newPassword) }

    fun refreshVerificationStatus() {
        viewModelScope.launch { repo.refreshVerificationStatus() }
    }

    fun signOut() = repo.signOut()

    private fun submit(validate: () -> String? = { null }, action: suspend () -> AuthResult) {
        val validationError = validate()
        if (validationError != null) {
            _ui.value = AuthUiState(errorMessage = validationError)
            return
        }
        viewModelScope.launch {
            _ui.value = AuthUiState(isLoading = true)
            _ui.value = when (val result = action()) {
                is AuthResult.Success -> AuthUiState(infoMessage = result.message)
                is AuthResult.Error -> AuthUiState(errorMessage = result.message)
            }
        }
    }

    private fun emailError(email: String): String? =
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) "Enter a valid email address" else null

    private fun passwordError(password: String): String? =
        if (password.length < 8) "Password must be at least 8 characters" else null

    class Factory(private val repo: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repo) as T
    }
}
