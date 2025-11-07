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

    // Audios
    suspend fun insertNoteAudio(audio: NoteAudio): Int {
        return noteDao.insertNoteAudio(audio).toInt()
    }

    suspend fun insertAudiosForNote(noteId: Int, audioUris: List<String>, fileNames: List<String>, durations: List<Long>) {
        for (i in audioUris.indices) {
            noteDao.insertNoteAudio(
                NoteAudio(
                    noteId = noteId,
                    uri = audioUris[i],
                    fileName = fileNames.getOrElse(i) { "录音_${System.currentTimeMillis()}.m4a" },
                    duration = durations.getOrElse(i) { 0L }
                )
            )
        }
    }

    suspend fun getAudiosForNote(noteId: Int): List<NoteAudio> {
        return noteDao.getAudiosForNote(noteId)
    }

    suspend fun deleteAudiosForNote(noteId: Int) {
        noteDao.deleteAudiosForNote(noteId)
    }
}