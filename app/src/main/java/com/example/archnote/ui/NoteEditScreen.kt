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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.archnote.utils.AudioRecorderManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import java.io.File
import android.Manifest
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.archnote.data.NoteAudio
import kotlinx.coroutines.delay
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
    val context = LocalContext.current
    val selectedImageUris = remember { mutableStateListOf<android.net.Uri>() }
    val existingImageUris = remember { mutableStateListOf<android.net.Uri>() }
    
    // 录音相关状态
    val audioRecorderManager = remember { AudioRecorderManager(context) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    val newRecordings = remember { mutableStateListOf<Pair<String, Long>>() } // (filePath, duration)
    val existingAudios = remember { mutableStateListOf<NoteAudio>() }
    
    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && !isRecording) {
            val filePath = audioRecorderManager.startRecording()
            if (filePath != null) {
                isRecording = true
            }
        }
    }
    
    // 录音时长更新
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(100)
            recordingDuration = audioRecorderManager.getRecordingDuration()
        }
    }
    
    // 清理资源
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_DESTROY) {
                if (isRecording) {
                    audioRecorderManager.stopRecording()
                    isRecording = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioRecorderManager.release()
        }
    }

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
            val audios = viewModel.getAudiosForNote(noteId)
            existingAudios.clear()
            existingAudios.addAll(audios)
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
                    if (newRecordings.isNotEmpty()) {
                        viewModel.addAudiosToExistingNote(
                            noteId,
                            newRecordings.map { it.first },
                            newRecordings.map { File(it.first).name },
                            newRecordings.map { it.second }
                        )
                    }
                    onSaveClick()
                }
            } else {
                coroutineScope.launch {
                    viewModel.insertNoteWithImagesAndAudios(
                        note,
                        selectedImageUris.map { it.toString() },
                        newRecordings.map { it.first },
                        newRecordings.map { File(it.first).name },
                        newRecordings.map { it.second }
                    ) {
                        onSaveClick()
                    }
                }
            }
        }
    }
    
    // 开始/停止录音
    fun toggleRecording() {
        if (isRecording) {
            val duration = audioRecorderManager.stopRecording()
            val filePath = audioRecorderManager.getCurrentOutputFile()?.absolutePath
            if (filePath != null && duration > 0) {
                newRecordings.add(Pair(filePath, duration))
            }
            isRecording = false
            recordingDuration = 0
        } else {
            if (audioRecorderManager.hasRecordPermission()) {
                val filePath = audioRecorderManager.startRecording()
                if (filePath != null) {
                    isRecording = true
                    recordingDuration = 0
                }
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    // 格式化录音时长
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
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
                    
                    // 显示录音列表
                    val allAudios = (existingAudios + newRecordings.mapIndexed { index, (path, duration) ->
                        NoteAudio(
                            id = -index - 1, // 临时ID
                            noteId = noteId ?: 0,
                            uri = path,
                            fileName = File(path).name,
                            duration = duration
                        )
                    })
                    if (allAudios.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow {
                            items(allAudios) { audio ->
                                Card(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .width(200.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = audio.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatDuration(audio.duration),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // 显示录音状态
                    if (isRecording) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Mic,
                                    contentDescription = "录音中",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "录音中: ${formatDuration(recordingDuration)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // 录音和图片按钮
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 录音按钮
                    FloatingActionButton(
                        onClick = { toggleRecording() },
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecording) "停止录音" else "开始录音"
                        )
                    }
                    
                    // 图片按钮
                    FloatingActionButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = "添加图片")
                    }
                }
            }
        }
    }
}