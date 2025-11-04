// app/src/main/java/com/example/archnote/ui/navigation/NavRoutes.kt
package com.example.archnote.ui.navigation

object NavRoutes {
    const val NOTE_LIST = "note_list"
    const val NOTE_DETAIL = "note_detail/{noteId}"
    const val NEW_NOTE = "new_note"

    fun noteDetailRoute(noteId: Int) = "note_detail/$noteId"
}