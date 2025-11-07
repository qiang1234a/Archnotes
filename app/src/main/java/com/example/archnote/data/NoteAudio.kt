package com.example.archnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_audios")
data class NoteAudio(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val uri: String,
    val fileName: String,
    val duration: Long = 0 // 录音时长（毫秒）
)

