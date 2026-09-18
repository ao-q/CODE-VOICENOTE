package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AudioRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var outputFile: File? = null
    private var startTimestampMs: Long = 0L
    private var accumulatedDurationMs: Long = 0L
    private var isPausedInternal: Boolean = false

    companion object {
        const val CHANNEL_ID = "voice_notes_recording"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.voicenotes.action.START"
        const val ACTION_PAUSE = "com.example.voicenotes.action.PAUSE"
        const val ACTION_RESUME = "com.example.voicenotes.action.RESUME"
        const val ACTION_STOP = "com.example.voicenotes.action.STOP"

        const val EXTRA_OUTPUT_PATH = "extra_output_path"

        fun startRecording(context: Context, outputPath: String) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_OUTPUT_PATH, outputPath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, AudioRecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VoiceNotes::RecordingWakeLock")
        wakeLock?.acquire(120 * 60 * 1000L) // 2 hours max safe timeout
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val path = intent.getStringExtra(EXTRA_OUTPUT_PATH)
                if (path != null) {
                    startRecordingInternal(File(path))
                }
            }
            ACTION_PAUSE -> pauseRecordingInternal()
            ACTION_RESUME -> resumeRecordingInternal()
            ACTION_STOP -> stopRecordingInternal()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Notes Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing voice note recording status in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(elapsedFormatted: String, isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "Paused • $elapsedFormatted" else "Recording • $elapsedFormatted (High Clarity)"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Notes")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun startRecordingInternal(file: File) {
        outputFile = file
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // Highest possible clarity / quality settings
            recorder.setAudioEncodingBitRate(256000) // 256 kbps
            recorder.setAudioSamplingRate(48000) // 48 kHz
            try {
                recorder.setAudioChannels(2) // Stereo
            } catch (_: Exception) {
                recorder.setAudioChannels(1) // Fallback to mono if device doesn't support stereo mic
            }
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()

            mediaRecorder = recorder
            startTimestampMs = System.currentTimeMillis()
            accumulatedDurationMs = 0L
            isPausedInternal = false

            val initialNotification = buildNotification("00:00", false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }

            RecordingStateManager.updateState {
                it.copy(
                    isRecording = true,
                    isPaused = false,
                    elapsedSeconds = 0L,
                    elapsedFormatted = "00:00",
                    outputFile = file,
                    amplitudeHistory = emptyList()
                )
            }

            startTimer()
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecordingInternal()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var lastSecUpdated = -1L
            val history = mutableListOf<Float>()

            while (isActive && mediaRecorder != null) {
                delay(100)
                if (!isPausedInternal) {
                    val currentElapsedMs = accumulatedDurationMs + (System.currentTimeMillis() - startTimestampMs)
                    val totalSec = currentElapsedMs / 1000
                    val mins = totalSec / 60
                    val secs = totalSec % 60
                    val formatted = String.format("%02d:%02d", mins, secs)

                    // Get amplitude for soundwave visualizer
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    val normalized = (maxAmp / 32767f).coerceIn(0.05f, 1.0f)
                    history.add(normalized)
                    if (history.size > 45) {
                        history.removeAt(0)
                    }

                    RecordingStateManager.updateState {
                        it.copy(
                            elapsedSeconds = totalSec,
                            elapsedFormatted = formatted,
                            currentAmplitude = maxAmp,
                            amplitudeHistory = history.toList()
                        )
                    }

                    if (totalSec != lastSecUpdated) {
                        lastSecUpdated = totalSec
                        val notificationManager = getSystemService(NotificationManager::class.java)
                        notificationManager.notify(NOTIFICATION_ID, buildNotification(formatted, false))
                    }
                }
            }
        }
    }

    private fun pauseRecordingInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isPausedInternal) {
            try {
                mediaRecorder?.pause()
                accumulatedDurationMs += (System.currentTimeMillis() - startTimestampMs)
                isPausedInternal = true
                RecordingStateManager.updateState { it.copy(isPaused = true) }

                val currentSec = accumulatedDurationMs / 1000
                val formatted = String.format("%02d:%02d", currentSec / 60, currentSec % 60)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(formatted, true))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resumeRecordingInternal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPausedInternal) {
            try {
                mediaRecorder?.resume()
                startTimestampMs = System.currentTimeMillis()
                isPausedInternal = false
                RecordingStateManager.updateState { it.copy(isPaused = false) }

                val currentSec = accumulatedDurationMs / 1000
                val formatted = String.format("%02d:%02d", currentSec / 60, currentSec % 60)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, buildNotification(formatted, false))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopRecordingInternal() {
        timerJob?.cancel()
        timerJob = null

        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        RecordingStateManager.updateState {
            it.copy(isRecording = false, isPaused = false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }
}
