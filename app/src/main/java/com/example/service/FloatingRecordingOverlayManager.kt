package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Manages the floating overlay window that displays recording controls
 * (timer, status, pause/resume, flag timestamp, stop & save, open app)
 * when the user leaves or exits the app during active recording.
 */
object FloatingRecordingOverlayManager {

    const val ACTION_STOP_AND_SAVE = "com.example.voicenotes.action.STOP_AND_SAVE"

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var stateJob: Job? = null
    private var isShowing = false

    /**
     * Checks if the app has permission to draw overlays on top of other apps.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Opens system settings to request the display over other apps permission.
     */
    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Shows the floating recording controls overlay window.
     */
    fun show(context: Context) {
        val appContext = context.applicationContext
        if (!canDrawOverlays(appContext)) {
            return
        }

        val sessionState = RecordingStateManager.state.value
        if (!sessionState.isRecording) {
            return
        }

        if (isShowing && overlayView != null) {
            return
        }

        try {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 36
                y = 180
            }

            val inflater = LayoutInflater.from(appContext)
            val view = inflater.inflate(R.layout.layout_floating_recording_overlay, null)
            overlayView = view

            val statusDot = view.findViewById<ImageView>(R.id.float_status_dot)
            val timerText = view.findViewById<TextView>(R.id.float_timer_text)
            val btnFlag = view.findViewById<ImageButton>(R.id.float_btn_flag)
            val btnPauseResume = view.findViewById<ImageButton>(R.id.float_btn_pause_resume)
            val btnStop = view.findViewById<ImageButton>(R.id.float_btn_stop)
            val btnOpen = view.findViewById<ImageButton>(R.id.float_btn_open)

            // Setup Draggable behavior on root view
            val touchSlop = ViewConfiguration.get(appContext).scaledTouchSlop
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isDragging = false

            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                            isDragging = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(overlayView, params)
                            } catch (_: Exception) {}
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Single tap on container opens app
                            openApp(appContext)
                        }
                        true
                    }
                    else -> false
                }
            }

            // Flag Timestamp
            btnFlag.setOnClickListener {
                vibrate(appContext)
                RecordingStateManager.addTimestamp()
            }

            // Pause or Resume
            btnPauseResume.setOnClickListener {
                vibrate(appContext)
                val currentState = RecordingStateManager.state.value
                if (currentState.isPaused) {
                    AudioRecordingService.resumeRecording(appContext)
                } else {
                    AudioRecordingService.pauseRecording(appContext)
                }
            }

            // Stop & Save
            btnStop.setOnClickListener {
                vibrate(appContext)
                hide()
                openApp(appContext, ACTION_STOP_AND_SAVE)
            }

            // Open Full App
            btnOpen.setOnClickListener {
                vibrate(appContext)
                hide()
                openApp(appContext)
            }

            wm.addView(view, params)
            isShowing = true

            // Live state updates for timer and pause/resume button
            stateJob?.cancel()
            stateJob = CoroutineScope(Dispatchers.Main).launch {
                RecordingStateManager.state.collectLatest { state ->
                    if (!state.isRecording) {
                        hide()
                        return@collectLatest
                    }

                    timerText.text = state.elapsedFormatted

                    if (state.isPaused) {
                        statusDot.setImageResource(R.drawable.ic_float_dot_paused)
                        btnPauseResume.setImageResource(R.drawable.ic_float_play)
                        btnPauseResume.contentDescription = "Resume Recording"
                    } else {
                        statusDot.setImageResource(R.drawable.ic_float_dot_recording)
                        btnPauseResume.setImageResource(R.drawable.ic_float_pause)
                        btnPauseResume.contentDescription = "Pause Recording"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isShowing = false
            overlayView = null
        }
    }

    /**
     * Hides and cleans up the floating overlay window.
     */
    fun hide() {
        stateJob?.cancel()
        stateJob = null

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
        isShowing = false
    }

    private fun openApp(context: Context, action: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (action != null) {
                this.action = action
            }
        }
        context.startActivity(intent)
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
            }
        } catch (_: Exception) {}
    }
}
