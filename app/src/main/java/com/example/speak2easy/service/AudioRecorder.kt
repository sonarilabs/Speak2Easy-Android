package com.example.speak2easy.service

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records mic audio to an MPEG-4/AAC `.m4a` file (the format the backend expects).
 * Mirrors the iOS AVAudioRecorder usage in PracticeView.
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
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
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
