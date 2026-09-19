package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoiceNoteEntity
import com.example.player.PlayerState
import com.example.ui.theme.RecorderRed
import com.example.ui.theme.TimestampFlagColor
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-Screen Responsive Markdown Text Editor.
 *
 * Supports:
 * - Direct editing of written notes and notes associated with recordings.
 * - Real-time Markdown preview (segmented toggle on mobile, side-by-side split on tablets).
 * - Integrated Audio Player banner for voice notes with one-tap timestamp insertion.
 * - Debounced auto-save directly to the user's storage directory (.md file).
 * - Live word count, character count, and file location badge.
 * - Tactile haptic feedback and micro-interactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMarkdownEditor(
    note: VoiceNoteEntity?,
    isNewNote: Boolean,
    playerState: PlayerState,
    onBack: () -> Unit,
    onSave: (noteId: Long?, title: String, content: String, isPinned: Boolean) -> Unit,
    onDelete: () -> Unit,
    onPlayToggle: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onSetSpeed: (speed: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var title by remember(note?.id) { mutableStateOf(note?.title ?: "Untitled Note") }
    var contentValue by remember(note?.id) {
        val initialText = note?.noteContent ?: ""
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    var isPinned by remember(note?.id) { mutableStateOf(note?.isPinned ?: false) }
    var selectedViewMode by remember { mutableStateOf(0) } // 0 = Edit, 1 = Preview
    var isDirty by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val hasAudio = note != null && note.audioFilePath.isNotBlank()
    val isPlayingThis = playerState.isPlaying && playerState.currentNoteId == note?.id
    val effectivePos = if (playerState.currentNoteId == note?.id) playerState.currentPositionMs else 0L
    val effectiveDur = if ((note?.durationMs ?: 0L) > 0L) note!!.durationMs else playerState.durationMs.coerceAtLeast(1000L)

    val currentTimestampFormatted = remember(effectivePos) {
        val totalSec = effectivePos / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        String.format("%02d:%02d", mins, secs)
    }

    val totalDurationFormatted = remember(effectiveDur) {
        val totalSec = effectiveDur / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        String.format("%02d:%02d", mins, secs)
    }

    val wordCount = remember(contentValue.text) {
        if (contentValue.text.isBlank()) 0
        else contentValue.text.trim().split(Regex("\\s+")).size
    }
    val charCount = remember(contentValue.text) { contentValue.text.length }

    // Auto-save debounce effect (saves 1.8s after the user stops typing)
    LaunchedEffect(title, contentValue.text, isPinned) {
        if (isDirty) {
            delay(1800)
            isSaving = true
            onSave(note?.id, title, contentValue.text, isPinned)
            delay(300)
            isSaving = false
            isDirty = false
        }
    }

    // System back handler: Save changes if dirty and dismiss
    BackHandler {
        if (isDirty) {
            onSave(note?.id, title, contentValue.text, isPinned)
        }
        onBack()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("full_screen_markdown_editor"),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTabletOrWide = maxWidth >= 720.dp

            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Column {
                            BasicTextField(
                                value = title,
                                onValueChange = {
                                    title = it
                                    isDirty = true
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    if (title.isBlank()) {
                                        Text(
                                            text = "Note Title...",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_title_input")
                            )

                            // Status pill: Auto-saved indicator or dirty state
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSaving) {
                                    Text(
                                        text = "Saving...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (isDirty) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE65100))
                                    )
                                    Text(
                                        text = "Unsaved changes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE65100)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Saved to storage (.md)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (isDirty) {
                                    onSave(note?.id, title, contentValue.text, isPinned)
                                }
                                onBack()
                            },
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // View mode toggle on mobile
                        if (!isTabletOrWide) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                SegmentedButton(
                                    selected = selectedViewMode == 0,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedViewMode = 0
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text("Edit", fontSize = 12.sp)
                                }
                                SegmentedButton(
                                    selected = selectedViewMode == 1,
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedViewMode = 1
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("Preview", fontSize = 12.sp)
                                }
                            }
                        }

                        // Pin toggle button
                        IconButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                isPinned = !isPinned
                                isDirty = true
                            },
                            modifier = Modifier.testTag("editor_pin_button")
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) "Unpin" else "Pin",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Explicit Save Button
                        if (isDirty) {
                            FilledTonalButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSave(note?.id, title, contentValue.text, isPinned)
                                    isDirty = false
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("editor_save_button")
                            ) {
                                Text("Save", fontSize = 12.sp)
                            }
                        }

                        // Overflow Menu
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("editor_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share Note (Markdown)") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TITLE, title)
                                        putExtra(Intent.EXTRA_SUBJECT, title)
                                        putExtra(Intent.EXTRA_TEXT, "# $title\n\n${contentValue.text}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Markdown Note"))
                                }
                            )

                            if (hasAudio) {
                                DropdownMenuItem(
                                    text = { Text("Insert Audio Timestamp") },
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = TimestampFlagColor) },
                                    onClick = {
                                        showMenu = false
                                        val tag = "\n- **[$currentTimestampFormatted]** "
                                        contentValue = insertTextAtCursor(contentValue, tag)
                                        isDirty = true
                                    }
                                )
                            }

                            if (!isNewNote) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Associated Voice Recording Player Banner (if this note has audio)
                if (hasAudio) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPlayToggle()
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(RecorderRed)
                                            .testTag("editor_audio_play_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingThis) "Pause Recording" else "Play Recording",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Voice Recording Audio",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                            )
                                            if (isPlayingThis) {
                                                PlayingEqualizerBars(isPlaying = true)
                                            }
                                        }
                                        Text(
                                            text = "$currentTimestampFormatted / $totalDurationFormatted",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Quick Insert Timestamp Chip
                                AssistChip(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val tag = "\n- **[$currentTimestampFormatted]** "
                                        contentValue = insertTextAtCursor(contentValue, tag)
                                        isDirty = true
                                    },
                                    label = { Text("+$currentTimestampFormatted", fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = TimestampFlagColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("insert_current_audio_time_chip")
                                )
                            }

                            // Audio Slider
                            val sliderFraction = if (effectiveDur > 0) {
                                (effectivePos.toFloat() / effectiveDur.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Slider(
                                value = sliderFraction,
                                onValueChange = { frac ->
                                    val targetMs = (frac * effectiveDur).toLong()
                                    onSeek(targetMs)
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = RecorderRed,
                                    activeTrackColor = RecorderRed
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                            )
                        }
                    }
                }

                // Markdown Formatting Toolbar (B, I, S, H1, H2, List, Checkbox, Quote, Code, Timestamp)
                MarkdownEditorToolbar(
                    value = contentValue,
                    onValueChange = {
                        contentValue = it
                        isDirty = true
                    },
                    currentTimestampFormatted = if (hasAudio) currentTimestampFormatted else null,
                    onInsertTimestamp = if (hasAudio) {
                        {
                            val tag = "\n- **[$currentTimestampFormatted]** "
                            contentValue = insertTextAtCursor(contentValue, tag)
                            isDirty = true
                        }
                    } else null,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Main Content Canvas (Split on Tablet / Large screen, Single mode on Phone)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (isTabletOrWide) {
                        // Wide Screen: Side-by-side Editor & Live Preview
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Left Pane: Editor
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                EditorTextField(
                                    value = contentValue,
                                    onValueChange = {
                                        contentValue = it
                                        isDirty = true
                                    }
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier.fillMaxHeight(),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // Right Pane: Live Markdown Preview
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                            ) {
                                MarkdownPreview(
                                    markdown = contentValue.text,
                                    onTimestampClick = { timeTag ->
                                        // Seek if time tag clicked
                                        parseTimestampMs(timeTag)?.let { ms -> onSeek(ms) }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        // Phone: Toggle between Edit and Preview
                        if (selectedViewMode == 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                EditorTextField(
                                    value = contentValue,
                                    onValueChange = {
                                        contentValue = it
                                        isDirty = true
                                    }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                MarkdownPreview(
                                    markdown = contentValue.text,
                                    onTimestampClick = { timeTag ->
                                        parseTimestampMs(timeTag)?.let { ms -> onSeek(ms) }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Bottom Metadata Info Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount words • $charCount chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val location = note?.noteFilePath ?: "Storage > .md"
                        Text(
                            text = location.substringAfterLast("/"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Markdown Note?") },
            text = { Text("Are you sure you want to delete '$title'? This will permanently remove the note and its file from storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EditorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 24.sp,
                fontFamily = FontFamily.SansSerif
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = "Start writing your markdown note here...\n\nUse the toolbar above for formatting: bold, italic, headings, lists, checklists, and code.",
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("markdown_editor_text_area")
        )
    }
}

private fun parseTimestampMs(timeTag: String): Long? {
    val clean = timeTag.replace("[", "").replace("]", "").trim()
    val parts = clean.split(":")
    if (parts.size == 2) {
        val m = parts[0].toLongOrNull() ?: return null
        val s = parts[1].toLongOrNull() ?: return null
        return (m * 60 + s) * 1000L
    }
    return null
}
