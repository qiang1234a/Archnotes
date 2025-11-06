// app/src/main/java/com/example/archnote/ArchnoteApplication.kt
package com.example.archnote

import android.app.Application
import com.example.archnote.data.AppDatabase
import com.example.archnote.data.NoteRepository

class ArchnoteApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NoteRepository(database.noteDao()) }
}