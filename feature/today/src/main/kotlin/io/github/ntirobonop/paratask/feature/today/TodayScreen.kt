package io.github.ntirobonop.paratask.feature.today

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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.ui.ParaTaskBottomNavigation
import io.github.ntirobonop.paratask.core.ui.TaskComposerSheet
import io.github.ntirobonop.paratask.core.ui.TaskDraft
import io.github.ntirobonop.paratask.core.ui.TaskListContent
import io.github.ntirobonop.paratask.core.ui.TaskListDestination
import java.time.LocalDate

@Composable
fun TodayRoute(
    taskRepository: TaskRepository,
    snackbarHostState: SnackbarHostState,
    onOpenTask: (TaskId) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    onNavigateToBrowse: () -> Unit = {},
    activeProjects: List<Project> = emptyList(),
    taskProjects: List<Project> = activeProjects,
    today: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.factory(taskRepository, today)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is TodayUiEvent.TaskCompleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Задача выполнена",
                        actionLabel = "Отменить",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoCompletion(event.taskId)
                    }
                }

                is TodayUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    TodayScreen(
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
                projectId = draft.projectId,
            )
            showQuickAdd = false
        },
        onCompleteTask = viewModel::completeTask,
        onOpenTask = onOpenTask,
        onNavigateToInbox = onNavigateToInbox,
        onNavigateToUpcoming = onNavigateToUpcoming,
        onNavigateToBrowse = onNavigateToBrowse,
        activeProjects = activeProjects,
        taskProjects = taskProjects,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    snackbarHostState: SnackbarHostState,
    showQuickAdd: Boolean,
    onAddTask: () -> Unit,
    onDismissQuickAdd: () -> Unit,
    onCreateTask: (TaskDraft) -> Unit,
    onCompleteTask: (TaskId) -> Unit,
    onOpenTask: (TaskId) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    onNavigateToBrowse: () -> Unit = {},
    activeProjects: List<Project> = emptyList(),
    taskProjects: List<Project> = activeProjects,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text("Сегодня") }) },
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
                selectedDestination = TaskListDestination.TODAY,
                onDestinationSelected = { destination ->
                    when (destination) {
                        TaskListDestination.INBOX -> onNavigateToInbox()
                        TaskListDestination.UPCOMING -> onNavigateToUpcoming()
                        TaskListDestination.BROWSE -> onNavigateToBrowse()
                        else -> Unit
                    }
                },
            )
        },
    ) { contentPadding ->
        TaskListContent(
            tasks = uiState.tasks,
            isLoading = uiState.isLoading,
            emptyTitle = "На сегодня задач нет",
            emptyMessage = "Создайте задачу или назначьте существующей сегодняшнюю дату",
            onCompleteTask = onCompleteTask,
            onOpenTask = onOpenTask,
            contentPadding = contentPadding,
            projects = taskProjects,
        )
    }

    if (showQuickAdd) {
        TaskComposerSheet(
            initialDueDate = uiState.date,
            onDismissRequest = onDismissQuickAdd,
            onCreateTask = onCreateTask,
            projects = activeProjects,
        )
    }
}
