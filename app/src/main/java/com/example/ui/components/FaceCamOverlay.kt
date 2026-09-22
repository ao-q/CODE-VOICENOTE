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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.service.FloatingFaceCamOverlayManager
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
        bindCameraSafe(context, lifecycleOwner, pView, useFrontCamera)
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
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
                .shadow(12.dp, clipShape)
                .clip(clipShape)
                .background(Color.Black)
                .border(2.5.dp, MaterialTheme.colorScheme.primary, clipShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showControls = !showControls
                },
            contentAlignment = Alignment.Center
        ) {
            // CameraX Preview View
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                    }
                },
                update = {
                    previewViewRef = it
                },
                modifier = Modifier.fillMaxSize()
            )

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
    preferFront: Boolean
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
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)

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
