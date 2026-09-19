package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TimestampFlagColor

/**
 * Advanced Markdown formatting toolbar.
 * Provides quick actions to wrap or insert markdown tokens:
 * Bold (B), Italic (I), Strikethrough (S), Heading (H1/H2), Bullet List,
 * Checkbox Task List, Blockquote, Inline Code, and Icon-only Timestamp.
 */
@Composable
fun MarkdownEditorToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    currentTimestampFormatted: String? = null,
    onInsertTimestamp: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Bold (B)
            FormatIconButton(
                icon = Icons.Default.FormatBold,
                description = "Bold",
                testTag = "format_bold_button",
                onClick = {
                    onValueChange(wrapSelection(value, "**", "**", "bold"))
                }
            )

            // Italic (I)
            FormatIconButton(
                icon = Icons.Default.FormatItalic,
                description = "Italic",
                testTag = "format_italic_button",
                onClick = {
                    onValueChange(wrapSelection(value, "*", "*", "italic"))
                }
            )

            // Strikethrough (S)
            FormatIconButton(
                icon = Icons.Default.FormatStrikethrough,
                description = "Strikethrough",
                testTag = "format_strikethrough_button",
                onClick = {
                    onValueChange(wrapSelection(value, "~~", "~~", "text"))
                }
            )

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Heading 1 (H1)
            FormatTextButton(
                label = "H1",
                description = "Heading 1",
                testTag = "format_h1_button",
                onClick = {
                    onValueChange(insertLinePrefix(value, "# "))
                }
            )

            // Heading 2 (H2)
            FormatTextButton(
                label = "H2",
                description = "Heading 2",
                testTag = "format_h2_button",
                onClick = {
                    onValueChange(insertLinePrefix(value, "## "))
                }
            )

            // Bullet List
            FormatIconButton(
                icon = Icons.Default.FormatListBulleted,
                description = "Bullet List",
                testTag = "format_bullet_button",
                onClick = {
                    onValueChange(insertLinePrefix(value, "- "))
                }
            )

            // Task List / Checkbox
            FormatIconButton(
                icon = Icons.Default.CheckBox,
                description = "Task Checkbox",
                testTag = "format_checkbox_button",
                onClick = {
                    onValueChange(insertLinePrefix(value, "- [ ] "))
                }
            )

            // Blockquote
            FormatIconButton(
                icon = Icons.Default.FormatQuote,
                description = "Quote",
                testTag = "format_quote_button",
                onClick = {
                    onValueChange(insertLinePrefix(value, "> "))
                }
            )

            // Code
            FormatIconButton(
                icon = Icons.Default.Code,
                description = "Code",
                testTag = "format_code_button",
                onClick = {
                    onValueChange(wrapSelection(value, "`", "`", "code"))
                }
            )

            // Timestamp (Icon only - no text, as explicitly requested)
            if (onInsertTimestamp != null || currentTimestampFormatted != null) {
                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                IconButton(
                    onClick = {
                        if (onInsertTimestamp != null) {
                            onInsertTimestamp()
                        } else if (currentTimestampFormatted != null) {
                            val tag = "\n- **[$currentTimestampFormatted]** "
                            onValueChange(insertTextAtCursor(value, tag))
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("format_timestamp_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Insert Current Timestamp",
                        tint = TimestampFlagColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatIconButton(
    icon: ImageVector,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FormatTextButton(
    label: String,
    description: String,
    testTag: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .height(32.dp)
            .testTag(testTag),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Helper to wrap selected text or insert placeholder at cursor.
 */
fun wrapSelection(
    tfv: TextFieldValue,
    prefix: String,
    suffix: String,
    defaultPlaceholder: String
): TextFieldValue {
    val text = tfv.text
    val selection = tfv.selection

    return if (!selection.collapsed) {
        val selectedText = text.substring(selection.start, selection.end)
        val newText = text.replaceRange(selection.start, selection.end, "$prefix$selectedText$suffix")
        val newCursorPos = selection.start + prefix.length + selectedText.length + suffix.length
        TextFieldValue(text = newText, selection = TextRange(newCursorPos))
    } else {
        val cursorPos = selection.start
        val newText = text.replaceRange(cursorPos, cursorPos, "$prefix$defaultPlaceholder$suffix")
        val selectStart = cursorPos + prefix.length
        val selectEnd = selectStart + defaultPlaceholder.length
        TextFieldValue(text = newText, selection = TextRange(selectStart, selectEnd))
    }
}

/**
 * Helper to insert prefix on a new line or at current line.
 */
fun insertLinePrefix(tfv: TextFieldValue, linePrefix: String): TextFieldValue {
    val text = tfv.text
    val cursorPos = tfv.selection.start

    val needsNewline = cursorPos > 0 && text.getOrNull(cursorPos - 1) != '\n'
    val insertion = if (needsNewline) "\n$linePrefix" else linePrefix

    val newText = text.replaceRange(cursorPos, cursorPos, insertion)
    val newCursor = cursorPos + insertion.length
    return TextFieldValue(text = newText, selection = TextRange(newCursor))
}

/**
 * Helper to insert plain text at cursor.
 */
fun insertTextAtCursor(tfv: TextFieldValue, insertion: String): TextFieldValue {
    val text = tfv.text
    val cursorPos = tfv.selection.start
    val newText = text.replaceRange(cursorPos, cursorPos, insertion)
    val newCursor = cursorPos + insertion.length
    return TextFieldValue(text = newText, selection = TextRange(newCursor))
}
