package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.File

class StoragePreferences(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_notes_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_CONFIGURED = "is_storage_configured"
        private const val KEY_STORAGE_PATH = "selected_storage_path"
        private const val KEY_FOLDER_NAME = "storage_folder_name"
        private const val KEY_STORAGE_URI = "selected_storage_uri"
        private const val DEFAULT_FOLDER_NAME = "VoiceNotes"
    }

    var isStorageConfigured: Boolean
        get() = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CONFIGURED, value).apply()

    var storageFolderName: String
        get() = prefs.getString(KEY_FOLDER_NAME, DEFAULT_FOLDER_NAME) ?: DEFAULT_FOLDER_NAME
        set(value) = prefs.edit().putString(KEY_FOLDER_NAME, value.ifBlank { DEFAULT_FOLDER_NAME }).apply()

    var customStoragePath: String?
        get() = prefs.getString(KEY_STORAGE_PATH, null)
        set(value) = prefs.edit().putString(KEY_STORAGE_PATH, value).apply()

    var storageTreeUri: String?
        get() = prefs.getString(KEY_STORAGE_URI, null)
        set(value) = prefs.edit().putString(KEY_STORAGE_URI, value).apply()

    /**
     * Returns the physical directory on the device where all voice notes will be stored.
     * Guaranteed to point to a real accessible location (e.g. /storage/emulated/0/Documents/VoiceNotes
     * or a user-selected folder), NOT a hidden app-internal cache.
     */
    fun getEffectiveBaseDirectory(): File {
        val custom = customStoragePath
        val baseDir: File = if (!custom.isNullOrBlank()) {
            val file = File(custom)
            if (file.name.equals(storageFolderName, ignoreCase = true)) {
                file
            } else {
                File(file, storageFolderName)
            }
        } else {
            // Default to real public Documents folder on external storage:
            // e.g. /storage/emulated/0/Documents/VoiceNotes
            val publicDocuments = StorageUriHelper.getPublicDocumentsDir()
            File(publicDocuments, storageFolderName)
        }

        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    /**
     * Produces a human-readable breadcrumb location for the user interface.
     */
    fun getFriendlyStoragePath(): String {
        val dir = getEffectiveBaseDirectory()
        val path = dir.absolutePath
        val internalRoot = Environment.getExternalStorageDirectory().absolutePath

        return if (path.startsWith(internalRoot)) {
            val relative = path.substring(internalRoot.length).trimStart('/')
            "Internal Storage > " + relative.replace("/", " > ")
        } else {
            path
        }
    }
}
