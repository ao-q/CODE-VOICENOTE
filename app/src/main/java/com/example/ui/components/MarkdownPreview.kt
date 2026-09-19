package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TimestampFlagColor

/**
 * Beautiful, native Material 3 Markdown preview composable.
 * Supports H1, H2, H3, bold, italic, strikethrough, inline code, code blocks,
 * blockquotes, bullet lists, checkboxes, and interactive timestamp badges.
 */
@Composable
fun MarkdownPreview(
    markdown: String,
    modifier: Modifier = Modifier,
    onTimestampClick: ((String) -> Unit)? = null
) {
    if (markdown.isBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No notes to preview. Switch to Edit mode to write notes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val lines = markdown.lines()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            // Code block delimiter check
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    val codeContent = codeBlockLines.joinToString("\n")
                    CodeBlockView(codeContent)
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                continue
            }

            when {
                // Horizontal divider
                trimmed == "---" || trimmed == "***" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // Heading 1
                trimmed.startsWith("# ") -> {
                    val content = trimmed.removePrefix("# ").trim()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parseInlineMarkdown(content),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }

                // Heading 2
                trimmed.startsWith("## ") -> {
                    val content = trimmed.removePrefix("## ").trim()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parseInlineMarkdown(content),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Heading 3
                trimmed.startsWith("### ") -> {
                    val content = trimmed.removePrefix("### ").trim()
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parseInlineMarkdown(content),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Blockquote
                trimmed.startsWith("> ") -> {
                    val quoteContent = trimmed.removePrefix("> ").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = parseInlineMarkdown(quoteContent),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Checkbox / Task list item
                trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                    val taskContent = trimmed.substring(6).trim()
                    TaskItemRow(checked = false, content = taskContent, onTimestampClick = onTimestampClick)
                }

                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                    val taskContent = trimmed.substring(6).trim()
                    TaskItemRow(checked = true, content = taskContent, onTimestampClick = onTimestampClick)
                }

                // Bullet list item
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val bulletContent = trimmed.substring(2).trim()
                    BulletItemRow(content = bulletContent, onTimestampClick = onTimestampClick)
                }

                // Empty line
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Standard paragraph text
                else -> {
                    MarkdownParagraph(content = trimmed, onTimestampClick = onTimestampClick)
                }
            }
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            CodeBlockView(codeBlockLines.joinToString("\n"))
        }
    }
}

@Composable
private fun BulletItemRow(
    content: String,
    onTimestampClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp, start = 4.dp, end = 8.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        MarkdownParagraph(content = content, onTimestampClick = onTimestampClick)
    }
}

@Composable
private fun TaskItemRow(
    checked: Boolean,
    content: String,
    onTimestampClick: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = if (checked) "Completed" else "Incomplete",
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = parseInlineMarkdown(content),
            style = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None
            ),
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CodeBlockView(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
    ) {
        Text(
            text = code,
            color = Color(0xFFD4D4D4),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun MarkdownParagraph(
    content: String,
    onTimestampClick: ((String) -> Unit)? = null
) {
    // Check if the content is primarily a timestamp marker like `**[00:05]** Note text`
    val timestampRegex = Regex("""(?:\*\*)?\[(\d{1,2}:\d{2})\](?:\*\*)?(.*)""")
    val match = timestampRegex.matchEntire(content.trim())

    if (match != null) {
        val timestamp = match.groupValues[1]
        val remaining = match.groupValues[2].trim()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TimestampFlagColor.copy(alpha = 0.15f),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (onTimestampClick != null) Modifier.clickable { onTimestampClick(timestamp) }
                        else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = TimestampFlagColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TimestampFlagColor
                    )
                }
            }

            if (remaining.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = parseInlineMarkdown(remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        Text(
            text = parseInlineMarkdown(content),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Parses inline markdown: **bold**, *italic*, ~~strikethrough~~, and `inline code`.
 */
fun parseInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // Inline code: `code`
            if (text[i] == '`') {
                val nextBacktick = text.indexOf('`', i + 1)
                if (nextBacktick != -1) {
                    val codeSnippet = text.substring(i + 1, nextBacktick)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x22888888),
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(" $codeSnippet ")
                    pop()
                    i = nextBacktick + 1
                    continue
                }
            }

            // Bold: **text**
            if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
                val nextDoubleAsterisk = text.indexOf("**", i + 2)
                if (nextDoubleAsterisk != -1) {
                    val boldText = text.substring(i + 2, nextDoubleAsterisk)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(boldText)
                    pop()
                    i = nextDoubleAsterisk + 2
                    continue
                }
            }

            // Strikethrough: ~~text~~
            if (i + 1 < len && text[i] == '~' && text[i + 1] == '~') {
                val nextTilde = text.indexOf("~~", i + 2)
                if (nextTilde != -1) {
                    val strikeText = text.substring(i + 2, nextTilde)
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(strikeText)
                    pop()
                    i = nextTilde + 2
                    continue
                }
            }

            // Italic: *text*
            if (text[i] == '*') {
                val nextAsterisk = text.indexOf('*', i + 1)
                if (nextAsterisk != -1 && (nextAsterisk + 1 >= len || text[nextAsterisk + 1] != '*')) {
                    val italicText = text.substring(i + 1, nextAsterisk)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(italicText)
                    pop()
                    i = nextAsterisk + 1
                    continue
                }
            }

            append(text[i])
            i++
        }
    }
}
