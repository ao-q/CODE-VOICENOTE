package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.service.FloatingFaceCamOverlayManager
import com.example.service.FloatingRecordingOverlayManager
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
    val context = LocalContext.current
    val title by titleFlow.collectAsState()
    val notes by notesFlow.collectAsState()
    val isFaceCamActive by FloatingFaceCamOverlayManager.isShowingState.collectAsState()
    val isSystemOverlayActive by FloatingFaceCamOverlayManager.isSystemOverlayActive.collectAsState()
    var showOverlayPermissionPrompt by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!FloatingFaceCamOverlayManager.canDrawOverlays(context)) {
                showOverlayPermissionPrompt = true
            } else {
                FloatingFaceCamOverlayManager.show(context)
            }
        }
    }

    val toggleFaceCamAction = {
        if (isFaceCamActive) {
            FloatingFaceCamOverlayManager.hide(context)
        } else {
            if (!FloatingFaceCamOverlayManager.canDrawOverlays(context)) {
                showOverlayPermissionPrompt = true
            } else {
                FloatingFaceCamOverlayManager.show(context)
            }
        }
    }

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
                            isFaceCamActive = isFaceCamActive,
                            onToggleFaceCam = toggleFaceCamAction,
                            onLaunchCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
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
                        isFaceCamActive = isFaceCamActive,
                        onToggleFaceCam = toggleFaceCamAction,
                        onLaunchCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
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

            // Floating FaceCam Selfie Overlay (renders in-app only if system-wide WindowManager overlay is not active)
            if (isFaceCamActive && !isSystemOverlayActive) {
                FaceCamOverlay(
                    onClose = { FloatingFaceCamOverlayManager.hide(context) }
                )
            }

            if (showOverlayPermissionPrompt) {
                AlertDialog(
                    onDismissRequest = {
                        showOverlayPermissionPrompt = false
                        FloatingFaceCamOverlayManager.show(context)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    title = {
                        Text(
                            text = "Floating Camera Overlay",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            text = "To keep FaceCam floating outside the app and over other apps while recording, enable 'Display over other apps' in Android settings.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showOverlayPermissionPrompt = false
                                FloatingFaceCamOverlayManager.requestOverlayPermission(context)
                            }
                        ) {
                            Text("Enable in Settings")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showOverlayPermissionPrompt = false
                                FloatingFaceCamOverlayManager.show(context)
                            }
                        ) {
                            Text("Use In-App Only")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RecordingStudioSection(
    sessionState: RecordingSessionState,
    pulseScale: Float,
    isFaceCamActive: Boolean,
    onToggleFaceCam: () -> Unit,
    onLaunchCameraPermission: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFlagTimestamp: (String?) -> Unit,
    onStopAndSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // FaceCam Overlay Button (Selfie Camera Popup for Screen Recording)
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                onToggleFaceCam()
                            } else {
                                onLaunchCameraPermission()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("facecam_overlay_button")
                    ) {
                        Icon(
                            imageVector = if (isFaceCamActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "FaceCam Popup Overlay",
                            tint = if (isFaceCamActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Minimize Overlay Button (Minimize voice recording bar to floating overlay)
                    IconButton(
                        onClick = {
                            if (FloatingRecordingOverlayManager.canDrawOverlays(context)) {
                                FloatingRecordingOverlayManager.show(context)
                                (context as? Activity)?.moveTaskToBack(true)
                            } else {
                                FloatingRecordingOverlayManager.requestOverlayPermission(context)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("minimize_overlay_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Minimize Overlay",
                            tint = MaterialTheme.colorScheme.primary
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
                // Pause / Resume Button (Large, prominent Stock Android design)
                FilledTonalButton(
                    onClick = {
                        if (sessionState.isPaused) onResume() else onPause()
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (sessionState.isPaused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (sessionState.isPaused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (sessionState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (sessionState.isPaused) "Resume Recording" else "Pause Recording",
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Stop & Save Button (Large Prominent Recorder Button)
                FloatingActionButton(
                    onClick = onStopAndSave,
                    containerColor = RecorderRed,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("stop_and_save_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop and Save Note",
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating Window Status / Permission Pill
            val hasOverlayPermission = FloatingRecordingOverlayManager.canDrawOverlays(context)
            if (hasOverlayPermission) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.clickable {
                        (context as? Activity)?.moveTaskToBack(true)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Floating controls active on exit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.clickable {
                        FloatingRecordingOverlayManager.requestOverlayPermission(context)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Enable floating controls on exit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
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
    var showTimestampsDialog by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var notesFieldValue by remember(notes) {
        mutableStateOf(
            TextFieldValue(
                text = notes,
                selection = TextRange(notes.length)
            )
        )
    }

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
            // Section Header: "Notes (.md)", Mode Switcher, and Timestamps Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = "Notes",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Same folder",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit / Preview Pill Toggle
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isPreviewMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .clickable { isPreviewMode = false }
                                    .testTag("notes_edit_tab")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (!isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPreviewMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .clickable { isPreviewMode = true }
                                    .testTag("notes_preview_tab")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = "Preview",
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // "Clock and Plus" icon button: shows all timestamps to insert in one click
                    IconButton(
                        onClick = { showTimestampsDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("recording_clock_and_plus_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreTime,
                            contentDescription = "Insert Timestamps",
                            tint = TimestampFlagColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (!isPreviewMode) {
                // EDIT MODE
                // Note Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title of your notes") },
                    placeholder = { Text("e.g., Team Sync (defaults to recording time)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Advanced Formatting Toolbar (B, I, S, H1, H2, Bullet, Checkbox, Quote, Code, Timestamp Icon)
                MarkdownEditorToolbar(
                    value = notesFieldValue,
                    onValueChange = { newValue ->
                        notesFieldValue = newValue
                        onNotesChange(newValue.text)
                    },
                    currentTimestampFormatted = sessionState.elapsedFormatted,
                    onInsertTimestamp = {
                        val currentTs = sessionState.elapsedFormatted
                        val tag = "\n- **[$currentTs]** "
                        val updated = insertTextAtCursor(notesFieldValue, tag)
                        notesFieldValue = updated
                        onNotesChange(updated.text)
                        onFlagTimestamp("Flag at $currentTs")
                    },
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Line-Numbered Notes Text Area
                val notesScrollState = rememberScrollState()
                val lineCount = remember(notesFieldValue.text) {
                    if (notesFieldValue.text.isEmpty()) 1 else notesFieldValue.text.count { it == '\n' } + 1
                }
                val lineNumbersText = remember(lineCount) {
                    (1..lineCount).joinToString("\n")
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 10.dp)
                            .verticalScroll(notesScrollState)
                    ) {
                        // Line numbers column
                        Text(
                            text = lineNumbersText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            ),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .widthIn(min = 30.dp)
                                .padding(start = 8.dp, end = 8.dp)
                        )

                        // Vertical subtle separator
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(IntrinsicSize.Min)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        )

                        // Text field editing area
                        BasicTextField(
                            value = notesFieldValue,
                            onValueChange = { newValue ->
                                notesFieldValue = newValue
                                onNotesChange(newValue.text)
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 22.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 10.dp, end = 10.dp)
                                ) {
                                    if (notesFieldValue.text.isEmpty()) {
                                        Text(
                                            text = "Write notes in Markdown, format with toolbar above, or tap clock icon to insert timestamps...",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                lineHeight = 22.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("note_content_input")
                        )
                    }
                }
            } else {
                // PREVIEW MODE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        MarkdownPreview(
                            markdown = notes,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showTimestampsDialog) {
        TimestampsSelectionDialog(
            timestamps = sessionState.flaggedTimestamps,
            onInsertAll = {
                val mdBlock = buildString {
                    append("\n\n### Timestamps\n")
                    sessionState.flaggedTimestamps.forEach { ts ->
                        append("- **[${ts.formattedTime}]** ${ts.label}\n")
                    }
                }
                onNotesChange(notes + mdBlock)
            },
            onInsertSingle = { item ->
                onNotesChange(notes + "\n- **[${item.formattedTime}]** ${item.label}")
            },
            onFlagCurrentTime = {
                val currentTs = sessionState.elapsedFormatted
                onFlagTimestamp("Flag at $currentTs")
            },
            onDismiss = { showTimestampsDialog = false }
        )
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
