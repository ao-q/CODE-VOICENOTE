package com.example.service

import android.content.Context
import android.graphics.Outline
import android.graphics.PixelFormat
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
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SystemFaceCamShape {
    CIRCLE,
    ROUNDED_RECT,
    SQUIRCLE,
    RECTANGLE
}

/**
 * System-Wide Floating FaceCam Overlay Manager.
 * Displays a live, draggable selfie/front camera video overlay on top of all Android apps
 * using WindowManager (TYPE_APPLICATION_OVERLAY).
 *
 * Stays active even when audio recording stops so that creators can record their screen
 * with an uninterrupted facecam stream (OBS / Streamlabs style).
 */
object FloatingFaceCamOverlayManager {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var previewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val _isShowingState = MutableStateFlow(false)
    val isShowingState: StateFlow<Boolean> = _isShowingState.asStateFlow()

    private var currentShape = SystemFaceCamShape.CIRCLE
    private var currentSizeDp = 150
    private var isFrontCamera = true

    // Custom LifecycleOwner for running CameraX in a System Overlay Window
    private val overlayLifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry

        fun markResumed() {
            try {
                registry.currentState = Lifecycle.State.CREATED
                registry.currentState = Lifecycle.State.STARTED
                registry.currentState = Lifecycle.State.RESUMED
            } catch (_: Exception) {}
        }

        fun markDestroyed() {
            try {
                registry.currentState = Lifecycle.State.DESTROYED
            } catch (_: Exception) {}
        }
    }

    fun isShowing(): Boolean = _isShowingState.value

    fun toggle(context: Context) {
        if (isShowing()) {
            hide()
        } else {
            show(context)
        }
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Shows the system-wide FaceCam floating window if overlay permission is present,
     * and always activates the in-app FaceCam state.
     */
    fun show(context: Context) {
        _isShowingState.value = true
        val appContext = context.applicationContext
        if (!canDrawOverlays(appContext)) {
            // Overlay permission is not granted, but in-app Compose FaceCamOverlay is active!
            return
        }

        if (overlayView != null) {
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

            val density = appContext.resources.displayMetrics.density
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
                x = (appContext.resources.displayMetrics.widthPixels - sizePx - (16 * density).toInt()).coerceAtLeast(30)
                y = (120 * density).toInt()
            }

            val inflater = LayoutInflater.from(appContext)
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

            applyShapeAndSize(appContext, cameraContainer, currentShape, currentSizeDp)

            // Setup Draggable physics
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
                vibrate(appContext)
                hide()
            }

            btnFlip.setOnClickListener {
                vibrate(appContext)
                isFrontCamera = !isFrontCamera
                bindCamera(appContext, pView)
            }

            btnShape.setOnClickListener {
                vibrate(appContext)
                currentShape = when (currentShape) {
                    SystemFaceCamShape.CIRCLE -> SystemFaceCamShape.ROUNDED_RECT
                    SystemFaceCamShape.ROUNDED_RECT -> SystemFaceCamShape.SQUIRCLE
                    SystemFaceCamShape.SQUIRCLE -> SystemFaceCamShape.RECTANGLE
                    SystemFaceCamShape.RECTANGLE -> SystemFaceCamShape.CIRCLE
                }
                applyShapeAndSize(appContext, cameraContainer, currentShape, currentSizeDp)
            }

            btnSize.setOnClickListener {
                vibrate(appContext)
                currentSizeDp = when (currentSizeDp) {
                    110 -> 150
                    150 -> 200
                    200 -> 110
                    else -> 150
                }
                applyShapeAndSize(appContext, cameraContainer, currentShape, currentSizeDp)
                try {
                    windowManager?.updateViewLayout(overlayView, params)
                } catch (_: Exception) {}
            }

            wm.addView(view, params)
            _isShowingState.value = true

            // Start Camera
            overlayLifecycleOwner.markResumed()
            bindCamera(appContext, pView)

        } catch (e: Exception) {
            e.printStackTrace()
            _isShowingState.value = false
            overlayView = null
        }
    }

    private fun bindCamera(context: Context, pView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = pView.surfaceProvider
                }
                val hasFront = cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) == true
                val hasBack = cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) == true
                val selector = when {
                    isFrontCamera && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                    !isFrontCamera && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                    hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                    hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                    else -> null
                }

                if (selector != null) {
                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        overlayLifecycleOwner,
                        selector,
                        preview
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun applyShapeAndSize(context: Context, container: ViewGroup, shape: SystemFaceCamShape, sizeDp: Int) {
        val density = context.resources.displayMetrics.density
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

    /**
     * Hides and cleans up the floating FaceCam overlay.
     */
    fun hide() {
        try {
            cameraProvider?.unbindAll()
            overlayLifecycleOwner.markDestroyed()
        } catch (_: Exception) {}

        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
        previewView = null
        _isShowingState.value = false
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
