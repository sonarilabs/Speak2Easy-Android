package com.sonari.speak2easy.data.feedback

import android.content.Context
import android.os.Build
import com.sonari.speak2easy.data.remote.FeedbackApi
import com.sonari.speak2easy.data.remote.apiCall
import com.sonari.speak2easy.data.remote.dto.FeedbackRequest
import com.sonari.speak2easy.data.remote.dto.FeedbackResponse
import kotlinx.serialization.json.Json
import java.util.Locale

/**
 * Submits bug / feature / general feedback to the backend. Auth header is added automatically by
 * `AuthInterceptor` — caller just needs to pass the correct `userId` from `AuthRepository`.
 *
 * Also exposes [collectDeviceInfo] + [appVersion] helpers so the screen doesn't have to know
 * about Build internals or PackageManager. Mirrors the metadata iOS sends from FeedbackView.
 */
class FeedbackRepository(
    private val api: FeedbackApi,
    private val json: Json,
    private val context: Context,
) {

    suspend fun submit(userId: String, request: FeedbackRequest): FeedbackResponse =
        apiCall(json) { api.sendFeedback(userId, request) }

    /** Pipe-separated key/value device summary, capped at 500 chars to satisfy the backend schema. */
    fun collectDeviceInfo(): String {
        val parts = listOf(
            "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
            "Android" to "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Locale" to Locale.getDefault().toLanguageTag(),
        )
        val joined = parts.joinToString(" | ") { (k, v) -> "$k: $v" }
        return if (joined.length > 500) joined.take(497) + "..." else joined
    }

    /** "1.0 (1)" — combined `versionName (versionCode)`, capped at the backend's 20-char limit. */
    fun appVersion(): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        val raw = "${info.versionName ?: "?"} ($code)"
        if (raw.length > 20) raw.take(20) else raw
    }.getOrDefault("?")
}
