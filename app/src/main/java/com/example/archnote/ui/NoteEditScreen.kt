// app/src/main/java/com/example/archnote/ui/NoteEditScreen.kt
package com.example.archnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.archnote.ArchnoteApplication
import com.example.archnote.data.Note
import com.example.archnote.ui.theme.ArchnoteTheme
import kotlinx.coroutines.android.awaitFrame
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import coil.compose.AsyncImage
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
    val selectedImageUris = remember { mutableStateListOf<android.net.Uri>() }
    val existingImageUris = remember { mutableStateListOf<android.net.Uri>() }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUris.add(uri)
        }
    }

    // 如果是编辑现有笔记，加载笔记内容和已有图片
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0) {
            // 直接调用挂起函数并获取返回值（关键修改）
            val note = viewModel.getNoteById(noteId)
            title = note?.title ?: ""
            content = TextFieldValue(note?.content ?: "")
            val images = viewModel.getImagesForNote(noteId)
            existingImageUris.clear()
            existingImageUris.addAll(images.mapNotNull { android.net.Uri.parse(it.uri) })
        } else {
            awaitFrame()
            titleFocusRequester.requestFocus()
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
                coroutineScope.launch {
                    viewModel.updateNote(note)
                    if (selectedImageUris.isNotEmpty()) {
                        viewModel.addImagesToExistingNote(
                            noteId,
                            selectedImageUris.map { it.toString() }
                        )
                    }
                    onSaveClick()
                }
            } else {
                coroutineScope.launch {
                    viewModel.insertNoteWithImages(
                        note,
                        selectedImageUris.map { it.toString() }
                    ) {
                        onSaveClick()
                    }
                }
            }
        }
    }

    ArchnoteTheme {
        Column(modifier = modifier.fillMaxSize()) {
            // 顶部导航栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }

                IconButton(onClick = ::saveNote) {
                    Icon(Icons.Filled.Save, contentDescription = "保存")
                }
            }

            // 编辑区域放在按钮下方
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
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
                        TextButton(onClick = {
                            val sel = content.selection
                            val text = content.text
                            val start = sel.start.coerceAtLeast(0).coerceAtMost(text.length)
                            val end = sel.end.coerceAtLeast(0).coerceAtMost(text.length)
                            if (start == end) {
                                // 插入 **|** 并将光标置于中间
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
                        }) {
                            Text("U")
                        }
                    }
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

                    val allPreviewUris = (existingImageUris + selectedImageUris)
                    if (allPreviewUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow {
                            items(allPreviewUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "已选择图片",
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(96.dp)
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = "添加图片")
                }
            }
        }
    }
}