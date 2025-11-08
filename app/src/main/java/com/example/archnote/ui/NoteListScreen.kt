// app/src/main/java/com/example/archnote/ui/NoteListScreen.kt
package com.example.archnote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.archnote.ArchnoteApplication
import com.example.archnote.data.Note
import com.example.archnote.ui.theme.ArchnoteTheme
import java.time.format.DateTimeFormatter

@Composable
fun NoteListScreen(
    onNoteClick: (Int) -> Unit,
    onAddNoteClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory((LocalContext.current.applicationContext as ArchnoteApplication).repository)
        )
) {
    val notes = viewModel.allNotes.collectAsStateWithLifecycle(emptyList())
    var searchQuery by remember { mutableStateOf("") }

    ArchnoteTheme {
        Column(modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
        ) {
            // 顶部标题栏，显示笔记数量
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "笔记",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = "(${notes.value.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索笔记...") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "搜索")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "清除")
                        }
                    }
                },
                singleLine = true
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy年MM月dd日") }
                
                // 根据搜索关键词过滤笔记
                val filteredNotes = remember(notes.value, searchQuery) {
                    if (searchQuery.isBlank()) {
                        notes.value
                    } else {
                        val query = searchQuery.lowercase()
                        notes.value.filter { note ->
                            note.title.lowercase().contains(query) ||
                            note.content.lowercase().contains(query)
                        }
                    }
                }
                
                val groupedNotes = remember(filteredNotes) {
                    filteredNotes.groupBy { it.updatedAt.toLocalDate() }
                }

                if (notes.value.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "没有笔记",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "点击右下角按钮创建第一条笔记",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else if (filteredNotes.isEmpty() && searchQuery.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "未找到",
                            modifier = Modifier.padding(bottom = 16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "未找到匹配的笔记",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "尝试使用其他关键词搜索",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        groupedNotes.forEach { (date, noteList) ->
                            item(key = "header-${'$'}date") {
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = dateFormatter.format(date),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            items(noteList, key = { it.id }) { note ->
                                NoteItem(
                                    note = note,
                                    onNoteClick = onNoteClick
                                )
                            }
                        }
                    }
                }

                FloatingActionButton(
                    onClick = onAddNoteClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, "添加笔记")
                }
            }
        }
    }
}

@Composable
private fun NoteItem(
    note: Note,
    onNoteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = Color(note.color)
    val contentColor = if (backgroundColor.luminance() < 0.5f) Color.White else Color.Black

    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onNoteClick(note.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = contentColor
            )
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
                color = contentColor.copy(alpha = 0.9f)
            )
            Text(
                text = note.formattedCreatedAt(),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}