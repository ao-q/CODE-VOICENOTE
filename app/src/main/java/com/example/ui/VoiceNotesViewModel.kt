package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.FolderEntity
import com.example.data.model.SyncTimestampItem
import com.example.data.model.TimestampMarkerEntity
import com.example.data.model.VoiceNoteEntity
import com.example.data.repository.VoiceNotesRepository
import com.example.data.storage.RecordingFileLocations
import com.example.data.storage.StorageManager
import com.example.data.storage.StoragePreferences
import com.example.player.AudioPlayerManager
import com.example.player.PlayerState
import com.example.service.AudioRecordingService
import com.example.service.RecordingSessionState
import com.example.service.RecordingStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class MainTab {
    HOME,
    PINS,
    WRITE
}

data class VoiceNotesUiState(
    val selectedTab: MainTab = MainTab.HOME,
    val showWriteNoteDialog: Boolean = false,
    val isFullScreenEditorOpen: Boolean = false,
    val fullScreenEditorNote: VoiceNoteEntity? = null,
    val isNewNoteMode: Boolean = false,
    val isStorageConfigured: Boolean = false,
    val showStorageConfigDialog: Boolean = false,
    val storageBaseDirectoryDisplay: String = "",
    val friendlyStoragePath: String = "",
    val currentFolderId: Long? = null,
    val breadcrumbs: List<FolderEntity> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showNewFolderDialog: Boolean = false,
    val showRenameNoteDialog: Boolean = false,
    val showRenameFolderDialog: Boolean = false,
    val noteToRename: VoiceNoteEntity? = null,
    val folderToRename: FolderEntity? = null,
    val showMoveNoteDialog: Boolean = false,
    val noteToMove: VoiceNoteEntity? = null,
    val showDeleteConfirmDialog: Boolean = false,
    val noteToDelete: VoiceNoteEntity? = null,
    val folderToDelete: FolderEntity? = null,
    val selectedDetailNote: VoiceNoteEntity? = null,
    val isDetailViewOpen: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isFaceCamActive: Boolean = false,
    val statusMessage: String? = null
)

class VoiceNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val storagePreferences = StoragePreferences(application)
    private val storageManager = StorageManager(application)
    private val database = AppDatabase.getInstance(application)
    private val repository = VoiceNotesRepository(database.voiceNotesDao(), storageManager)
    val audioPlayerManager = AudioPlayerManager(application)

    private val _uiState = MutableStateFlow(
        VoiceNotesUiState(
            isDarkTheme = storagePreferences.isDarkTheme,
            isStorageConfigured = storagePreferences.isStorageConfigured,
            showStorageConfigDialog = !storagePreferences.isStorageConfigured,
            storageBaseDirectoryDisplay = storagePreferences.getEffectiveBaseDirectory().absolutePath,
            friendlyStoragePath = storagePreferences.getFriendlyStoragePath()
        )
    )
    val uiState: StateFlow<VoiceNotesUiState> = _uiState.asStateFlow()

    fun toggleTheme() {
        val newDark = !_uiState.value.isDarkTheme
        storagePreferences.isDarkTheme = newDark
        _uiState.value = _uiState.value.copy(isDarkTheme = newDark)
    }

    fun toggleFaceCam(active: Boolean? = null) {
        val newActive = active ?: !_uiState.value.isFaceCamActive
        _uiState.value = _uiState.value.copy(isFaceCamActive = newActive)
    }

    // Player state
    val playerState: StateFlow<PlayerState> = audioPlayerManager.state

    // Live Recording State from Service
    val recordingSessionState: StateFlow<RecordingSessionState> = RecordingStateManager.state

    // Current Folder ID flow
    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId.asStateFlow()

    // Current selected tab flow
    private val _selectedTab = MutableStateFlow(MainTab.HOME)

    // Search query flow
    private val _searchQuery = MutableStateFlow("")

    // Folders in current folder
    val currentFolders: StateFlow<List<FolderEntity>> = _currentFolderId.flatMapLatest { folderId ->
        repository.getFoldersByParent(folderId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All folders (for move dialog & breadcrumbs)
    val allFolders: StateFlow<List<FolderEntity>> = repository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All notes across all folders (for Write Notes section access)
    val allNotes: StateFlow<List<VoiceNoteEntity>> = repository.getAllVoiceNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All pinned notes across all folders (for Pins badge and list)
    val pinnedNotes: StateFlow<List<VoiceNoteEntity>> = repository.getPinnedVoiceNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice notes in current folder, or search, or pinned notes based on tab
    val currentVoiceNotes: StateFlow<List<VoiceNoteEntity>> = combine(
        _currentFolderId,
        _searchQuery,
        _selectedTab
    ) { folderId, query, tab ->
        Triple(folderId, query, tab)
    }.flatMapLatest { (folderId, query, tab) ->
        when {
            tab == MainTab.PINS -> {
                if (query.isNotBlank()) {
                    repository.getPinnedVoiceNotes().map { list ->
                        list.filter { note ->
                            note.title.contains(query, ignoreCase = true) ||
                            note.noteContent.contains(query, ignoreCase = true)
                        }
                    }
                } else {
                    repository.getPinnedVoiceNotes()
                }
            }
            query.isNotBlank() -> repository.searchVoiceNotes(query)
            else -> repository.getVoiceNotesByFolder(folderId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Markers for currently selected note
    private val _detailMarkers = MutableStateFlow<List<TimestampMarkerEntity>>(emptyList())
    val detailMarkers: StateFlow<List<TimestampMarkerEntity>> = _detailMarkers.asStateFlow()

    // In-recording live fields ("Write Notes" section)
    var liveRecordingTitle = MutableStateFlow("")
    var liveRecordingNotes = MutableStateFlow("")
    private var activeRecordingLocations: RecordingFileLocations? = null
    private var recordingStartTimeMs: Long = 0L

    init {
        viewModelScope.launch {
            _currentFolderId.collectLatest { folderId ->
                val crumbs = repository.getBreadcrumbs(folderId)
                _uiState.value = _uiState.value.copy(
                    currentFolderId = folderId,
                    breadcrumbs = crumbs
                )
            }
        }
    }

    // Storage Configuration
    fun configureStorage(folderName: String, customPath: String?, treeUri: String? = null) {
        val resolvedName = folderName.ifBlank { "VoiceNotes" }
        storagePreferences.storageFolderName = resolvedName
        storagePreferences.customStoragePath = customPath
        if (!treeUri.isNullOrBlank()) {
            storagePreferences.storageTreeUri = treeUri
        }
        storagePreferences.isStorageConfigured = true
        val baseDir = storagePreferences.getEffectiveBaseDirectory()
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val friendly = storagePreferences.getFriendlyStoragePath()
        _uiState.value = _uiState.value.copy(
            isStorageConfigured = true,
            showStorageConfigDialog = false,
            storageBaseDirectoryDisplay = baseDir.absolutePath,
            friendlyStoragePath = friendly,
            statusMessage = "Storage saved to: $friendly"
        )
    }

    fun openStorageConfigDialog() {
        _uiState.value = _uiState.value.copy(showStorageConfigDialog = true)
    }

    fun dismissStorageConfigDialog() {
        // Only allow dismissing if already configured
        if (storagePreferences.isStorageConfigured) {
            _uiState.value = _uiState.value.copy(showStorageConfigDialog = false)
        }
    }

    // Tab Navigation
    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // Full Screen Markdown Editor Operations
    fun openFullScreenEditorForNew() {
        _uiState.value = _uiState.value.copy(
            isFullScreenEditorOpen = true,
            fullScreenEditorNote = null,
            isNewNoteMode = true
        )
    }

    fun openFullScreenEditorForNote(note: VoiceNoteEntity) {
        _uiState.value = _uiState.value.copy(
            isFullScreenEditorOpen = true,
            fullScreenEditorNote = note,
            isNewNoteMode = false
        )
    }

    fun closeFullScreenEditor() {
        _uiState.value = _uiState.value.copy(
            isFullScreenEditorOpen = false,
            fullScreenEditorNote = null,
            isNewNoteMode = false
        )
    }

    fun saveFullScreenNote(
        noteId: Long?,
        title: String,
        content: String,
        isPinned: Boolean
    ) {
        val safeTitle = title.trim().ifBlank { "Untitled Note" }
        viewModelScope.launch {
            if (noteId == null || _uiState.value.isNewNoteMode) {
                // Create new written note
                val newId = repository.createWrittenNote(
                    folderId = _currentFolderId.value,
                    title = safeTitle,
                    content = content
                )
                if (isPinned) {
                    repository.togglePinNote(newId, true)
                }
                val created = repository.getVoiceNoteById(newId)
                _uiState.value = _uiState.value.copy(
                    fullScreenEditorNote = created,
                    isNewNoteMode = false,
                    statusMessage = "Note '$safeTitle' saved (.md)"
                )
            } else {
                // Update existing note
                val currentNote = repository.getVoiceNoteById(noteId)
                if (currentNote != null) {
                    if (currentNote.title != safeTitle) {
                        repository.renameVoiceNote(noteId, safeTitle)
                    }
                    repository.updateNoteContent(noteId, content)
                    if (currentNote.isPinned != isPinned) {
                        repository.togglePinNote(noteId, isPinned)
                    }
                    val updated = repository.getVoiceNoteById(noteId)
                    _uiState.value = _uiState.value.copy(
                        fullScreenEditorNote = updated,
                        statusMessage = "Synced to ${updated?.noteFileName ?: "storage"}"
                    )
                }
            }
        }
    }

    fun deleteCurrentFullScreenNote() {
        val note = _uiState.value.fullScreenEditorNote ?: return
        viewModelScope.launch {
            if (audioPlayerManager.state.value.currentNoteId == note.id) {
                audioPlayerManager.stop()
            }
            repository.deleteVoiceNote(note.id)
            closeFullScreenEditor()
            _uiState.value = _uiState.value.copy(statusMessage = "Note '${note.title}' deleted")
        }
    }

    fun openWriteNoteDialog() {
        openFullScreenEditorForNew()
    }

    fun dismissWriteNoteDialog() {
        _uiState.value = _uiState.value.copy(showWriteNoteDialog = false)
    }

    fun createWrittenNote(title: String, content: String) {
        val safeTitle = title.trim().ifBlank { "Written Note" }
        viewModelScope.launch {
            repository.createWrittenNote(
                folderId = _currentFolderId.value,
                title = safeTitle,
                content = content
            )
            _uiState.value = _uiState.value.copy(
                showWriteNoteDialog = false,
                statusMessage = "Note '$safeTitle' saved to storage"
            )
        }
    }

    fun togglePinNote(note: VoiceNoteEntity) {
        viewModelScope.launch {
            val nextPinned = !note.isPinned
            repository.togglePinNote(note.id, nextPinned)
            _uiState.value = _uiState.value.copy(
                statusMessage = if (nextPinned) "Pinned '${note.title}'" else "Unpinned '${note.title}'"
            )
        }
    }

    // Navigation (Folder in Folder)
    fun navigateToFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }

    fun navigateUp() {
        val currentId = _currentFolderId.value ?: return
        viewModelScope.launch {
            val crumbs = repository.getBreadcrumbs(currentId)
            val parentId = if (crumbs.size > 1) crumbs[crumbs.size - 2].id else null
            _currentFolderId.value = parentId
        }
    }

    // Search
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query, isSearchActive = query.isNotBlank())
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(searchQuery = "", isSearchActive = false)
    }

    // Create Folder
    fun openNewFolderDialog() {
        _uiState.value = _uiState.value.copy(showNewFolderDialog = true)
    }

    fun dismissNewFolderDialog() {
        _uiState.value = _uiState.value.copy(showNewFolderDialog = false)
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createFolder(name.trim(), _currentFolderId.value)
            dismissNewFolderDialog()
            _uiState.value = _uiState.value.copy(statusMessage = "Folder '$name' created")
        }
    }

    // Rename Folder
    fun openRenameFolderDialog(folder: FolderEntity) {
        _uiState.value = _uiState.value.copy(showRenameFolderDialog = true, folderToRename = folder)
    }

    fun dismissRenameFolderDialog() {
        _uiState.value = _uiState.value.copy(showRenameFolderDialog = false, folderToRename = null)
    }

    fun renameFolder(newName: String) {
        val folder = _uiState.value.folderToRename ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameFolder(folder.id, newName.trim())
            dismissRenameFolderDialog()
            _uiState.value = _uiState.value.copy(statusMessage = "Folder renamed to '$newName'")
        }
    }

    // Delete Folder
    fun confirmDeleteFolder(folder: FolderEntity) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true, folderToDelete = folder)
    }

    fun deleteFolder() {
        val folder = _uiState.value.folderToDelete ?: return
        viewModelScope.launch {
            repository.deleteFolder(folder.id)
            _uiState.value = _uiState.value.copy(
                showDeleteConfirmDialog = false,
                folderToDelete = null,
                statusMessage = "Folder '${folder.name}' deleted"
            )
        }
    }

    // ==========================================
    // Recording & Write Notes
    // ==========================================
    fun startRecording(initialTitle: String = "", initialNotes: String = "") {
        viewModelScope.launch {
            recordingStartTimeMs = System.currentTimeMillis()
            liveRecordingTitle.value = initialTitle
            liveRecordingNotes.value = initialNotes
            RecordingStateManager.reset()

            val folderHierarchy = repository.getFolderHierarchyNames(_currentFolderId.value)
            // Prepare initial placeholder locations; will be finalized upon saving
            val locations = storageManager.prepareRecordingLocations(
                parentFolderHierarchy = folderHierarchy,
                userTitle = initialTitle.ifBlank { null },
                timestamp = recordingStartTimeMs
            )
            activeRecordingLocations = locations

            AudioRecordingService.startRecording(
                getApplication(),
                locations.audioFile.absolutePath
            )
        }
    }

    fun pauseRecording() {
        AudioRecordingService.pauseRecording(getApplication())
    }

    fun resumeRecording() {
        AudioRecordingService.resumeRecording(getApplication())
    }

    fun flagTimestamp(label: String? = null) {
        RecordingStateManager.addTimestamp(label)
    }

    fun stopAndSaveRecording() {
        val sessionState = RecordingStateManager.state.value
        val title = liveRecordingTitle.value.trim()
        val notes = liveRecordingNotes.value.trim()
        val durationMs = sessionState.elapsedSeconds * 1000L
        val timestamps = sessionState.flaggedTimestamps
        val initialLocations = activeRecordingLocations

        AudioRecordingService.stopRecording(getApplication())

        viewModelScope.launch {
            val folderHierarchy = repository.getFolderHierarchyNames(_currentFolderId.value)
            // Prepare final titled locations based on user title (or time of recording if blank)
            val finalLocations = storageManager.prepareRecordingLocations(
                parentFolderHierarchy = folderHierarchy,
                userTitle = title.ifBlank { null },
                timestamp = recordingStartTimeMs
            )

            // If initial temporary file was located in placeholder folder, move/rename audio file
            val sourceAudioFile = initialLocations?.audioFile
            if (sourceAudioFile != null && sourceAudioFile.exists() &&
                sourceAudioFile.absolutePath != finalLocations.audioFile.absolutePath
            ) {
                sourceAudioFile.renameTo(finalLocations.audioFile)
                // Remove initial placeholder folder if different and empty
                initialLocations.folderDirectory.delete()
            }

            val savedNoteId = repository.saveCompletedVoiceNote(
                folderId = _currentFolderId.value,
                title = finalLocations.resolvedTitle,
                audioFile = finalLocations.audioFile,
                notesFile = finalLocations.notesFile,
                folderDir = finalLocations.folderDirectory,
                noteContent = notes,
                durationMs = durationMs,
                createdAt = recordingStartTimeMs,
                timestampMarkers = timestamps
            )

            activeRecordingLocations = null
            RecordingStateManager.reset()
            _uiState.value = _uiState.value.copy(
                statusMessage = "Voice note '${finalLocations.resolvedTitle}' saved (.mp3)"
            )
        }
    }

    fun cancelRecording() {
        AudioRecordingService.stopRecording(getApplication())
        val loc = activeRecordingLocations
        loc?.let { storageManager.deleteNoteFolder(it.folderDirectory) }
        activeRecordingLocations = null
        RecordingStateManager.reset()
    }

    // ==========================================
    // Rename Voice Note (Auto-sync to disk)
    // ==========================================
    fun openRenameNoteDialog(note: VoiceNoteEntity) {
        _uiState.value = _uiState.value.copy(showRenameNoteDialog = true, noteToRename = note)
    }

    fun dismissRenameNoteDialog() {
        _uiState.value = _uiState.value.copy(showRenameNoteDialog = false, noteToRename = null)
    }

    fun renameVoiceNote(newTitle: String) {
        val note = _uiState.value.noteToRename ?: return
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val updated = repository.renameVoiceNote(note.id, newTitle.trim())
            dismissRenameNoteDialog()
            if (updated != null) {
                if (_uiState.value.selectedDetailNote?.id == note.id) {
                    _uiState.value = _uiState.value.copy(selectedDetailNote = updated)
                }
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Renamed & synced to local storage: '$newTitle'"
                )
            }
        }
    }

    // ==========================================
    // Move Note to Folder
    // ==========================================
    fun openMoveNoteDialog(note: VoiceNoteEntity) {
        _uiState.value = _uiState.value.copy(showMoveNoteDialog = true, noteToMove = note)
    }

    fun dismissMoveNoteDialog() {
        _uiState.value = _uiState.value.copy(showMoveNoteDialog = false, noteToMove = null)
    }

    fun moveNoteToFolder(targetFolderId: Long?) {
        val note = _uiState.value.noteToMove ?: return
        viewModelScope.launch {
            repository.moveVoiceNote(note.id, targetFolderId)
            dismissMoveNoteDialog()
            _uiState.value = _uiState.value.copy(statusMessage = "Moved note to new folder")
        }
    }

    // ==========================================
    // Delete Note
    // ==========================================
    fun confirmDeleteNote(note: VoiceNoteEntity) {
        _uiState.value = _uiState.value.copy(showDeleteConfirmDialog = true, noteToDelete = note)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmDialog = false,
            noteToDelete = null,
            folderToDelete = null
        )
    }

    fun deleteVoiceNote() {
        val note = _uiState.value.noteToDelete ?: return
        viewModelScope.launch {
            if (audioPlayerManager.state.value.currentNoteId == note.id) {
                audioPlayerManager.stop()
            }
            repository.deleteVoiceNote(note.id)
            if (_uiState.value.selectedDetailNote?.id == note.id) {
                _uiState.value = _uiState.value.copy(selectedDetailNote = null, isDetailViewOpen = false)
            }
            dismissDeleteDialog()
            _uiState.value = _uiState.value.copy(statusMessage = "Note '${note.title}' deleted from storage")
        }
    }

    // ==========================================
    // Note Detail View & Live Note Editing
    // ==========================================
    fun openNoteDetail(note: VoiceNoteEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedDetailNote = note, isDetailViewOpen = true)
            repository.getMarkersForNote(note.id).collectLatest { markers ->
                _detailMarkers.value = markers
            }
        }
    }

    fun closeNoteDetail() {
        _uiState.value = _uiState.value.copy(selectedDetailNote = null, isDetailViewOpen = false)
    }

    fun updateNoteContent(noteId: Long, newContent: String) {
        viewModelScope.launch {
            repository.updateNoteContent(noteId, newContent)
            val updated = repository.getVoiceNoteById(noteId)
            if (updated != null && _uiState.value.selectedDetailNote?.id == noteId) {
                _uiState.value = _uiState.value.copy(selectedDetailNote = updated)
            }
        }
    }

    fun addMarkerToCurrentNote(noteId: Long, timeMs: Long, label: String) {
        viewModelScope.launch {
            val mins = (timeMs / 1000) / 60
            val secs = (timeMs / 1000) % 60
            val formatted = String.format("%02d:%02d", mins, secs)
            repository.addTimestampMarker(noteId, timeMs, formatted, label.ifBlank { "Flagged Point" })
            _uiState.value = _uiState.value.copy(statusMessage = "Timestamp flagged at $formatted")
        }
    }

    // Playback Helpers
    fun togglePlayNote(note: VoiceNoteEntity) {
        audioPlayerManager.playOrToggle(note.id, note.audioFilePath)
    }

    fun seekToMarker(marker: TimestampMarkerEntity) {
        audioPlayerManager.seekTo(marker.timeMs)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    override fun onCleared() {
        audioPlayerManager.release()
        super.onCleared()
    }
}
