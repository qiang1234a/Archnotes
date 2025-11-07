// app/src/main/java/com/example/archnote/ui/NoteEditScreen.kt
package com.example.archnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
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
    var content by remember { mutableStateOf(TextFieldValue("")) }
    val titleFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    
    // 撤销功能相关
    data class EditState(val title: String, val content: TextFieldValue)
    val undoHistory = remember { mutableStateListOf<EditState>() }
    var isUndoing by remember { mutableStateOf(false) }
    var saveHistoryJob by remember { mutableStateOf<Job?>(null) }

    // 如果是编辑现有笔记，加载笔记内容
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0) {
            // 直接调用挂起函数并获取返回值（关键修改）
            val note = viewModel.getNoteById(noteId)
            title = note?.title ?: ""
            content = TextFieldValue(note?.content ?: "")
            // 初始化撤销历史
            undoHistory.clear()
            undoHistory.add(EditState(title, content))
        } else {
            // 新笔记，初始化撤销历史
            undoHistory.clear()
            undoHistory.add(EditState("", TextFieldValue("")))
            awaitFrame()
            titleFocusRequester.requestFocus()
        }
    }
    
    // 保存状态到撤销历史（防抖）
    fun saveToHistory() {
        if (!isUndoing) {
            saveHistoryJob?.cancel()
            saveHistoryJob = coroutineScope.launch {
                delay(300) // 300ms 防抖
                val currentState = EditState(title, content)
                // 如果与最后一个状态相同（只比较文本内容），不保存
                val lastState = undoHistory.lastOrNull()
                if (lastState == null || lastState.title != currentState.title || lastState.content.text != currentState.content.text) {
                    undoHistory.add(currentState)
                    // 限制历史记录数量，最多保留50个
                    if (undoHistory.size > 50) {
                        undoHistory.removeAt(0)
                    }
                }
            }
        }
    }
    
    // 撤销操作
    fun undo() {
        if (undoHistory.size > 1) {
            isUndoing = true
            // 移除当前状态
            undoHistory.removeLast()
            // 恢复到上一个状态
            val previousState = undoHistory.last()
            title = previousState.title
            content = previousState.content
            isUndoing = false
        }
    }


    // 保存笔记
    fun saveNote() {
        if (title.isNotBlank() || content.text.isNotBlank()) {
            val note = if (noteId != null && noteId != 0) {
                Note(id = noteId, title = title, content = content.text)
            } else {
                Note(title = title, content = content.text)
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
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
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
                // 简易格式工具栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 撤销按钮
                    IconButton(
                        onClick = { undo() },
                        enabled = undoHistory.size > 1
                    ) {
                        Icon(
                            Icons.Filled.Undo,
                            contentDescription = "撤销",
                            tint = if (undoHistory.size > 1) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                    
                    TextButton(onClick = {
                        val sel = content.selection
                        val text = content.text
                        val start = sel.start.coerceAtLeast(0).coerceAtMost(text.length)
                        val end = sel.end.coerceAtLeast(0).coerceAtMost(text.length)
                        if (start == end) {
                            val inserted = text.substring(0, start) + "****" + text.substring(start)
                            val cursor = start + 2
                            content = TextFieldValue(
                                inserted,
                                selection = TextRange(cursor, cursor)
                            )
                        } else {
                            val selected = text.substring(start, end)
                            val replaced = text.substring(0, start) + "**" + selected + "**" + text.substring(end)
                            content = TextFieldValue(
                                replaced,
                                selection = TextRange(start, start + 2 + selected.length + 2)
                            )
                        }
                        saveToHistory()
                    }) {
                        Text("B")
                    }

                    TextButton(onClick = {
                        val sel = content.selection
                        val text = content.text
                        val start = sel.start.coerceAtLeast(0).coerceAtMost(text.length)
                        val end = sel.end.coerceAtLeast(0).coerceAtMost(text.length)
                        if (start == end) {
                            val inserted = text.substring(0, start) + "<u></u>" + text.substring(start)
                            val cursor = start + 3
                            content = TextFieldValue(
                                inserted,
                                selection = TextRange(cursor, cursor)
                            )
                        } else {
                            val selected = text.substring(start, end)
                            val replaced = text.substring(0, start) + "<u>" + selected + "</u>" + text.substring(end)
                            content = TextFieldValue(
                                replaced,
                                selection = TextRange(start, start + 3 + selected.length + 4)
                            )
                        }
                        saveToHistory()
                    }) {
                        Text("U")
                    }
                }
                // 标题输入
                BasicTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        saveToHistory()
                    },
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
                    onValueChange = { 
                        content = it
                        saveToHistory()
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxSize(),
                    decorationBox = { innerTextField ->
                        if (content.text.isEmpty()) {
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