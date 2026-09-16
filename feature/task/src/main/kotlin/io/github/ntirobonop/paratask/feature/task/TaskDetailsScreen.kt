package io.github.ntirobonop.paratask.feature.task

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.Section
import io.github.ntirobonop.paratask.core.model.SectionId
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.ui.TaskDateField
import io.github.ntirobonop.paratask.core.ui.TaskProjectField
import io.github.ntirobonop.paratask.core.ui.TaskSectionField
import java.time.LocalDate

@Composable
fun TaskDetailsRoute(
    taskRepository: TaskRepository,
    taskId: TaskId,
    snackbarHostState: SnackbarHostState,
    onClose: () -> Unit,
    onDeleted: (TaskId) -> Unit,
    activeProjects: List<Project> = emptyList(),
    sections: List<Section> = emptyList(),
    modifier: Modifier = Modifier,
    viewModel: TaskDetailsViewModel = viewModel(
        key = taskId.value,
        factory = TaskDetailsViewModel.factory(taskRepository, taskId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = viewModel::navigateBack)

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                TaskDetailsUiEvent.Close -> onClose()
                is TaskDetailsUiEvent.Deleted -> onDeleted(event.taskId)
                is TaskDetailsUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    TaskDetailsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTitleChange = viewModel::updateTitle,
        onDescriptionChange = viewModel::updateDescription,
        onDateChange = viewModel::updateDueDate,
        onProjectChange = viewModel::updateProject,
        onSectionChange = viewModel::updateSection,
        onCompletedChange = viewModel::setCompleted,
        onBack = viewModel::navigateBack,
        onDelete = viewModel::deleteTask,
        activeProjects = activeProjects,
        sections = sections,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailsScreen(
    uiState: TaskDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onDateChange: (LocalDate?) -> Unit = {},
    onProjectChange: (ProjectId?) -> Unit = {},
    onSectionChange: (SectionId?) -> Unit = {},
    activeProjects: List<Project> = emptyList(),
    sections: List<Section> = emptyList(),
    modifier: Modifier = Modifier,
) {
    var showActions by remember { mutableStateOf(false) }
    val canEdit = !uiState.isLoading && !uiState.taskMissing

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Задача") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Назад" },
                    ) {
                        Text("←")
                    }
                },
                actions = {
                    if (canEdit) {
                        Box {
                            TextButton(
                                onClick = { showActions = true },
                                modifier = Modifier.semantics {
                                    contentDescription = "Действия задачи"
                                },
                            ) {
                                Text("⋮")
                            }
                            DropdownMenu(
                                expanded = showActions,
                                onDismissRequest = { showActions = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Удалить") },
                                    onClick = {
                                        showActions = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            uiState.taskMissing -> MissingTaskState(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )

            else -> TaskEditor(
                uiState = uiState,
                onTitleChange = onTitleChange,
                onDescriptionChange = onDescriptionChange,
                onDateChange = onDateChange,
                onProjectChange = onProjectChange,
                onSectionChange = onSectionChange,
                activeProjects = activeProjects,
                sections = sections,
                onCompletedChange = onCompletedChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun TaskEditor(
    uiState: TaskDetailsUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onProjectChange: (ProjectId?) -> Unit,
    onSectionChange: (SectionId?) -> Unit,
    activeProjects: List<Project>,
    sections: List<Section>,
    onCompletedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = uiState.isCompleted,
                onCheckedChange = onCompletedChange,
                modifier = Modifier.semantics {
                    contentDescription = if (uiState.isCompleted) {
                        "Вернуть задачу"
                    } else {
                        "Выполнить задачу"
                    }
                },
            )
            OutlinedTextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
        }
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Описание") },
            minLines = 5,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
        )
        TaskDateField(
            dueDate = uiState.dueDate,
            onDateChange = onDateChange,
        )
        TaskProjectField(
            projectId = uiState.projectId,
            projects = activeProjects,
            onProjectChange = onProjectChange,
        )
        TaskSectionField(
            projectId = uiState.projectId,
            sectionId = uiState.sectionId,
            sections = sections,
            onSectionChange = onSectionChange,
        )
    }
}

@Composable
private fun MissingTaskState(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Задача не найдена",
            style = MaterialTheme.typography.titleLarge,
        )
        Button(
            onClick = onBack,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Вернуться во Входящие")
        }
    }
}
