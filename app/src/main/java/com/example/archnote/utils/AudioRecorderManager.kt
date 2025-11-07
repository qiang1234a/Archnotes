package com.example.archnote.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private var startTime: Long = 0
    private var totalDuration: Long = 0

    fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startRecording(): String? {
        if (!hasRecordPermission()) {
            return null
        }

        if (isRecording) {
            return null
        }

        try {
            // 创建录音文件
            val audioDir = File(context.getExternalFilesDir(null), "recordings")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFile = File(audioDir, "recording_$timestamp.m4a")

            // 初始化 MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            startTime = System.currentTimeMillis()
            return outputFile?.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            releaseRecorder()
            return null
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            releaseRecorder()
            return null
        }
    }

    fun stopRecording(): Long {
        if (!isRecording) {
            return 0
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRecording = false
            val duration = System.currentTimeMillis() - startTime
            totalDuration = duration
            releaseRecorder()
        }

        return totalDuration
    }

    fun cancelRecording() {
        if (isRecording) {
            stopRecording()
            outputFile?.delete()
            outputFile = null
        }
    }

    fun getCurrentOutputFile(): File? {
        return outputFile
    }

    fun getRecordingDuration(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - startTime
        } else {
            totalDuration
        }
    }

    fun isRecording(): Boolean {
        return isRecording
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
    }

    fun release() {
        if (isRecording) {
            stopRecording()
        }
        releaseRecorder()
    }
}

