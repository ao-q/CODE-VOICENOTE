package com.example.data.storage

import android.content.Context
import com.example.data.model.SyncKeyPayload
import com.example.data.model.SyncTimestampItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingFileLocations(
    val folderDirectory: File,
    val audioFile: File,
    val notesFile: File,
    val syncKeyFile: File,
    val resolvedTitle: String,
    val isAutoNamed: Boolean
)

data class RenamedFileLocations(
    val newFolderDirectory: File,
    val newAudioFile: File,
    val newNotesFile: File,
    val newSyncKeyFile: File,
    val newTitle: String
)

class StorageManager(private val context: Context) {
    private val preferences = StoragePreferences(context)

    fun getBaseDirectory(): File {
        return preferences.getEffectiveBaseDirectory()
    }

    fun sanitizeFilename(name: String): String {
        val sanitized = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return if (sanitized.isBlank()) "VoiceNote" else sanitized
    }

    fun formatTimestampDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatReadableDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Resolves the parent directory for a nested folder structure.
     */
    fun resolveParentFolder(folderNames: List<String>): File {
        var current = getBaseDirectory()
        for (name in folderNames) {
            val safeName = sanitizeFilename(name)
            current = File(current, safeName)
            if (!current.exists()) {
                current.mkdirs()
            }
        }
        return current
    }

    /**
     * Prepares the titled folder and file destinations for a recording.
     * If title is not available or blank: names recording after time of recording and metadata.
     */
    fun prepareRecordingLocations(
        parentFolderHierarchy: List<String>,
        userTitle: String?,
        timestamp: Long
    ): RecordingFileLocations {
        val parentDir = resolveParentFolder(parentFolderHierarchy)
        val timeStampStr = formatTimestampDate(timestamp)

        val (folderName, resolvedTitle, isAutoNamed) = if (!userTitle.isNullOrBlank()) {
            val safeTitle = sanitizeFilename(userTitle)
            Triple(safeTitle, userTitle.trim(), false)
        } else {
            val defaultName = "Recording_$timeStampStr"
            Triple(defaultName, defaultName, true)
        }

        // Dedicated titled folder for this voice note
        var targetFolder = File(parentDir, folderName)
        if (targetFolder.exists() && isAutoNamed) {
            targetFolder = File(parentDir, "${folderName}_${System.currentTimeMillis() % 1000}")
        }
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }

        val baseFileName = if (!userTitle.isNullOrBlank()) {
            sanitizeFilename(userTitle)
        } else {
            "Recording_$timeStampStr"
        }

        val audioFile = File(targetFolder, "$baseFileName.mp3")
        val notesFile = File(targetFolder, "$baseFileName.txt")
        val syncKeyFile = File(targetFolder, "sync_keys.json")

        return RecordingFileLocations(
            folderDirectory = targetFolder,
            audioFile = audioFile,
            notesFile = notesFile,
            syncKeyFile = syncKeyFile,
            resolvedTitle = resolvedTitle,
            isAutoNamed = isAutoNamed
        )
    }

    /**
     * Writes written notes to the titled .txt file.
     */
    fun writeNotesFile(
        notesFile: File,
        title: String,
        content: String,
        createdAt: Long,
        durationMs: Long,
        timestamps: List<SyncTimestampItem>
    ) {
        try {
            val builder = StringBuilder()
            builder.append("=========================================\n")
            builder.append("VOICE NOTE: ").append(title).append("\n")
            builder.append("Recorded on: ").append(formatReadableDate(createdAt)).append("\n")
            builder.append(String.format(Locale.getDefault(), "Duration: %02d:%02d\n", (durationMs / 1000) / 60, (durationMs / 1000) % 60))
            builder.append("=========================================\n\n")
            builder.append("--- WRITTEN NOTES ---\n")
            builder.append(if (content.isBlank()) "(No additional notes taken)" else content).append("\n\n")

            if (timestamps.isNotEmpty()) {
                builder.append("--- FLAGGED TIMESTAMPS ---\n")
                for (ts in timestamps) {
                    builder.append("[${ts.formattedTime}] ${ts.label}\n")
                }
                builder.append("\n")
            }
            notesFile.writeText(builder.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Writes the Sync Keys metadata JSON file into the note's folder.
     */
    fun writeSyncKeysFile(syncKeyFile: File, payload: SyncKeyPayload) {
        try {
            syncKeyFile.writeText(payload.toJsonString(), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Automatically renames the voice note folder, .mp3, .txt, and sync_keys.json
     * on the local storage device to stay perfectly in sync.
     */
    fun renameVoiceNote(
        currentFolderDir: File,
        currentAudioFile: File,
        currentNotesFile: File,
        newTitle: String,
        noteContent: String,
        createdAt: Long,
        durationMs: Long,
        timestamps: List<SyncTimestampItem>
    ): RenamedFileLocations {
        val safeNewTitle = sanitizeFilename(newTitle)
        val parentDir = currentFolderDir.parentFile ?: getBaseDirectory()

        // New folder location
        val newFolderDir = if (currentFolderDir.name != safeNewTitle) {
            var dest = File(parentDir, safeNewTitle)
            if (dest.exists() && dest.absolutePath != currentFolderDir.absolutePath) {
                dest = File(parentDir, "${safeNewTitle}_${System.currentTimeMillis() % 1000}")
            }
            val renamed = currentFolderDir.renameTo(dest)
            if (renamed) dest else currentFolderDir
        } else {
            currentFolderDir
        }

        // Now inside the folder (whether renamed or not), rename the files
        val newAudioFile = File(newFolderDir, "$safeNewTitle.mp3")
        val newNotesFile = File(newFolderDir, "$safeNewTitle.txt")
        val syncKeyFile = File(newFolderDir, "sync_keys.json")

        // If audio file name needs changing:
        val activeAudioFile = if (currentAudioFile.parentFile?.absolutePath != newFolderDir.absolutePath) {
            File(newFolderDir, currentAudioFile.name)
        } else {
            currentAudioFile
        }

        val finalAudioFile = if (activeAudioFile.exists() && activeAudioFile.absolutePath != newAudioFile.absolutePath) {
            activeAudioFile.renameTo(newAudioFile)
            newAudioFile
        } else if (newAudioFile.exists()) {
            newAudioFile
        } else {
            activeAudioFile
        }

        // Delete old txt file if differently named
        val activeNotesFile = if (currentNotesFile.parentFile?.absolutePath != newFolderDir.absolutePath) {
            File(newFolderDir, currentNotesFile.name)
        } else {
            currentNotesFile
        }
        if (activeNotesFile.exists() && activeNotesFile.absolutePath != newNotesFile.absolutePath) {
            activeNotesFile.delete()
        }

        // Re-write notes file with updated title
        writeNotesFile(newNotesFile, newTitle, noteContent, createdAt, durationMs, timestamps)

        // Re-write sync_keys.json
        val payload = SyncKeyPayload(
            title = newTitle,
            audioFileName = finalAudioFile.name,
            durationMs = durationMs,
            createdAt = createdAt,
            noteContent = noteContent,
            timestamps = timestamps
        )
        writeSyncKeysFile(syncKeyFile, payload)

        return RenamedFileLocations(
            newFolderDirectory = newFolderDir,
            newAudioFile = finalAudioFile,
            newNotesFile = newNotesFile,
            newSyncKeyFile = syncKeyFile,
            newTitle = newTitle
        )
    }

    /**
     * Moves a note's titled folder to a different parent folder.
     */
    fun moveNoteFolder(currentFolderDir: File, targetParentDir: File): File {
        if (!targetParentDir.exists()) {
            targetParentDir.mkdirs()
        }
        var targetFolder = File(targetParentDir, currentFolderDir.name)
        if (targetFolder.exists() && targetFolder.absolutePath != currentFolderDir.absolutePath) {
            targetFolder = File(targetParentDir, "${currentFolderDir.name}_${System.currentTimeMillis() % 1000}")
        }
        val moved = currentFolderDir.renameTo(targetFolder)
        return if (moved) targetFolder else currentFolderDir
    }

    /**
     * Deletes the note's titled folder and all contents from device storage.
     */
    fun deleteNoteFolder(folderDir: File) {
        try {
            if (folderDir.exists() && folderDir.isDirectory) {
                folderDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
