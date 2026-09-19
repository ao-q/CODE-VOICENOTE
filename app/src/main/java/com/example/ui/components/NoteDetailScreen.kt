package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimestampMarkerEntity
import com.example.data.model.VoiceNoteEntity
import com.example.player.PlayerState
import com.example.ui.theme.RecorderRed
import com.example.ui.theme.TimestampFlagColor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    note: VoiceNoteEntity,
    markers: List<TimestampMarkerEntity>,
    playerState: PlayerState,
    onBack: () -> Unit,
    onPlayToggle: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onSeekToMarker: (marker: TimestampMarkerEntity) -> Unit,
    onSetSpeed: (speed: Float) -> Unit,
    onSaveContent: (newContent: String) -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit = {},
    onOpenFullScreenEditor: () -> Unit = {},
    onAddTimestamp: (timeMs: Long, formattedTime: String, label: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var contentText by remember(note.id, note.noteContent) { mutableStateOf(note.noteContent) }
    var contentFieldValue by remember(note.id, note.noteContent) {
        mutableStateOf(
            TextFieldValue(
                text = note.noteContent,
                selection = TextRange(note.noteContent.length)
            )
        )
    }
    var isPreviewMode by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isDirty by remember { mutableStateOf(false) }
    var showTimestampsDialog by remember { mutableStateOf(false) }
    var showAddTimestampDialog by remember { mutableStateOf(false) }
    var timestampLabelInput by remember { mutableStateOf("") }
    var pendingTimestampPos by remember { mutableStateOf(0L) }
    var pendingTimestampFormatted by remember { mutableStateOf("00:00") }

    val isThisNotePlaying = playerState.isPlaying && playerState.currentNoteId == note.id
    val effectivePosition = if (playerState.currentNoteId == note.id) playerState.currentPositionMs else 0L
    val effectiveDuration = if (note.durationMs > 0) note.durationMs else playerState.durationMs.coerceAtLeast(1000L)

    val currentFormatted = formatMs(effectivePosition)
    val totalFormatted = formatMs(effectiveDuration)

    val dateFormatted = remember(note.createdAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    // System back handler: Save note if edited and close
    BackHandler {
        if (isDirty) {
            onSaveContent(contentFieldValue.text)
        }
        onBack()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("note_detail_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_detail_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenFullScreenEditor,
                        modifier = Modifier.testTag("detail_open_full_editor_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Open in Full Screen Editor",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.testTag("detail_pin_button")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isDirty) {
                        IconButton(onClick = {
                            onSaveContent(contentText)
                            isDirty = false
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Notes", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    IconButton(onClick = { showMenu = true }, modifier = Modifier.testTag("note_detail_overflow_menu")) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open in Full-Screen Editor") },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onOpenFullScreenEditor()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                            leadingIcon = {
                                Icon(
                                    if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Rename Note") },
                            leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Folder") },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onMove()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            // Audio Player Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Audio scrubber slider
                    val sliderValue = if (effectiveDuration > 0) {
                        (effectivePosition.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = sliderValue,
                        onValueChange = { frac ->
                            val targetMs = (frac * effectiveDuration).toLong()
                            onSeek(targetMs)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = RecorderRed,
                            activeTrackColor = RecorderRed,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Timers: Current and Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = currentFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player Controls Row: Speed, Play/Pause
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed control
                        val nextSpeed = when (playerState.playbackSpeed) {
                            1.0f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                        FilledTonalButton(
                            onClick = { onSetSpeed(nextSpeed) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("${playerState.playbackSpeed}x", fontSize = 12.sp)
                        }

                        // Play / Pause FAB
                        FloatingActionButton(
                            onClick = onPlayToggle,
                            containerColor = RecorderRed,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("detail_play_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isThisNotePlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isThisNotePlaying) "Pause" else "Play",
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Format info pill & alive equalizer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = ".mp3",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isThisNotePlaying) {
                                PlayingEqualizerBars(isPlaying = true)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Flag Timestamp Button at Current Playback Time
                    FilledTonalButton(
                        onClick = {
                            pendingTimestampPos = effectivePosition
                            pendingTimestampFormatted = formatMs(effectivePosition)
                            timestampLabelInput = "Flag at $pendingTimestampFormatted"
                            showAddTimestampDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = TimestampFlagColor.copy(alpha = 0.15f),
                            contentColor = TimestampFlagColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_flag_timestamp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = "Flag Timestamp",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Flag Timestamp at $currentFormatted",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Flagged Timestamps Section
            if (markers.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Meeting Timestamps (tap to jump):",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(markers) { marker ->
                            AssistChip(
                                onClick = { onSeekToMarker(marker) },
                                label = { Text("${marker.formattedTime} - ${marker.label}") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = TimestampFlagColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Write Notes editor section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
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
                                        color = if (!isPreviewMode) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                        modifier = Modifier
                                            .clickable { isPreviewMode = false }
                                            .testTag("detail_notes_edit_tab")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                modifier = Modifier.size(13.dp),
                                                tint = if (!isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Edit",
                                                fontSize = 11.sp,
                                                fontWeight = if (!isPreviewMode) FontWeight.Bold else FontWeight.Medium,
                                                color = if (!isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPreviewMode) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                                        modifier = Modifier
                                            .clickable { isPreviewMode = true }
                                            .testTag("detail_notes_preview_tab")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Visibility,
                                                contentDescription = "Preview",
                                                modifier = Modifier.size(13.dp),
                                                tint = if (isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Preview",
                                                fontSize = 11.sp,
                                                fontWeight = if (isPreviewMode) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isPreviewMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // "Clock and Plus" icon button: shows timestamps that user can add to the note automatically in one click
                            IconButton(
                                onClick = { showTimestampsDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clock_and_plus_timestamps_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreTime,
                                    contentDescription = "Show and Add Timestamps",
                                    tint = TimestampFlagColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            if (isDirty) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Unsaved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isPreviewMode) {
                        // EDIT MODE: Toolbar + OutlinedTextField
                        MarkdownEditorToolbar(
                            value = contentFieldValue,
                            onValueChange = { newValue ->
                                contentFieldValue = newValue
                                contentText = newValue.text
                                isDirty = true
                            },
                            currentTimestampFormatted = currentFormatted,
                            onInsertTimestamp = {
                                val tag = "\n- **[$currentFormatted]** "
                                val updated = insertTextAtCursor(contentFieldValue, tag)
                                contentFieldValue = updated
                                contentText = updated.text
                                isDirty = true
                            },
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        OutlinedTextField(
                            value = contentFieldValue,
                            onValueChange = {
                                contentFieldValue = it
                                contentText = it.text
                                isDirty = true
                            },
                            placeholder = { Text("Write in Markdown, format with toolbar above, or tap clock icon to insert timestamps...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag("detail_note_content_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                    } else {
                        // PREVIEW MODE: Rich Markdown preview with clickable timestamps
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            MarkdownPreview(
                                markdown = contentFieldValue.text,
                                onTimestampClick = { tsStr ->
                                    val ms = parseFormattedTimeToMs(tsStr)
                                    if (ms >= 0) {
                                        onSeek(ms)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Flag Timestamp Dialog once recording is done
    if (showAddTimestampDialog) {
        AlertDialog(
            onDismissRequest = { showAddTimestampDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, tint = TimestampFlagColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Flag Timestamp")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Flag timestamp at $pendingTimestampFormatted for this voice note.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = timestampLabelInput,
                        onValueChange = { timestampLabelInput = it },
                        label = { Text("Label / Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val label = timestampLabelInput.trim().ifBlank { "Point at $pendingTimestampFormatted" }
                        onAddTimestamp(pendingTimestampPos, pendingTimestampFormatted, label)
                        showAddTimestampDialog = false
                    }
                ) {
                    Text("Save Timestamp")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddTimestampDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Timestamps Selection Dialog (Clock and Plus action)
    if (showTimestampsDialog) {
        TimestampsSelectionDialog(
            timestamps = markers.map {
                com.example.data.model.SyncTimestampItem(timeMs = it.timeMs, formattedTime = it.formattedTime, label = it.label)
            },
            onInsertAll = {
                val mdBlock = buildString {
                    append("\n\n### Timestamps\n")
                    markers.forEach { m ->
                        append("- **[${m.formattedTime}]** ${m.label}\n")
                    }
                }
                val newText = contentText + mdBlock
                contentText = newText
                contentFieldValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
                isDirty = true
            },
            onInsertSingle = { item ->
                val newText = contentText + "\n- **[${item.formattedTime}]** ${item.label}"
                contentText = newText
                contentFieldValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
                isDirty = true
            },
            onFlagCurrentTime = {
                val currentPos = effectivePosition
                val formatted = formatMs(currentPos)
                onAddTimestamp(currentPos, formatted, "Point at $formatted")
            },
            onDismiss = { showTimestampsDialog = false }
        )
    }
}

private fun parseFormattedTimeToMs(time: String): Long {
    val parts = time.split(":")
    return if (parts.size == 2) {
        val mins = parts[0].toLongOrNull() ?: 0L
        val secs = parts[1].toLongOrNull() ?: 0L
        (mins * 60 + secs) * 1000L
    } else {
        0L
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
