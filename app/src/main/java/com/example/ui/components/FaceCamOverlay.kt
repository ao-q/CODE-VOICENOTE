package com.example.ui.components

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.FloatingFaceCamOverlayManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

enum class FaceCamShape(val label: String) {
    CIRCLE("Circle"),
    ROUNDED_RECT("Rounded"),
    SQUIRCLE("Squircle"),
    RECTANGLE("Rectangle")
}

enum class FaceCamSize(val dpSize: Int) {
    SMALL(110),
    MEDIUM(150),
    LARGE(200)
}

/**
 * Floating Selfie FaceCam Overlay for screen-recording / streamer workflow.
 * Renders the live front/back camera preview in customizable cutout shapes
 * (Circle, Rounded Rectangle, Squircle, Rectangle) with drag-to-position support.
 */
@Composable
fun FaceCamOverlay(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var shapeMode by remember { mutableStateOf(FaceCamShape.CIRCLE) }
    var sizeMode by remember { mutableStateOf(FaceCamSize.MEDIUM) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(false) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var boundCamera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var showZoomBadge by remember { mutableStateOf(false) }
    var zoomBadgeTimeoutJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    val clipShape: Shape = remember(shapeMode) {
        when (shapeMode) {
            FaceCamShape.CIRCLE -> CircleShape
            FaceCamShape.ROUNDED_RECT -> RoundedCornerShape(20.dp)
            FaceCamShape.SQUIRCLE -> RoundedCornerShape(36.dp)
            FaceCamShape.RECTANGLE -> RoundedCornerShape(10.dp)
        }
    }

    val currentSize = sizeMode.dpSize.dp

    // Safe camera binding triggered ONLY when useFrontCamera or previewView changes
    LaunchedEffect(useFrontCamera, previewViewRef) {
        val pView = previewViewRef ?: return@LaunchedEffect
        bindCameraSafe(context, lifecycleOwner, pView, useFrontCamera) { camera ->
            boundCamera = camera
            camera.cameraControl.setZoomRatio(currentZoomRatio)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
                .size(currentSize)
                .pointerInput(boundCamera, currentZoomRatio) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalPan = androidx.compose.ui.geometry.Offset.Zero
                        var isPinching = false
                        var isDragging = false
                        val touchSlop = viewConfiguration.touchSlop

                        do {
                            val event = awaitPointerEvent()
                            if (event.changes.size >= 2) {
                                // Multi-touch pinch-to-zoom
                                isPinching = true
                                val zoomChange = event.calculateZoom()
                                if (zoomChange != 1f) {
                                    val maxZoom = boundCamera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 6f
                                    val minZoom = boundCamera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f
                                    val newRatio = (currentZoomRatio * zoomChange).coerceIn(minZoom, maxZoom)
                                    currentZoomRatio = newRatio
                                    boundCamera?.cameraControl?.setZoomRatio(newRatio)
                                    showZoomBadge = true
                                    zoomBadgeTimeoutJob?.cancel()
                                    zoomBadgeTimeoutJob = coroutineScope.launch {
                                        delay(1200)
                                        showZoomBadge = false
                                    }
                                }
                                val panChange = event.calculatePan()
                                offsetX += panChange.x
                                offsetY += panChange.y
                                event.changes.forEach { it.consume() }
                            } else if (event.changes.size == 1) {
                                val change = event.changes[0]
                                val diff = change.position - change.previousPosition
                                totalPan += diff
                                if (!isDragging && totalPan.getDistance() > touchSlop) {
                                    isDragging = true
                                }
                                if (isDragging) {
                                    offsetX += diff.x
                                    offsetY += diff.y
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })

                        if (!isDragging && !isPinching) {
                            showControls = !showControls
                        }
                    }
                }
                .shadow(12.dp, clipShape)
                .clip(clipShape)
                .background(Color.Black)
                .border(2.5.dp, MaterialTheme.colorScheme.primary, clipShape),
            contentAlignment = Alignment.Center
        ) {
            // CameraX Preview View (PERFORMANCE mode = direct SurfaceView zero-copy rendering, eliminating lag)
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                        previewViewRef = this
                    }
                },
                update = {
                    previewViewRef = it
                },
                modifier = Modifier.fillMaxSize()
            )

            // Real-time Zoom Badge during pinch gesture
            AnimatedVisibility(
                visible = showZoomBadge,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fx", currentZoomRatio),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // On-Screen Quick Control Bar when tapped
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(4.dp)
                    ) {
                        // Action Icons Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cycle Shape
                            IconButton(
                                onClick = {
                                    shapeMode = when (shapeMode) {
                                        FaceCamShape.CIRCLE -> FaceCamShape.ROUNDED_RECT
                                        FaceCamShape.ROUNDED_RECT -> FaceCamShape.SQUIRCLE
                                        FaceCamShape.SQUIRCLE -> FaceCamShape.RECTANGLE
                                        FaceCamShape.RECTANGLE -> FaceCamShape.CIRCLE
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Cutout Shape",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Cycle Size
                            IconButton(
                                onClick = {
                                    sizeMode = when (sizeMode) {
                                        FaceCamSize.SMALL -> FaceCamSize.MEDIUM
                                        FaceCamSize.MEDIUM -> FaceCamSize.LARGE
                                        FaceCamSize.LARGE -> FaceCamSize.SMALL
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Resize FaceCam",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Switch Camera Lens (Front / Back)
                            IconButton(
                                onClick = {
                                    useFrontCamera = !useFrontCamera
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (useFrontCamera) Icons.Default.CameraRear else Icons.Default.CameraFront,
                                    contentDescription = if (useFrontCamera) "Switch to Back Camera" else "Switch to Front Camera",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Quick Zoom Cycle button (1x -> 2x -> 3x -> 1x)
                            IconButton(
                                onClick = {
                                    val maxZoom = boundCamera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 5f
                                    val nextZoom = when {
                                        currentZoomRatio < 1.9f && maxZoom >= 2f -> 2.0f
                                        currentZoomRatio < 2.9f && maxZoom >= 3f -> 3.0f
                                        else -> 1.0f
                                    }
                                    currentZoomRatio = nextZoom
                                    boundCamera?.cameraControl?.setZoomRatio(nextZoom)
                                    showZoomBadge = true
                                    zoomBadgeTimeoutJob?.cancel()
                                    zoomBadgeTimeoutJob = coroutineScope.launch {
                                        delay(1200)
                                        showZoomBadge = false
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.0fx", currentZoomRatio),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Float over other apps (System Overlay)
                            IconButton(
                                onClick = {
                                    if (FloatingFaceCamOverlayManager.canDrawOverlays(context)) {
                                        FloatingFaceCamOverlayManager.show(context)
                                    } else {
                                        FloatingFaceCamOverlayManager.requestOverlayPermission(context)
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Float over other apps",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Close FaceCam
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close FaceCam",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Safely resolves and binds either Front or Back camera use case.
 */
private fun bindCameraSafe(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    preferFront: Boolean,
    onCameraBound: (androidx.camera.core.Camera) -> Unit = {}
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()
            val availableCameras = cameraProvider.availableCameraInfos

            val hasBack = try { cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) } catch (_: Exception) { false }
            val hasFront = try { cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) } catch (_: Exception) { false }

            val selector = when {
                !preferFront && hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                preferFront && hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                // Fallback for emulators/custom devices where camera isn't tagged DEFAULT_BACK
                !preferFront && availableCameras.size > 1 -> {
                    CameraSelector.Builder().addCameraFilter { list ->
                        val nonFront = list.filter { it.lensFacing != CameraSelector.LENS_FACING_FRONT }
                        if (nonFront.isNotEmpty()) nonFront else list.takeLast(1)
                    }.build()
                }
                preferFront && availableCameras.size > 1 -> {
                    CameraSelector.Builder().addCameraFilter { list ->
                        val front = list.filter { it.lensFacing == CameraSelector.LENS_FACING_FRONT }
                        if (front.isNotEmpty()) front else list.take(1)
                    }.build()
                }
                hasBack -> CameraSelector.DEFAULT_BACK_CAMERA
                hasFront -> CameraSelector.DEFAULT_FRONT_CAMERA
                else -> CameraSelector.DEFAULT_BACK_CAMERA
            }

            cameraProvider.unbindAll()
            // Set 640x480 resolution for lag-free performance on all devices
            val preview = Preview.Builder()
                .setTargetResolution(android.util.Size(640, 480))
                .build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
            onCameraBound(camera)

            if (availableCameras.size <= 1) {
                Toast.makeText(context, "1 camera detected on device", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, if (preferFront) "Front Camera" else "Back Camera", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }, ContextCompat.getMainExecutor(context))
}
