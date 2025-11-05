// app/src/main/java/com/example/archnote/ui/NoteViewModel.kt
package com.example.archnote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.archnote.data.Note
import com.example.archnote.data.NoteRepository
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
}