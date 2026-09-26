package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreateCustomTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (emoji: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickEmojis = listOf(
        "⭐", "💡", "💼", "🎓", "🚀", "📌", "❤️", "🎵",
        "🏷️", "🔑", "🏆", "✈️", "📚", "☕", "💻", "🎯"
    )

    var selectedEmoji by remember { mutableStateOf("⭐") }
    var tagName by remember { mutableStateOf("") }
    var customEmojiInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("create_custom_tag_dialog"),
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "New Tag",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Emoji picker grid
                Text(
                    text = "Select Emoji",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickEmojis) { emoji ->
                        val isSelected = selectedEmoji == emoji && customEmojiInput.isBlank()
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    selectedEmoji = emoji
                                    customEmojiInput = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }

                // Custom Emoji text input
                OutlinedTextField(
                    value = customEmojiInput,
                    onValueChange = {
                        customEmojiInput = it
                        if (it.isNotBlank()) {
                            selectedEmoji = it.trim()
                        }
                    },
                    placeholder = { Text("Or custom emoji (e.g. 🔥)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tag Name
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text("Tag Name") },
                    placeholder = { Text("e.g. Work, Ideas") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalEmoji = if (customEmojiInput.isNotBlank()) customEmojiInput.trim() else selectedEmoji
                    val finalName = tagName.trim().ifBlank { "Tag" }
                    onConfirm(finalEmoji, finalName)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_create_tag_button")
            ) {
                Text("Add Tag")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_create_tag_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
