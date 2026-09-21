package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TimestampFlagColor

/**
 * Advanced Markdown formatting toolbar.
 * Provides quick actions to wrap or insert markdown tokens:
 * Bold (B), Italic (I), Strikethrough (S), Heading (H1/H2), Bullet List,
 * Checkbox Task List, Blockquote, Inline Code, Table Query Tool,
 * Flowchart Down Arrow Tool, and Icon-only Timestamp.
 */
@Composable
fun MarkdownEditorToolbar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    currentTimestampFormatted: String? = null,
    onInsertTimestamp: (() -> Unit)? = null
) {
    var showTableQueryDialog by remember { mutableStateOf(false) }
    var showFlowchartArrowDialog by remember { mutableStateOf(false) }

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

            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Table with Rows and Columns Query Tool
            FormatIconButton(
                icon = Icons.Default.TableChart,
                description = "Insert Markdown Table (Rows x Columns)",
                testTag = "format_table_button",
                onClick = {
                    showTableQueryDialog = true
                }
            )

            // Flowchart Down Arrow Tool
            FormatIconButton(
                icon = Icons.Default.ArrowDownward,
                description = "Insert Down Arrow / Flowchart",
                testTag = "format_flowchart_arrow_button",
                onClick = {
                    showFlowchartArrowDialog = true
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

    // Table Creation Query Dialog
    if (showTableQueryDialog) {
        TableQueryDialog(
            onDismiss = { showTableQueryDialog = false },
            onInsertTable = { rows, cols ->
                showTableQueryDialog = false
                val tableMarkdown = generateMarkdownTable(rows, cols)
                onValueChange(insertTextAtCursor(value, "\n$tableMarkdown\n"))
            }
        )
    }

    // Flowchart & Down Arrow Tool Dialog
    if (showFlowchartArrowDialog) {
        FlowchartArrowDialog(
            onDismiss = { showFlowchartArrowDialog = false },
            onInsert = { snippet ->
                showFlowchartArrowDialog = false
                onValueChange(insertTextAtCursor(value, snippet))
            }
        )
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

/**
 * Interactive Dialog to Query Number of Rows and Columns for Markdown Table.
 */
@Composable
fun TableQueryDialog(
    onDismiss: () -> Unit,
    onInsertTable: (rows: Int, cols: Int) -> Unit
) {
    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.TableChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Insert Markdown Table",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Configure the grid dimensions for your markdown table:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Columns Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Columns (Headers)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "$cols columns",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (cols > 1) cols-- },
                            enabled = cols > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Columns", modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "$cols",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        FilledTonalIconButton(
                            onClick = { if (cols < 8) cols++ },
                            enabled = cols < 8,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Columns", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Rows Counter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Data Rows",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "$rows data rows",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { if (rows > 1) rows-- },
                            enabled = rows > 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Rows", modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "$rows",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.width(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        FilledTonalIconButton(
                            onClick = { if (rows < 15) rows++ },
                            enabled = rows < 15,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Rows", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = { cols = 2; rows = 2 },
                        label = { Text("2×2", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { cols = 3; rows = 3 },
                        label = { Text("3×3", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { cols = 4; rows = 4 },
                        label = { Text("4×4", fontSize = 11.sp) }
                    )
                    AssistChip(
                        onClick = { cols = 5; rows = 3 },
                        label = { Text("5×3", fontSize = 11.sp) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInsertTable(rows, cols) },
                modifier = Modifier.testTag("confirm_insert_table_button")
            ) {
                Text("Insert ${cols}×${rows} Table")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Generates a clean Markdown Table String based on specified Rows and Columns.
 */
fun generateMarkdownTable(rows: Int, cols: Int): String {
    val sb = StringBuilder()

    // Header row
    sb.append("|")
    for (c in 1..cols) {
        sb.append(" Header $c |")
    }
    sb.append("\n")

    // Divider row
    sb.append("|")
    for (c in 1..cols) {
        sb.append(" :--- |")
    }
    sb.append("\n")

    // Data rows
    for (r in 1..rows) {
        sb.append("|")
        for (c in 1..cols) {
            sb.append(" Row $r Col $c |")
        }
        sb.append("\n")
    }

    return sb.toString().trimEnd()
}

/**
 * Flowchart and Directional Arrow Insertion Tool Dialog.
 * Beautifully organized into clean categories:
 * - Directional Arrows (Down, Right, Up, Left, Connectors)
 * - Flowchart Step Sequences (Vertical, Horizontal, Loops, Branching)
 * - Mermaid Diagrams (Top-Down & Left-Right)
 */
@Composable
fun FlowchartArrowDialog(
    onDismiss: () -> Unit,
    onInsert: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(0) } // 0 = Quick Arrows, 1 = Flow Sequences, 2 = Mermaid

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AccountTree,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Arrows & Flowcharts",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Segmented Tabs
                TabRow(
                    selectedTabIndex = selectedCategory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedCategory == 0,
                        onClick = { selectedCategory = 0 },
                        text = { Text("Arrows", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedCategory == 1,
                        onClick = { selectedCategory = 1 },
                        text = { Text("Sequences", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedCategory == 2,
                        onClick = { selectedCategory = 2 },
                        text = { Text("Diagrams", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                when (selectedCategory) {
                    0 -> {
                        // Category 0: Quick Directional Arrows (Organized by Down, Right, Up, Left)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Downward Section
                            ArrowCategoryRow(
                                title = "Downward (↓)",
                                arrows = listOf(
                                    ArrowItem("↓", "↓ ", "Down"),
                                    ArrowItem("⬇", "⬇ ", "Thick"),
                                    ArrowItem("↓↓", "↓↓ ", "Double"),
                                    ArrowItem("▼", "▼ ", "Triangle"),
                                    ArrowItem("Line ↓", "\n      ↓\n", "Connector")
                                ),
                                onSelect = onInsert
                            )

                            // Rightward Section
                            ArrowCategoryRow(
                                title = "Rightward (→)",
                                arrows = listOf(
                                    ArrowItem("→", "→ ", "Right"),
                                    ArrowItem("➔", "➔ ", "Bold"),
                                    ArrowItem("──►", " ──► ", "Long"),
                                    ArrowItem("⇒", "⇒ ", "Double"),
                                    ArrowItem("▶", "▶ ", "Triangle")
                                ),
                                onSelect = onInsert
                            )

                            // Upward Section
                            ArrowCategoryRow(
                                title = "Upward (↑)",
                                arrows = listOf(
                                    ArrowItem("↑", "↑ ", "Up"),
                                    ArrowItem("⬆", "⬆ ", "Thick"),
                                    ArrowItem("▲", "▲ ", "Triangle"),
                                    ArrowItem("Line ↑", "\n      ↑\n", "Connector")
                                ),
                                onSelect = onInsert
                            )

                            // Leftward & Bidirectional Section
                            ArrowCategoryRow(
                                title = "Left / Dual (← ↔)",
                                arrows = listOf(
                                    ArrowItem("←", "← ", "Left"),
                                    ArrowItem("◄", "◄ ", "Triangle"),
                                    ArrowItem("↔", "↔ ", "Bi-Dir"),
                                    ArrowItem("⇄", "⇄ ", "Cycle")
                                ),
                                onSelect = onInsert
                            )
                        }
                    }

                    1 -> {
                        // Category 1: Step Sequences
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowchartOptionItem(
                                title = "Vertical 3-Step Flow",
                                subtitle = "Standard top-to-bottom workflow",
                                preview = "[ Step 1: Input ]\n      ↓\n[ Step 2: Process ]\n      ↓\n[ Step 3: Done ]",
                                onClick = {
                                    onInsert("\n[ Step 1: Input ]\n      ↓\n[ Step 2: Action / Process ]\n      ↓\n[ Step 3: Complete ]\n")
                                }
                            )

                            FlowchartOptionItem(
                                title = "Horizontal Pipeline (──►)",
                                subtitle = "Left-to-right processing stream",
                                preview = "[ Start ] ──► [ Process ] ──► [ Output ]",
                                onClick = {
                                    onInsert("\n[ Start ] ──► [ Process / Transform ] ──► [ Output / Save ]\n")
                                }
                            )

                            FlowchartOptionItem(
                                title = "Upward Loop / Retry (↑)",
                                subtitle = "Iterative retry or return loop",
                                preview = "[ Step ] ──► [ Check Status ]\n  ↑                 │\n  └───── (Retry) ───┘",
                                onClick = {
                                    onInsert("\n[ Step Action ] ──► [ Check Status ]\n      ↑                     │\n      └────── (Retry) ──────┘\n")
                                }
                            )

                            FlowchartOptionItem(
                                title = "Branching Decision",
                                subtitle = "Condition split with yes/no branches",
                                preview = "[ Start ]\n   ↓\n< Decision? >\n   ├─ Yes ──► [ Action A ]\n   └─ No  ──► [ Action B ]",
                                onClick = {
                                    onInsert("\n[ Start ]\n   ↓\n< Condition / Decision? >\n   ├─ Yes ──► [ Action A ]\n   └─ No  ──► [ Action B ]\n")
                                }
                            )
                        }
                    }

                    2 -> {
                        // Category 2: Mermaid Diagram Code Blocks
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowchartOptionItem(
                                title = "Mermaid Top-Down (TD)",
                                subtitle = "Rendered tree diagram",
                                preview = "```mermaid\ngraph TD\n    A[Start] --> B{Check}\n    B -->|Yes| C[Done]\n    B -->|No| A\n```",
                                onClick = {
                                    onInsert("\n```mermaid\ngraph TD\n    A[Start] --> B{Condition}\n    B -->|Yes| C[Success]\n    B -->|No| A\n```\n")
                                }
                            )

                            FlowchartOptionItem(
                                title = "Mermaid Left-to-Right (LR)",
                                subtitle = "Horizontal diagram structure",
                                preview = "```mermaid\ngraph LR\n    A[Input] --> B[Process] --> C[Output]\n```",
                                onClick = {
                                    onInsert("\n```mermaid\ngraph LR\n    A[Input] --> B[Process] --> C[Output]\n```\n")
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private data class ArrowItem(
    val display: String,
    val token: String,
    val label: String
)

@Composable
private fun ArrowCategoryRow(
    title: String,
    arrows: List<ArrowItem>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            arrows.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(item.token) }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = item.display,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowchartOptionItem(
    title: String,
    subtitle: String,
    preview: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

