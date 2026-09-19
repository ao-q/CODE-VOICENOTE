package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Beautiful Empty State displaying the artist easel with blank canvas
 * and "The canvas is empty." text matching the user's hand-drawn concept.
 */
@Composable
fun EmptyCanvasView(
    message: String = "The canvas is empty.",
    modifier: Modifier = Modifier
) {
    val cyanColor = Color(0xFF38BDF8) // Bright Cyan matching the hand-drawn ink

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hand-drawn style Easel and Blank Canvas
        Canvas(
            modifier = Modifier.size(width = 170.dp, height = 180.dp)
        ) {
            val strokeWidth = 3.5.dp.toPx()
            val thinStroke = 2.dp.toPx()

            // 1. Easel Top Mast (vertical peg at top of canvas)
            drawLine(
                color = cyanColor,
                start = Offset(size.width * 0.5f, size.height * 0.04f),
                end = Offset(size.width * 0.5f, size.height * 0.20f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Top Mast Clamp / knob
            drawLine(
                color = cyanColor,
                start = Offset(size.width * 0.44f, size.height * 0.12f),
                end = Offset(size.width * 0.56f, size.height * 0.12f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // 2. Outer Canvas Board (Rectangular canvas on the easel)
            val canvasLeft = size.width * 0.18f
            val canvasTop = size.height * 0.14f
            val canvasWidth = size.width * 0.64f
            val canvasHeight = size.height * 0.50f

            // Fill canvas with very light translucent tint
            drawRoundRect(
                color = cyanColor.copy(alpha = 0.05f),
                topLeft = Offset(canvasLeft, canvasTop),
                size = Size(canvasWidth, canvasHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Outer canvas border
            drawRoundRect(
                color = cyanColor,
                topLeft = Offset(canvasLeft, canvasTop),
                size = Size(canvasWidth, canvasHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Inner canvas margin/frame (the inset line sketched in the user's drawing)
            val innerInset = 8.dp.toPx()
            drawRoundRect(
                color = cyanColor.copy(alpha = 0.75f),
                topLeft = Offset(canvasLeft + innerInset, canvasTop + innerInset),
                size = Size(canvasWidth - (innerInset * 2), canvasHeight - (innerInset * 2)),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(
                    width = thinStroke,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // 3. Horizontal Support Shelf (holding the canvas)
            val shelfY = canvasTop + canvasHeight + 2.dp.toPx()
            drawLine(
                color = cyanColor,
                start = Offset(size.width * 0.12f, shelfY),
                end = Offset(size.width * 0.88f, shelfY),
                strokeWidth = strokeWidth * 1.2f,
                cap = StrokeCap.Round
            )

            // 4. Easel Tripod Legs (angled downwards as in the sketch)
            // Left front leg
            val legStartY = shelfY
            drawLine(
                color = cyanColor,
                start = Offset(size.width * 0.35f, legStartY),
                end = Offset(size.width * 0.25f, size.height * 0.96f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Middle / center back leg (angled slightly)
            drawLine(
                color = cyanColor,
                start = Offset(size.width * 0.50f, legStartY),
                end = Offset(size.width * 0.44f, size.height * 0.98f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Right front leg
            drawLine(
                color = cyanColor,
                start = Offset(size.width * 0.65f, legStartY),
                end = Offset(size.width * 0.76f, size.height * 0.96f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Cross-brace between left and right legs
            val crossY = size.height * 0.82f
            drawLine(
                color = cyanColor.copy(alpha = 0.8f),
                start = Offset(size.width * 0.30f, crossY),
                end = Offset(size.width * 0.70f, crossY),
                strokeWidth = thinStroke,
                cap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // "The canvas is empty."
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
