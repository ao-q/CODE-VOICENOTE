package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FolderEntity
import com.example.data.model.VoiceNoteEntity
import com.example.player.PlayerState
import com.example.service.RecordingSessionState
import com.example.ui.VoiceNotesViewModel
import com.example.ui.components.ActiveRecordingPane
import com.example.ui.components.AudioPlayerBottomBar
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.MoveNoteDialog
import com.example.ui.components.NewFolderDialog
import com.example.ui.components.NoteDetailScreen
import com.example.ui.components.RenameFolderDialog
import com.example.ui.components.RenameNoteDialog
import com.example.ui.components.StorageConfigOverlay
import com.example.ui.components.SyncKeysDialog
import com.example.ui.theme.RecorderRed
import com.example.ui.theme.TimestampFlagColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVoiceNotesScreen(
    viewModel: VoiceNotesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.currentFolders.collectAsStateWithLifecycle()
    val voiceNotes by viewModel.currentVoiceNotes.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingSessionState.collectAsStateWithLifecycle()
    val detailMarkers by viewModel.detailMarkers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showTopMenu by remember { mutableStateOf(false) }

    // Permission request handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordGranted) {
            viewModel.startRecording()
        }
    }

    fun checkAndStartRecording() {
        val hasAudio = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val permissionsToAsk = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToAsk.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (hasAudio) {
            viewModel.startRecording()
        } else {
            permissionLauncher.launch(permissionsToAsk.toTypedArray())
        }
    }

    // Status message snackbar
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // First launch or user triggered Storage Configuration Overlay
    if (uiState.showStorageConfigDialog) {
        StorageConfigOverlay(
            currentFolderName = uiState.storageBaseDirectoryDisplay.substringAfterLast("/", "VoiceNotes"),
            currentBasePath = uiState.storageBaseDirectoryDisplay,
            isFirstLaunch = !uiState.isStorageConfigured,
            onConfirm = { folderName, path, treeUri ->
                viewModel.configureStorage(folderName, path, treeUri)
            },
            onDismiss = {
                viewModel.dismissStorageConfigDialog()
            }
        )
    }

    // Full screen Active Recording View with "Write Notes" side section
    if (recordingState.isRecording) {
        ActiveRecordingPane(
            sessionState = recordingState,
            titleFlow = viewModel.liveRecordingTitle,
            notesFlow = viewModel.liveRecordingNotes,
            onTitleChange = { viewModel.liveRecordingTitle.value = it },
            onNotesChange = { viewModel.liveRecordingNotes.value = it },
            onPause = { viewModel.pauseRecording() },
            onResume = { viewModel.resumeRecording() },
            onFlagTimestamp = { viewModel.flagTimestamp(it) },
            onStopAndSave = { viewModel.stopAndSaveRecording() },
            onCancel = { viewModel.cancelRecording() }
        )
        return
    }

    // Full Note Detail Screen
    if (uiState.isDetailViewOpen && uiState.selectedDetailNote != null) {
        NoteDetailScreen(
            note = uiState.selectedDetailNote!!,
            markers = detailMarkers,
            playerState = playerState,
            onBack = { viewModel.closeNoteDetail() },
            onPlayToggle = { viewModel.togglePlayNote(uiState.selectedDetailNote!!) },
            onSeek = { viewModel.audioPlayerManager.seekTo(it) },
            onSeekToMarker = { viewModel.seekToMarker(it) },
            onSetSpeed = { viewModel.audioPlayerManager.setSpeed(it) },
            onSaveContent = { viewModel.updateNoteContent(uiState.selectedDetailNote!!.id, it) },
            onRename = { viewModel.openRenameNoteDialog(uiState.selectedDetailNote!!) },
            onSyncKeys = { viewModel.openSyncKeysForNote(uiState.selectedDetailNote!!) },
            onMove = { viewModel.openMoveNoteDialog(uiState.selectedDetailNote!!) },
            onDelete = { viewModel.confirmDeleteNote(uiState.selectedDetailNote!!) }
        )
        return
    }

    // Dialogs
    if (uiState.showSyncKeysDialog) {
        SyncKeysDialog(
            payload = uiState.activeSyncKeyPayload,
            onImportSyncKey = { rawJson ->
                viewModel.importSyncKeyString(rawJson)
            },
            onDismiss = { viewModel.dismissSyncKeysDialog() }
        )
    }

    if (uiState.showRenameNoteDialog && uiState.noteToRename != null) {
        RenameNoteDialog(
            note = uiState.noteToRename!!,
            onConfirmRename = { newTitle ->
                viewModel.renameVoiceNote(newTitle)
            },
            onDismiss = { viewModel.dismissRenameNoteDialog() }
        )
    }

    if (uiState.showMoveNoteDialog && uiState.noteToMove != null) {
        MoveNoteDialog(
            note = uiState.noteToMove!!,
            allFolders = allFolders,
            onConfirmMove = { targetId ->
                viewModel.moveNoteToFolder(targetId)
            },
            onDismiss = { viewModel.dismissMoveNoteDialog() }
        )
    }

    if (uiState.showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { name -> viewModel.createFolder(name) },
            onDismiss = { viewModel.dismissNewFolderDialog() }
        )
    }

    if (uiState.showRenameFolderDialog && uiState.folderToRename != null) {
        RenameFolderDialog(
            folder = uiState.folderToRename!!,
            onConfirm = { newName -> viewModel.renameFolder(newName) },
            onDismiss = { viewModel.dismissRenameFolderDialog() }
        )
    }

    if (uiState.showDeleteConfirmDialog) {
        val deleteTitle = if (uiState.noteToDelete != null) "Delete Voice Note?" else "Delete Folder?"
        val deleteMsg = if (uiState.noteToDelete != null) {
            "This will delete '${uiState.noteToDelete?.title}' (.mp3, .txt notes, and sync_keys.json) from device storage."
        } else {
            "This will permanently delete '${uiState.folderToDelete?.name}' and all subfolders and notes inside."
        }
        DeleteConfirmDialog(
            title = deleteTitle,
            message = deleteMsg,
            onConfirm = {
                if (uiState.noteToDelete != null) {
                    viewModel.deleteVoiceNote()
                } else if (uiState.folderToDelete != null) {
                    viewModel.deleteFolder()
                }
            },
            onDismiss = { viewModel.dismissDeleteDialog() }
        )
    }

    // Active playing note for bottom mini player
    val playingNote = voiceNotes.find { it.id == playerState.currentNoteId }
        ?: allFolders.flatMap { emptyList<VoiceNoteEntity>() }.find { it.id == playerState.currentNoteId }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Voice Notes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        // Breadcrumbs path
                        BreadcrumbsBar(
                            breadcrumbs = uiState.breadcrumbs,
                            onNavigateTo = { folderId -> viewModel.navigateToFolder(folderId) }
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.currentFolderId != null) {
                        IconButton(
                            onClick = { viewModel.navigateUp() },
                            modifier = Modifier.testTag("navigate_up_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate Up")
                        }
                    }
                },
                actions = {
                    // Storage indicator button
                    IconButton(
                        onClick = { viewModel.openStorageConfigDialog() },
                        modifier = Modifier.testTag("open_storage_config_button")
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = "Configure Storage")
                    }

                    // 3-dots overflow menu
                    IconButton(
                        onClick = { showTopMenu = true },
                        modifier = Modifier.testTag("main_overflow_menu_button")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                    }

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sync Keys") },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = TimestampFlagColor) },
                            onClick = {
                                showTopMenu = false
                                viewModel.openSyncKeysGeneral()
                            },
                            modifier = Modifier.testTag("menu_sync_keys")
                        )
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                viewModel.openNewFolderDialog()
                            },
                            modifier = Modifier.testTag("menu_new_folder")
                        )
                        DropdownMenuItem(
                            text = { Text("Configure Local Storage") },
                            leadingIcon = { Icon(Icons.Default.Storage, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                viewModel.openStorageConfigDialog()
                            },
                            modifier = Modifier.testTag("menu_storage_config")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            // Prominent Stock Android / Pixel Recorder FAB
            ExtendedFloatingActionButton(
                onClick = { checkAndStartRecording() },
                containerColor = RecorderRed,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record Voice Note",
                        modifier = Modifier.size(26.dp)
                    )
                },
                text = {
                    Text(
                        text = "Record",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                modifier = Modifier
                    .padding(bottom = if (playerState.isPlaying) 60.dp else 8.dp)
                    .testTag("start_recording_fab")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (playerState.isPlaying && playingNote != null) {
                AudioPlayerBottomBar(
                    playingNote = playingNote,
                    playerState = playerState,
                    onTogglePlay = { viewModel.togglePlayNote(playingNote) },
                    onOpenDetail = { viewModel.openNoteDetail(it) },
                    onClosePlayer = { viewModel.audioPlayerManager.stop() }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search voice notes & written notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.clearSearch() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_voice_notes_input")
            )

            // Device Storage Location Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { viewModel.openStorageConfigDialog() }
                    .testTag("storage_location_banner"),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Device Storage",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.friendlyStoragePath.isNotBlank()) uiState.friendlyStoragePath else "Internal Storage > Documents > VoiceNotes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Content List (Folders + Notes)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Section: Nested Folders (Folder in folder ability)
                if (folders.isNotEmpty() && uiState.searchQuery.isBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Folders",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AssistChip(
                                onClick = { viewModel.openNewFolderDialog() },
                                label = { Text("+ Folder", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(folders) { folder ->
                                FolderChipCard(
                                    folder = folder,
                                    onClick = { viewModel.navigateToFolder(folder.id) },
                                    onRename = { viewModel.openRenameFolderDialog(folder) },
                                    onDelete = { viewModel.confirmDeleteFolder(folder) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Section: Voice Notes Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) "Search Results" else "Voice Notes (${voiceNotes.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (folders.isEmpty() && uiState.searchQuery.isBlank()) {
                            AssistChip(
                                onClick = { viewModel.openNewFolderDialog() },
                                label = { Text("+ Folder", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                // Empty State
                if (voiceNotes.isEmpty()) {
                    item {
                        EmptyNotesView(
                            isSearching = uiState.searchQuery.isNotBlank(),
                            hasFolders = folders.isNotEmpty(),
                            onNewFolder = { viewModel.openNewFolderDialog() },
                            onRecord = { checkAndStartRecording() }
                        )
                    }
                } else {
                    // Voice Notes Items
                    items(voiceNotes, key = { it.id }) { note ->
                        VoiceNoteListItem(
                            note = note,
                            isPlaying = playerState.isPlaying && playerState.currentNoteId == note.id,
                            onClick = { viewModel.openNoteDetail(note) },
                            onTogglePlay = { viewModel.togglePlayNote(note) },
                            onRename = { viewModel.openRenameNoteDialog(note) },
                            onSyncKeys = { viewModel.openSyncKeysForNote(note) },
                            onMove = { viewModel.openMoveNoteDialog(note) },
                            onDelete = { viewModel.confirmDeleteNote(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BreadcrumbsBar(
    breadcrumbs: List<FolderEntity>,
    onNavigateTo: (folderId: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = 2.dp)
    ) {
        // Root crumb
        Text(
            text = "Voice Notes",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (breadcrumbs.isEmpty()) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (breadcrumbs.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onNavigateTo(null) }
        )

        breadcrumbs.forEachIndexed { index, crumb ->
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            val isLast = index == breadcrumbs.size - 1
            Text(
                text = crumb.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                ),
                color = if (isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { onNavigateTo(crumb.id) }
            )
        }
    }
}

@Composable
fun FolderChipCard(
    folder: FolderEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .clickable { onClick() }
            .testTag("folder_item_${folder.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Folder options",
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename Folder") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
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
            }
        }
    }
}

@Composable
fun VoiceNoteListItem(
    note: VoiceNoteEntity,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onTogglePlay: () -> Unit,
    onRename: () -> Unit,
    onSyncKeys: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val formattedDuration = remember(note.durationMs) {
        val totalSec = note.durationMs / 1000
        val mins = totalSec / 60
        val secs = totalSec % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    val formattedDate = remember(note.createdAt) {
        val sdf = SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault())
        sdf.format(Date(note.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("voice_note_item_${note.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause round button
            FilledTonalIconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("play_button_note_${note.id}")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) RecorderRed else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Note Info & Written Notes preview
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Written Notes Preview
                if (note.noteContent.isNotBlank()) {
                    Text(
                        text = note.noteContent,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                }

                // Metadata: Duration, .mp3 badge, Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = ".mp3",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3-dots Menu for this Voice Note
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("note_menu_${note.id}")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Sync Keys") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = TimestampFlagColor) },
                        onClick = {
                            showMenu = false
                            onSyncKeys()
                        },
                        modifier = Modifier.testTag("menu_item_sync_keys")
                    )
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        modifier = Modifier.testTag("menu_item_rename")
                    )
                    DropdownMenuItem(
                        text = { Text("Move to Folder") },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onMove()
                        },
                        modifier = Modifier.testTag("menu_item_move")
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        modifier = Modifier.testTag("menu_item_delete")
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyNotesView(
    isSearching: Boolean,
    hasFolders: Boolean,
    onNewFolder: () -> Unit,
    onRecord: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (isSearching) "No matching notes found" else "No voice notes yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isSearching) "Try searching for a different keyword or check spelling."
            else "Tap the Record button to record high-clarity voice notes and take notes simultaneously.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}
