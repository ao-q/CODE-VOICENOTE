package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timestamp_markers",
    foreignKeys = [
        ForeignKey(
            entity = VoiceNoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["voiceNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["voiceNoteId"])]
)
data class TimestampMarkerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val voiceNoteId: Long,
    val timeMs: Long,
    val formattedTime: String,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)
