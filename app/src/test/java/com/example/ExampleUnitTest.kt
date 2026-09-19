package com.example

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.service.RecordingStateManager
import com.example.ui.components.insertLinePrefix
import com.example.ui.components.insertTextAtCursor
import com.example.ui.components.wrapSelection
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMarkdownBoldFormatting_wrapsSelection() {
        val initial = TextFieldValue(text = "hello world", selection = TextRange(0, 5))
        val formatted = wrapSelection(initial, "**", "**", "text")
        assertEquals("**hello** world", formatted.text)
    }

    @Test
    fun testMarkdownHeaderFormatting_prependsHeader() {
        val initial = TextFieldValue(text = "Title", selection = TextRange(0, 0))
        val formatted = insertLinePrefix(initial, "### ")
        assertEquals("### Title", formatted.text)
    }

    @Test
    fun testInsertTextAtCursor() {
        val initial = TextFieldValue(text = "Hello ", selection = TextRange(6, 6))
        val updated = insertTextAtCursor(initial, "[01:30]")
        assertEquals("Hello [01:30]", updated.text)
    }

    @Test
    fun testTimestampDeduplication() {
        RecordingStateManager.reset()
        RecordingStateManager.updateState { it.copy(elapsedSeconds = 15, elapsedFormatted = "00:15") }

        val first = RecordingStateManager.addTimestamp("Point 1")
        val second = RecordingStateManager.addTimestamp("Point 1 again")

        assertEquals(first.formattedTime, second.formattedTime)
        assertEquals(1, RecordingStateManager.state.value.flaggedTimestamps.size)
    }
}
