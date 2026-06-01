package com.sonari.speak2easy.ui.feedback

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.auth.AuthRepository
import com.sonari.speak2easy.data.feedback.FeedbackRepository
import com.sonari.speak2easy.data.remote.dto.FeedbackRequest
import com.sonari.speak2easy.util.TextSanitizer
import kotlinx.coroutines.launch

/** Visible state of the bug-report screen. */
data class FeedbackUiState(
    val category: FeedbackCategory = FeedbackCategory.GENERAL,
    val message: String = "",
    /** Set true while we read+compress the picked images on a background thread. */
    val isProcessingImages: Boolean = false,
    val isSubmitting: Boolean = false,
    val sent: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Owns the feedback form state. Attached image previews live in [attachments] (URIs for display);
 * the compressed base64 payloads live in [encodedImages] and are sent on submit.
 */
class FeedbackViewModel(
    initialCategory: FeedbackCategory,
    private val currentScreen: String,
    private val feedbackRepo: FeedbackRepository,
    private val authRepo: AuthRepository,
    private val appContext: Context,
) : ViewModel() {

    var state by mutableStateOf(FeedbackUiState(category = initialCategory))
        private set

    /** Up to 3 picked image Uris — displayed as thumbnails in the screen's photo grid. */
    val attachments: SnapshotStateList<Uri> = emptyList<Uri>().toMutableStateList()

    /** Same indexing as [attachments]; null until the IO compress finishes for that slot. */
    private val encodedImages: SnapshotStateList<String?> = emptyList<String?>().toMutableStateList()

    fun setCategory(c: FeedbackCategory) {
        state = state.copy(category = c)
    }

    fun setMessage(text: String) {
        // Soft cap at MAX_MESSAGE_CHARS — the field-level enforcement also clamps, but defending
        // here guards against programmatic setMessage calls (e.g. tests).
        state = state.copy(message = TextSanitizer.cleanMultilineText(text, MAX_MESSAGE_CHARS))
    }

    /** Adds the picked Uris, ignoring any that would push past [MAX_IMAGES]. */
    fun addAttachments(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val capacity = MAX_IMAGES - attachments.size
        val toAdd = uris.take(capacity)
        if (toAdd.isEmpty()) return
        val startIndex = attachments.size
        attachments.addAll(toAdd)
        toAdd.forEach { encodedImages.add(null) }
        state = state.copy(isProcessingImages = true)
        viewModelScope.launch {
            toAdd.forEachIndexed { offset, uri ->
                val encoded = ImageCompressor.toBase64Jpeg(appContext, uri)
                val slot = startIndex + offset
                if (slot < encodedImages.size) encodedImages[slot] = encoded
            }
            state = state.copy(isProcessingImages = false)
        }
    }

    fun removeAttachment(index: Int) {
        if (index !in attachments.indices) return
        attachments.removeAt(index)
        if (index in encodedImages.indices) encodedImages.removeAt(index)
    }

    fun submit() {
        if (state.message.isBlank() || state.isSubmitting) return
        val userId = authRepo.currentUser?.userId
        if (userId.isNullOrEmpty()) {
            state = state.copy(errorMessage = "Sign in required to send feedback.")
            return
        }
        // Wait for image compression to finish before sending.
        if (state.isProcessingImages) {
            state = state.copy(errorMessage = "Still preparing attachments — try again in a moment.")
            return
        }
        val payloadImages = encodedImages.filterNotNull()
        val cleanMessage = TextSanitizer.cleanMultilineText(state.message, MAX_MESSAGE_CHARS).trim()
        val cleanScreen = TextSanitizer.cleanFreeText(currentScreen, 100)
        val request = FeedbackRequest(
            category = state.category.apiValue,
            message = cleanMessage,
            imagesBase64 = payloadImages,
            deviceInfo = feedbackRepo.collectDeviceInfo(),
            appVersion = feedbackRepo.appVersion(),
            currentScreen = cleanScreen,
        )
        state = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            runCatching { feedbackRepo.submit(userId, request) }
                .onSuccess { state = state.copy(isSubmitting = false, sent = true) }
                .onFailure { state = state.copy(isSubmitting = false, errorMessage = it.message ?: "Couldn't send feedback.") }
        }
    }

    fun clearError() { state = state.copy(errorMessage = null) }

    class Factory(
        private val initialCategory: FeedbackCategory,
        private val currentScreen: String,
        private val feedbackRepo: FeedbackRepository,
        private val authRepo: AuthRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FeedbackViewModel(initialCategory, currentScreen, feedbackRepo, authRepo, appContext) as T
    }

    companion object {
        const val MAX_MESSAGE_CHARS = 1000
        const val MAX_IMAGES = 3
    }
}
