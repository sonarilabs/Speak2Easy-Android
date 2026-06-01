package com.sonari.speak2easy.ui.feedback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Converts user-picked image Uris to base64 JPEG strings that fit under the backend's
 * 2,000,000-char-per-image limit (≈1.5 MB of binary payload).
 *
 * Strategy:
 * 1. Decode via ImageDecoder (minSdk 30 ⇒ always available); ask it to downscale the longest
 *    edge to [MAX_DIMENSION] in a single pass. EXIF orientation is applied automatically.
 * 2. Try JPEG quality 80; step down the [QUALITY_LADDER] until the base64 string fits, or give up.
 *
 * Returns `null` if decoding fails or the image is still too large at the lowest quality.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1080
    private const val MAX_BASE64_CHARS = 1_900_000  // leave headroom under the 2,000,000 backend limit
    private val QUALITY_LADDER = intArrayOf(80, 70, 60, 50, 40)

    suspend fun toBase64Jpeg(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(context, uri) ?: return@withContext null
            try {
                for (quality in QUALITY_LADDER) {
                    val bytes = ByteArrayOutputStream().use { os ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, os)
                        os.toByteArray()
                    }
                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    if (encoded.length <= MAX_BASE64_CHARS) return@withContext encoded
                }
                null
            } finally {
                bitmap.recycle()
            }
        }.getOrNull()
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return runCatching {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.isMutableRequired = true
                val (w, h) = info.size.width to info.size.height
                val longest = maxOf(w, h)
                if (longest > MAX_DIMENSION) {
                    val scale = MAX_DIMENSION.toFloat() / longest
                    decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
                }
            }
        }.getOrNull()
    }
}
