package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Outline
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.MainActivity
import com.example.R

/**
 * Foreground Service that manages the system-wide Floating FaceCam overlay window.
 * Keeps camera access alive in background and outside the app with FOREGROUND_SERVICE_TYPE_CAMERA.
 */
class FloatingFaceCamService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private var currentShape = SystemFaceCamShape.CIRCLE
    private var currentSizeDp = 150
    private var isFrontCamera = true

    companion object {
        const val CHANNEL_ID = "facecam_overlay_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START = "com.example.voicenotes.action.START_FACECAM"
        const val ACTION_STOP = "com.example.voicenotes.action.STOP_FACECAM"

        fun start(context: Context) {
            val intent = Intent(context, FloatingFaceCamService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingFaceCamService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for devices with strict background startup
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        if (overlayView == null && Settings.canDrawOverlays(this)) {
            initOverlay()
        }

        FloatingFaceCamOverlayManager.setShowing(true)

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FaceCam Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of floating FaceCam overlay outside the app"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 1, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val closeIntent = Intent(this, FloatingFaceCamService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingClose = PendingIntent.getService(
            this, 2, closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FaceCam Active")
            .setContentText("Camera is floating over apps • Tap to open Voice Notes")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpen)
            .addAction(R.drawable.ic_facecam_close, "Close FaceCam", pendingClose)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun initOverlay() {
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val sizePx = (currentSizeDp * density).toInt()

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
                x = (resources.displayMetrics.widthPixels - sizePx - (16 * density).toInt()).coerceAtLeast(30)
                y = (120 * density).toInt()
            }

            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.layout_floating_facecam_overlay, null)
            overlayView = view

            val cameraContainer = view.findViewById<ViewGroup>(R.id.facecam_camera_container)
            val pView = view.findViewById<PreviewView>(R.id.facecam_preview_view)
            previewView = pView

            val controlsPanel = view.findViewById<LinearLayout>(R.id.facecam_controls_panel)
            val btnClose = view.findViewById<ImageButton>(R.id.facecam_btn_close)
            val btnFlip = view.findViewById<ImageButton>(R.id.facecam_btn_flip)
            val btnShape = view.findViewById<ImageButton>(R.id.facecam_btn_shape)
            val btnSize = view.findViewById<ImageButton>(R.id.facecam_btn_size)

            applyShapeAndSize(cameraContainer, currentShape, currentSizeDp)

            // Setup Draggable physics
            val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
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
                            val currentPx = (currentSizeDp * resources.displayMetrics.density).toInt()
                            val maxX = (resources.displayMetrics.widthPixels - currentPx).coerceAtLeast(0)
                            val maxY = (resources.displayMetrics.heightPixels - currentPx).coerceAtLeast(0)
                            params.x = (initialX + dx).coerceIn(0, maxX)
                            params.y = (initialY + dy).coerceIn(0, maxY)
                            try {
                                windowManager?.updateViewLayout(overlayView, params)
                            } catch (_: Exception) {}
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Toggle controls on single tap
                            controlsPanel.visibility = if (controlsPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                        }
                        true
                    }
                    else -> false
                }
            }

            // Controls listeners
            btnClose.setOnClickListener {
                vibrate()
                stopSelf()
            }

            btnFlip.setOnClickListener {
                vibrate()
                isFrontCamera = !isFrontCamera
                bindCamera(pView)
            }

            btnShape.setOnClickListener {
                vibrate()
                currentShape = when (currentShape) {
                    SystemFaceCamShape.CIRCLE -> SystemFaceCamShape.ROUNDED_RECT
                    SystemFaceCamShape.ROUNDED_RECT -> SystemFaceCamShape.SQUIRCLE
                    SystemFaceCamShape.SQUIRCLE -> SystemFaceCamShape.RECTANGLE
                    SystemFaceCamShape.RECTANGLE -> SystemFaceCamShape.CIRCLE
                }
                applyShapeAndSize(cameraContainer, currentShape, currentSizeDp)
            }

            btnSize.setOnClickListener {
                vibrate()
                currentSizeDp = when (currentSizeDp) {
                    110 -> 150
                    150 -> 200
                    200 -> 110
                    else -> 150
                }
                applyShapeAndSize(cameraContainer, currentShape, currentSizeDp)
                try {
                    windowManager?.updateViewLayout(overlayView, params)
                } catch (_: Exception) {}
            }

            wm.addView(view, params)

            // Start Camera
            bindCamera(pView)

        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
        }
    }

    private fun bindCamera(pView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val provider = cameraProvider ?: return@addListener
                val availableCameras = provider.availableCameraInfos

                val hasBack = try { provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) } catch (_: Exception) { false }
                val hasFront = try { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) } catch (_: Exception) { false }

                val selector = when {
                    !isFrontCamera && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                    isFrontCamera && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                    !isFrontCamera && availableCameras.size > 1 -> {
                        CameraSelector.Builder().addCameraFilter { list ->
                            val nonFront = list.filter { it.lensFacing != CameraSelector.LENS_FACING_FRONT }
                            if (nonFront.isNotEmpty()) nonFront else list.takeLast(1)
                        }.build()
                    }
                    isFrontCamera && availableCameras.size > 1 -> {
                        CameraSelector.Builder().addCameraFilter { list ->
                            val front = list.filter { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                            if (front.isNotEmpty()) front else list.take(1)
                        }.build()
                    }
                    hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                    hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> CameraSelector.DEFAULT_BACK_CAMERA
                }

                provider.unbindAll()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = pView.surfaceProvider
                }
                provider.bindToLifecycle(
                    this,
                    selector,
                    preview
                )

                if (availableCameras.size <= 1) {
                    Toast.makeText(this, "1 camera detected on device", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, if (isFrontCamera) "Front Camera" else "Back Camera", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun applyShapeAndSize(container: ViewGroup, shape: SystemFaceCamShape, sizeDp: Int) {
        val density = resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()

        val lp = container.layoutParams
        lp.width = sizePx
        lp.height = sizePx
        container.layoutParams = lp

        container.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val w = view.width
                val h = view.height
                when (shape) {
                    SystemFaceCamShape.CIRCLE -> {
                        outline.setOval(0, 0, w, h)
                    }
                    SystemFaceCamShape.ROUNDED_RECT -> {
                        outline.setRoundRect(0, 0, w, h, 20 * density)
                    }
                    SystemFaceCamShape.SQUIRCLE -> {
                        outline.setRoundRect(0, 0, w, h, 36 * density)
                    }
                    SystemFaceCamShape.RECTANGLE -> {
                        outline.setRoundRect(0, 0, w, h, 10 * density)
                    }
                }
            }
        }
        container.clipToOutline = true
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraProvider?.unbindAll()
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        } catch (_: Exception) {}

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
        previewView = null
        FloatingFaceCamOverlayManager.setShowing(false)
    }
}
