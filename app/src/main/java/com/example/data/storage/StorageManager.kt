package com.example.data.storage

import android.content.Context
import android.media.MediaScannerConnection
import com.example.data.model.SyncTimestampItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecordingFileLocations(
    val folderDirectory: File,
    val audioFile: File,
    val notesFile: File,
    val resolvedTitle: String,
    val isAutoNamed: Boolean
)

data class RenamedFileLocations(
    val newFolderDirectory: File,
    val newAudioFile: File,
    val newNotesFile: File,
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
        val notesFile = File(targetFolder, "$baseFileName.md")

        return RecordingFileLocations(
            folderDirectory = targetFolder,
            audioFile = audioFile,
            notesFile = notesFile,
            resolvedTitle = resolvedTitle,
            isAutoNamed = isAutoNamed
        )
    }

    /**
     * Writes written notes to the titled .md (Markdown) file.
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
            builder.append("# ").append(title).append("\n\n")
            builder.append("- **Recorded on:** ").append(formatReadableDate(createdAt)).append("\n")
            val mins = (durationMs / 1000) / 60
            val secs = (durationMs / 1000) % 60
            builder.append(String.format(Locale.getDefault(), "- **Duration:** %02d:%02d\n\n", mins, secs))

            builder.append("## Notes\n\n")
            builder.append(if (content.isBlank()) "*(No additional notes taken)*" else content).append("\n\n")

            if (timestamps.isNotEmpty()) {
                builder.append("## Timestamps\n\n")
                for (ts in timestamps) {
                    builder.append("- **[${ts.formattedTime}]** ${ts.label}\n")
                }
                builder.append("\n")
            }
            notesFile.writeText(builder.toString(), Charsets.UTF_8)
            notifyMediaScanner(notesFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Automatically renames the voice note folder, .mp3, and .md
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
        val newNotesFile = File(newFolderDir, "$safeNewTitle.md")

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

        // Delete old txt or md file if differently named
        val activeNotesFile = if (currentNotesFile.parentFile?.absolutePath != newFolderDir.absolutePath) {
            File(newFolderDir, currentNotesFile.name)
        } else {
            currentNotesFile
        }
        if (activeNotesFile.exists() && activeNotesFile.absolutePath != newNotesFile.absolutePath) {
            activeNotesFile.delete()
        }
        val legacyTxtFile = File(newFolderDir, "$safeNewTitle.txt")
        if (legacyTxtFile.exists()) {
            legacyTxtFile.delete()
        }
        val oldSyncKeyFile = File(newFolderDir, "sync_keys.json")
        if (oldSyncKeyFile.exists()) {
            oldSyncKeyFile.delete()
        }

        // Re-write notes file with updated title
        writeNotesFile(newNotesFile, newTitle, noteContent, createdAt, durationMs, timestamps)
        notifyMediaScanner(finalAudioFile, newNotesFile)

        return RenamedFileLocations(
            newFolderDirectory = newFolderDir,
            newAudioFile = finalAudioFile,
            newNotesFile = newNotesFile,
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
     * Notifies Android MediaStore and system file indexer so the files appear immediately in device file managers.
     */
    fun notifyMediaScanner(vararg files: File?) {
        try {
            val paths = files.filterNotNull().filter { it.exists() }.map { it.absolutePath }.toTypedArray()
            if (paths.isNotEmpty()) {
                MediaScannerConnection.scanFile(context, paths, null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
