// app/src/main/java/com/example/archnote/ui/NoteEditScreen.kt
package com.example.archnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.archnote.ArchnoteApplication
import com.example.archnote.data.Note
import com.example.archnote.ui.theme.ArchnoteTheme
import kotlinx.coroutines.android.awaitFrame
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
@Composable
fun NoteEditScreen(
    noteId: Int?,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory((LocalContext.current.applicationContext as ArchnoteApplication).repository)
    )
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val titleFocusRequester = remember { FocusRequester() }

    // 如果是编辑现有笔记，加载笔记内容
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0) {
            // 直接调用挂起函数并获取返回值（关键修改）
            val note = viewModel.getNoteById(noteId)
            title = note?.title ?: ""
            content = note?.content ?: ""
        } else {
            awaitFrame()
            titleFocusRequester.requestFocus()
        }
    }


    // 保存笔记
    fun saveNote() {
        if (title.isNotBlank() || content.isNotBlank()) {
            val note = if (noteId != null && noteId != 0) {
                Note(id = noteId, title = title, content = content)
            } else {
                Note(title = title, content = content)
            }

            if (noteId != null && noteId != 0) {
                viewModel.updateNote(note)
            } else {
                viewModel.insertNote(note)
            }
            onSaveClick()
        }
    }

    ArchnoteTheme {
        Column(modifier = modifier.fillMaxSize()) {
            // 顶部导航栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }

                IconButton(onClick = ::saveNote) {
                    Icon(Icons.Filled.Save, contentDescription = "保存")
                }
            }

            // 编辑区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题输入
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(titleFocusRequester)
                        .padding(bottom = 16.dp),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "输入标题...",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )

                // 内容输入
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = "输入内容...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}