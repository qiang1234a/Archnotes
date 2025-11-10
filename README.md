# Archnote 代码风格与规范

本文档描述了 Archnote 项目的代码风格、规范和最佳实践。所有贡献者都应遵循这些规范。

---

## 📋 目录

- [项目概述](#项目概述)
- [代码风格](#代码风格)
- [命名规范](#命名规范)
- [文件结构](#文件结构)
- [注释规范](#注释规范)
- [Git 提交规范](#git-提交规范)
- [最佳实践](#最佳实践)
- [代码审查](#代码审查)

---

## 📱 项目概述

**Archnote** 是一个 Android 笔记应用，使用以下技术栈：

- **语言**：Kotlin
- **UI 框架**：Jetpack Compose
- **架构**：MVVM (Model-View-ViewModel)
- **数据库**：Room
- **依赖注入**：手动依赖注入
- **最低 SDK**：API 24 (Android 7.0)

---

## 🎨 代码风格

### Kotlin 代码风格

#### 基本规则

1. **使用 4 个空格缩进**（不使用 Tab）
2. **行长度限制**：最大 120 个字符
3. **文件编码**：UTF-8
4. **换行符**：Unix 风格（LF）

#### 代码格式

```kotlin
// ✅ 正确：使用 4 个空格缩进
fun exampleFunction() {
    if (condition) {
        doSomething()
    }
}

// ❌ 错误：使用 Tab 或 2 个空格
fun exampleFunction() {
  if (condition) {
    doSomething()
  }
}
```

#### 大括号风格

```kotlin
// ✅ 正确：Kotlin 风格
if (condition) {
    doSomething()
}

// ✅ 正确：单行表达式可以省略大括号
if (condition) doSomething()

// ❌ 错误：其他风格
if (condition)
{
    doSomething()
}
```

#### 空行规则

- 类/函数之间：2 个空行
- 函数内部逻辑块之间：1 个空行
- 导入语句分组之间：1 个空行

```kotlin
// ✅ 正确
package com.example.archnote.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import com.example.archnote.data.Note


@Composable
fun MyComponent() {
    val state = remember { mutableStateOf("") }
    
    Text(text = state.value)
}
```

---

## 📝 命名规范

### 包名

- **全小写**
- 使用点分隔
- 简短且有意义的名称

```kotlin
// ✅ 正确
package com.example.archnote.ui
package com.example.archnote.data
package com.example.archnote.utils

// ❌ 错误
package com.example.archnote.UI
package com.example.archnote.DataModels
```

### 类名

- **PascalCase**（首字母大写的驼峰命名）
- 名词或名词短语
- 清晰描述类的用途

```kotlin
// ✅ 正确
class NoteViewModel
class NoteRepository
class AudioRecorderManager

// ❌ 错误
class noteViewModel
class Note_Repository
class NoteVM
```

### 函数名

- **camelCase**（首字母小写的驼峰命名）
- 动词开头
- 清晰描述函数功能

```kotlin
// ✅ 正确
fun saveNote()
fun getNoteById(id: Int)
fun formatDuration(millis: Long)

// ❌ 错误
fun SaveNote()
fun get_note_by_id()
fun format_duration()
```

### 变量名

- **camelCase**
- 名词或形容词
- 避免缩写（除非是通用缩写）

```kotlin
// ✅ 正确
var noteTitle: String
var isRecording: Boolean
val noteList: List<Note>

// ❌ 错误
var NoteTitle: String
var is_recording: Boolean
val nl: List<Note>
```

### 常量

- **UPPER_SNAKE_CASE**
- 使用 `const val` 或 `val` 在 `object` 中

```kotlin
// ✅ 正确
const val MAX_UNDO_HISTORY = 50
const val AUTO_SAVE_DELAY = 2000L

object Constants {
    val DEFAULT_TABLE_ROWS = 3
    val DEFAULT_TABLE_COLS = 3
}

// ❌ 错误
const val maxUndoHistory = 50
const val auto_save_delay = 2000L
```

### 私有成员

- 私有属性/函数使用 **camelCase**
- 不需要特殊前缀

```kotlin
// ✅ 正确
private var isUndoing = false
private fun saveToHistory() { }

// ❌ 错误
private var mIsUndoing = false
private var _isUndoing = false
private fun _saveToHistory() { }
```

---

## 📁 文件结构

### 项目结构

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/archnote/
│   │   │   ├── data/              # 数据层
│   │   │   │   ├── Note.kt
│   │   │   │   ├── NoteDao.kt
│   │   │   │   ├── NoteRepository.kt
│   │   │   │   └── AppDatabase.kt
│   │   │   ├── ui/                # UI 层
│   │   │   │   ├── NoteListScreen.kt
│   │   │   │   ├── NoteEditScreen.kt
│   │   │   │   ├── NoteDetailScreen.kt
│   │   │   │   ├── NoteViewModel.kt
│   │   │   │   └── theme/         # 主题
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Type.kt
│   │   │   ├── utils/             # 工具类
│   │   │   │   └── AudioRecorderManager.kt
│   │   │   └── ArchnoteApplication.kt
│   │   ├── res/                   # 资源文件
│   │   └── AndroidManifest.xml
│   └── test/                      # 测试代码
└── build.gradle.kts
```

### 文件命名

- **PascalCase**，与类名一致
- 一个文件一个类（主要类）
- 相关类可以放在同一文件（如数据类）

```kotlin
// ✅ 正确
NoteEditScreen.kt      // 包含 NoteEditScreen 类
NoteViewModel.kt       // 包含 NoteViewModel 类
Note.kt                // 包含 Note 数据类

// ❌ 错误
note_edit_screen.kt
NoteEditScreenKt.kt
AllScreens.kt          // 不要把所有屏幕放在一个文件
```

---

## 💬 注释规范

### 文件头注释

```kotlin
// app/src/main/java/com/example/archnote/ui/NoteEditScreen.kt
package com.example.archnote.ui
```

### KDoc 注释

使用 KDoc 为公共 API 添加文档：

```kotlin
/**
 * 笔记编辑界面
 * 
 * @param noteId 笔记ID，null表示新建笔记
 * @param onSaveClick 保存完成回调
 * @param onBackClick 返回回调
 * @param modifier 修饰符
 * @param viewModel ViewModel实例
 */
@Composable
fun NoteEditScreen(
    noteId: Int?,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteViewModel = viewModel(...)
) { }
```

### 行内注释

```kotlin
// ✅ 正确：解释"为什么"，而非"是什么"
// 使用防抖机制避免频繁保存
saveHistoryJob = coroutineScope.launch {
    delay(300)
    // ...
}

// ❌ 错误：不必要的注释
// 设置标题
title = newTitle
```

### 复杂逻辑注释

```kotlin
// ✅ 正确：解释复杂逻辑
// 检查是否是表格行：以|开头和结尾，且包含至少3个|
val isTableLine = trimmedLine.startsWith("|") &&
                 trimmedLine.endsWith("|") &&
                 trimmedLine.length > 2 &&
                 trimmedLine.count { it == '|' } >= 3
```

---

## 🔀 Git 提交规范

### 提交消息格式

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行）
- `refactor`: 重构（既不是新功能也不是修复 bug）
- `perf`: 性能优化
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 示例

```bash
# ✅ 正确
feat(ui): 添加撤销/重做功能
fix(edit): 修复自动保存延迟问题
docs: 更新用户手册
refactor(data): 重构数据库访问层
style: 格式化代码

# ❌ 错误
update code
fix bug
new feature
```

### 提交消息示例

```bash
feat(edit): 添加文本格式工具栏

- 添加加粗和下划线格式按钮
- 支持选中文本应用格式
- 在详情页正确渲染格式

Closes #123
```

```bash
fix(save): 修复自动保存不触发的问题

修复了当内容为空时自动保存不触发的问题。
现在即使内容为空，只要有标题也会触发保存。

Fixes #456
```

### 提交频率

- **小步提交**：完成一个功能点就提交
- **原子性提交**：每次提交只做一件事
- **频繁提交**：避免大量代码一次性提交

---

## ✨ 最佳实践

### Compose 代码

#### 使用 remember 和 mutableStateOf

```kotlin
// ✅ 正确
var title by remember { mutableStateOf("") }
val notes = remember { mutableStateListOf<Note>() }

// ❌ 错误
var title = ""  // 不会触发重组
```

#### 函数参数默认值

```kotlin
// ✅ 正确
@Composable
fun NoteItem(
    note: Note,
    onNoteClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) { }

// ❌ 错误
@Composable
fun NoteItem(
    note: Note,
    onNoteClick: (Int) -> Unit,
    modifier: Modifier
) { }
```

#### 避免在 Composable 中执行耗时操作

```kotlin
// ✅ 正确
LaunchedEffect(noteId) {
    val note = viewModel.getNoteById(noteId)
    // ...
}

// ❌ 错误
@Composable
fun NoteDetailScreen(noteId: Int) {
    val note = viewModel.getNoteById(noteId)  // 阻塞操作
    // ...
}
```

### 数据层代码

#### Repository 模式

```kotlin
// ✅ 正确
class NoteRepository(private val noteDao: NoteDao) {
    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }
}

// ❌ 错误：在 ViewModel 中直接访问 DAO
class NoteViewModel(private val noteDao: NoteDao) { }
```

#### 使用 Flow 处理异步数据

```kotlin
// ✅ 正确
val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

// ❌ 错误：使用 LiveData（除非必要）
val allNotes: LiveData<List<Note>> = noteDao.getAllNotes()
```

### ViewModel 代码

#### 使用 suspend 函数

```kotlin
// ✅ 正确
suspend fun getNoteById(id: Int): Note? {
    return repository.getNoteById(id)
}

// ❌ 错误：在 ViewModel 中使用协程作用域
fun getNoteById(id: Int) {
    viewModelScope.launch {
        // ...
    }
}
```

#### 状态管理

```kotlin
// ✅ 正确
private val _currentNote = MutableStateFlow<Note?>(null)
val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

// ❌ 错误：直接暴露 MutableStateFlow
val currentNote = MutableStateFlow<Note?>(null)
```

### 错误处理

```kotlin
// ✅ 正确
try {
    val result = performOperation()
} catch (e: Exception) {
    // 记录错误或显示用户友好的消息
    Log.e(TAG, "操作失败", e)
    // 处理错误
}

// ❌ 错误：忽略异常
try {
    performOperation()
} catch (e: Exception) {
    // 空 catch 块
}
```

### 资源管理

```kotlin
// ✅ 正确：使用 use 自动关闭资源
cursor?.use {
    // 使用 cursor
}

// ✅ 正确：在 DisposableEffect 中清理
DisposableEffect(Unit) {
    onDispose {
        audioRecorderManager.release()
    }
}

// ❌ 错误：忘记关闭资源
val cursor = query(...)
// 使用 cursor
// 忘记关闭
```

---

## 🔍 代码审查

### 审查清单

提交代码前检查：

- [ ] 代码遵循命名规范
- [ ] 代码格式正确（4 空格缩进）
- [ ] 添加了必要的注释
- [ ] 没有硬编码的字符串（使用资源）
- [ ] 没有未使用的导入
- [ ] 没有 TODO 注释（除非有 issue）
- [ ] 提交消息符合规范
- [ ] 代码可以编译通过
- [ ] 没有明显的性能问题

### 审查重点

1. **功能正确性**：代码是否实现了预期功能
2. **代码质量**：是否遵循最佳实践
3. **可维护性**：代码是否易于理解和维护
4. **性能**：是否有性能问题
5. **安全性**：是否有安全漏洞

---

## 🛠️ 工具配置

### IDE 设置

#### Android Studio / IntelliJ IDEA

1. **代码风格**：
   - Settings → Editor → Code Style → Kotlin
   - 设置缩进为 4 个空格
   - 设置行长度为 120

2. **格式化**：
   - Settings → Editor → Code Style → Formatter
   - 启用 "Enable EditorConfig support"

3. **导入优化**：
   - Settings → Editor → Code Style → Kotlin → Imports
   - 启用 "Optimize imports"

#### EditorConfig

创建 `.editorconfig` 文件：

```ini
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_style = space
indent_size = 4
max_line_length = 120
```

### 代码格式化

使用 Kotlin 官方格式化工具：

```bash
# 格式化所有 Kotlin 文件
./gradlew ktlintFormat
```

---

## 📚 参考资源

- [Kotlin 编码规范](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Kotlin 风格指南](https://developer.android.com/kotlin/style-guide)
- [Jetpack Compose 最佳实践](https://developer.android.com/jetpack/compose/performance)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

## 📝 更新日志

- **2024-01-01**: 初始版本

---

**注意**：本文档会随着项目发展持续更新。如有疑问，请提交 Issue 或 Pull Request。

