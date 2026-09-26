package com.example.data.repository

import android.media.MediaMetadataRetriever
import android.os.Environment
import com.example.data.db.VoiceNotesDao
import com.example.data.model.FolderEntity
import com.example.data.model.SyncTimestampItem
import com.example.data.model.TimestampMarkerEntity
import com.example.data.model.VoiceNoteEntity
import com.example.data.storage.StorageManager
import com.example.data.storage.StorageUriHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

data class SyncResult(
    val notesCount: Int,
    val foldersCount: Int,
    val message: String
)

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

    /**
     * Creates a handwritten note from stylus drawing canvas with PNG image in local storage.
     */
    suspend fun createHandwrittenNote(
        folderId: Long?,
        title: String,
        bitmap: android.graphics.Bitmap
    ): Long = withContext(Dispatchers.IO) {
        val hierarchy = getFolderHierarchyNames(folderId)
        val createdAt = System.currentTimeMillis()
        val imageFile = storageManager.saveDrawingToStorage(hierarchy, title, bitmap, createdAt)

        val entity = VoiceNoteEntity(
            folderId = folderId,
            title = title.trim().ifBlank { imageFile.nameWithoutExtension },
            audioFileName = "",
            audioFilePath = "",
            noteFileName = imageFile.name,
            noteFilePath = imageFile.absolutePath,
            noteContent = imageFile.absolutePath,
            noteFolderDirectory = imageFile.parentFile?.absolutePath ?: "",
            durationMs = 0L,
            createdAt = createdAt,
            isPinned = false
        )
        dao.insertVoiceNote(entity)
    }

    /**
     * Updates an existing handwritten drawing note with new bitmap edits.
     */
    suspend fun updateHandwrittenNote(
        noteId: Long,
        title: String,
        bitmap: android.graphics.Bitmap
    ): VoiceNoteEntity? = withContext(Dispatchers.IO) {
        val note = dao.getVoiceNoteById(noteId) ?: return@withContext null
        val targetFile = File(note.noteFilePath)
        try {
            if (!targetFile.parentFile.exists()) {
                targetFile.parentFile.mkdirs()
            }
            java.io.FileOutputStream(targetFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            storageManager.notifyMediaScanner(targetFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val updated = note.copy(
            title = title.trim().ifBlank { note.title }
        )
        dao.updateVoiceNote(updated)
        updated
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

    /**
     * Imports and synchronizes all voice notes and markdown notes from Documents/Voice Notes
     * or custom storage directory into the app database.
     */
    suspend fun syncStorageFiles(): SyncResult = withContext(Dispatchers.IO) {
        val baseDir = storageManager.getBaseDirectory()
        val publicDocs = StorageUriHelper.getPublicDocumentsDir()
        val extRoot = Environment.getExternalStorageDirectory()

        val candidateDirs = listOf(
            baseDir,
            File(publicDocs, "Voice Notes"),
            File(publicDocs, "VoiceNotes"),
            File(extRoot, "Documents/Voice Notes"),
            File(extRoot, "Documents/VoiceNotes"),
            File(extRoot, "Voice Notes"),
            File(extRoot, "VoiceNotes")
        ).filter { it.exists() && it.isDirectory }
            .distinctBy { it.canonicalPath }

        var totalImportedNotes = 0
        var totalImportedFolders = 0

        for (dir in candidateDirs) {
            val (notesCount, foldersCount) = syncDirectoryRecursive(dir, parentFolderId = null)
            totalImportedNotes += notesCount
            totalImportedFolders += foldersCount
        }

        val msg = if (totalImportedNotes > 0 || totalImportedFolders > 0) {
            "Synced $totalImportedNotes notes & $totalImportedFolders folders"
        } else {
            "Storage up to date (Documents/Voice Notes)"
        }
        SyncResult(totalImportedNotes, totalImportedFolders, msg)
    }

    private suspend fun syncDirectoryRecursive(
        directory: File,
        parentFolderId: Long?
    ): Pair<Int, Int> {
        val children = directory.listFiles() ?: return Pair(0, 0)
        var notesCount = 0
        var foldersCount = 0

        val audioExtensions = setOf("mp3", "m4a", "wav", "aac", "ogg", "3gp", "flac")
        val noteExtensions = setOf("md", "txt", "png")

        val subDirs = children.filter { it.isDirectory }
        val directFiles = children.filter { it.isFile }

        // 1. Group direct files by their base name
        val filesByBase = directFiles.groupBy { it.nameWithoutExtension }

        for ((baseName, filesGroup) in filesByBase) {
            val audioFile = filesGroup.firstOrNull { it.extension.lowercase() in audioExtensions }
            val noteFile = filesGroup.firstOrNull { it.extension.lowercase() in noteExtensions }

            if (audioFile != null || noteFile != null) {
                val imported = syncFileOrPair(
                    folderId = parentFolderId,
                    folderDir = directory,
                    baseName = baseName,
                    audioFile = audioFile,
                    noteFile = noteFile
                )
                if (imported) notesCount++
            }
        }

        // 2. Process subdirectories
        for (subDir in subDirs) {
            val subChildren = subDir.listFiles() ?: emptyArray()
            val subAudio = subChildren.firstOrNull { it.isFile && it.extension.lowercase() in audioExtensions }
            val subNote = subChildren.firstOrNull { it.isFile && it.extension.lowercase() in noteExtensions }
            val hasNestedDirs = subChildren.any { it.isDirectory }

            // If it's a dedicated single note folder: e.g., folder named "Meeting" containing "Meeting.mp3" and "Meeting.md"
            val isDedicatedNoteFolder = !hasNestedDirs && (subAudio != null || subNote != null) &&
                    (subAudio?.nameWithoutExtension.equals(subDir.name, ignoreCase = true) ||
                     subNote?.nameWithoutExtension.equals(subDir.name, ignoreCase = true) ||
                     subChildren.count { it.isFile } <= 3)

            if (isDedicatedNoteFolder) {
                val base = subAudio?.nameWithoutExtension ?: subNote?.nameWithoutExtension ?: subDir.name
                val imported = syncFileOrPair(
                    folderId = parentFolderId,
                    folderDir = subDir,
                    baseName = base,
                    audioFile = subAudio,
                    noteFile = subNote
                )
                if (imported) notesCount++
            } else {
                // It's a user folder (like "Work", "Lecture", etc.)
                var folder = dao.getFolderByNameAndParent(subDir.name, parentFolderId)
                val folderId = if (folder != null) {
                    folder.id
                } else {
                    val newFolder = FolderEntity(
                        parentId = parentFolderId,
                        name = subDir.name
                    )
                    val newId = dao.insertFolder(newFolder)
                    foldersCount++
                    newId
                }

                // Recursively sync its contents
                val (childNotes, childFolders) = syncDirectoryRecursive(subDir, folderId)
                notesCount += childNotes
                foldersCount += childFolders
            }
        }

        return Pair(notesCount, foldersCount)
    }

    private suspend fun syncFileOrPair(
        folderId: Long?,
        folderDir: File,
        baseName: String,
        audioFile: File?,
        noteFile: File?
    ): Boolean {
        val audioPath = audioFile?.absolutePath ?: ""
        val notePath = noteFile?.absolutePath ?: ""

        val existingNote = dao.findVoiceNoteByFilePath(audioPath, notePath)
            ?: dao.findVoiceNoteByTitleAndFolder(baseName, folderId)

        val (parsedTitle, parsedContent, parsedMarkers) = if (noteFile != null && noteFile.exists() && noteFile.extension.lowercase() in setOf("md", "txt")) {
            parseMarkdownFile(noteFile)
        } else if (noteFile != null && noteFile.exists() && noteFile.extension.lowercase() == "png") {
            Triple(baseName, noteFile.absolutePath, emptyList())
        } else {
            Triple(baseName, "", emptyList())
        }

        val effectiveTitle = parsedTitle.ifBlank { baseName }
        val durationMs = if (audioFile != null && audioFile.exists()) {
            getAudioDuration(audioFile)
        } else 0L

        val lastModified = (audioFile?.lastModified() ?: noteFile?.lastModified() ?: System.currentTimeMillis())

        if (existingNote != null) {
            // Update existing note with any missing paths or updated content
            val updated = existingNote.copy(
                title = if (existingNote.title.isBlank()) effectiveTitle else existingNote.title,
                audioFileName = audioFile?.name ?: existingNote.audioFileName,
                audioFilePath = if (audioPath.isNotBlank()) audioPath else existingNote.audioFilePath,
                noteFileName = noteFile?.name ?: existingNote.noteFileName,
                noteFilePath = if (notePath.isNotBlank()) notePath else existingNote.noteFilePath,
                noteFolderDirectory = folderDir.absolutePath,
                noteContent = if (parsedContent.isNotBlank() && existingNote.noteContent.isBlank()) parsedContent else existingNote.noteContent,
                durationMs = if (existingNote.durationMs == 0L && durationMs > 0L) durationMs else existingNote.durationMs
            )
            dao.updateVoiceNote(updated)

            if (parsedMarkers.isNotEmpty()) {
                val currentMarkers = dao.getMarkersListForNote(existingNote.id)
                if (currentMarkers.isEmpty()) {
                    dao.insertMarkers(parsedMarkers.map {
                        TimestampMarkerEntity(voiceNoteId = existingNote.id, timeMs = it.timeMs, formattedTime = it.formattedTime, label = it.label)
                    })
                }
            }

            storageManager.notifyMediaScanner(audioFile, noteFile)
            return false // Was already present
        }

        // Insert newly discovered voice note
        val newNote = VoiceNoteEntity(
            folderId = folderId,
            title = effectiveTitle,
            audioFileName = audioFile?.name ?: "",
            audioFilePath = audioPath,
            noteFileName = noteFile?.name ?: "",
            noteFilePath = notePath,
            noteFolderDirectory = folderDir.absolutePath,
            createdAt = lastModified,
            durationMs = durationMs,
            noteContent = parsedContent,
            isPinned = false
        )
        val newNoteId = dao.insertVoiceNote(newNote)

        if (parsedMarkers.isNotEmpty()) {
            dao.insertMarkers(parsedMarkers.map {
                TimestampMarkerEntity(
                    voiceNoteId = newNoteId,
                    timeMs = it.timeMs,
                    formattedTime = it.formattedTime,
                    label = it.label
                )
            })
        }

        storageManager.notifyMediaScanner(audioFile, noteFile)
        return true
    }

    private fun parseMarkdownFile(file: File): Triple<String, String, List<SyncTimestampItem>> {
        val text = try {
            file.readText(Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
        if (text.isBlank()) return Triple(file.nameWithoutExtension, "", emptyList())

        val lines = text.lines()
        var title = file.nameWithoutExtension
        val notesContent = java.lang.StringBuilder()
        val timestamps = mutableListOf<SyncTimestampItem>()
        var inNotesSection = false
        var inTimestampsSection = false

        if (lines.isNotEmpty() && lines[0].startsWith("# ")) {
            title = lines[0].removePrefix("# ").trim().ifBlank { file.nameWithoutExtension }
            for (i in 1 until lines.size) {
                val line = lines[i]
                val trimmed = line.trim()
                if (trimmed == "## Notes") {
                    inNotesSection = true
                    inTimestampsSection = false
                    continue
                }
                if (trimmed == "## Timestamps") {
                    inNotesSection = false
                    inTimestampsSection = true
                    continue
                }
                if (inNotesSection) {
                    if (trimmed == "*(No additional notes taken)*") continue
                    notesContent.append(line).append("\n")
                } else if (inTimestampsSection) {
                    val match = Regex("""-\s+\*\*\[(\d+:\d+)\]\*\*\s+(.*)""").find(trimmed)
                    if (match != null) {
                        val formatted = match.groupValues[1]
                        val label = match.groupValues[2].trim()
                        val parts = formatted.split(":")
                        val timeMs = if (parts.size == 2) {
                            (parts[0].toLongOrNull() ?: 0L) * 60000 + (parts[1].toLongOrNull() ?: 0L) * 1000
                        } else 0L
                        timestamps.add(SyncTimestampItem(timeMs = timeMs, formattedTime = formatted, label = label))
                    }
                } else if (!line.startsWith("- **Recorded on:**") && !line.startsWith("- **Duration:**")) {
                    if (!inNotesSection && !inTimestampsSection && line.isNotBlank()) {
                        notesContent.append(line).append("\n")
                    }
                }
            }
        } else {
            notesContent.append(text)
        }

        return Triple(title, notesContent.toString().trim(), timestamps)
    }

    private fun getAudioDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
