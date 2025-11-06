// app/src/main/java/com/example/archnote/ui/NoteListScreen.kt
package com.example.archnote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.archnote.ArchnoteApplication
import com.example.archnote.data.Note
import com.example.archnote.ui.theme.ArchnoteTheme

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

    ArchnoteTheme {
        Column(modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(notes.value) { note ->
                            NoteItem(
                                note = note,
                                onNoteClick = onNoteClick
                            )
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
    Card(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable { onNoteClick(note.id) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = note.formattedCreatedAt(),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}