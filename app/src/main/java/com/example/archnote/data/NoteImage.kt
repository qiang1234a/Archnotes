package com.example.archnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_images")
data class NoteImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val uri: String
)


