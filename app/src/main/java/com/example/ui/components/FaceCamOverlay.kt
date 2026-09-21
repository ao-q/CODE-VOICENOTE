package com.example.ui.components

import android.view.ViewGroup
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
import androidx.lifecycle.compose.LocalLifecycleOwner
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
 * Renders the live front camera preview in customizable cutout shapes
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

    val clipShape: Shape = remember(shapeMode) {
        when (shapeMode) {
            FaceCamShape.CIRCLE -> CircleShape
            FaceCamShape.ROUNDED_RECT -> RoundedCornerShape(20.dp)
            FaceCamShape.SQUIRCLE -> RoundedCornerShape(36.dp)
            FaceCamShape.RECTANGLE -> RoundedCornerShape(10.dp)
        }
    }

    val currentSize = sizeMode.dpSize.dp

    Box(
        modifier = modifier
            .fillMaxSize()
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
                    }
                },
                update = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val cameraSelector = if (useFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
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

                            // Flip Front/Back Lens
                            IconButton(
                                onClick = { useFrontCamera = !useFrontCamera },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cameraswitch,
                                    contentDescription = "Switch Camera",
                                    tint = Color.White,
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
                                    tint = Color(0xFFFF5252),
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
