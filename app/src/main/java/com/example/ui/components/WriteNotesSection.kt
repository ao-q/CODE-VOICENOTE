package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FolderEntity
import com.example.data.model.VoiceNoteEntity
import com.example.player.PlayerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Minimalist Write Notes Section.
 * - Icon-only quick actions for recording and writing.
 * - Folders visible and pinnable.
 * - Emoji-only file sorter (📁, 🎙️, 📝).
 * - Ultra-minimal text counts.
 */
@Composable
fun WriteNotesSection(
    notes: List<VoiceNoteEntity>,
    folders: List<FolderEntity> = emptyList(),
    pinnedFolders: List<FolderEntity> = emptyList(),
    playerState: PlayerState,
    onCreateNewNote: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenNoteInEditor: (VoiceNoteEntity) -> Unit,
    onTogglePin: (VoiceNoteEntity) -> Unit,
    onTogglePinFolder: (FolderEntity) -> Unit = {},
    onPlayToggle: (VoiceNoteEntity) -> Unit,
    onRename: (VoiceNoteEntity) -> Unit,
    onMove: (VoiceNoteEntity) -> Unit,
    onDelete: (VoiceNoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    var selectedFilterIndex by remember { mutableStateOf(0) } // 0 = All, 1 = 🎙️, 2 = 📝
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredNotes = remember(notes, selectedFilterIndex, selectedFolderId, searchQuery) {
        notes.filter { note ->
            val matchesFolder = if (selectedFolderId != null) note.folderId == selectedFolderId else true
            val matchesFilter = when (selectedFilterIndex) {
                1 -> note.audioFilePath.isNotBlank()
                2 -> note.audioFilePath.isBlank()
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.noteContent.contains(searchQuery, ignoreCase = true)
            }
            matchesFolder && matchesFilter && matchesSearch
        }
    }

    val recordingsCount = remember(notes, selectedFolderId) {
        notes.count { (selectedFolderId == null || it.folderId == selectedFolderId) && it.audioFilePath.isNotBlank() }
    }
    val writtenCount = remember(notes, selectedFolderId) {
        notes.count { (selectedFolderId == null || it.folderId == selectedFolderId) && it.audioFilePath.isBlank() }
    }
    val totalCount = remember(notes, selectedFolderId) {
        notes.count { selectedFolderId == null || it.folderId == selectedFolderId }
    }

    // Combine pinned and all folders (pinned first, no duplicates)
    val displayFolders = remember(folders, pinnedFolders) {
        val pinnedIds = pinnedFolders.map { it.id }.toSet()
        val rest = folders.filter { it.id !in pinnedIds }
        pinnedFolders + rest
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("write_notes_section")
    ) {
        // Quick Action Bar: ONLY Icons (No text for record note and write note) + Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Icon-only Record Action Button (Dark Blue gradient)
            FilledIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartRecording()
                },
                modifier = Modifier
                    .size(46.dp)
                    .testTag("write_notes_record_voice_card"),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Icon-only Write Note Action Button
            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCreateNewNote()
                },
                modifier = Modifier
                    .size(46.dp)
                    .testTag("create_new_markdown_note_card"),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Write",
                    modifier = Modifier.size(26.dp)
                )
            }

            // Search input (Minimalist)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("write_notes_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            )
        }

        // Visible Folders row (Pinned folders highlighted with 📌)
        if (displayFolders.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(displayFolders, key = { it.id }) { folder ->
                    val isSelected = selectedFolderId == folder.id
                    val isPinned = folder.isPinned

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedFolderId = if (isSelected) null else folder.id
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                                if (isPinned) {
                                    Text("📌", fontSize = 10.sp)
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onTogglePinFolder(folder)
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (isPinned) "Unpin" else "Pin",
                                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // File sorter chips: ONLY EMOJI (No "Recordings" or "Written" words)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFilterIndex == 0,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedFilterIndex = 0
                },
                label = { Text("📁 $totalCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedFilterIndex == 1,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedFilterIndex = 1
                },
                label = { Text("🎙️ $recordingsCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedFilterIndex == 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedFilterIndex = 2
                },
                label = { Text("📝 $writtenCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            if (selectedFolderId != null) {
                val currentFolderName = displayFolders.find { it.id == selectedFolderId }?.name ?: ""
                AssistChip(
                    onClick = { selectedFolderId = null },
                    label = { Text(currentFolderName, fontSize = 11.sp, maxLines = 1) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // List of Notes
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No results" else "Empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    MarkdownNoteCard(
                        note = note,
                        playerState = playerState,
                        onOpen = { onOpenNoteInEditor(note) },
                        onTogglePin = { onTogglePin(note) },
                        onPlayToggle = { onPlayToggle(note) },
                        onRename = { onRename(note) },
                        onMove = { onMove(note) },
                        onDelete = { onDelete(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownNoteCard(
    note: VoiceNoteEntity,
    playerState: PlayerState,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onPlayToggle: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val hasAudio = note.audioFilePath.isNotBlank()
    val isPlayingThis = playerState.isPlaying && playerState.currentNoteId == note.id

    val wordCount = remember(note.noteContent) {
        if (note.noteContent.isBlank()) 0
        else note.noteContent.trim().split(Regex("\\s+")).size
    }

    val dateFormatted = remember(note.createdAt) {
        val sdf = SimpleDateFormat("MMM dd • h:mm a", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpen()
            }
            .testTag("markdown_note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (note.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: Emoji badge + Pin + Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (hasAudio) "🎙️" else "📝",
                        fontSize = 14.sp
                    )

                    if (note.isPinned) {
                        Text("📌", fontSize = 12.sp)
                    }

                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Pin & Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTogglePin()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin" else "Pin") },
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
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onRename()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move") },
                                leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onMove()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Note Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Content preview (first 2 lines)
            if (note.noteContent.isNotBlank()) {
                Text(
                    text = note.noteContent.trim(),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Footer info + audio mini player
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$wordCount words",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (hasAudio) {
                    FilledTonalIconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayToggle()
                        },
                        modifier = Modifier.size(32.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isPlayingThis) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isPlayingThis) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlayingThis) "Pause" else "Play",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
