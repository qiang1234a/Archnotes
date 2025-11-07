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
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
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
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.archnote.utils.AudioRecorderManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.io.File
import android.Manifest
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.archnote.data.NoteAudio
import com.example.archnote.data.NoteFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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
    
    // 文件相关状态
    val selectedFileUris = remember { mutableStateListOf<android.net.Uri>() }
    val existingFiles = remember { mutableStateListOf<NoteFile>() }
    
    // 自动保存相关
    var saveJob by remember { mutableStateOf<Job?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var lastSavedNoteId by remember { mutableStateOf<Int?>(null) }
    
    // 表格对话框相关
    var showTableDialog by remember { mutableStateOf(false) }
    var tableRows by remember { mutableStateOf("3") }
    var tableCols by remember { mutableStateOf("3") }
    
    // 撤销/重做功能相关
    data class EditState(val title: String, val content: TextFieldValue)
    val undoHistory = remember { mutableStateListOf<EditState>() }
    val redoHistory = remember { mutableStateListOf<EditState>() }
    var isUndoingOrRedoing by remember { mutableStateOf(false) }
    var saveHistoryJob by remember { mutableStateOf<Job?>(null) }
    
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
    
    // 文件选择器
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUris.add(uri)
        }
    }

    // 如果是编辑现有笔记，加载笔记内容和已有图片
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0) {
            // 直接调用挂起函数并获取返回值（关键修改）
            val note = viewModel.getNoteById(noteId)
            title = note?.title ?: ""
            content = TextFieldValue(note?.content ?: "")
            // 初始化撤销/重做历史
            undoHistory.clear()
            redoHistory.clear()
            undoHistory.add(EditState(title, content))
            val images = viewModel.getImagesForNote(noteId)
            existingImageUris.clear()
            existingImageUris.addAll(images.mapNotNull { android.net.Uri.parse(it.uri) })
            val audios = viewModel.getAudiosForNote(noteId)
            existingAudios.clear()
            existingAudios.addAll(audios)
            val files = viewModel.getFilesForNote(noteId)
            existingFiles.clear()
            existingFiles.addAll(files)
        } else {
            // 新笔记，初始化撤销/重做历史
            undoHistory.clear()
            redoHistory.clear()
            undoHistory.add(EditState("", TextFieldValue("")))
            awaitFrame()
            titleFocusRequester.requestFocus()
        }
    }
    
    // 保存状态到撤销历史（防抖）
    fun saveToHistory() {
        if (!isUndoingOrRedoing) {
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
                    // 有新的编辑操作时，清空重做历史
                    redoHistory.clear()
                }
            }
        }
    }
    
    // 撤销操作
    fun undo() {
        if (undoHistory.size > 1) {
            isUndoingOrRedoing = true
            val currentState = EditState(title, content)
            // 将当前状态保存到重做栈
            redoHistory.add(currentState)
            // 限制重做历史数量
            if (redoHistory.size > 50) {
                redoHistory.removeAt(0)
            }
            // 从撤销栈恢复上一个状态
            undoHistory.removeAt(undoHistory.lastIndex)
            val previousState = undoHistory.last()
            title = previousState.title
            content = previousState.content
            isUndoingOrRedoing = false
        }
    }
    
    // 重做操作
    fun redo() {
        if (redoHistory.isNotEmpty()) {
            isUndoingOrRedoing = true
            val currentState = EditState(title, content)
            // 将当前状态保存到撤销栈
            undoHistory.add(currentState)
            // 限制撤销历史数量
            if (undoHistory.size > 50) {
                undoHistory.removeAt(0)
            }
            // 从重做栈恢复下一个状态
            val nextState = redoHistory.removeAt(redoHistory.lastIndex)
            title = nextState.title
            content = nextState.content
            isUndoingOrRedoing = false
        }
    }


    // 保存笔记（自动保存）
    fun saveNote(auto: Boolean = false) {
        if (title.isNotBlank() || content.text.isNotBlank() || selectedImageUris.isNotEmpty() || newRecordings.isNotEmpty() || selectedFileUris.isNotEmpty()) {
            isSaving = true
            val currentNoteId = noteId ?: lastSavedNoteId
            val note = if (currentNoteId != null && currentNoteId != 0) {
                Note(id = currentNoteId, title = title, content = content.text)
            } else {
                Note(title = title, content = content.text)
            }

            if (currentNoteId != null && currentNoteId != 0) {
                // 更新现有笔记
                coroutineScope.launch {
                    viewModel.updateNote(note)
                    val imagesToSave = selectedImageUris.toList()
                    val recordingsToSave = newRecordings.toList()
                    val filesToSave = selectedFileUris.toList()
                    
                    if (imagesToSave.isNotEmpty()) {
                        viewModel.addImagesToExistingNote(
                            currentNoteId,
                            imagesToSave.map { it.toString() }
                        )
                        // 清除已保存的图片
                        selectedImageUris.removeAll(imagesToSave)
                    }
                    if (recordingsToSave.isNotEmpty()) {
                        viewModel.addAudiosToExistingNote(
                            currentNoteId,
                            recordingsToSave.map { it.first },
                            recordingsToSave.map { File(it.first).name },
                            recordingsToSave.map { it.second }
                        )
                        // 清除已保存的录音
                        newRecordings.removeAll(recordingsToSave)
                    }
                    if (filesToSave.isNotEmpty()) {
                        // 获取文件信息
                        val fileNames = filesToSave.map { uri ->
                            try {
                                val cursor = context.contentResolver.query(uri, null, null, null, null)
                                cursor?.use {
                                    if (it.moveToFirst()) {
                                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        if (nameIndex >= 0) {
                                            it.getString(nameIndex) ?: "文件_${System.currentTimeMillis()}"
                                        } else {
                                            "文件_${System.currentTimeMillis()}"
                                        }
                                    } else {
                                        "文件_${System.currentTimeMillis()}"
                                    }
                                } ?: "文件_${System.currentTimeMillis()}"
                            } catch (e: Exception) {
                                "文件_${System.currentTimeMillis()}"
                            }
                        }
                        val fileSizes = filesToSave.map { uri ->
                            try {
                                val cursor = context.contentResolver.query(uri, null, null, null, null)
                                cursor?.use {
                                    if (it.moveToFirst()) {
                                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                        if (sizeIndex >= 0) {
                                            it.getLong(sizeIndex)
                                        } else {
                                            0L
                                        }
                                    } else {
                                        0L
                                    }
                                } ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                        }
                        val mimeTypes = filesToSave.map { uri ->
                            context.contentResolver.getType(uri) ?: ""
                        }
                        viewModel.addFilesToExistingNote(
                            currentNoteId,
                            filesToSave.map { it.toString() },
                            fileNames,
                            fileSizes,
                            mimeTypes
                        )
                        // 清除已保存的文件
                        selectedFileUris.removeAll(filesToSave)
                    }
                    lastSavedNoteId = currentNoteId
                    isSaving = false
                    if (!auto) {
                        onSaveClick()
                    }
                }
            } else {
                // 创建新笔记
                coroutineScope.launch {
                    val imagesToSave = selectedImageUris.toList()
                    val recordingsToSave = newRecordings.toList()
                    val filesToSave = selectedFileUris.toList()
                    
                    // 获取文件信息
                    val fileNames = filesToSave.map { uri ->
                        try {
                            val cursor = context.contentResolver.query(uri, null, null, null, null)
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                    if (nameIndex >= 0) {
                                        it.getString(nameIndex) ?: "文件_${System.currentTimeMillis()}"
                                    } else {
                                        "文件_${System.currentTimeMillis()}"
                                    }
                                } else {
                                    "文件_${System.currentTimeMillis()}"
                                }
                            } ?: "文件_${System.currentTimeMillis()}"
                        } catch (e: Exception) {
                            "文件_${System.currentTimeMillis()}"
                        }
                    }
                    val fileSizes = filesToSave.map { uri ->
                        try {
                            val cursor = context.contentResolver.query(uri, null, null, null, null)
                            cursor?.use {
                                if (it.moveToFirst()) {
                                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                    if (sizeIndex >= 0) {
                                        it.getLong(sizeIndex)
                                    } else {
                                        0L
                                    }
                                } else {
                                    0L
                                }
                            } ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                    }
                    val mimeTypes = filesToSave.map { uri ->
                        context.contentResolver.getType(uri) ?: ""
                    }
                    
                    viewModel.insertNoteWithImagesAndAudiosAndFiles(
                        note,
                        imagesToSave.map { it.toString() },
                        recordingsToSave.map { it.first },
                        recordingsToSave.map { File(it.first).name },
                        recordingsToSave.map { it.second },
                        filesToSave.map { it.toString() },
                        fileNames,
                        fileSizes,
                        mimeTypes
                    ) { savedNoteId ->
                        lastSavedNoteId = savedNoteId
                        isSaving = false
                        // 清除已保存的图片、录音和文件
                        selectedImageUris.removeAll(imagesToSave)
                        newRecordings.removeAll(recordingsToSave)
                        selectedFileUris.removeAll(filesToSave)
                        if (!auto) {
                            onSaveClick()
                        }
                    }
                }
            }
        } else {
            isSaving = false
        }
    }
    
    // 自动保存：监听标题、内容、图片和录音变化，使用防抖机制
    LaunchedEffect(title, content.text, selectedImageUris.size, newRecordings.size, selectedFileUris.size) {
        // 取消之前的保存任务
        saveJob?.cancel()
        
        // 如果内容为空且没有图片和录音，不保存
        if (title.isBlank() && content.text.isBlank() && selectedImageUris.isEmpty() && newRecordings.isEmpty() && selectedFileUris.isEmpty()) {
            return@LaunchedEffect
        }
        
        // 延迟2秒后自动保存（防抖）
        saveJob = coroutineScope.launch {
            delay(2000) // 2秒防抖
            saveNote(auto = true)
        }
    }
    
    // 页面离开时自动保存
    DisposableEffect(Unit) {
        onDispose {
            // 取消待执行的保存任务
            saveJob?.cancel()
            // 立即保存
            if (title.isNotBlank() || content.text.isNotBlank()) {
                saveNote(auto = true)
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
    
    // 格式化文件大小
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    // 格式化录音时长
    fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
    
    // 插入表格
    fun insertTable(rows: Int, cols: Int) {
        val sel = content.selection
        val text = content.text
        val start = sel.start.coerceAtLeast(0).coerceAtMost(text.length)
        
        // 生成Markdown表格
        val tableBuilder = StringBuilder()
        
        // 如果当前位置不在行首，先添加换行
        if (start > 0 && start < text.length && text[start - 1] != '\n') {
            tableBuilder.append("\n")
        }
        
        // 表头
        tableBuilder.append("|")
        repeat(cols) {
            tableBuilder.append(" 列${it + 1} |")
        }
        tableBuilder.append("\n|")
        repeat(cols) {
            tableBuilder.append("-----|")
        }
        // 数据行
        repeat(rows) {
            tableBuilder.append("\n|")
            repeat(cols) {
                tableBuilder.append("     |")
            }
        }
        // 表格后添加换行
        tableBuilder.append("\n")
        
        val inserted = text.substring(0, start) + tableBuilder.toString() + text.substring(start)
        val cursor = start + tableBuilder.length
        content = TextFieldValue(
            inserted,
            selection = TextRange(cursor, cursor)
        )
        showTableDialog = false
        saveToHistory()
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
                IconButton(onClick = {
                    // 返回前自动保存
                    saveJob?.cancel()
                    if (title.isNotBlank() || content.text.isNotBlank() || selectedImageUris.isNotEmpty() || newRecordings.isNotEmpty() || selectedFileUris.isNotEmpty()) {
                        saveNote(auto = true)
                    }
                    onBackClick()
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }

                Spacer(modifier = Modifier.weight(1f))
                
                // 显示保存状态
                if (isSaving) {
                    Text(
                        text = "保存中...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                } else if (lastSavedNoteId != null || (noteId != null && noteId != 0)) {
                    Text(
                        text = "已自动保存",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
                        
                        // 重做按钮
                        IconButton(
                            onClick = { redo() },
                            enabled = redoHistory.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Filled.Redo,
                                contentDescription = "重做",
                                tint = if (redoHistory.isNotEmpty()) 
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
                        
                        TextButton(onClick = {
                            showTableDialog = true
                        }) {
                            Icon(
                                Icons.Filled.TableChart,
                                contentDescription = "插入表格",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // 表格创建对话框
                    if (showTableDialog) {
                        AlertDialog(
                            onDismissRequest = { showTableDialog = false },
                            title = { Text("创建表格") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = tableRows,
                                        onValueChange = { 
                                            if (it.all { char -> char.isDigit() } && it.isNotEmpty()) {
                                                tableRows = it
                                            }
                                        },
                                        label = { Text("行数") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = tableCols,
                                        onValueChange = { 
                                            if (it.all { char -> char.isDigit() } && it.isNotEmpty()) {
                                                tableCols = it
                                            }
                                        },
                                        label = { Text("列数") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        val rows = tableRows.toIntOrNull() ?: 3
                                        val cols = tableCols.toIntOrNull() ?: 3
                                        if (rows > 0 && cols > 0) {
                                            insertTable(rows.coerceAtMost(20), cols.coerceAtMost(10))
                                        }
                                    }
                                ) {
                                    Text("确定")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTableDialog = false }) {
                                    Text("取消")
                                }
                            }
                        )
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
                    
                    // 显示文件列表
                    val allFiles = (existingFiles + selectedFileUris.mapIndexed { index, uri ->
                        NoteFile(
                            id = -index - 1, // 临时ID
                            noteId = noteId ?: 0,
                            uri = uri.toString(),
                            fileName = try {
                                val cursor = context.contentResolver.query(uri, null, null, null, null)
                                cursor?.use {
                                    if (it.moveToFirst()) {
                                        val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                        if (nameIndex >= 0) {
                                            it.getString(nameIndex) ?: "文件_${System.currentTimeMillis()}"
                                        } else {
                                            "文件_${System.currentTimeMillis()}"
                                        }
                                    } else {
                                        "文件_${System.currentTimeMillis()}"
                                    }
                                } ?: "文件_${System.currentTimeMillis()}"
                            } catch (e: Exception) {
                                "文件_${System.currentTimeMillis()}"
                            },
                            fileSize = try {
                                val cursor = context.contentResolver.query(uri, null, null, null, null)
                                cursor?.use {
                                    if (it.moveToFirst()) {
                                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                                        if (sizeIndex >= 0) {
                                            it.getLong(sizeIndex)
                                        } else {
                                            0L
                                        }
                                    } else {
                                        0L
                                    }
                                } ?: 0L
                            } catch (e: Exception) {
                                0L
                            },
                            mimeType = context.contentResolver.getType(uri) ?: ""
                        )
                    })
                    if (allFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow {
                            items(allFiles) { file ->
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
                                        Icon(
                                            Icons.Filled.AttachFile,
                                            contentDescription = "文件",
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = file.fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (file.fileSize > 0) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = formatFileSize(file.fileSize),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
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
                    
                    // 文件按钮
                    FloatingActionButton(
                        onClick = {
                            filePicker.launch("*/*")
                        }
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = "添加文件")
                    }
                }
            }
        }
    }
}