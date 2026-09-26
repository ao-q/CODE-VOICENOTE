package com.example.data.model

data class CustomNoteTag(
    val id: String,
    val emoji: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
