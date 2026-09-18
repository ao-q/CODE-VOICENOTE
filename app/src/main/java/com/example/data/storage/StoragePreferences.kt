package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import java.io.File

class StoragePreferences(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_notes_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_CONFIGURED = "is_storage_configured"
        private const val KEY_STORAGE_PATH = "selected_storage_path"
        private const val KEY_FOLDER_NAME = "storage_folder_name"
        private const val DEFAULT_FOLDER_NAME = "VoiceNotes"
    }

    var isStorageConfigured: Boolean
        get() = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_CONFIGURED, value).apply()

    var storageFolderName: String
        get() = prefs.getString(KEY_FOLDER_NAME, DEFAULT_FOLDER_NAME) ?: DEFAULT_FOLDER_NAME
        set(value) = prefs.edit().putString(KEY_FOLDER_NAME, value).apply()

    var customStoragePath: String?
        get() = prefs.getString(KEY_STORAGE_PATH, null)
        set(value) = prefs.edit().putString(KEY_STORAGE_PATH, value).apply()

    fun getEffectiveBaseDirectory(): File {
        val custom = customStoragePath
        val base = if (!custom.isNullOrBlank()) {
            File(custom)
        } else {
            // Default to app external files directory under documents/VoiceNotes
            val docDir = context.getExternalFilesDir(null) ?: context.filesDir
            File(docDir, storageFolderName)
        }
        if (!base.exists()) {
            base.mkdirs()
        }
        return base
    }
}
