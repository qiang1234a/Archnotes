// app/src/main/java/com/example/archnote/ui/navigation/AppNavHost.kt
package com.example.archnote.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.archnote.ui.NoteDetailScreen
import com.example.archnote.ui.NoteEditScreen
import com.example.archnote.ui.NoteListScreen
import androidx.compose.ui.Modifier

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier:
    Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTE_LIST,
        modifier = modifier
    ) {
        composable(NavRoutes.NOTE_LIST) {
            NoteListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(NavRoutes.noteDetailRoute(noteId))
                },
                onAddNoteClick = {
                    navController.navigate(NavRoutes.NEW_NOTE)
                }
            )
        }
        composable(NavRoutes.NOTE_DETAIL) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")?.toIntOrNull() ?: 0
            NoteDetailScreen(
                noteId = noteId,
                onEditClick = {
                    navController.navigate(NavRoutes.noteDetailRoute(noteId))
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onDeleteClick = {
                    navController.popBackStack()
                }
            )
        }
        composable(NavRoutes.NEW_NOTE) {
            NoteEditScreen(
                noteId = null,
                onSaveClick = {
                    navController.popBackStack()
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}