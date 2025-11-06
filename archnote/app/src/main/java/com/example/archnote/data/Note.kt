
package com.example.archnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    // 格式化日期显示
    fun formattedCreatedAt(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return createdAt.format(formatter)
    }
}