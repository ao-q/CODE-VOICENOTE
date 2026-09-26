package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.data.model.CustomNoteTag
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class StoragePreferences(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("voice_notes_storage_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_CONFIGURED = "is_storage_configured"
        private const val KEY_STORAGE_PATH = "selected_storage_path"
        private const val KEY_FOLDER_NAME = "storage_folder_name"
        private const val KEY_STORAGE_URI = "selected_storage_uri"
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
        private const val KEY_IS_MINIMAL_MODE = "is_minimal_mode"
        private const val KEY_CUSTOM_TAGS = "custom_tags_json"
        private const val KEY_NOTE_TAGS = "note_tag_assignments_json"
        private const val DEFAULT_FOLDER_NAME = "VoiceNotes"
    }

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_IS_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_IS_DARK_THEME, value).apply()

    var isMinimalMode: Boolean
        get() = prefs.getBoolean(KEY_IS_MINIMAL_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_MINIMAL_MODE, value).apply()

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

    fun getCustomTags(): List<CustomNoteTag> {
        val jsonStr = prefs.getString(KEY_CUSTOM_TAGS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<CustomNoteTag>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    CustomNoteTag(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        emoji = obj.optString("emoji", "🏷️"),
                        name = obj.optString("name", "Tag"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveCustomTags(tags: List<CustomNoteTag>) {
        try {
            val jsonArray = JSONArray()
            tags.forEach { tag ->
                val obj = JSONObject()
                obj.put("id", tag.id)
                obj.put("emoji", tag.emoji)
                obj.put("name", tag.name)
                obj.put("createdAt", tag.createdAt)
                jsonArray.put(obj)
            }
            prefs.edit().putString(KEY_CUSTOM_TAGS, jsonArray.toString()).apply()
        } catch (_: Exception) {
        }
    }

    fun getNoteTagAssignments(): Map<Long, List<String>> {
        val jsonStr = prefs.getString(KEY_NOTE_TAGS, null) ?: return emptyMap()
        return try {
            val jsonObject = JSONObject(jsonStr)
            val map = mutableMapOf<Long, List<String>>()
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val noteId = key.toLongOrNull() ?: continue
                val arr = jsonObject.optJSONArray(key) ?: continue
                val tagIds = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    tagIds.add(arr.getString(i))
                }
                map[noteId] = tagIds
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveNoteTagAssignments(assignments: Map<Long, List<String>>) {
        try {
            val jsonObject = JSONObject()
            assignments.forEach { (noteId, tagIds) ->
                val arr = JSONArray()
                tagIds.forEach { arr.put(it) }
                jsonObject.put(noteId.toString(), arr)
            }
            prefs.edit().putString(KEY_NOTE_TAGS, jsonObject.toString()).apply()
        } catch (_: Exception) {
        }
    }

    /**
     * Exports all user preferences and note tags as a retraceable, formatted JSON string.
     */
    fun exportSettingsJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("isDarkTheme", isDarkTheme)
        root.put("isMinimalMode", isMinimalMode)
        root.put("storageFolderName", storageFolderName)
        root.put("customStoragePath", customStoragePath ?: "")

        val tagsArray = JSONArray()
        getCustomTags().forEach { tag ->
            val obj = JSONObject()
            obj.put("id", tag.id)
            obj.put("emoji", tag.emoji)
            obj.put("name", tag.name)
            obj.put("createdAt", tag.createdAt)
            tagsArray.put(obj)
        }
        root.put("customTags", tagsArray)

        val assignmentsObj = JSONObject()
        getNoteTagAssignments().forEach { (noteId, tagIds) ->
            val arr = JSONArray()
            tagIds.forEach { arr.put(it) }
            assignmentsObj.put(noteId.toString(), arr)
        }
        root.put("noteTagAssignments", assignmentsObj)

        return root.toString(2)
    }

    /**
     * Imports preferences and custom tags from a JSON backup.
     */
    fun importSettingsJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            if (root.has("isDarkTheme")) {
                isDarkTheme = root.getBoolean("isDarkTheme")
            }
            if (root.has("isMinimalMode")) {
                isMinimalMode = root.getBoolean("isMinimalMode")
            }
            if (root.has("storageFolderName")) {
                val folder = root.getString("storageFolderName")
                if (folder.isNotBlank()) storageFolderName = folder
            }
            if (root.has("customStoragePath")) {
                val path = root.getString("customStoragePath")
                if (path.isNotBlank()) customStoragePath = path
            }

            if (root.has("customTags")) {
                val arr = root.getJSONArray("customTags")
                val tagsList = mutableListOf<CustomNoteTag>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    tagsList.add(
                        CustomNoteTag(
                            id = obj.optString("id", System.currentTimeMillis().toString()),
                            emoji = obj.optString("emoji", "🏷️"),
                            name = obj.optString("name", "Tag"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                saveCustomTags(tagsList)
            }

            if (root.has("noteTagAssignments")) {
                val assignmentsObj = root.getJSONObject("noteTagAssignments")
                val map = mutableMapOf<Long, List<String>>()
                val keys = assignmentsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val noteId = key.toLongOrNull() ?: continue
                    val arr = assignmentsObj.optJSONArray(key) ?: continue
                    val tagIds = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        tagIds.add(arr.getString(i))
                    }
                    map[noteId] = tagIds
                }
                saveNoteTagAssignments(map)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns the physical directory on the device where all voice notes will be stored.
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
