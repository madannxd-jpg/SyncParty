package com.syncparty.app.player

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileInputStream

class VoiceRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingStartTime: Long = 0L

    fun startRecording(): Boolean {
        return try {
            val audioDir = File(context.cacheDir, "voice_notes").apply { mkdirs() }
            val file = File(audioDir, "vn_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Failed to start recording", e)
            false
        }
    }

    fun stopRecording(): Pair<String?, Int> {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            val durationSec = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt().coerceAtLeast(1)
            val file = currentRecordingFile
            if (file != null && file.exists()) {
                val bytes = FileInputStream(file).use { it.readBytes() }
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Pair(base64, durationSec)
            } else {
                Pair(null, 0)
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Failed to stop recording", e)
            Pair(null, 0)
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {}
        mediaRecorder = null
        currentRecordingFile?.delete()
    }

    fun playVoiceNote(base64Audio: String, onCompletion: () -> Unit = {}) {
        try {
            mediaPlayer?.release()
            val bytes = Base64.decode(base64Audio, Base64.NO_WRAP)
            val tempFile = File.createTempFile("play_vn", ".m4a", context.cacheDir)
            tempFile.writeBytes(bytes)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                setOnCompletionListener {
                    onCompletion()
                    tempFile.delete()
                }
                start()
            }
        } catch (e: Exception) {
            Log.e("VoiceRecorderHelper", "Failed to play voice note", e)
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
    }
}
