package com.example.data.storage

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object StorageUriHelper {

    /**
     * Resolves a Storage Access Framework tree Uri into a real File path on the device, if possible.
     */
    fun getPathFromTreeUri(context: Context, treeUri: Uri): String? {
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri) ?: return null
            if (docId.startsWith("primary:", ignoreCase = true)) {
                val relativePath = docId.substringAfter(":", "").trimStart('/')
                val primaryStorage = Environment.getExternalStorageDirectory()
                val target = if (relativePath.isNotBlank()) File(primaryStorage, relativePath) else primaryStorage
                return target.absolutePath
            } else if (docId.contains(":")) {
                val parts = docId.split(":")
                val volumeId = parts[0]
                val relativePath = if (parts.size > 1) parts[1].trimStart('/') else ""
                val extDirs = context.getExternalFilesDirs(null)
                for (dir in extDirs) {
                    if (dir != null) {
                        val path = dir.absolutePath
                        val idx = path.indexOf("/Android/data")
                        if (idx != -1) {
                            val root = path.substring(0, idx)
                            if (root.contains(volumeId)) {
                                val target = if (relativePath.isNotBlank()) File(root, relativePath) else File(root)
                                return target.absolutePath
                            }
                        }
                    }
                }
                // Fallback attempt with /storage/<volumeId>
                val potentialStorage = File("/storage/$volumeId", relativePath)
                if (potentialStorage.exists() || potentialStorage.parentFile?.exists() == true) {
                    return potentialStorage.absolutePath
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Standard visible public storage locations on Android devices.
     */
    fun getPublicDocumentsDir(): File {
        val publicDocs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (publicDocs != null && (publicDocs.exists() || publicDocs.mkdirs())) {
            return publicDocs
        }
        return Environment.getExternalStorageDirectory()
    }

    fun getPublicRecordingsDir(): File {
        val publicRecordings = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS)
        if (publicRecordings != null && (publicRecordings.exists() || publicRecordings.mkdirs())) {
            return publicRecordings
        }
        return getPublicDocumentsDir()
    }

    fun getPublicMusicDir(): File {
        val publicMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (publicMusic != null && (publicMusic.exists() || publicMusic.mkdirs())) {
            return publicMusic
        }
        return getPublicDocumentsDir()
    }
}
