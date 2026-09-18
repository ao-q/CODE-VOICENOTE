package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.RecordingSessionState
import com.example.ui.theme.RecorderRed
import com.example.ui.theme.TimestampFlagColor
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ActiveRecordingPane(
    sessionState: RecordingSessionState,
    titleFlow: StateFlow<String>,
    notesFlow: StateFlow<String>,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFlagTimestamp: (String?) -> Unit,
    onStopAndSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title by titleFlow.collectAsState()
    val notes by notesFlow.collectAsState()

    // Pulsing recording indicator animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("active_recording_pane"),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth >= 700.dp

            if (isWide) {
                // Side by side layout for tablets / landscape
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Left Section: Recording Studio
                    Box(modifier = Modifier.weight(1f)) {
                        RecordingStudioSection(
                            sessionState = sessionState,
                            pulseScale = pulseScale,
                            onPause = onPause,
                            onResume = onResume,
                            onFlagTimestamp = onFlagTimestamp,
                            onStopAndSave = onStopAndSave,
                            onCancel = onCancel
                        )
                    }

                    // Right Section: Write Notes
                    Box(modifier = Modifier.weight(1.2f)) {
                        WriteNotesSection(
                            title = title,
                            notes = notes,
                            sessionState = sessionState,
                            onTitleChange = onTitleChange,
                            onNotesChange = onNotesChange,
                            onFlagTimestamp = onFlagTimestamp
                        )
                    }
                }
            } else {
                // Vertical stacked layout for phones
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Recording Section (Top)
                    RecordingStudioSection(
                        sessionState = sessionState,
                        pulseScale = pulseScale,
                        onPause = onPause,
                        onResume = onResume,
                        onFlagTimestamp = onFlagTimestamp,
                        onStopAndSave = onStopAndSave,
                        onCancel = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Side section beside recording called "Write Notes"
                    WriteNotesSection(
                        title = title,
                        notes = notes,
                        sessionState = sessionState,
                        onTitleChange = onTitleChange,
                        onNotesChange = onNotesChange,
                        onFlagTimestamp = onFlagTimestamp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingStudioSection(
    sessionState: RecordingSessionState,
    pulseScale: Float,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFlagTimestamp: (String?) -> Unit,
    onStopAndSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar of Recording Card: Status & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (sessionState.isPaused) MaterialTheme.colorScheme.secondaryContainer
                            else RecorderRed.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .scale(if (!sessionState.isPaused) pulseScale else 1.0f)
                            .clip(CircleShape)
                            .background(if (sessionState.isPaused) Color.Gray else RecorderRed)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sessionState.isPaused) "PAUSED" else "RECORDING • MP3 (48kHz)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (sessionState.isPaused) MaterialTheme.colorScheme.onSecondaryContainer else RecorderRed
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("cancel_recording_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Recording",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timer Display
            Text(
                text = sessionState.elapsedFormatted,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("recording_timer_text")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Amplitude Canvas
            WaveformVisualizer(
                amplitudes = sessionState.amplitudeHistory,
                isPaused = sessionState.isPaused,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Flag Timestamp Button & Flagged Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onFlagTimestamp(null) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("flag_timestamp_button")
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = "Flag Timestamp",
                        tint = TimestampFlagColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Flag Timestamp", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Scrollable row of flagged timestamps
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sessionState.flaggedTimestamps.forEach { ts ->
                        AssistChip(
                            onClick = {},
                            label = { Text(ts.formattedTime, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.FiberManualRecord,
                                    contentDescription = null,
                                    tint = TimestampFlagColor,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Recording Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Button
                FilledTonalButton(
                    onClick = {
                        if (sessionState.isPaused) onResume() else onPause()
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (sessionState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (sessionState.isPaused) "Resume" else "Pause",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(28.dp))

                // Stop & Save Button (Large Prominent Recorder Button)
                FloatingActionButton(
                    onClick = onStopAndSave,
                    containerColor = RecorderRed,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("stop_and_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop and Save Note",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WriteNotesSection(
    title: String,
    notes: String,
    sessionState: RecordingSessionState,
    onTitleChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onFlagTimestamp: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Section Header: "Write Notes"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = "Write Notes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Write Notes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Quick insert timestamp tag button
                AssistChip(
                    onClick = {
                        val currentTs = sessionState.elapsedFormatted
                        val insertion = "\n[Flag: $currentTs] "
                        onNotesChange(notes + insertion)
                        onFlagTimestamp("Note flag at $currentTs")
                    },
                    label = { Text("+ Timestamp", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Note Title Input
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Title of your notes") },
                placeholder = { Text("e.g., Team Sync (defaults to recording time)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Regular Notes App Text Area
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Take notes here while recording...") },
                placeholder = { Text("Type summary, meeting agenda, decisions, or key remarks...") },
                singleLine = false,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("note_content_input")
            )
        }
    }
}

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = if (isPaused) Color.Gray else RecorderRed

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val totalBars = 36
        val barSpacing = 4.dp.toPx()
        val barWidth = (width - (totalBars - 1) * barSpacing) / totalBars

        val paddedAmplitudes = if (amplitudes.size < totalBars) {
            List(totalBars - amplitudes.size) { 0.08f } + amplitudes
        } else {
            amplitudes.takeLast(totalBars)
        }

        for (i in 0 until totalBars) {
            val amp = paddedAmplitudes.getOrElse(i) { 0.08f }.coerceIn(0.08f, 1.0f)
            val barHeight = (height * amp).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + barSpacing)
            val y = (height - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
