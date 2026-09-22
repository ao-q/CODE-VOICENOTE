package com.example.data.repository

import com.example.data.db.VoiceNotesDao
import com.example.data.model.FolderEntity
import com.example.data.model.SyncTimestampItem
import com.example.data.model.TimestampMarkerEntity
import com.example.data.model.VoiceNoteEntity
import com.example.data.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class VoiceNotesRepository(
    private val dao: VoiceNotesDao,
    private val storageManager: StorageManager
) {
    fun getAllFolders(): Flow<List<FolderEntity>> = dao.getAllFolders()

    fun getFoldersByParent(parentId: Long?): Flow<List<FolderEntity>> =
        dao.getFoldersByParent(parentId)

    suspend fun createFolder(name: String, parentId: Long?): Long = withContext(Dispatchers.IO) {
        val folder = FolderEntity(parentId = parentId, name = name.trim())
        dao.insertFolder(folder)
    }

    suspend fun renameFolder(folderId: Long, newName: String) = withContext(Dispatchers.IO) {
        val folder = dao.getFolderById(folderId) ?: return@withContext
        dao.updateFolder(folder.copy(name = newName.trim()))
    }

    suspend fun deleteFolder(folderId: Long) = withContext(Dispatchers.IO) {
        dao.deleteFolderById(folderId)
    }

    fun getPinnedFolders(): Flow<List<FolderEntity>> = dao.getPinnedFolders()

    suspend fun togglePinFolder(folder: FolderEntity) = withContext(Dispatchers.IO) {
        dao.updateFolderPinned(folder.id, !folder.isPinned)
    }

    suspend fun getBreadcrumbs(currentFolderId: Long?): List<FolderEntity> = withContext(Dispatchers.IO) {
        val breadcrumbs = mutableListOf<FolderEntity>()
        var currId = currentFolderId
        while (currId != null) {
            val folder = dao.getFolderById(currId) ?: break
            breadcrumbs.add(0, folder)
            currId = folder.parentId
        }
        breadcrumbs
    }

    suspend fun getFolderHierarchyNames(currentFolderId: Long?): List<String> {
        val breadcrumbs = getBreadcrumbs(currentFolderId)
        return breadcrumbs.map { it.name }
    }

    fun getAllVoiceNotes(): Flow<List<VoiceNoteEntity>> = dao.getAllVoiceNotes()

    fun getVoiceNotesByFolder(folderId: Long?): Flow<List<VoiceNoteEntity>> =
        dao.getVoiceNotesByFolder(folderId)

    fun searchVoiceNotes(query: String): Flow<List<VoiceNoteEntity>> =
        dao.searchVoiceNotes(query)

    fun getPinnedVoiceNotes(): Flow<List<VoiceNoteEntity>> =
        dao.getPinnedVoiceNotes()

    suspend fun togglePinNote(noteId: Long, isPinned: Boolean) = withContext(Dispatchers.IO) {
        dao.updatePinned(noteId, isPinned)
    }

    /**
     * Creates a written markdown note directly (without audio).
     */
    suspend fun createWrittenNote(
        folderId: Long?,
        title: String,
        content: String
    ): Long = withContext(Dispatchers.IO) {
        val hierarchy = getFolderHierarchyNames(folderId)
        val createdAt = System.currentTimeMillis()
        val locations = storageManager.prepareRecordingLocations(hierarchy, title, createdAt)

        storageManager.writeNotesFile(
            notesFile = locations.notesFile,
            title = locations.resolvedTitle,
            content = content,
            createdAt = createdAt,
            durationMs = 0L,
            timestamps = emptyList()
        )

        val entity = VoiceNoteEntity(
            folderId = folderId,
            title = locations.resolvedTitle,
            audioFileName = "",
            audioFilePath = "",
            noteFileName = locations.notesFile.name,
            noteFilePath = locations.notesFile.absolutePath,
            noteContent = content,
            noteFolderDirectory = locations.folderDirectory.absolutePath,
            durationMs = 0L,
            createdAt = createdAt,
            isPinned = false
        )
        dao.insertVoiceNote(entity)
    }

    suspend fun getVoiceNoteById(id: Long): VoiceNoteEntity? = withContext(Dispatchers.IO) {
        dao.getVoiceNoteById(id)
    }

    fun getVoiceNoteFlow(id: Long): Flow<VoiceNoteEntity?> = dao.getVoiceNoteFlowById(id)

    fun getMarkersForNote(noteId: Long): Flow<List<TimestampMarkerEntity>> =
        dao.getMarkersForNote(noteId)

    suspend fun getMarkersListForNote(noteId: Long): List<TimestampMarkerEntity> = withContext(Dispatchers.IO) {
        dao.getMarkersListForNote(noteId)
    }

    /**
     * Saves a recorded voice note into Room database and writes note md to disk.
     */
    suspend fun saveCompletedVoiceNote(
        folderId: Long?,
        title: String,
        audioFile: File,
        notesFile: File,
        folderDir: File,
        noteContent: String,
        durationMs: Long,
        createdAt: Long,
        timestampMarkers: List<SyncTimestampItem>
    ): Long = withContext(Dispatchers.IO) {
        // Write the notes text file
        storageManager.writeNotesFile(
            notesFile = notesFile,
            title = title,
            content = noteContent,
            createdAt = createdAt,
            durationMs = durationMs,
            timestamps = timestampMarkers
        )

        val entity = VoiceNoteEntity(
            folderId = folderId,
            title = title,
            audioFileName = audioFile.name,
            audioFilePath = audioFile.absolutePath,
            noteFileName = notesFile.name,
            noteFilePath = notesFile.absolutePath,
            noteContent = noteContent,
            noteFolderDirectory = folderDir.absolutePath,
            durationMs = durationMs,
            createdAt = createdAt
        )

        val noteId = dao.insertVoiceNote(entity)

        // Insert timestamp markers into database
        val markerEntities = timestampMarkers.map {
            TimestampMarkerEntity(
                voiceNoteId = noteId,
                timeMs = it.timeMs,
                formattedTime = it.formattedTime,
                label = it.label,
                createdAt = createdAt
            )
        }
        if (markerEntities.isNotEmpty()) {
            dao.insertMarkers(markerEntities)
        }

        noteId
    }

    /**
     * Renames a voice note and automatically syncs to local storage:
     * Renames the titled folder, .mp3 file, and .md file.
     */
    suspend fun renameVoiceNote(noteId: Long, newTitle: String): VoiceNoteEntity? = withContext(Dispatchers.IO) {
        val note = dao.getVoiceNoteById(noteId) ?: return@withContext null
        if (newTitle.isBlank() || newTitle.trim() == note.title) return@withContext note

        val markers = dao.getMarkersListForNote(noteId).map {
            SyncTimestampItem(timeMs = it.timeMs, formattedTime = it.formattedTime, label = it.label)
        }

        val renamed = storageManager.renameVoiceNote(
            currentFolderDir = File(note.noteFolderDirectory),
            currentAudioFile = File(note.audioFilePath),
            currentNotesFile = File(note.noteFilePath),
            newTitle = newTitle.trim(),
            noteContent = note.noteContent,
            createdAt = note.createdAt,
            durationMs = note.durationMs,
            timestamps = markers
        )

        val updatedNote = note.copy(
            title = renamed.newTitle,
            audioFileName = renamed.newAudioFile.name,
            audioFilePath = renamed.newAudioFile.absolutePath,
            noteFileName = renamed.newNotesFile.name,
            noteFilePath = renamed.newNotesFile.absolutePath,
            noteFolderDirectory = renamed.newFolderDirectory.absolutePath
        )

        dao.updateVoiceNote(updatedNote)
        updatedNote
    }

    /**
     * Updates note text content for a voice note, synchronizing the .md file on disk.
     */
    suspend fun updateNoteContent(noteId: Long, newContent: String) = withContext(Dispatchers.IO) {
        val note = dao.getVoiceNoteById(noteId) ?: return@withContext
        val markers = dao.getMarkersListForNote(noteId).map {
            SyncTimestampItem(timeMs = it.timeMs, formattedTime = it.formattedTime, label = it.label)
        }

        // Re-write notes md file
        val notesFile = File(note.noteFilePath)
        storageManager.writeNotesFile(
            notesFile = notesFile,
            title = note.title,
            content = newContent,
            createdAt = note.createdAt,
            durationMs = note.durationMs,
            timestamps = markers
        )

        val updated = note.copy(
            noteContent = newContent
        )
        dao.updateVoiceNote(updated)
    }

    /**
     * Moves a note's folder on disk and updates its folderId in database.
     */
    suspend fun moveVoiceNote(noteId: Long, targetFolderId: Long?) = withContext(Dispatchers.IO) {
        val note = dao.getVoiceNoteById(noteId) ?: return@withContext
        val targetHierarchy = getFolderHierarchyNames(targetFolderId)
        val targetParentDir = storageManager.resolveParentFolder(targetHierarchy)

        val currentFolderDir = File(note.noteFolderDirectory)
        val movedFolder = storageManager.moveNoteFolder(currentFolderDir, targetParentDir)

        val newAudioFile = File(movedFolder, note.audioFileName)
        val newNotesFile = File(movedFolder, note.noteFileName)

        val updated = note.copy(
            folderId = targetFolderId,
            noteFolderDirectory = movedFolder.absolutePath,
            audioFilePath = newAudioFile.absolutePath,
            noteFilePath = newNotesFile.absolutePath
        )
        dao.updateVoiceNote(updated)
    }

    /**
     * Deletes a voice note from database and removes its folder from storage.
     */
    suspend fun deleteVoiceNote(noteId: Long) = withContext(Dispatchers.IO) {
        val note = dao.getVoiceNoteById(noteId) ?: return@withContext
        dao.deleteVoiceNoteById(noteId)
        storageManager.deleteNoteFolder(File(note.noteFolderDirectory))
    }

    /**
     * Adds a timestamp marker and syncs to disk notes file.
     */
    suspend fun addTimestampMarker(noteId: Long, timeMs: Long, formattedTime: String, label: String) = withContext(Dispatchers.IO) {
        val marker = TimestampMarkerEntity(
            voiceNoteId = noteId,
            timeMs = timeMs,
            formattedTime = formattedTime,
            label = label
        )
        dao.insertMarker(marker)

        val note = dao.getVoiceNoteById(noteId) ?: return@withContext
        val allMarkers = dao.getMarkersListForNote(noteId).map {
            SyncTimestampItem(timeMs = it.timeMs, formattedTime = it.formattedTime, label = it.label)
        }

        // Also update the note file with the new timestamp list
        storageManager.writeNotesFile(
            notesFile = File(note.noteFilePath),
            title = note.title,
            content = note.noteContent,
            createdAt = note.createdAt,
            durationMs = note.durationMs,
            timestamps = allMarkers
        )
    }
}
