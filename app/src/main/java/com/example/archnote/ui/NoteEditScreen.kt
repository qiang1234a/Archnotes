// app/src/main/java/com/example/archnote/ui/NoteEditScreen.kt
package com.example.archnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Preview
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.graphics.Typeface
import android.widget.EditText
import android.widget.TextView
import androidx.core.text.HtmlCompat
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
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var editTextRef by remember { mutableStateOf<EditText?>(null) }
    val titleFocusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val selectedImageUris = remember { mutableStateListOf<android.net.Uri>() }
    var showPreview by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUris.add(uri)
        }
    }

    // 如果是编辑现有笔记，加载笔记内容
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != 0) {
            // 直接调用挂起函数并获取返回值（关键修改）
            val note = viewModel.getNoteById(noteId)
            title = note?.title ?: ""
            // 延后到 AndroidView factory/update 中设置 EditText 的内容
        } else {
            awaitFrame()
            titleFocusRequester.requestFocus()
        }
    }


    // 保存笔记
    fun saveNote() {
        val htmlToSave = editTextRef?.text?.let { txt ->
            android.text.Html.toHtml(txt, android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        } ?: ""
        if (title.isNotBlank() || htmlToSave.isNotBlank()) {
            val note = if (noteId != null && noteId != 0) {
                Note(id = noteId, title = title, content = htmlToSave)
            } else {
                Note(title = title, content = htmlToSave)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = {
                            editTextRef?.let { et ->
                                val start = et.selectionStart
                                val end = et.selectionEnd
                                if (start != end) {
                                    val editable = et.text
                                    // 若区间已有粗体则移除，否则增加
                                    val spans = editable.getSpans(start, end, StyleSpan::class.java)
                                    var removed = false
                                    spans.forEach { span ->
                                        if (span.style == Typeface.BOLD) {
                                            editable.removeSpan(span)
                                            removed = true
                                        }
                                    }
                                    if (!removed) {
                                        editable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.FormatBold, contentDescription = "加粗")
                        }

                        IconButton(onClick = {
                            editTextRef?.let { et ->
                                val start = et.selectionStart
                                val end = et.selectionEnd
                                if (start != end) {
                                    val editable = et.text
                                    val spans = editable.getSpans(start, end, UnderlineSpan::class.java)
                                    if (spans.isNotEmpty()) {
                                        spans.forEach { span -> editable.removeSpan(span) }
                                    } else {
                                        editable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Filled.FormatUnderlined, contentDescription = "下划线")
                        }

                        IconButton(onClick = { showPreview = !showPreview }) {
                            Icon(Icons.Filled.Preview, contentDescription = "预览")
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

                    // 内容输入 或 预览
                    if (!showPreview) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                EditText(ctx).apply {
                                    setText(
                                        HtmlCompat.fromHtml("", HtmlCompat.FROM_HTML_MODE_LEGACY),
                                        TextView.BufferType.SPANNABLE
                                    )
                                    isSingleLine = false
                                    maxLines = Int.MAX_VALUE
                                    // 记录引用
                                    editTextRef = this
                                }
                            },
                            update = { et ->
                                // 初次加载/切换笔记时，将 HTML 填充成 Spannable
                                if (et.text.isNullOrEmpty() && (noteId != null && noteId != 0)) {
                                    val html = viewModel.currentNote.value?.content ?: contentValue.text
                                    et.setText(
                                        HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY),
                                        TextView.BufferType.SPANNABLE
                                    )
                                } else if (noteId == null || noteId == 0) {
                                    // 新建时保持现状
                                }
                                if (editTextRef !== et) editTextRef = et
                            }
                        )
                    } else {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                android.widget.TextView(ctx).apply {
                                    setText(HtmlCompat.fromHtml(
                                        editTextRef?.let { android.text.Html.toHtml(it.text, android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) } ?: "",
                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                    ))
                                }
                            },
                            update = { tv ->
                                val html = editTextRef?.let { android.text.Html.toHtml(it.text, android.text.Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE) } ?: ""
                                tv.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
                            }
                        )
                    }

                    if (selectedImageUris.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow {
                            items(selectedImageUris) { uri ->
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