// app/src/main/java/com/example/archnote/ui/NoteViewModel.kt
package com.example.archnote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.archnote.data.Note
import com.example.archnote.data.NoteRepository
import com.example.archnote.data.NoteImage
import com.example.archnote.data.NoteAudio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {
    val allNotes = repository.allNotes

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()



    fun insertNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun clearCurrentNote() {
        _currentNote.value = null
    }

    suspend fun getNoteById(id: Int): Note? {
        return repository.getNoteById(id)
    }

    fun loadNoteById(id: Int) {
        viewModelScope.launch {
            _currentNote.value = repository.getNoteById(id)
        }
    }

    fun insertNoteWithImages(note: Note, imageUris: List<String>, onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val noteId = repository.insertNote(note)
            if (imageUris.isNotEmpty()) {
                repository.insertImagesForNote(noteId, imageUris)
            }
            onDone(noteId)
        }
    }

    fun addImagesToExistingNote(noteId: Int, imageUris: List<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (imageUris.isNotEmpty()) {
                repository.insertImagesForNote(noteId, imageUris)
            }
            onDone()
        }
    }

    suspend fun getImagesForNote(noteId: Int): List<NoteImage> {
        return repository.getImagesForNote(noteId)
    }

    fun insertNoteWithImagesAndAudios(
        note: Note,
        imageUris: List<String>,
        audioUris: List<String>,
        audioFileNames: List<String>,
        audioDurations: List<Long>,
        onDone: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val noteId = repository.insertNote(note)
            if (imageUris.isNotEmpty()) {
                repository.insertImagesForNote(noteId, imageUris)
            }
            if (audioUris.isNotEmpty()) {
                repository.insertAudiosForNote(noteId, audioUris, audioFileNames, audioDurations)
            }
            onDone(noteId)
        }
    }

    fun addAudiosToExistingNote(
        noteId: Int,
        audioUris: List<String>,
        audioFileNames: List<String>,
        audioDurations: List<Long>,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (audioUris.isNotEmpty()) {
                repository.insertAudiosForNote(noteId, audioUris, audioFileNames, audioDurations)
            }
            onDone()
        }
    }

    suspend fun getAudiosForNote(noteId: Int): List<NoteAudio> {
        return repository.getAudiosForNote(noteId)
    }
}