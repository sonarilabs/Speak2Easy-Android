package com.sonari.speak2easy.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Speaks Japanese text via the on-device TTS engine. Replaces iOS's reliance on the
 * unofficial Google-Translate-TTS endpoint with a robust, offline-capable engine.
 */
class TtsPlayer(context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.JAPANESE
                ready = true
            }
        }
    }

    /** Speaks [text], invoking [onDone] on the main thread when finished (or immediately if unavailable). */
    fun speak(text: String, onDone: () -> Unit = {}) {
        val engine = tts
        if (engine == null || !ready) {
            onDone()
            return
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                mainHandler.post(onDone)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post(onDone)
            }
        })
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private companion object {
        const val UTTERANCE_ID = "sonari-tts"
    }
}
