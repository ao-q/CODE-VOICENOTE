package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

sealed interface MarkdownBlock {
    data object Divider : MarkdownBlock
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Blockquote(val text: String) : MarkdownBlock
    data class TaskItem(val checked: Boolean, val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class FlowchartArrow(val arrow: String) : MarkdownBlock
    data class Table(val rawLines: List<String>) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data object EmptyLine : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()

    var inCodeBlock = false
    var codeBlockLanguage = ""
    val codeBlockLines = mutableListOf<String>()

    val tableLines = mutableListOf<String>()

    fun flushTable() {
        if (tableLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Table(tableLines.toList()))
            tableLines.clear()
        }
    }

    fun flushCodeBlock() {
        if (codeBlockLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.CodeBlock(codeBlockLines.joinToString("\n"), codeBlockLanguage))
            codeBlockLines.clear()
            inCodeBlock = false
            codeBlockLanguage = ""
        }
    }

    for (line in lines) {
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushTable()
            if (inCodeBlock) {
                flushCodeBlock()
            } else {
                inCodeBlock = true
                codeBlockLanguage = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBlockLines.add(line)
            continue
        }

        if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length > 2) {
            tableLines.add(trimmed)
            continue
        } else {
            flushTable()
        }

        when {
            trimmed == "---" || trimmed == "***" -> {
                blocks.add(MarkdownBlock.Divider)
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("> ") -> {
                blocks.add(MarkdownBlock.Blockquote(trimmed.removePrefix("> ").trim()))
            }
            trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                blocks.add(MarkdownBlock.TaskItem(false, trimmed.substring(6).trim()))
            }
            trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
            trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                blocks.add(MarkdownBlock.TaskItem(true, trimmed.substring(6).trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                blocks.add(MarkdownBlock.BulletItem(trimmed.substring(2).trim()))
            }
            trimmed in setOf("↓", "⬇", "↓↓", "↑", "⬆", "▲", "▼", "→", "➔", "──►", "←", "◄", "↔", "⇄", "⇒") -> {
                blocks.add(MarkdownBlock.FlowchartArrow(trimmed))
            }
            trimmed.isEmpty() -> {
                blocks.add(MarkdownBlock.EmptyLine)
            }
            else -> {
                blocks.add(MarkdownBlock.Paragraph(trimmed))
            }
        }
    }

    flushTable()
    if (inCodeBlock) {
        flushCodeBlock()
    }

    return blocks
}

/**
 * Beautiful, native Material 3 Markdown preview composable.
 * Supports H1, H2, H3, bold, italic, strikethrough, inline code, code blocks,
 * blockquotes, bullet lists, checkboxes, interactive timestamp badges,
 * native grid tables, and flowchart down arrow sequences.
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

    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Divider -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                is MarkdownBlock.Heading -> {
                    when (block.level) {
                        1 -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = parseInlineMarkdown(block.text),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                        2 -> {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = parseInlineMarkdown(block.text),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        else -> {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = parseInlineMarkdown(block.text),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                is MarkdownBlock.Blockquote -> {
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
                            text = parseInlineMarkdown(block.text),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownBlock.TaskItem -> {
                    TaskItemRow(
                        checked = block.checked,
                        content = block.text,
                        onTimestampClick = onTimestampClick
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    BulletItemRow(
                        content = block.text,
                        onTimestampClick = onTimestampClick
                    )
                }
                is MarkdownBlock.FlowchartArrow -> {
                    FlowchartArrowRow(arrow = block.arrow)
                }
                is MarkdownBlock.Table -> {
                    MarkdownTableView(block.rawLines)
                }
                is MarkdownBlock.CodeBlock -> {
                    if (block.language.equals("mermaid", ignoreCase = true)) {
                        MermaidFlowchartBlockView(block.code)
                    } else {
                        CodeBlockView(block.code)
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    MarkdownParagraph(content = block.text, onTimestampClick = onTimestampClick)
                }
                is MarkdownBlock.EmptyLine -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Beautiful Material 3 Table Component for Markdown tables.
 */
@Composable
private fun MarkdownTableView(rawLines: List<String>) {
    if (rawLines.isEmpty()) return

    val cleanRows = rawLines.map { line ->
        line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { it.trim() }
    }.filter { it.isNotEmpty() }

    if (cleanRows.isEmpty()) return

    val isDividerRow: (List<String>) -> Boolean = { row ->
        row.all { cell -> cell.replace("-", "").replace(":", "").isBlank() }
    }

    val headerRow = cleanRows.firstOrNull()
    val dataRows = cleanRows.drop(1).filterNot { isDividerRow(it) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            // Header Row
            if (headerRow != null) {
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headerRow.forEach { header ->
                        Text(
                            text = parseInlineMarkdown(header),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.widthIn(min = 80.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Data Rows
            dataRows.forEachIndexed { index, row ->
                val rowBg = if (index % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) else Color.Transparent
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { cell ->
                        Text(
                            text = parseInlineMarkdown(cell),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.widthIn(min = 80.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Centered Flowchart Down Arrow Row.
 */
@Composable
private fun FlowchartArrowRow(arrow: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = arrow,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Visual Flowchart Mermaid Code Block Preview.
 */
@Composable
private fun MermaidFlowchartBlockView(code: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountTree,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Flowchart Diagram (Mermaid)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF38BDF8)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = code,
                color = Color(0xFFE2E8F0),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
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
