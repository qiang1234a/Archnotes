// app/src/main/java/com/example/archnote/ui/navigation/NavRoutes.kt
package com.example.archnote.ui.navigation

object NavRoutes {
    const val NOTE_LIST = "note_list"
    const val NOTE_DETAIL = "note_detail/{noteId}"
    const val NEW_NOTE = "new_note"
    const val NOTE_EDIT = "note_edit/{noteId}"

    fun noteDetailRoute(noteId: Int) = "note_detail/$noteId"
    fun noteEditRoute(noteId: Int) = "note_edit/$noteId"
}