// app/src/main/java/com/example/archnote/ui/NoteDetailScreen.kt
package com.example.archnote.ui
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Mic
import com.example.archnote.data.NoteAudio
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.archnote.ArchnoteApplication
import com.example.archnote.ui.theme.ArchnoteTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun NoteDetailScreen(
    noteId: Int,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory((LocalContext.current.applicationContext as ArchnoteApplication).repository)
    )
) {
    val note by viewModel.currentNote.collectAsStateWithLifecycle()
    val audios = remember { mutableStateListOf<NoteAudio>() }

    // 加载笔记和录音列表
    LaunchedEffect(noteId) {
        viewModel.loadNoteById(noteId)
        val audioList = viewModel.getAudiosForNote(noteId)
        audios.clear()
        audios.addAll(audioList)
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
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "编辑")
                }

                IconButton(onClick = {
                    note?.let { viewModel.deleteNote(it) }
                    onDeleteClick()
                }) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除")
                }
            }

            // 笔记内容
            note?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // 解析并显示内容（包括表格）
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        parseContentWithTables(it.content)
                    }

                    // 显示录音列表
                    if (audios.isNotEmpty()) {
                        Spacer(modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            text = "录音文件",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(audios) { audio ->
                                Card(
                                    modifier = Modifier.width(200.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Mic,
                                            contentDescription = "录音",
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = audio.fileName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
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
                    }

                    Text(
                        text = "更新于: ${it.formattedCreatedAt()}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        textAlign = TextAlign.End
                    )
                }
            } ?: run {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "笔记不存在或已被删除")
                    Button(onClick = onBackClick, modifier = Modifier.padding(top = 16.dp)) {
                        Text("返回列表")
                    }
                }
            }
        }
    }
}

@Composable
private fun parseContentWithTables(input: String) {
    if (input.isEmpty()) {
        return
    }
    
    val lines = input.split("\n")
    var i = 0
    val textBuffer = StringBuilder()
    
    while (i < lines.size) {
        val line = lines[i]
        val trimmedLine = line.trim()
        
        // 检查是否是表格行（以|开头和结尾，且包含至少3个|，即至少有一列）
        val isTableLine = trimmedLine.startsWith("|") && 
                         trimmedLine.endsWith("|") && 
                         trimmedLine.length > 2 &&
                         trimmedLine.count { it == '|' } >= 3
        
        if (isTableLine) {
            // 先输出之前的文本
            if (textBuffer.isNotEmpty()) {
                Text(
                    text = parseSimpleStyles(textBuffer.toString()),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                textBuffer.clear()
            }
            
            // 解析表格 - 收集连续的表格行（允许中间有一个空行）
            val tableLines = mutableListOf<String>()
            var consecutiveEmptyLines = 0
            
            while (i < lines.size) {
                val currentLine = lines[i]
                val currentTrimmed = currentLine.trim()
                
                val isCurrentTableLine = currentTrimmed.startsWith("|") && 
                                         currentTrimmed.endsWith("|") && 
                                         currentTrimmed.length > 2 &&
                                         currentTrimmed.count { it == '|' } >= 3
                
                if (isCurrentTableLine) {
                    // 表格行
                    tableLines.add(currentLine)
                    consecutiveEmptyLines = 0
                    i++
                } else if (currentTrimmed.isEmpty() && tableLines.isNotEmpty() && consecutiveEmptyLines == 0) {
                    // 允许一个空行（用于分隔）
                    consecutiveEmptyLines++
                    i++
                } else {
                    // 非表格行，结束表格收集
                    break
                }
            }
            
            // 至少需要表头行和分隔行，或者至少两行数据
            if (tableLines.size >= 2) {
                // 解析表格数据
                val tableData = tableLines.map { line ->
                    line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                }
                
                // 检查第二行是否是分隔行（只包含-、:和|）
                val isSeparatorLine = tableLines.size > 1 && {
                    val separatorLine = tableLines[1].trim()
                    separatorLine.replace("|", "").replace("-", "").replace(":", "").trim().isEmpty()
                }()
                
                // 验证所有行的列数一致
                if (tableData.isNotEmpty() && tableData[0].isNotEmpty()) {
                    val expectedCols = tableData[0].size
                    val allSameSize = tableData.all { it.size == expectedCols }
                    
                    if (allSameSize && expectedCols > 0) {
                        // 显示表格
                        Spacer(modifier = Modifier.height(8.dp))
                        val headerRow = tableData[0]
                        val dataRows = if (isSeparatorLine && tableData.size > 2) {
                            // 跳过分隔行
                            tableData.subList(2, tableData.size)
                        } else if (!isSeparatorLine && tableData.size > 1) {
                            // 没有分隔行，第一行是表头，其余是数据
                            tableData.subList(1, tableData.size)
                        } else {
                            emptyList()
                        }
                        
                        // 即使没有数据行，也显示表头
                        TableView(
                            headers = headerRow,
                            rows = dataRows
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            continue
        }
        
        // 普通文本行
        if (textBuffer.isNotEmpty()) {
            textBuffer.append("\n")
        }
        textBuffer.append(line)
        i++
    }
    
    // 输出剩余的文本
    if (textBuffer.isNotEmpty()) {
        Text(
            text = parseSimpleStyles(textBuffer.toString()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun TableView(headers: List<String>, rows: List<List<String>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(1.dp)
        ) {
            // 表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                headers.forEachIndexed { index, header ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                if (index < headers.size - 1) 1.dp else 0.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                androidx.compose.foundation.shape.RoundedCornerShape(0)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // 数据行
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    row.forEachIndexed { index, cell ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    if (index < row.size - 1) 1.dp else 0.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(0)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseSimpleStyles(input: String): AnnotatedString {
    // 支持 **bold** 与 <u>underline</u>
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < input.length) {
        if (i + 1 < input.length && input[i] == '*' && input[i + 1] == '*') {
            val start = i + 2
            val end = input.indexOf("**", start)
            if (end != -1) {
                val segment = input.substring(start, end)
                val startIndex = builder.length
                builder.append(segment)
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), startIndex, startIndex + segment.length)
                i = end + 2
                continue
            }
        }
        if (i + 2 < input.length && input.startsWith("<u>", i)) {
            val start = i + 3
            val end = input.indexOf("</u>", start)
            if (end != -1) {
                val segment = input.substring(start, end)
                val startIndex = builder.length
                builder.append(segment)
                builder.addStyle(SpanStyle(textDecoration = TextDecoration.Underline), startIndex, startIndex + segment.length)
                i = end + 4
                continue
            }
        }
        builder.append(input[i])
        i += 1
    }
    return builder.toAnnotatedString()
}