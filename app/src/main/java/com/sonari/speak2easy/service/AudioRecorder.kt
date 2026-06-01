package com.sonari.speak2easy.service

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.math.max

/**
 * Records uncompressed mono PCM into a WAV container.
 *
 * The pronunciation backend accepts audio/wav and forwards the bytes to Whisper. Using
 * AudioRecord avoids AAC artifacts and the call-mode DSP from VOICE_COMMUNICATION, both of
 * which can hide short pronunciation details in kana practice.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: AudioRecord? = null
    private var outputFile: File? = null
    private var recordingThread: Thread? = null
    @Volatile private var recording = false
    @Volatile private var dataBytes = 0L

    /** Begins recording. Throws if the mic is unavailable or permission is missing. */
    fun start() {
        cancel()

        val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.wav")
        val (audioRecord, bufferSize) = createAudioRecord()

        outputFile = file
        recorder = audioRecord
        dataBytes = 0L
        recording = true

        FileOutputStream(file).use { stream -> writeWavHeader(stream, 0L) }

        try {
            audioRecord.startRecording()
        } catch (e: Exception) {
            audioRecord.release()
            file.delete()
            recorder = null
            outputFile = null
            recording = false
            throw e
        }

        recordingThread = Thread({
            val buffer = ByteArray(bufferSize)
            FileOutputStream(file, true).use { stream ->
                while (recording) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        stream.write(buffer, 0, read)
                        dataBytes += read.toLong()
                    }
                }
                stream.flush()
            }
        }, "sonari-audio-record").also { it.start() }
    }

    /** Stops recording and returns the WAV file, or null if nothing usable was captured. */
    fun stop(): File? {
        val file = outputFile
        recording = false
        stopRecorder()
        recordingThread?.joinQuietly()
        recorder?.release()
        recorder = null
        recordingThread = null
        outputFile = null

        return if (file != null && dataBytes > MIN_AUDIO_BYTES) {
            patchWavHeader(file, dataBytes)
            file
        } else {
            file?.delete()
            null
        }
    }

    /** Aborts recording and discards the file. */
    fun cancel() {
        recording = false
        stopRecorder()
        recordingThread?.joinQuietly()
        recorder?.release()
        recorder = null
        recordingThread = null
        outputFile?.delete()
        outputFile = null
        dataBytes = 0L
    }

    @SuppressLint("MissingPermission")
    private fun createAudioRecord(): Pair<AudioRecord, Int> {
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) error("PCM recording is not supported on this device.")

        val bufferSize = max(minBuffer, SAMPLE_RATE * BYTES_PER_SAMPLE / 5)
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        for (source in AUDIO_SOURCE_FALLBACKS) {
            val record = runCatching {
                AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            }.getOrNull()

            if (record?.state == AudioRecord.STATE_INITIALIZED) {
                return record to bufferSize
            }
            record?.release()
        }

        error("Couldn't initialize microphone recording.")
    }

    private fun stopRecorder() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // ignore - may not have started or may already be stopped
        }
    }

    private fun writeWavHeader(stream: FileOutputStream, audioDataBytes: Long) {
        val byteRate = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE
        val totalDataLen = audioDataBytes + 36
        stream.writeAscii("RIFF")
        stream.writeIntLe(totalDataLen.toInt())
        stream.writeAscii("WAVE")
        stream.writeAscii("fmt ")
        stream.writeIntLe(16)
        stream.writeShortLe(1)
        stream.writeShortLe(CHANNELS)
        stream.writeIntLe(SAMPLE_RATE)
        stream.writeIntLe(byteRate)
        stream.writeShortLe(CHANNELS * BYTES_PER_SAMPLE)
        stream.writeShortLe(BITS_PER_SAMPLE)
        stream.writeAscii("data")
        stream.writeIntLe(audioDataBytes.toInt())
    }

    private fun patchWavHeader(file: File, audioDataBytes: Long) {
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(4)
            raf.writeIntLe((audioDataBytes + 36).toInt())
            raf.seek(40)
            raf.writeIntLe(audioDataBytes.toInt())
        }
    }

    private fun Thread?.joinQuietly() {
        try {
            this?.join(1_000)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun FileOutputStream.writeAscii(value: String) {
        write(value.toByteArray(Charsets.US_ASCII))
    }

    private fun FileOutputStream.writeIntLe(value: Int) {
        write(byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte(),
        ))
    }

    private fun FileOutputStream.writeShortLe(value: Int) {
        write(byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
        ))
    }

    private fun RandomAccessFile.writeIntLe(value: Int) {
        write(byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte(),
        ))
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
        const val BYTES_PER_SAMPLE = BITS_PER_SAMPLE / 8
        const val MIN_AUDIO_BYTES = SAMPLE_RATE * BYTES_PER_SAMPLE / 10

        val AUDIO_SOURCE_FALLBACKS = intArrayOf(
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.MIC,
        )
    }
}
