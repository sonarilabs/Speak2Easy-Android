package com.sonari.speak2easy.ui.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.sonari.speak2easy.data.remote.SonariJson
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class GoogleCredential(
    val idToken: String,
    val email: String?,
    val displayName: String?,
)

object GoogleAuthHelper {
    /**
     * Launches the Credential Manager Google chooser and returns the raw Google ID token
     * plus the email/display name. The backend verifies the ID token against Google's JWKS
     * and derives the stable `sub` — the client no longer sends provider_user_id directly.
     */
    suspend fun signIn(context: Context, webClientId: String): GoogleCredential {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(context).getCredential(context, request)
        val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
        return GoogleCredential(
            idToken = credential.idToken,
            email = credential.id,
            displayName = credential.displayName,
        )
    }
}
