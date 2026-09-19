package com.example.service

import com.example.data.model.SyncTimestampItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class RecordingSessionState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val elapsedFormatted: String = "00:00",
    val currentAmplitude: Int = 0,
    val amplitudeHistory: List<Float> = emptyList(), // Normalized 0.0f..1.0f for waveforms
    val flaggedTimestamps: List<SyncTimestampItem> = emptyList(),
    val outputFile: File? = null
)

object RecordingStateManager {
    private val _state = MutableStateFlow(RecordingSessionState())
    val state: StateFlow<RecordingSessionState> = _state.asStateFlow()

    fun updateState(transform: (RecordingSessionState) -> RecordingSessionState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = RecordingSessionState()
    }

    fun addTimestamp(label: String? = null): SyncTimestampItem {
        val current = _state.value
        val totalSec = current.elapsedSeconds
        val mins = totalSec / 60
        val secs = totalSec % 60
        val formatted = String.format("%02d:%02d", mins, secs)

        // Check if timestamp for this exact formatted second already exists to avoid duplicate chips
        val existing = current.flaggedTimestamps.find { it.formattedTime == formatted }
        if (existing != null) {
            return existing
        }

        val markerIndex = current.flaggedTimestamps.size + 1
        val safeLabel = if (!label.isNullOrBlank()) label else "Flag #$markerIndex"
        val item = SyncTimestampItem(
            timeMs = totalSec * 1000L,
            formattedTime = formatted,
            label = safeLabel
        )
        val updatedList = current.flaggedTimestamps + item
        _state.value = current.copy(flaggedTimestamps = updatedList)
        return item
    }
}
