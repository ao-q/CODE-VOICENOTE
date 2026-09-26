package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomNoteTag
import com.example.data.model.FolderEntity
import com.example.data.model.VoiceNoteEntity
import com.example.player.PlayerState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Minimalist Write Notes Section.
 * - Icon-only quick actions for recording, writing, and canvas.
 * - Folders visible and pinnable.
 * - Emoji-only file sorter (📁, 🎙️, 📝, 🎨) with customizable '+' emoji categories.
 * - Drawing re-access and editing support.
 * - Compact Minimal Mode layout toggle support.
 */
@Composable
fun WriteNotesSection(
    notes: List<VoiceNoteEntity>,
    folders: List<FolderEntity> = emptyList(),
    pinnedFolders: List<FolderEntity> = emptyList(),
    playerState: PlayerState,
    isMinimalMode: Boolean = false,
    customTags: List<CustomNoteTag> = emptyList(),
    noteTagAssignments: Map<Long, List<String>> = emptyMap(),
    selectedTagId: String? = null,
    onSelectTagFilter: (String?) -> Unit = {},
    onCreateTag: () -> Unit = {},
    onAssignTag: (VoiceNoteEntity) -> Unit = {},
    onCreateNewNote: () -> Unit,
    onStartRecording: () -> Unit,
    onOpenHandwrittenBoard: () -> Unit = {},
    onOpenDrawingModal: (VoiceNoteEntity) -> Unit = {},
    onOpenNoteInEditor: (VoiceNoteEntity) -> Unit,
    onTogglePin: (VoiceNoteEntity) -> Unit,
    onTogglePinFolder: (FolderEntity) -> Unit = {},
    onPlayToggle: (VoiceNoteEntity) -> Unit,
    onRename: (VoiceNoteEntity) -> Unit,
    onMove: (VoiceNoteEntity) -> Unit,
    onDelete: (VoiceNoteEntity) -> Unit,
    onMoveNoteDirectlyToFolder: (Long, Long?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    var selectedFilterIndex by remember { mutableStateOf(0) } // 0 = 📁, 1 = 🎙️, 2 = 📝, 3 = 🎨
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var draggedNote by remember { mutableStateOf<VoiceNoteEntity?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var hoveredFolderId by remember { mutableStateOf<Long?>(null) }
    val folderBounds = remember { mutableMapOf<Long, Rect>() }

    val filteredNotes = remember(notes, selectedFilterIndex, selectedTagId, noteTagAssignments, selectedFolderId, searchQuery) {
        notes.filter { note ->
            val matchesFolder = if (selectedFolderId != null) note.folderId == selectedFolderId else true
            val isDrawing = note.noteContent.contains("![Handwritten Note]") || note.noteFileName.endsWith(".png") || note.noteContent.endsWith(".png")
            val matchesFilter = if (selectedTagId != null) {
                noteTagAssignments[note.id]?.contains(selectedTagId) == true
            } else {
                when (selectedFilterIndex) {
                    1 -> note.audioFilePath.isNotBlank()
                    2 -> note.audioFilePath.isBlank() && !isDrawing
                    3 -> isDrawing
                    else -> true
                }
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
    val drawnCount = remember(notes, selectedFolderId) {
        notes.count { (selectedFolderId == null || it.folderId == selectedFolderId) && (it.noteContent.contains("![Handwritten Note]") || it.noteFileName.endsWith(".png") || it.noteContent.endsWith(".png")) }
    }
    val writtenCount = remember(notes, selectedFolderId) {
        notes.count { (selectedFolderId == null || it.folderId == selectedFolderId) && it.audioFilePath.isBlank() && !(it.noteContent.contains("![Handwritten Note]") || it.noteFileName.endsWith(".png") || it.noteContent.endsWith(".png")) }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("write_notes_section")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Quick Action Bar: ONLY Icons + Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (isMinimalMode) 4.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStartRecording()
                },
                modifier = Modifier
                    .size(if (isMinimalMode) 40.dp else 46.dp)
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
                    modifier = Modifier.size(if (isMinimalMode) 20.dp else 24.dp)
                )
            }

            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCreateNewNote()
                },
                modifier = Modifier
                    .size(if (isMinimalMode) 40.dp else 46.dp)
                    .testTag("create_new_markdown_note_card"),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Write Note",
                    modifier = Modifier.size(if (isMinimalMode) 22.dp else 26.dp)
                )
            }

            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenHandwrittenBoard()
                },
                modifier = Modifier
                    .size(if (isMinimalMode) 40.dp else 46.dp)
                    .testTag("create_new_handwritten_board_card"),
                shape = RoundedCornerShape(14.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = "Handwritten Board",
                    modifier = Modifier.size(if (isMinimalMode) 20.dp else 24.dp)
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search...", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(if (isMinimalMode) 42.dp else 48.dp)
                    .testTag("write_notes_search_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }

        // Visible Folders row
        if (displayFolders.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (isMinimalMode) 2.dp else 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    val isAllHovered = hoveredFolderId == -1L
                    AssistChip(
                        onClick = { selectedFolderId = null },
                        label = { Text("All", fontSize = 11.sp, fontWeight = if (selectedFolderId == null) FontWeight.Bold else FontWeight.Normal) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (isAllHovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else if (selectedFolderId == null) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (isAllHovered) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.onGloballyPositioned { coords ->
                            folderBounds[-1L] = coords.boundsInRoot()
                        }
                    )
                }

                items(displayFolders) { folder ->
                    val isSelected = selectedFolderId == folder.id
                    val isPinned = folder.isPinned
                    val isFolderHovered = hoveredFolderId == folder.id
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isFolderHovered) MaterialTheme.colorScheme.primaryContainer
                        else if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isFolderHovered) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                folderBounds[folder.id] = coords.boundsInRoot()
                            }
                            .clickable {
                                selectedFolderId = if (selectedFolderId == folder.id) null else folder.id
                            }
                            .testTag("folder_chip_${folder.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.FolderOpen else Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = folder.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )

                            if (isPinned) {
                                Text("📌", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // File sorter chips: EMOJIS + Custom Tags + [+] Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = if (isMinimalMode) 2.dp else 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedTagId == null && selectedFilterIndex == 0,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectTagFilter(null)
                    selectedFilterIndex = 0
                },
                label = { Text("📁 $totalCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedTagId == null && selectedFilterIndex == 1,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectTagFilter(null)
                    selectedFilterIndex = 1
                },
                label = { Text("🎙️ $recordingsCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedTagId == null && selectedFilterIndex == 2,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectTagFilter(null)
                    selectedFilterIndex = 2
                },
                label = { Text("📝 $writtenCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = selectedTagId == null && selectedFilterIndex == 3,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelectTagFilter(null)
                    selectedFilterIndex = 3
                },
                label = { Text("🎨 $drawnCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                shape = RoundedCornerShape(12.dp)
            )

            // Custom Tags Chips
            for (tag in customTags) {
                val tagNotesCount = remember(notes, noteTagAssignments, selectedFolderId, tag.id) {
                    notes.count { (selectedFolderId == null || it.folderId == selectedFolderId) && (noteTagAssignments[it.id]?.contains(tag.id) == true) }
                }
                val isTagSelected = selectedTagId == tag.id
                FilterChip(
                    selected = isTagSelected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (isTagSelected) {
                            onSelectTagFilter(null)
                        } else {
                            onSelectTagFilter(tag.id)
                        }
                    },
                    label = { Text("${tag.emoji} $tagNotesCount", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // [+] Add Tag Category Button
            FilledTonalIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCreateTag()
                },
                modifier = Modifier
                    .size(32.dp)
                    .testTag("create_custom_tag_chip"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "New Tag",
                    modifier = Modifier.size(16.dp)
                )
            }

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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Text(
                        text = if (searchQuery.isNotBlank()) "No notes match search" else "No notes here yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("write_notes_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = if (isMinimalMode) 4.dp else 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (isMinimalMode) 4.dp else 8.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    val isDrawing = note.noteContent.contains("![Handwritten Note]") || note.noteFileName.endsWith(".png") || note.noteContent.endsWith(".png")
                    val assignedTags = remember(note.id, noteTagAssignments, customTags) {
                        val tagIds = noteTagAssignments[note.id] ?: emptyList()
                        customTags.filter { it.id in tagIds }
                    }

                    if (isMinimalMode) {
                        CompactNoteItemCard(
                            note = note,
                            isDrawing = isDrawing,
                            assignedTags = assignedTags,
                            playerState = playerState,
                            onOpen = {
                                if (isDrawing) onOpenDrawingModal(note) else onOpenNoteInEditor(note)
                            },
                            onTogglePin = { onTogglePin(note) },
                            onPlayToggle = { onPlayToggle(note) },
                            onAssignTag = { onAssignTag(note) },
                            onRename = { onRename(note) },
                            onMove = { onMove(note) },
                            onDelete = { onDelete(note) }
                        )
                    } else {
                        val isDraggingThis = draggedNote?.id == note.id
                        NoteItemCard(
                            note = note,
                            isDrawing = isDrawing,
                            assignedTags = assignedTags,
                            playerState = playerState,
                            isDragging = isDraggingThis,
                            onDragStart = { offset ->
                                draggedNote = note
                                dragPosition = offset
                            },
                            onDrag = { dragAmount ->
                                dragPosition += dragAmount
                                val hovered = folderBounds.entries.find { it.value.contains(dragPosition) }?.key
                                if (hovered != hoveredFolderId) {
                                    hoveredFolderId = hovered
                                    if (hovered != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                            onDragEnd = {
                                val targetFolder = hoveredFolderId
                                val noteToMove = draggedNote
                                if (noteToMove != null && targetFolder != null) {
                                    val resolvedTarget = if (targetFolder == -1L) null else targetFolder
                                    if (noteToMove.folderId != resolvedTarget) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onMoveNoteDirectlyToFolder(noteToMove.id, resolvedTarget)
                                    }
                                }
                                draggedNote = null
                                hoveredFolderId = null
                            },
                            onDragCancel = {
                                draggedNote = null
                                hoveredFolderId = null
                            },
                            onOpen = {
                                if (isDrawing) onOpenDrawingModal(note) else onOpenNoteInEditor(note)
                            },
                            onTogglePin = { onTogglePin(note) },
                            onPlayToggle = { onPlayToggle(note) },
                            onAssignTag = { onAssignTag(note) },
                            onRename = { onRename(note) },
                            onMove = { onMove(note) },
                            onDelete = { onDelete(note) }
                        )
                    }
                }
            }
        }
    }

    // Floating drag preview badge
    if (draggedNote != null) {
        Surface(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (dragPosition.x - 80.dp.toPx()).toInt().coerceAtLeast(16),
                        (dragPosition.y - 30.dp.toPx()).toInt().coerceAtLeast(16)
                    )
                }
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        draggedNote!!.noteContent.contains("![Handwritten Note]") || draggedNote!!.noteFileName.endsWith(".png") -> Icons.Default.Brush
                        draggedNote!!.audioFilePath.isBlank() -> Icons.Default.EditNote
                        else -> Icons.Default.Mic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = draggedNote!!.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                if (hoveredFolderId != null) {
                    val targetName = if (hoveredFolderId == -1L) "All Notes"
                    else folders.find { it.id == hoveredFolderId }?.name
                        ?: pinnedFolders.find { it.id == hoveredFolderId }?.name
                        ?: "Folder"
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "➔ Drop in $targetName",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Standard Spacious Note Card.
 */
@Composable
private fun NoteItemCard(
    note: VoiceNoteEntity,
    isDrawing: Boolean,
    assignedTags: List<CustomNoteTag>,
    playerState: PlayerState,
    isDragging: Boolean = false,
    onDragStart: (Offset) -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onPlayToggle: () -> Unit,
    onAssignTag: () -> Unit,
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
        if (note.noteContent.isBlank() || isDrawing) 0
        else note.noteContent.trim().split(Regex("\\s+")).size
    }

    val dateFormatted = remember(note.createdAt) {
        val sdf = SimpleDateFormat("MMM dd • h:mm a", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(note.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDragStart(offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpen()
            }
            .testTag("markdown_note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            if (isDragging) 1.5.dp else 1.dp,
            if (isDragging || note.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
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
                        text = if (hasAudio) "🎙️" else if (isDrawing) "🎨" else "📝",
                        fontSize = 14.sp
                    )

                    // Assigned custom emoji tags
                    for (tag in assignedTags) {
                        Text(text = tag.emoji, fontSize = 13.sp)
                    }

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
                                text = { Text("Tag") },
                                leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onAssignTag()
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

            // Content preview (first 2 lines) - not for drawing notes
            if (!isDrawing && note.noteContent.isNotBlank()) {
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
                    text = if (isDrawing) "Drawing" else "$wordCount words",
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

/**
 * Compact Note Card for Densely Packed Minimal Mode.
 */
@Composable
private fun CompactNoteItemCard(
    note: VoiceNoteEntity,
    isDrawing: Boolean,
    assignedTags: List<CustomNoteTag>,
    playerState: PlayerState,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onPlayToggle: () -> Unit,
    onAssignTag: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }

    val hasAudio = note.audioFilePath.isNotBlank()
    val isPlayingThis = playerState.isPlaying && playerState.currentNoteId == note.id

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onOpen()
            }
            .testTag("compact_note_card_${note.id}"),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (note.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type emoji
            Text(
                text = if (hasAudio) "🎙️" else if (isDrawing) "🎨" else "📝",
                fontSize = 13.sp
            )

            // Assigned tags emojis
            for (tag in assignedTags) {
                Text(text = tag.emoji, fontSize = 12.sp)
            }

            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (note.isPinned) {
                Text("📌", fontSize = 10.sp)
            }

            if (hasAudio) {
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayToggle()
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlayingThis) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(14.dp)
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
                        text = { Text("Tag") },
                        leadingIcon = { Icon(Icons.Default.Sell, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onAssignTag()
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
}
