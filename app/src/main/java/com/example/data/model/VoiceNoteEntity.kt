package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_notes",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class VoiceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val folderId: Long? = null,
    val title: String,
    val audioFileName: String,
    val audioFilePath: String,
    val noteFileName: String,
    val noteFilePath: String,
    val noteContent: String = "",
    val noteFolderDirectory: String,
    val durationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val syncKey: String = ""
)
