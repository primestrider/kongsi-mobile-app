package com.primestrider.kotlinstarter.feature.example.sdk.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File

/**
 * One voice note at a time, AAC in MPEG-4, written to `cacheDir/voice/note.m4a` (each recording replaces the last).
 * Needs RECORD_AUDIO. Call [stop] (or [release]) before leaving: the recorder holds the microphone.
 */
class VoiceRecorder(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    val file: File get() = File(context.cacheDir, "voice/note.m4a")

    val isRecording: Boolean get() = recorder != null

    /** Starts recording; throws when the microphone cannot be opened (the recorder is released first). */
    fun start() {
        if (recorder != null) return
        file.parentFile?.mkdirs()
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else newLegacyRecorder()
        try {
            created.setAudioSource(MediaRecorder.AudioSource.MIC)
            created.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            created.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            created.setAudioEncodingBitRate(128_000)
            created.setAudioSamplingRate(44_100)
            created.setOutputFile(file.absolutePath)
            created.prepare()
            created.start()
        } catch (e: Exception) {
            created.release()
            throw e
        }
        recorder = created
        startedAt = SystemClock.elapsedRealtime()
    }

    /** Stops and returns the note; a recording too short to be valid is deleted, so the file may not exist. */
    fun stop(): File {
        val active = recorder ?: return file
        recorder = null
        try {
            active.stop()
        } catch (_: RuntimeException) {
            // stop() right after start() has no valid data.
            file.delete()
        } finally {
            active.release()
        }
        return file
    }

    /** Milliseconds since [start], or 0 when not recording. */
    fun elapsedMillis(): Long = if (recorder != null) SystemClock.elapsedRealtime() - startedAt else 0L

    fun release() {
        stop()
    }

    @Suppress("DEPRECATION")
    private fun newLegacyRecorder() = MediaRecorder()
}
