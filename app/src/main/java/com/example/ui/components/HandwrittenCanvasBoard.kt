package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.storage.ExportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

enum class CanvasTool {
    PEN,
    HIGHLIGHTER,
    ERASER
}

enum class PaperStyle {
    PLAIN,
    RULED,
    DOT_GRID
}

enum class CanvasPageSize {
    A4,
    INFINITE
}

data class StrokePoint(val x: Float, val y: Float)

data class CanvasStroke(
    val points: List<StrokePoint>,
    val color: Color,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false
)

/**
 * Full-screen Stylus and Finger Handwritten Canvas Board.
 * The canvas extends over the entire screen (including behind the top bar and bottom toolbar),
 * allowing pinch-to-zoom and pan gestures to extend seamlessly without top/bottom black spaces.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwrittenCanvasBoard(
    onDismiss: () -> Unit,
    onSave: (title: String, bitmap: Bitmap) -> Unit,
    initialTitle: String? = null,
    initialBitmap: Bitmap? = null,
    modifier: Modifier = Modifier
) {
    val defaultTitle = remember(initialTitle) {
        if (!initialTitle.isNullOrBlank()) initialTitle
        else {
            val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            "Drawing - ${sdf.format(Date())}"
        }
    }
    var noteTitle by remember(initialTitle) { mutableStateOf(defaultTitle) }

    val initialImageBitmap = remember(initialBitmap) {
        initialBitmap?.asImageBitmap()
    }

    var selectedTool by remember { mutableStateOf(CanvasTool.PEN) }
    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var selectedStrokeWidth by remember { mutableFloatStateOf(4f) }
    var paperStyle by remember { mutableStateOf(PaperStyle.RULED) }

    // Pinch-to-zoom & Pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val strokes = remember { mutableStateListOf<CanvasStroke>() }
    val redoStack = remember { mutableStateListOf<CanvasStroke>() }
    val currentPoints = remember { mutableStateListOf<StrokePoint>() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val colorPalette = listOf(
        Color(0xFF0F172A), // Dark Slate / Black
        Color(0xFF1D4ED8), // Deep Blue
        Color(0xFF0284C7), // Sky Blue
        Color(0xFFDC2626), // Crimson Red
        Color(0xFF16A34A), // Emerald Green
        Color(0xFFD97706), // Amber Gold
        Color(0xFF7C3AED), // Violet
        Color(0xFFFFFFFF)  // White
    )

    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .testTag("handwritten_canvas_board"),
            color = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // 1. FULL-SCREEN CANVAS DRAWING LAYER (Spans entire screen under top bar and bottom dock)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(selectedTool, selectedColor, selectedStrokeWidth, zoomScale, panOffset) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var isMultiTouch = false
                                currentPoints.clear()

                                val initX = (down.position.x - panOffset.x) / zoomScale
                                val initY = (down.position.y - panOffset.y) / zoomScale
                                currentPoints.add(StrokePoint(initX, initY))

                                do {
                                    val event = awaitPointerEvent()
                                    val pressedPointers = event.changes.filter { it.pressed }

                                    if (pressedPointers.size >= 2) {
                                        // Multi-touch: PINCH GESTURE FOR ZOOM & PAN
                                        isMultiTouch = true
                                        currentPoints.clear()

                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()
                                        val centroid = event.calculateCentroid(useCurrent = false)

                                        val oldScale = zoomScale
                                        val newScale = (zoomScale * zoomChange).coerceIn(0.5f, 6.0f)

                                        if (centroid != Offset.Unspecified) {
                                            panOffset = centroid - (centroid - panOffset) * (newScale / oldScale) + panChange
                                        } else {
                                            panOffset += panChange
                                        }
                                        zoomScale = newScale

                                        event.changes.forEach { it.consume() }
                                    } else if (pressedPointers.size == 1 && !isMultiTouch) {
                                        // Single-touch: DRAWING OR ERASING
                                        val change = pressedPointers.first()
                                        change.consume()
                                        val cx = (change.position.x - panOffset.x) / zoomScale
                                        val cy = (change.position.y - panOffset.y) / zoomScale
                                        currentPoints.add(StrokePoint(cx, cy))
                                    }
                                } while (event.changes.any { it.pressed })

                                // Gesture finished
                                if (!isMultiTouch && currentPoints.isNotEmpty()) {
                                    if (selectedTool == CanvasTool.ERASER) {
                                        eraseIntersectingStrokes(strokes, currentPoints, (selectedStrokeWidth * 4f) / zoomScale)
                                    } else {
                                        val stroke = CanvasStroke(
                                            points = currentPoints.toList(),
                                            color = if (selectedTool == CanvasTool.HIGHLIGHTER) selectedColor.copy(alpha = 0.38f) else selectedColor,
                                            strokeWidth = if (selectedTool == CanvasTool.HIGHLIGHTER) selectedStrokeWidth * 3.5f else selectedStrokeWidth,
                                            isHighlighter = selectedTool == CanvasTool.HIGHLIGHTER
                                        )
                                        strokes.add(stroke)
                                        redoStack.clear()
                                    }
                                }
                                currentPoints.clear()
                            }
                        }
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                    ) {
                        withTransform({
                            translate(panOffset.x, panOffset.y)
                            scale(zoomScale, zoomScale, pivot = Offset.Zero)
                        }) {
                            val canvasW = size.width
                            val canvasH = size.height

                            if (initialImageBitmap != null) {
                                drawImage(
                                    image = initialImageBitmap,
                                    dstSize = IntSize(canvasW.toInt(), canvasH.toInt())
                                )
                            } else {
                                // Render Paper Guide Lines / Dot Grid across the full expanse
                                when (paperStyle) {
                                    PaperStyle.RULED -> {
                                        val lineSpacing = 36.dp.toPx()
                                        var y = -canvasH
                                        while (y < canvasH * 3) {
                                            drawLine(
                                                color = Color(0xFFE2E8F0),
                                                start = Offset(-canvasW * 2, y),
                                                end = Offset(canvasW * 3, y),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                            y += lineSpacing
                                        }
                                    }
                                    PaperStyle.DOT_GRID -> {
                                        val gridSpacing = 28.dp.toPx()
                                        var x = -canvasW
                                        while (x < canvasW * 3) {
                                            var y = -canvasH
                                            while (y < canvasH * 3) {
                                                drawCircle(
                                                    color = Color(0xFFCBD5E1),
                                                    radius = 1.5.dp.toPx(),
                                                    center = Offset(x, y)
                                                )
                                                y += gridSpacing
                                            }
                                            x += gridSpacing
                                        }
                                    }
                                    PaperStyle.PLAIN -> { /* Clean blank white */ }
                                }
                            }

                            // Draw Completed Strokes
                            for (stroke in strokes) {
                                drawSmoothStroke(stroke)
                            }

                            // Draw In-Progress Stroke
                            if (currentPoints.isNotEmpty() && selectedTool != CanvasTool.ERASER) {
                                val activeStroke = CanvasStroke(
                                    points = currentPoints.toList(),
                                    color = if (selectedTool == CanvasTool.HIGHLIGHTER) selectedColor.copy(alpha = 0.38f) else selectedColor,
                                    strokeWidth = if (selectedTool == CanvasTool.HIGHLIGHTER) selectedStrokeWidth * 3.5f else selectedStrokeWidth,
                                    isHighlighter = selectedTool == CanvasTool.HIGHLIGHTER
                                )
                                drawSmoothStroke(activeStroke)
                            }
                        }
                    }
                }

                // 2. FLOATING TOP APP BAR (Frosted / Translucent overlay)
                if (!isFullscreen) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {
                        TopAppBar(
                            title = {
                                OutlinedTextField(
                                    value = noteTitle,
                                    onValueChange = { noteTitle = it },
                                    placeholder = { Text("Drawing Title", fontSize = 15.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("handwritten_canvas_title_input")
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("handwritten_canvas_back_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close Canvas")
                                }
                            },
                            actions = {
                                // Undo
                                IconButton(
                                    onClick = {
                                        if (strokes.isNotEmpty()) {
                                            val last = strokes.removeAt(strokes.lastIndex)
                                            redoStack.add(last)
                                        }
                                    },
                                    enabled = strokes.isNotEmpty(),
                                    modifier = Modifier.testTag("handwritten_canvas_undo_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                                }

                                // Redo
                                IconButton(
                                    onClick = {
                                        if (redoStack.isNotEmpty()) {
                                            val last = redoStack.removeAt(redoStack.lastIndex)
                                            strokes.add(last)
                                        }
                                    },
                                    enabled = redoStack.isNotEmpty(),
                                    modifier = Modifier.testTag("handwritten_canvas_redo_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                                }

                                // Save Note Button
                                FilledTonalIconButton(
                                    onClick = {
                                        val cw = if (canvasSize.width > 0) canvasSize.width else 1080
                                        val ch = if (canvasSize.height > 0) canvasSize.height else 1920
                                        val bitmap = renderStrokesToBitmap(cw, ch, strokes, paperStyle, initialBitmap)
                                        onSave(noteTitle, bitmap)
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .testTag("handwritten_canvas_save_button"),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Save Drawing")
                                }

                                // 3-dots Menu for Document Options & Export (Declutters top bar)
                                Box {
                                    IconButton(
                                        onClick = { showExportMenu = true },
                                        modifier = Modifier.testTag("handwritten_canvas_export_menu_button")
                                    ) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                                    }

                                    DropdownMenu(
                                        expanded = showExportMenu,
                                        onDismissRequest = { showExportMenu = false },
                                        modifier = Modifier.testTag("canvas_more_options_menu")
                                    ) {
                                        // Paper Style Cycle
                                        DropdownMenuItem(
                                            text = {
                                                val styleName = when (paperStyle) {
                                                    PaperStyle.RULED -> "Ruled Lines"
                                                    PaperStyle.DOT_GRID -> "Dot Grid"
                                                    PaperStyle.PLAIN -> "Plain Blank"
                                                }
                                                Text("Paper Style: $styleName")
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = when (paperStyle) {
                                                        PaperStyle.RULED -> Icons.Default.FormatAlignJustify
                                                        PaperStyle.DOT_GRID -> Icons.Default.Grain
                                                        PaperStyle.PLAIN -> Icons.Default.CropPortrait
                                                    },
                                                    contentDescription = null
                                                )
                                            },
                                            onClick = {
                                                paperStyle = when (paperStyle) {
                                                    PaperStyle.RULED -> PaperStyle.DOT_GRID
                                                    PaperStyle.DOT_GRID -> PaperStyle.PLAIN
                                                    PaperStyle.PLAIN -> PaperStyle.RULED
                                                }
                                                showExportMenu = false
                                            }
                                        )

                                        HorizontalDivider()

                                        // Fullscreen
                                        DropdownMenuItem(
                                            text = { Text("Full Screen") },
                                            leadingIcon = { Icon(Icons.Default.Fullscreen, contentDescription = null) },
                                            onClick = {
                                                isFullscreen = true
                                                showExportMenu = false
                                            }
                                        )

                                        // Clear Canvas
                                        DropdownMenuItem(
                                            text = { Text("Clear Canvas", color = MaterialTheme.colorScheme.error) },
                                            leadingIcon = {
                                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                            },
                                            onClick = {
                                                if (strokes.isNotEmpty()) {
                                                    strokes.clear()
                                                    redoStack.clear()
                                                }
                                                showExportMenu = false
                                            },
                                            enabled = strokes.isNotEmpty()
                                        )

                                        HorizontalDivider()

                                        // Export Lossless PNG
                                        DropdownMenuItem(
                                            text = { Text("Export Lossless PNG") },
                                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                                            onClick = {
                                                showExportMenu = false
                                                val cw = if (canvasSize.width > 0) canvasSize.width else 1080
                                                val ch = if (canvasSize.height > 0) canvasSize.height else 1920
                                                val bitmap = renderStrokesToBitmap(cw, ch, strokes, paperStyle, initialBitmap)
                                                ExportHelper.exportLosslessPng(context, noteTitle, bitmap)
                                            }
                                        )

                                        // Export PDF
                                        DropdownMenuItem(
                                            text = { Text("Export as PDF") },
                                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                            onClick = {
                                                showExportMenu = false
                                                val cw = if (canvasSize.width > 0) canvasSize.width else 1080
                                                val ch = if (canvasSize.height > 0) canvasSize.height else 1920
                                                val bitmap = renderStrokesToBitmap(cw, ch, strokes, paperStyle, initialBitmap)
                                                ExportHelper.exportPdf(context, noteTitle, bitmap)
                                            }
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }

                // 3. FLOATING ZOOM INDICATOR (Positioned cleanly below top bar)
                if (zoomScale != 1f || panOffset != Offset.Zero) {
                    Surface(
                        onClick = {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(top = if (isFullscreen) 16.dp else 72.dp, start = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomIn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• Reset",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 4. FLOATING BOTTOM DOCK (Floats cleanly over canvas above navigation bar)
                if (!isFullscreen) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("canvas_bottom_dock"),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                        shadowElevation = 8.dp,
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Tools & Width Dots
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Tools (Pen, Highlighter, Eraser)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ToolIconButton(
                                        icon = Icons.Default.Edit,
                                        label = "Pen",
                                        isSelected = selectedTool == CanvasTool.PEN,
                                        onClick = { selectedTool = CanvasTool.PEN }
                                    )
                                    ToolIconButton(
                                        icon = Icons.Default.Brush,
                                        label = "Highlighter",
                                        isSelected = selectedTool == CanvasTool.HIGHLIGHTER,
                                        onClick = { selectedTool = CanvasTool.HIGHLIGHTER }
                                    )
                                    ToolIconButton(
                                        icon = Icons.Default.AutoFixHigh,
                                        label = "Eraser",
                                        isSelected = selectedTool == CanvasTool.ERASER,
                                        onClick = { selectedTool = CanvasTool.ERASER }
                                    )
                                }

                                VerticalDivider(
                                    modifier = Modifier.height(26.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // Compact Stroke Width Dots: Fine (4f), Medium (8f), Bold (16f)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CompactWidthDot(
                                        dotSize = 4.dp,
                                        label = "Fine",
                                        isSelected = selectedStrokeWidth == 4f,
                                        onClick = { selectedStrokeWidth = 4f }
                                    )
                                    CompactWidthDot(
                                        dotSize = 7.dp,
                                        label = "Medium",
                                        isSelected = selectedStrokeWidth == 8f,
                                        onClick = { selectedStrokeWidth = 8f }
                                    )
                                    CompactWidthDot(
                                        dotSize = 11.dp,
                                        label = "Bold",
                                        isSelected = selectedStrokeWidth == 16f,
                                        onClick = { selectedStrokeWidth = 16f }
                                    )
                                }
                            }

                            // Row 2: Color Palette (smoothly hides when eraser is selected)
                            AnimatedVisibility(visible = selectedTool != CanvasTool.ERASER) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (col in colorPalette) {
                                        val isChosen = selectedColor == col
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(col)
                                                .border(
                                                    width = if (isChosen) 2.5.dp else 1.dp,
                                                    color = if (isChosen) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.35f),
                                                    shape = CircleShape
                                                )
                                                .clickable { selectedColor = col },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isChosen) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(if (col == Color.White) Color.Black else Color.White)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. MINIMAL FLOATING CONTROLS FOR FULLSCREEN MODE
                if (isFullscreen) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        shadowElevation = 8.dp,
                        tonalElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = { isFullscreen = false },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("handwritten_canvas_fullscreen_toggle")
                            ) {
                                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen")
                            }

                            IconButton(
                                onClick = { selectedTool = CanvasTool.PEN },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Pen",
                                    tint = if (selectedTool == CanvasTool.PEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { selectedTool = CanvasTool.HIGHLIGHTER },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Brush,
                                    contentDescription = "Highlighter",
                                    tint = if (selectedTool == CanvasTool.HIGHLIGHTER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { selectedTool = CanvasTool.ERASER },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = "Eraser",
                                    tint = if (selectedTool == CanvasTool.ERASER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (strokes.isNotEmpty()) {
                                        val last = strokes.removeAt(strokes.lastIndex)
                                        redoStack.add(last)
                                    }
                                },
                                enabled = strokes.isNotEmpty(),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                            }

                            IconButton(
                                onClick = {
                                    if (redoStack.isNotEmpty()) {
                                        val last = redoStack.removeAt(redoStack.lastIndex)
                                        strokes.add(last)
                                    }
                                },
                                enabled = redoStack.isNotEmpty(),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                            }

                            FilledTonalIconButton(
                                onClick = {
                                    val w = if (canvasSize.width > 0) canvasSize.width else 1080
                                    val h = if (canvasSize.height > 0) canvasSize.height else 1920
                                    val bitmap = renderStrokesToBitmap(w, h, strokes, paperStyle, initialBitmap)
                                    onSave(noteTitle, bitmap)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("handwritten_canvas_save_button"),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save Drawing")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun CompactWidthDot(
    dotSize: Dp,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.size(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmoothStroke(stroke: CanvasStroke) {
    val pts = stroke.points
    if (pts.isEmpty()) return
    if (pts.size == 1) {
        drawCircle(
            color = stroke.color,
            radius = stroke.strokeWidth / 2f,
            center = Offset(pts[0].x, pts[0].y)
        )
        return
    }

    val path = Path()
    path.moveTo(pts[0].x, pts[0].y)

    for (i in 1 until pts.size) {
        val prev = pts[i - 1]
        val curr = pts[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        path.quadraticTo(prev.x, prev.y, midX, midY)
    }
    path.lineTo(pts.last().x, pts.last().y)

    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(
            width = stroke.strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun eraseIntersectingStrokes(
    strokes: MutableList<CanvasStroke>,
    eraserPoints: List<StrokePoint>,
    threshold: Float
) {
    strokes.removeAll { stroke ->
        stroke.points.any { sp ->
            eraserPoints.any { ep ->
                hypot(sp.x - ep.x, sp.y - ep.y) <= threshold
            }
        }
    }
}

/**
 * Renders all canvas strokes into a standalone high-resolution Bitmap.
 */
fun renderStrokesToBitmap(
    width: Int,
    height: Int,
    strokes: List<CanvasStroke>,
    paperStyle: PaperStyle,
    initialBitmap: Bitmap? = null
): Bitmap {
    val safeW = width.coerceAtLeast(300)
    val safeH = height.coerceAtLeast(300)
    val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // Fill background
    canvas.drawColor(android.graphics.Color.WHITE)

    if (initialBitmap != null) {
        val destRect = android.graphics.Rect(0, 0, safeW, safeH)
        canvas.drawBitmap(initialBitmap, null, destRect, null)
    } else {
        // Optional background grid
        val gridPaint = Paint().apply {
            color = android.graphics.Color.rgb(230, 235, 240)
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        if (paperStyle == PaperStyle.RULED) {
            var y = 80f
            while (y < safeH) {
                canvas.drawLine(0f, y, safeW.toFloat(), y, gridPaint)
                y += 80f
            }
        } else if (paperStyle == PaperStyle.DOT_GRID) {
            val dotPaint = Paint().apply {
                color = android.graphics.Color.rgb(210, 220, 230)
                style = Paint.Style.FILL
            }
            var x = 60f
            while (x < safeW) {
                var y = 60f
                while (y < safeH) {
                    canvas.drawCircle(x, y, 3f, dotPaint)
                    y += 60f
                }
                x += 60f
            }
        }
    }

    for (stroke in strokes) {
        val pts = stroke.points
        if (pts.isEmpty()) continue

        val paint = Paint().apply {
            color = stroke.color.toArgb()
            strokeWidth = stroke.strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        if (stroke.isHighlighter) {
            paint.alpha = 90
        } else {
            paint.alpha = 255
        }

        if (pts.size == 1) {
            val fillPaint = Paint(paint).apply { style = Paint.Style.FILL }
            canvas.drawCircle(pts[0].x, pts[0].y, stroke.strokeWidth / 2f, fillPaint)
            continue
        }

        val aPath = AndroidPath()
        aPath.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val curr = pts[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            aPath.quadTo(prev.x, prev.y, midX, midY)
        }
        aPath.lineTo(pts.last().x, pts.last().y)
        canvas.drawPath(aPath, paint)
    }

    return bitmap
}

/**
 * Three-dots dropdown menu for lossless PNG and PDF export.
 */
@Composable
fun CanvasExportDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onExportPng: () -> Unit,
    onExportPdf: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("canvas_export_dropdown_menu")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onExportPng,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("canvas_export_png_icon_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Lossless PNG",
                    modifier = Modifier.size(24.dp)
                )
            }

            FilledTonalIconButton(
                onClick = onExportPdf,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("canvas_export_pdf_icon_button"),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "PDF Document",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
