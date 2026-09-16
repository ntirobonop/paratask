package io.github.ntirobonop.paratask.feature.inbox

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.ui.ParaTaskBottomNavigation
import io.github.ntirobonop.paratask.core.ui.TaskComposerSheet
import io.github.ntirobonop.paratask.core.ui.TaskDraft
import io.github.ntirobonop.paratask.core.ui.TaskListContent
import io.github.ntirobonop.paratask.core.ui.TaskListDestination
import java.time.Instant

@Composable
fun InboxRoute(
    taskRepository: TaskRepository,
    snackbarHostState: SnackbarHostState,
    onOpenTask: (TaskId) -> Unit,
    onNavigateToToday: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = viewModel(factory = InboxViewModel.factory(taskRepository)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is InboxUiEvent.TaskCompleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Задача выполнена",
                        actionLabel = "Отменить",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoCompletion(event.taskId)
                    }
                }

                is InboxUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    InboxScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        showQuickAdd = showQuickAdd,
        onAddTask = { showQuickAdd = true },
        onDismissQuickAdd = { showQuickAdd = false },
        onCreateTask = { draft ->
            viewModel.createTask(
                title = draft.title,
                description = draft.description,
                dueDate = draft.dueDate,
            )
            showQuickAdd = false
        },
        onCompleteTask = viewModel::completeTask,
        onOpenTask = onOpenTask,
        onNavigateToToday = onNavigateToToday,
        onNavigateToUpcoming = onNavigateToUpcoming,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    uiState: InboxUiState,
    snackbarHostState: SnackbarHostState,
    showQuickAdd: Boolean,
    onAddTask: () -> Unit,
    onDismissQuickAdd: () -> Unit,
    onCreateTask: (TaskDraft) -> Unit,
    onCompleteTask: (TaskId) -> Unit,
    onOpenTask: (TaskId) -> Unit,
    onNavigateToToday: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Входящие") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTask,
                modifier = Modifier.semantics { contentDescription = "Добавить задачу" },
            ) {
                Text(text = "+", fontSize = 28.sp)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ParaTaskBottomNavigation(
                selectedDestination = TaskListDestination.INBOX,
                onDestinationSelected = { destination ->
                    when (destination) {
                        TaskListDestination.TODAY -> onNavigateToToday()
                        TaskListDestination.UPCOMING -> onNavigateToUpcoming()
                        else -> Unit
                    }
                },
            )
        },
    ) { contentPadding ->
        TaskListContent(
            tasks = uiState.tasks,
            isLoading = uiState.isLoading,
            emptyTitle = "Входящие пусты",
            emptyMessage = "Добавьте задачу, чтобы ничего не забыть",
            onCompleteTask = onCompleteTask,
            onOpenTask = onOpenTask,
            contentPadding = contentPadding,
        )
    }

    if (showQuickAdd) {
        TaskComposerSheet(
            initialDueDate = null,
            onDismissRequest = onDismissQuickAdd,
            onCreateTask = onCreateTask,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InboxEmptyPreview() {
    InboxScreen(
        uiState = InboxUiState(isLoading = false),
        snackbarHostState = SnackbarHostState(),
        showQuickAdd = false,
        onAddTask = {},
        onDismissQuickAdd = {},
        onCreateTask = {},
        onCompleteTask = {},
        onOpenTask = {},
        onNavigateToToday = {},
        onNavigateToUpcoming = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun InboxWithTasksPreview() {
    val now = Instant.parse("2026-09-15T12:00:00Z")
    InboxScreen(
        uiState = InboxUiState(
            tasks = listOf(
                Task(
                    id = TaskId("preview-task"),
                    title = "Купить продукты",
                    description = "Молоко, хлеб и яблоки",
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
            isLoading = false,
        ),
        snackbarHostState = SnackbarHostState(),
        showQuickAdd = false,
        onAddTask = {},
        onDismissQuickAdd = {},
        onCreateTask = {},
        onCompleteTask = {},
        onOpenTask = {},
        onNavigateToToday = {},
        onNavigateToUpcoming = {},
    )
}
