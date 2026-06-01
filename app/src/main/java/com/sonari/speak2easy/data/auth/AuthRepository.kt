package com.sonari.speak2easy.data.auth

import com.sonari.speak2easy.data.remote.ApiException
import com.sonari.speak2easy.data.remote.AuthApi
import com.sonari.speak2easy.data.remote.NetworkException
import com.sonari.speak2easy.data.remote.UserApi
import com.sonari.speak2easy.data.remote.apiCall
import com.sonari.speak2easy.data.remote.dto.ForgotPasswordRequest
import com.sonari.speak2easy.data.remote.dto.RefreshTokenRequest
import com.sonari.speak2easy.data.remote.dto.ResendVerificationRequest
import com.sonari.speak2easy.data.remote.dto.ResetPasswordRequest
import com.sonari.speak2easy.data.remote.dto.OnboardingRequest
import com.sonari.speak2easy.data.remote.dto.SignInRequest
import com.sonari.speak2easy.domain.model.User
import com.sonari.speak2easy.util.TextSanitizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** Mirrors iOS `AuthState`. */
sealed interface AuthState {
    data object Unknown : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: User) : AuthState
    data class PendingVerification(val user: User) : AuthState
}

sealed interface AuthResult {
    data class Success(val message: String? = null) : AuthResult
    data class Error(
        val message: String,
        val accountExists: Boolean = false,
        val accountNotFound: Boolean = false,
    ) : AuthResult
}

/**
 * Single source of truth for auth state. Ports iOS `AuthenticationService`:
 * Keychain → [TokenStore], UserDefaults user cache → [TokenStore], and the same
 * restore / sign-in / verification flows. Google credential retrieval itself lives
 * in the UI layer (Phase 4); this just consumes the resulting provider id + profile.
 */
class AuthRepository(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenStore: TokenStore,
    private val json: Json,
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: User?
        get() = when (val s = _authState.value) {
            is AuthState.Authenticated -> s.user
            is AuthState.PendingVerification -> s.user
            else -> null
        }

    suspend fun restoreSession() {
        val token = tokenStore.getToken()
        if (token == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }
        val cached = tokenStore.getCachedUser()
        if (cached == null) {
            tokenStore.clear()
            _authState.value = AuthState.Unauthenticated
            return
        }
        try {
            val fresh = apiCall(json) { userApi.getUser(cached.userId) }
                .copy(authProvider = cached.authProvider)
            tokenStore.saveSession(token, fresh)
            setAuthState(fresh)
        } catch (e: ApiException) {
            // Token rejected — try the stored refresh token before signing out.
            // Otherwise the short-lived access token would sign the user out every hour.
            if (e.statusCode == 401) {
                if (tryRefreshAccessToken(cached)) return
                tokenStore.clear()
                _authState.value = AuthState.Unauthenticated
            } else {
                setAuthState(cached)
            }
        } catch (_: NetworkException) {
            // Offline → keep the cached session (Apple-specific revalidation is not in the MVP).
            setAuthState(cached)
        }
    }

    private suspend fun tryRefreshAccessToken(cached: User): Boolean {
        val refreshToken = tokenStore.getRefreshToken() ?: return false
        return try {
            val resp = apiCall(json) { authApi.refreshToken(RefreshTokenRequest(refreshToken = refreshToken)) }
            val newAccess = resp.accessToken ?: resp.token
            tokenStore.saveAccessToken(newAccess)
            resp.refreshToken?.let(tokenStore::saveRefreshToken)
            val fresh = try {
                apiCall(json) { userApi.getUser(cached.userId) }.copy(authProvider = cached.authProvider)
            } catch (_: Exception) {
                cached
            }
            tokenStore.saveSession(newAccess, fresh)
            setAuthState(fresh)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signInWithGoogle(idToken: String, email: String?, displayName: String?): AuthResult = try {
        val cleanName = displayName?.let { TextSanitizer.cleanName(it).trim() }?.ifEmpty { null }
        val resp = apiCall(json) {
            authApi.signIn(SignInRequest(provider = "google", email = email, displayName = cleanName, idToken = idToken))
        }
        val user = resp.user.copy(authProvider = "google")
        tokenStore.saveSession(resp.accessToken ?: resp.token, user, resp.refreshToken)
        _authState.value = AuthState.Authenticated(user) // Google emails are always verified
        AuthResult.Success()
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Google sign-in failed")
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult = try {
        val clean = email.trim().lowercase()
        val resp = apiCall(json) {
            authApi.signIn(SignInRequest(provider = "email", email = clean, password = password))
        }
        if (resp.isNewUser == true) {
            AuthResult.Error("Account not found. Please sign up first.", accountNotFound = true)
        } else {
            val user = resp.user.copy(authProvider = "email")
            tokenStore.saveSession(resp.accessToken ?: resp.token, user, resp.refreshToken)
            setAuthState(user)
            AuthResult.Success()
        }
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Sign-in failed")
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String?): AuthResult = try {
        val clean = email.trim().lowercase()
        val cleanName = displayName?.let { TextSanitizer.cleanName(it).trim() }?.ifEmpty { null }
        val resp = apiCall(json) {
            authApi.signIn(
                SignInRequest(
                    provider = "email",
                    email = clean,
                    displayName = cleanName,
                    password = password,
                    isSignup = true,
                ),
            )
        }
        val user = resp.user.copy(authProvider = "email")
        tokenStore.saveSession(resp.accessToken ?: resp.token, user, resp.refreshToken)
        setAuthState(user)
        AuthResult.Success()
    } catch (e: ApiException) {
        if (e.statusCode == 409) AuthResult.Error(e.message, accountExists = true) else AuthResult.Error(e.message)
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Sign-up failed")
    }

    suspend fun resendVerificationEmail(): AuthResult {
        val email = currentUser?.email ?: return AuthResult.Error("No email address found")
        return try {
            val r = apiCall(json) { authApi.resendVerification(ResendVerificationRequest(email)) }
            AuthResult.Success(r.message.ifEmpty { "Verification email sent" })
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not resend verification email")
        }
    }

    suspend fun refreshVerificationStatus() {
        val token = tokenStore.getToken() ?: return
        val user = currentUser ?: return
        val provider = user.authProvider
        val updated = try {
            apiCall(json) { authApi.verificationStatus() }.copy(authProvider = provider)
        } catch (_: Exception) {
            try {
                apiCall(json) { userApi.getUser(user.userId) }.copy(authProvider = provider)
            } catch (_: Exception) {
                null
            }
        } ?: return
        tokenStore.saveSession(token, updated)
        setAuthState(updated)
    }

    suspend fun forgotPassword(email: String): AuthResult = try {
        val r = apiCall(json) { authApi.forgotPassword(ForgotPasswordRequest(email.trim().lowercase())) }
        AuthResult.Success(r.message.ifEmpty { "Password reset email sent" })
    } catch (e: ApiException) {
        AuthResult.Error(e.message)
    } catch (_: NetworkException) {
        AuthResult.Error("Network error. Please try again.")
    } catch (_: Exception) {
        // Unexpected response shape — the email likely still went out (mirrors iOS).
        AuthResult.Success("Password reset email sent")
    }

    suspend fun resetPassword(token: String, newPassword: String): AuthResult = try {
        val r = apiCall(json) { authApi.resetPassword(ResetPasswordRequest(token, newPassword)) }
        AuthResult.Success(r.message.ifEmpty { "Password reset" })
    } catch (e: Exception) {
        AuthResult.Error(e.message ?: "Could not reset password")
    }

    suspend fun completeOnboarding(request: OnboardingRequest): AuthResult {
        val user = currentUser ?: return AuthResult.Error("Not signed in")
        return try {
            val resp = apiCall(json) { userApi.completeOnboarding(user.userId, request) }
            updateUserAfterOnboarding(resp.user)
            AuthResult.Success()
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not complete onboarding")
        }
    }

    /** Called after onboarding succeeds; forces onboardingCompleted and authenticates. */
    fun updateUserAfterOnboarding(user: User) {
        val token = tokenStore.getToken() ?: return
        val updated = user.copy(
            onboardingCompleted = true,
            authProvider = currentUser?.authProvider ?: user.authProvider,
        )
        tokenStore.saveSession(token, updated)
        _authState.value = AuthState.Authenticated(updated)
    }

    fun signOut() {
        tokenStore.clear()
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Deletes the account server-side, then clears the local session regardless of the result —
     * we never want stale credentials to live on the device after the user asks to delete.
     */
    suspend fun deleteAccount(): AuthResult {
        val user = currentUser ?: return AuthResult.Error("Not signed in")
        return try {
            val resp = userApi.deleteAccount(user.userId)
            // Clear local state regardless of HTTP code — iOS does the same.
            signOut()
            if (resp.isSuccessful) {
                AuthResult.Success()
            } else {
                AuthResult.Error("Account delete returned ${resp.code()}")
            }
        } catch (e: Exception) {
            signOut()
            AuthResult.Error(e.message ?: "Couldn't delete the account")
        }
    }

    private fun setAuthState(user: User) {
        _authState.value = if (user.authProvider == "email" && !user.emailVerified) {
            AuthState.PendingVerification(user)
        } else {
            AuthState.Authenticated(user)
        }
    }
}
