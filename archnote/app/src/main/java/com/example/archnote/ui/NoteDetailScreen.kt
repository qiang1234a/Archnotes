// app/src/main/java/com/example/archnote/ui/NoteDetailScreen.kt
package com.example.archnote.ui
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
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
//import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.archnote.ArchnoteApplication
import com.example.archnote.ui.theme.ArchnoteTheme
import androidx.compose.ui.platform.LocalContext

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
    LaunchedEffect(noteId) {
        viewModel.getNoteById(noteId)
    }

    val note by viewModel.currentNote.collectAsStateWithLifecycle()

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

                    Text(
                        text = it.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )

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