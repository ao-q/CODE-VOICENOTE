package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FolderEntity
import com.example.data.model.TimestampMarkerEntity
import com.example.data.model.VoiceNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceNotesDao {
    // Folders
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId IS :parentId ORDER BY name ASC")
    fun getFoldersByParent(parentId: Long?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: Long)

    // Voice Notes
    @Query("SELECT * FROM voice_notes ORDER BY createdAt DESC")
    fun getAllVoiceNotes(): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE folderId IS :folderId ORDER BY createdAt DESC")
    fun getVoiceNotesByFolder(folderId: Long?): Flow<List<VoiceNoteEntity>>

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    suspend fun getVoiceNoteById(id: Long): VoiceNoteEntity?

    @Query("SELECT * FROM voice_notes WHERE id = :id")
    fun getVoiceNoteFlowById(id: Long): Flow<VoiceNoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoiceNote(note: VoiceNoteEntity): Long

    @Update
    suspend fun updateVoiceNote(note: VoiceNoteEntity)

    @Delete
    suspend fun deleteVoiceNote(note: VoiceNoteEntity)

    @Query("DELETE FROM voice_notes WHERE id = :id")
    suspend fun deleteVoiceNoteById(id: Long)

    @Query("SELECT * FROM voice_notes WHERE title LIKE '%' || :query || '%' OR noteContent LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchVoiceNotes(query: String): Flow<List<VoiceNoteEntity>>

    // Timestamp Markers
    @Query("SELECT * FROM timestamp_markers WHERE voiceNoteId = :noteId ORDER BY timeMs ASC")
    fun getMarkersForNote(noteId: Long): Flow<List<TimestampMarkerEntity>>

    @Query("SELECT * FROM timestamp_markers WHERE voiceNoteId = :noteId ORDER BY timeMs ASC")
    suspend fun getMarkersListForNote(noteId: Long): List<TimestampMarkerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarker(marker: TimestampMarkerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarkers(markers: List<TimestampMarkerEntity>)

    @Query("DELETE FROM timestamp_markers WHERE voiceNoteId = :noteId")
    suspend fun deleteMarkersForNote(noteId: Long)
}
