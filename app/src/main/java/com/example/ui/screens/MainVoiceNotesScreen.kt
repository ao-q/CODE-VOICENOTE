package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.service.FloatingRecordingOverlayManager
import com.example.service.RecordingSessionState
import com.example.ui.MainTab
import com.example.ui.VoiceNotesViewModel
import com.example.ui.components.ActiveRecordingPane
import com.example.ui.components.AudioPlayerBottomBar
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyCanvasView
import com.example.ui.components.FullScreenMarkdownEditor
import com.example.ui.components.MoveNoteDialog
import com.example.ui.components.NewFolderDialog
import com.example.ui.components.NoteDetailScreen
import com.example.ui.components.PlayingEqualizerBars
import com.example.ui.components.RenameFolderDialog
import com.example.ui.components.RenameNoteDialog
import com.example.ui.components.StorageConfigOverlay
import com.example.ui.components.WriteNoteDialog
import com.example.ui.components.WriteNotesSection
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
    val haptics = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val folders by viewModel.currentFolders.collectAsStateWithLifecycle()
    val voiceNotes by viewModel.currentVoiceNotes.collectAsStateWithLifecycle()
    val allFolders by viewModel.allFolders.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val pinnedNotes by viewModel.pinnedNotes.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val recordingState by viewModel.recordingSessionState.collectAsStateWithLifecycle()
    val detailMarkers by viewModel.detailMarkers.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showTopMenu by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Permission request handling
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordGranted) {
            if (!FloatingRecordingOverlayManager.canDrawOverlays(context)) {
                showOverlayPermissionDialog = true
            } else {
                viewModel.startRecording()
            }
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
            if (!FloatingRecordingOverlayManager.canDrawOverlays(context)) {
                showOverlayPermissionDialog = true
            } else {
                viewModel.startRecording()
            }
        } else {
            permissionLauncher.launch(permissionsToAsk.toTypedArray())
        }
    }

    // Overlay Permission Dialog for floating recording controls
    if (showOverlayPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showOverlayPermissionDialog = false
                viewModel.startRecording()
            },
            icon = {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text("Enable Floating Window?")
            },
            text = {
                Text(
                    "Voice Notes can display floating recording controls (Timer, Pause, Add Timestamp, Stop & Save) over other apps when you exit the app during recording.\n\nGrant the 'Display over other apps' permission to enable floating controls."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOverlayPermissionDialog = false
                        FloatingRecordingOverlayManager.requestOverlayPermission(context)
                        viewModel.startRecording()
                    }
                ) {
                    Text("Enable in Settings")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showOverlayPermissionDialog = false
                        viewModel.startRecording()
                    }
                ) {
                    Text("Continue Without")
                }
            }
        )
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

    // Full Screen Markdown Editor
    if (uiState.isFullScreenEditorOpen) {
        FullScreenMarkdownEditor(
            note = uiState.fullScreenEditorNote,
            isNewNote = uiState.isNewNoteMode,
            playerState = playerState,
            onBack = { viewModel.closeFullScreenEditor() },
            onSave = { noteId, title, content, isPinned ->
                viewModel.saveFullScreenNote(noteId, title, content, isPinned)
            },
            onDelete = {
                viewModel.deleteCurrentFullScreenNote()
            },
            onPlayToggle = {
                uiState.fullScreenEditorNote?.let { viewModel.togglePlayNote(it) }
            },
            onSeek = { viewModel.audioPlayerManager.seekTo(it) },
            onSetSpeed = { viewModel.audioPlayerManager.setSpeed(it) }
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
            onMove = { viewModel.openMoveNoteDialog(uiState.selectedDetailNote!!) },
            onDelete = { viewModel.confirmDeleteNote(uiState.selectedDetailNote!!) },
            onTogglePin = { viewModel.togglePinNote(uiState.selectedDetailNote!!) },
            onOpenFullScreenEditor = { viewModel.openFullScreenEditorForNote(uiState.selectedDetailNote!!) },
            onAddTimestamp = { timeMs, _, label ->
                viewModel.addMarkerToCurrentNote(uiState.selectedDetailNote!!.id, timeMs, label)
            }
        )
        return
    }

    // Dialogs
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
            "This will delete '${uiState.noteToDelete?.title}' (.mp3 and .md notes) from device storage."
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

    if (uiState.showWriteNoteDialog) {
        WriteNoteDialog(
            onConfirm = { title, content ->
                viewModel.createWrittenNote(title, content)
            },
            onDismiss = {
                viewModel.dismissWriteNoteDialog()
            }
        )
    }

    // BackHandler for intuitive Android back-button behavior
    BackHandler(enabled = uiState.currentFolderId != null || isSearchVisible || uiState.searchQuery.isNotBlank()) {
        when {
            uiState.searchQuery.isNotBlank() -> viewModel.clearSearch()
            isSearchVisible -> isSearchVisible = false
            uiState.currentFolderId != null -> viewModel.navigateUp()
        }
    }

    // Active playing note for bottom mini player (search in current notes, then fallback to all notes)
    val playingNote = voiceNotes.find { it.id == playerState.currentNoteId }
        ?: allNotes.find { it.id == playerState.currentNoteId }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.breadcrumbs.isEmpty()) {
                        VoiceNotesTitleWithRedDot()
                    } else {
                        Column {
                            Text(
                                text = uiState.breadcrumbs.last().name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            BreadcrumbsBar(
                                breadcrumbs = uiState.breadcrumbs,
                                onNavigateTo = { folderId -> viewModel.navigateToFolder(folderId) }
                            )
                        }
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
                    // Search toggle button
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Notes")
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
                            text = { Text("Write Note") },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                            onClick = {
                                showTopMenu = false
                                viewModel.openWriteNoteDialog()
                            },
                            modifier = Modifier.testTag("menu_write_note")
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
            // Prominent Stock Android / Pixel Recorder FAB: Simple Red Circular Mic
            FloatingActionButton(
                onClick = { checkAndStartRecording() },
                containerColor = RecorderRed,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .size(64.dp)
                    .testTag("start_recording_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record Voice Note",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                if (playerState.isPlaying && playingNote != null) {
                    AudioPlayerBottomBar(
                        playingNote = playingNote,
                        playerState = playerState,
                        onTogglePlay = { viewModel.togglePlayNote(playingNote) },
                        onOpenDetail = { viewModel.openNoteDetail(it) },
                        onClosePlayer = { viewModel.audioPlayerManager.stop() }
                    )
                }

                // Responsive 3-tab Bottom Navigation Bar: Home, Pins, Write Notes
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.testTag("main_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.HOME,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(MainTab.HOME)
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        modifier = Modifier.testTag("tab_home")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.PINS,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(MainTab.PINS)
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (pinnedNotes.isNotEmpty()) {
                                        Badge(
                                            containerColor = RecorderRed,
                                            contentColor = Color.White
                                        ) {
                                            Text("${pinnedNotes.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PushPin, contentDescription = "Pins")
                            }
                        },
                        label = { Text("Pins") },
                        modifier = Modifier.testTag("tab_pins")
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == MainTab.WRITE,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.selectTab(MainTab.WRITE)
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (allNotes.isNotEmpty()) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ) {
                                            Text("${allNotes.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.EditNote, contentDescription = "Write Notes")
                            }
                        },
                        label = { Text("Write Notes") },
                        modifier = Modifier.testTag("tab_write_notes")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage Location line matching sketch: "Int. Stor > Documents > VoiceNotes     Change"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (uiState.friendlyStoragePath.isNotBlank()) uiState.friendlyStoragePath else "Int. Stor > Documents > VoiceNotes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.openStorageConfigDialog() }
                        .testTag("storage_location_path")
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF38BDF8),
                    modifier = Modifier
                        .clickable { viewModel.openStorageConfigDialog() }
                        .testTag("storage_change_button")
                )
            }

            // Search input if open
            AnimatedVisibility(visible = isSearchVisible || uiState.searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text("Search voice notes & markdown notes...") },
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
            }

            // Animated Tab Content
            AnimatedContent(
                targetState = uiState.selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                },
                label = "tab_content_transition",
                modifier = Modifier.weight(1f)
            ) { currentTab ->
                when (currentTab) {
                    MainTab.WRITE -> {
                        WriteNotesSection(
                            notes = allNotes,
                            playerState = playerState,
                            onCreateNewNote = { viewModel.openFullScreenEditorForNew() },
                            onOpenNoteInEditor = { note -> viewModel.openFullScreenEditorForNote(note) },
                            onTogglePin = { note -> viewModel.togglePinNote(note) },
                            onPlayToggle = { note -> viewModel.togglePlayNote(note) },
                            onRename = { note -> viewModel.openRenameNoteDialog(note) },
                            onMove = { note -> viewModel.openMoveNoteDialog(note) },
                            onDelete = { note -> viewModel.confirmDeleteNote(note) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    MainTab.HOME, MainTab.PINS -> {
                        // Content List (Folders + Notes)
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            // Section: Nested Folders (Folder in folder ability) - only on Home tab
                            if (currentTab == MainTab.HOME && folders.isNotEmpty() && uiState.searchQuery.isBlank()) {
                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
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
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            // Section: Voice Notes Header if notes exist
                            if (voiceNotes.isNotEmpty() || uiState.searchQuery.isNotBlank()) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val headerTitle = when {
                                            uiState.searchQuery.isNotBlank() -> "Search Results"
                                            currentTab == MainTab.PINS -> "Pinned Recordings & Notes (${voiceNotes.size})"
                                            else -> "Voice Notes (${voiceNotes.size})"
                                        }
                                        Text(
                                            text = headerTitle,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Empty State
                            if (voiceNotes.isEmpty()) {
                                item {
                                    if (uiState.searchQuery.isNotBlank()) {
                                        EmptyNotesView(isSearching = true)
                                    } else {
                                        val emptyMsg = if (currentTab == MainTab.PINS) {
                                            "No pinned recordings yet.\nTap the pin icon on any recording or markdown file to access it here."
                                        } else {
                                            "The canvas is empty."
                                        }
                                        EmptyCanvasView(
                                            message = emptyMsg,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 40.dp)
                                        )
                                    }
                                }
                            } else {
                                // Voice Notes Items
                                items(voiceNotes, key = { it.id }) { note ->
                                    VoiceNoteListItem(
                                        note = note,
                                        isPlaying = playerState.isPlaying && playerState.currentNoteId == note.id,
                                        onClick = {
                                            if (note.audioFilePath.isBlank()) {
                                                viewModel.openFullScreenEditorForNote(note)
                                            } else {
                                                viewModel.openNoteDetail(note)
                                            }
                                        },
                                        onTogglePlay = { viewModel.togglePlayNote(note) },
                                        onTogglePin = { viewModel.togglePinNote(note) },
                                        onRename = { viewModel.openRenameNoteDialog(note) },
                                        onMove = { viewModel.openMoveNoteDialog(note) },
                                        onDelete = { viewModel.confirmDeleteNote(note) }
                                    )
                                }
                            }
                        }
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
    if (breadcrumbs.isEmpty()) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(top = 2.dp)
    ) {
        // Root crumb
        Text(
            text = "Home",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    val isWrittenOnly = note.audioFilePath.isBlank() || note.durationMs == 0L

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
            // Play / Pause or Note icon
            if (isWrittenOnly) {
                FilledTonalIconButton(
                    onClick = onClick,
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("open_written_note_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = "Open Note",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
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
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Note Info & Written Notes preview
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (note.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

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

                // Metadata: Duration, extension badge, Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isWrittenOnly) {
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
                        if (isPlaying) {
                            Spacer(modifier = Modifier.width(6.dp))
                            PlayingEqualizerBars(isPlaying = true)
                        }
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = ".md",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pin toggle icon button
            IconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePin()
                },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("pin_button_${note.id}")
            ) {
                Icon(
                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                    tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3-dots Menu for this Voice Note
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("note_menu_${note.id}")
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
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
                        },
                        modifier = Modifier.testTag("menu_item_pin")
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
fun VoiceNotesTitleWithRedDot() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Voice Notes",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFFE11D48))
        )
    }
}

@Composable
fun EmptyNotesView(
    isSearching: Boolean
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

        if (isSearching) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Try searching for a different keyword or check spelling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
