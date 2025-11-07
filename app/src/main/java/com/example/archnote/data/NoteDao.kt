
package com.example.archnote.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    // Images
    @Insert
    suspend fun insertNoteImage(image: NoteImage): Long

    @Query("SELECT * FROM note_images WHERE noteId = :noteId")
    suspend fun getImagesForNote(noteId: Int): List<NoteImage>

    @Query("DELETE FROM note_images WHERE noteId = :noteId")
    suspend fun deleteImagesForNote(noteId: Int)

    // Audios
    @Insert
    suspend fun insertNoteAudio(audio: NoteAudio): Long

    @Query("SELECT * FROM note_audios WHERE noteId = :noteId")
    suspend fun getAudiosForNote(noteId: Int): List<NoteAudio>

    @Query("DELETE FROM note_audios WHERE noteId = :noteId")
    suspend fun deleteAudiosForNote(noteId: Int)
    
    // Files
    @Insert
    suspend fun insertNoteFile(file: NoteFile): Long
    
    @Query("SELECT * FROM note_files WHERE noteId = :noteId")
    suspend fun getFilesForNote(noteId: Int): List<NoteFile>
    
    @Query("DELETE FROM note_files WHERE noteId = :noteId")
    suspend fun deleteFilesForNote(noteId: Int)
}