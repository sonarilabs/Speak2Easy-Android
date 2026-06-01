package com.sonari.speak2easy.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records mic audio to an MPEG-4/AAC `.m4a` file (the format the backend expects).
 * Mirrors the iOS AVAudioRecorder usage in PracticeView.
 *
 * Uses [MediaRecorder.AudioSource.VOICE_COMMUNICATION] to match iOS's
 * `AVAudioSession.Mode.voiceChat`: the platform applies AGC, noise suppression, and
 * acoustic echo cancellation server-side from the audio HAL when supported. This gives the
 * Japanese pronunciation model a cleaner signal (no fan/keyboard noise, no echo) on par with iOS.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /** Begins recording. Throws if the mic is unavailable or permission is missing. */
    fun start() {
        val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.m4a")
        outputFile = file
        @Suppress("DEPRECATION")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        rec.apply {
            // VOICE_COMMUNICATION enables platform AGC + NS + AEC (matches iOS .voiceChat).
            setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            // 44.1 kHz matches iOS exactly so the backend gets identical input across platforms;
            // platform DSP still works at this rate and just resamples internally.
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = rec
    }

    /** Stops recording and returns the file, or null if nothing usable was captured. */
    fun stop(): File? {
        val file = outputFile
        return try {
            recorder?.stop()
            recorder?.release()
            file
        } catch (_: Exception) {
            recorder?.runCatching { release() }
            file?.delete()
            null
        } finally {
            recorder = null
        }
    }

    /** Aborts recording and discards the file. */
    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // ignore — may not have started
        }
        recorder?.runCatching { release() }
        recorder = null
        outputFile?.delete()
        outputFile = null
    }
}
