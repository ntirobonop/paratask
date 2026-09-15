package io.github.ntirobonop.paratask.feature.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Instant
import kotlinx.coroutines.flow.collect

data class TaskDraft(
    val title: String = "",
    val description: String = "",
)

@Composable
fun InboxRoute(
    taskRepository: TaskRepository,
    modifier: Modifier = Modifier,
    viewModel: InboxViewModel = viewModel(factory = InboxViewModel.factory(taskRepository)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
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
            viewModel.createTask(title = draft.title, description = draft.description)
            showQuickAdd = false
        },
        onCompleteTask = viewModel::completeTask,
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
        bottomBar = { ParaTaskBottomNavigation() },
    ) { contentPadding ->
        InboxContent(
            uiState = uiState,
            onCompleteTask = onCompleteTask,
            contentPadding = contentPadding,
        )
    }

    if (showQuickAdd) {
        QuickAddSheet(
            onDismissRequest = onDismissQuickAdd,
            onCreateTask = onCreateTask,
        )
    }
}

@Composable
private fun InboxContent(
    uiState: InboxUiState,
    onCompleteTask: (TaskId) -> Unit,
    contentPadding: PaddingValues,
) {
    when {
        uiState.isLoading -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        uiState.tasks.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            InboxEmptyState()
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            items(
                items = uiState.tasks,
                key = { task -> task.id.value },
            ) { task ->
                TaskRow(task = task, onCompleteTask = onCompleteTask)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onCompleteTask: (TaskId) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = false,
            onCheckedChange = { checked ->
                if (checked) onCompleteTask(task.id)
            },
            modifier = Modifier.semantics {
                contentDescription = "Выполнить задачу ${task.title}"
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
            if (task.description.isNotEmpty()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    onDismissRequest: () -> Unit,
    onCreateTask: (TaskDraft) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val createTask = {
        if (title.isNotBlank()) {
            onCreateTask(TaskDraft(title = title, description = description))
        }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Новая задача",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                label = { Text("Название") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Описание") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { createTask() }),
            )
            Button(
                onClick = createTask,
                enabled = title.isNotBlank(),
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("Создать")
            }
        }

        LaunchedEffect(Unit) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}

@Composable
private fun ParaTaskBottomNavigation() {
    NavigationBar {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Text("●") },
            label = { Text("Входящие") },
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Text("○") },
            label = { Text("Сегодня") },
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Text("○") },
            label = { Text("Предстоящие") },
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Text("○") },
            label = { Text("Обзор") },
        )
    }
}

@Composable
private fun InboxEmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Входящие пусты",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Добавьте задачу, чтобы ничего не забыть",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InboxEmptyPreview() {
    MaterialTheme {
        InboxScreen(
            uiState = InboxUiState(isLoading = false),
            snackbarHostState = SnackbarHostState(),
            showQuickAdd = false,
            onAddTask = {},
            onDismissQuickAdd = {},
            onCreateTask = {},
            onCompleteTask = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InboxWithTasksPreview() {
    val now = Instant.parse("2026-09-15T12:00:00Z")
    MaterialTheme {
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
        )
    }
}
