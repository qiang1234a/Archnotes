// app/src/main/java/com/example/archnote/data/NoteRepository.kt
package com.example.archnote.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun insertNote(note: Note): Int {
        return noteDao.insertNote(note).toInt()
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    // Images
    suspend fun insertNoteImage(image: NoteImage): Int {
        return noteDao.insertNoteImage(image).toInt()
    }

    suspend fun insertImagesForNote(noteId: Int, uris: List<String>) {
        for (uri in uris) {
            noteDao.insertNoteImage(NoteImage(noteId = noteId, uri = uri))
        }
    }

    suspend fun getImagesForNote(noteId: Int): List<NoteImage> {
        return noteDao.getImagesForNote(noteId)
    }

    suspend fun deleteImagesForNote(noteId: Int) {
        noteDao.deleteImagesForNote(noteId)
    }
}