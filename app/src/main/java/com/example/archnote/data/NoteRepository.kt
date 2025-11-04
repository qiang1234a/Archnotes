// app/src/main/java/com/example/archnote/data/NoteRepository.kt
package com.example.archnote.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }
}