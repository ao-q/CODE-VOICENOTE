package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
 * System-Wide Floating FaceCam Overlay Coordinator.
 * Controls the Foreground Service (FloatingFaceCamService) that displays a live,
 * draggable selfie/front camera video overlay on top of all Android apps and home screen
 * using WindowManager (TYPE_APPLICATION_OVERLAY).
 */
object FloatingFaceCamOverlayManager {

    private val _isShowingState = MutableStateFlow(false)
    val isShowingState: StateFlow<Boolean> = _isShowingState.asStateFlow()

    private val _isSystemOverlayActive = MutableStateFlow(false)
    val isSystemOverlayActive: StateFlow<Boolean> = _isSystemOverlayActive.asStateFlow()

    fun isShowing(): Boolean = _isShowingState.value

    fun isSystemOverlay(): Boolean = _isSystemOverlayActive.value

    fun setShowing(active: Boolean) {
        _isShowingState.value = active
        _isSystemOverlayActive.value = active
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Opens system settings for granting "Display over other apps" permission.
     */
    fun requestOverlayPermission(context: Context) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    /**
     * Shows the floating FaceCam overlay.
     * If overlay permission is granted, starts the FloatingFaceCamService foreground service
     * so camera continues streaming smoothly over other apps and on the home screen.
     */
    fun show(context: Context) {
        val appContext = context.applicationContext
        _isShowingState.value = true

        if (canDrawOverlays(appContext)) {
            _isSystemOverlayActive.value = true
            FloatingFaceCamService.start(appContext)
        } else {
            _isSystemOverlayActive.value = false
        }
    }

    /**
     * Hides the floating FaceCam overlay and stops the foreground service.
     */
    fun hide(context: Context? = null) {
        _isShowingState.value = false
        _isSystemOverlayActive.value = false
        if (context != null) {
            FloatingFaceCamService.stop(context.applicationContext)
        }
    }

    fun toggle(context: Context) {
        if (isShowing()) {
            hide(context)
        } else {
            show(context)
        }
    }
}
