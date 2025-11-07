package com.example.archnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note_files")
data class NoteFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val uri: String,
    val fileName: String,
    val fileSize: Long = 0, // 文件大小（字节）
    val mimeType: String = "" // MIME类型
)

