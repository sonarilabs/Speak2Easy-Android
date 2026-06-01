package com.sonari.speak2easy.data.remote

import android.util.Log
import com.sonari.speak2easy.data.auth.TokenStore
import com.sonari.speak2easy.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Catches 401 responses, refreshes the access token via [AuthApi.refreshToken], and replays the
 * original request with the new Bearer token. OkHttp invokes this automatically on any 401
 * response — works across every Retrofit interface and survives token expiry without forcing
 * the user to sign in again.
 *
 * Three correctness details:
 * 1. The refresh endpoint itself must never trigger this (`/auth/refresh` 401 → give up).
 * 2. If two concurrent requests both get 401, only one refresh fires; the other re-reads the
 *    fresh token under the mutex and retries with it.
 * 3. We pass [authApi] as a lazy supplier so the construction order (Retrofit → AuthApi →
 *    Authenticator) can be built without a true circular dependency.
 */
class TokenAuthenticator(
    private val tokenStore: TokenStore,
    private val authApi: () -> AuthApi,
    private val onRefreshFailed: () -> Unit = {},
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Endpoint path is /api/v1/auth/token/refresh — don't recurse on its own 401.
        if (response.request.url.encodedPath.contains("/auth/token/refresh")) return null

        val staleAuth = response.request.header("Authorization") ?: return null
        val staleToken = staleAuth.removePrefix("Bearer ").trim().ifEmpty { return null }

        return runBlocking {
            mutex.withLock {
                val current = tokenStore.getToken()
                // Another concurrent 401 may have already refreshed by the time we got the lock.
                if (current != null && current != staleToken) {
                    return@withLock retryWith(response.request, current)
                }

                val refreshToken = tokenStore.getRefreshToken()
                if (refreshToken.isNullOrEmpty()) {
                    Log.w(TAG, "401 with no refresh token — giving up")
                    return@withLock null
                }

                val newToken = try {
                    val resp = authApi().refreshToken(RefreshTokenRequest(refreshToken = refreshToken))
                    val access = resp.accessToken ?: resp.token
                    tokenStore.saveAccessToken(access)
                    resp.refreshToken?.let(tokenStore::saveRefreshToken)
                    Log.d(TAG, "Refreshed access token after 401")
                    access
                } catch (e: Exception) {
                    Log.w(TAG, "Refresh failed: ${e.message}")
                    null
                }

                if (newToken == null) {
                    onRefreshFailed()
                    null
                } else {
                    retryWith(response.request, newToken)
                }
            }
        }
    }

    private fun retryWith(request: Request, token: String): Request =
        request.newBuilder().header("Authorization", "Bearer $token").build()

    private companion object {
        const val TAG = "TokenAuthenticator"
    }
}
