package io.github.ntirobonop.paratask.feature.projects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ntirobonop.paratask.core.data.ProjectMove
import io.github.ntirobonop.paratask.core.data.ProjectRepository
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.ui.ParaTaskBottomNavigation
import io.github.ntirobonop.paratask.core.ui.TaskComposerSheet
import io.github.ntirobonop.paratask.core.ui.TaskListContent
import io.github.ntirobonop.paratask.core.ui.TaskListDestination

@Composable
fun BrowseRoute(
    projectRepository: ProjectRepository,
    snackbarHostState: SnackbarHostState,
    onOpenProject: (ProjectId) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToToday: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrowseViewModel = viewModel(factory = BrowseViewModel.factory(projectRepository)),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    val editingProject = (uiState.activeProjects + uiState.archivedProjects)
        .firstOrNull { project -> project.id.value == editingProjectId }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is BrowseUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    BrowseScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAddProject = {
            editingProjectId = null
            showEditor = true
        },
        onEditProject = { project ->
            editingProjectId = project.id.value
            showEditor = true
        },
        onOpenProject = onOpenProject,
        onArchiveProject = { project -> viewModel.setArchived(project.id, true) },
        onRestoreProject = { project -> viewModel.setArchived(project.id, false) },
        onMoveProject = viewModel::moveProject,
        onNavigateToInbox = onNavigateToInbox,
        onNavigateToToday = onNavigateToToday,
        onNavigateToUpcoming = onNavigateToUpcoming,
        modifier = modifier,
    )

    if (showEditor) {
        ProjectEditorSheet(
            project = editingProject,
            onDismissRequest = { showEditor = false },
            onSave = { name, color, icon ->
                if (editingProject == null) {
                    viewModel.createProject(name = name, color = color, icon = icon)
                } else {
                    viewModel.updateProject(
                        project = editingProject,
                        name = name,
                        color = color,
                        icon = icon,
                    )
                }
                showEditor = false
            },
            onDelete = editingProject?.let { project ->
                {
                    viewModel.deleteProject(project.id)
                    showEditor = false
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    uiState: BrowseUiState,
    snackbarHostState: SnackbarHostState,
    onAddProject: () -> Unit,
    onEditProject: (Project) -> Unit,
    onOpenProject: (ProjectId) -> Unit,
    onArchiveProject: (Project) -> Unit,
    onRestoreProject: (Project) -> Unit,
    onMoveProject: (ProjectId, ProjectMove) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToToday: () -> Unit,
    onNavigateToUpcoming: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text("Обзор") }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProject,
                modifier = Modifier.semantics { contentDescription = "Создать проект" },
            ) {
                Text("+", fontSize = 28.sp)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ParaTaskBottomNavigation(
                selectedDestination = TaskListDestination.BROWSE,
                onDestinationSelected = { destination ->
                    when (destination) {
                        TaskListDestination.INBOX -> onNavigateToInbox()
                        TaskListDestination.TODAY -> onNavigateToToday()
                        TaskListDestination.UPCOMING -> onNavigateToUpcoming()
                        TaskListDestination.BROWSE -> Unit
                    }
                },
            )
        },
    ) { contentPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = contentPadding.calculateTopPadding() + 12.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text("Проекты", style = MaterialTheme.typography.titleLarge)
                }
                if (uiState.activeProjects.isEmpty()) {
                    item {
                        Text(
                            "Создайте первый проект",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(uiState.activeProjects, key = { project -> project.id.value }) { project ->
                    ProjectRow(
                        project = project,
                        onOpen = { onOpenProject(project.id) },
                        onEdit = { onEditProject(project) },
                        onArchive = { onArchiveProject(project) },
                        onMoveUp = { onMoveProject(project.id, ProjectMove.UP) },
                        onMoveDown = { onMoveProject(project.id, ProjectMove.DOWN) },
                    )
                }
                if (uiState.archivedProjects.isNotEmpty()) {
                    item {
                        Text(
                            "Архив",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    items(
                        uiState.archivedProjects,
                        key = { project -> project.id.value },
                    ) { project ->
                        ProjectRow(
                            project = project,
                            onOpen = {},
                            onEdit = { onEditProject(project) },
                            onRestore = { onRestoreProject(project) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectRow(
    project: Project,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !project.isArchived, onClick = onOpen)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(project.color), CircleShape),
            )
            Text(project.icon.glyph)
            Text(
                text = project.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            onMoveUp?.let { action -> TextButton(onClick = action) { Text("↑") } }
            onMoveDown?.let { action -> TextButton(onClick = action) { Text("↓") } }
            TextButton(onClick = onEdit) { Text("Изм.") }
            onArchive?.let { action -> TextButton(onClick = action) { Text("Архив") } }
            onRestore?.let { action -> TextButton(onClick = action) { Text("Вернуть") } }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectEditorSheet(
    project: Project?,
    onDismissRequest: () -> Unit,
    onSave: (String, Long, ProjectIcon) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by rememberSaveable(project?.id?.value) { mutableStateOf(project?.name.orEmpty()) }
    var color by rememberSaveable(project?.id?.value) {
        mutableStateOf(project?.color ?: PROJECT_COLORS.first())
    }
    var iconName by rememberSaveable(project?.id?.value) {
        mutableStateOf((project?.icon ?: ProjectIcon.LIST).name)
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (project == null) "Новый проект" else "Редактировать проект",
                style = MaterialTheme.typography.headlineSmall,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )
            Text("Цвет")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PROJECT_COLORS.forEach { candidate ->
                    FilterChip(
                        selected = color == candidate,
                        onClick = { color = candidate },
                        label = { Text("●", color = Color(candidate)) },
                    )
                }
            }
            Text("Иконка")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectIcon.entries.forEach { icon ->
                    FilterChip(
                        selected = iconName == icon.name,
                        onClick = { iconName = icon.name },
                        label = { Text(icon.glyph) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("Удалить") }
                } else {
                    Box(modifier = Modifier) {}
                }
                Button(
                    onClick = { onSave(name, color, ProjectIcon.valueOf(iconName)) },
                    enabled = name.isNotBlank(),
                ) {
                    Text(if (project == null) "Создать" else "Сохранить")
                }
            }
        }
    }
}

@Composable
fun ProjectRoute(
    projectId: ProjectId,
    taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    activeProjects: List<Project>,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenTask: (TaskId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = viewModel(
        key = projectId.value,
        factory = ProjectViewModel.factory(taskRepository, projectRepository, projectId),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }
    BackHandler(onBack = onBack)

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectUiEvent.TaskCompleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Задача выполнена",
                        actionLabel = "Отменить",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoCompletion(event.taskId)
                    }
                }

                is ProjectUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiState.project?.name ?: "Проект") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Назад" },
                    ) {
                        Text("←")
                    }
                },
            )
        },
        floatingActionButton = {
            if (uiState.project != null) {
                FloatingActionButton(
                    onClick = { showQuickAdd = true },
                    modifier = Modifier.semantics { contentDescription = "Добавить задачу" },
                ) {
                    Text("+", fontSize = 28.sp)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        TaskListContent(
            tasks = uiState.tasks,
            isLoading = uiState.isLoading,
            emptyTitle = "В проекте нет задач",
            emptyMessage = "Добавьте первую задачу проекта",
            onCompleteTask = viewModel::completeTask,
            onOpenTask = onOpenTask,
            contentPadding = contentPadding,
        )
    }

    if (showQuickAdd) {
        TaskComposerSheet(
            initialDueDate = null,
            onDismissRequest = { showQuickAdd = false },
            onCreateTask = { draft ->
                viewModel.createTask(
                    title = draft.title,
                    description = draft.description,
                    dueDate = draft.dueDate,
                    selectedProjectId = draft.projectId,
                )
                showQuickAdd = false
            },
            initialProjectId = projectId,
            projects = activeProjects,
        )
    }
}

private val ProjectIcon.glyph: String
    get() = when (this) {
        ProjectIcon.LIST -> "☰"
        ProjectIcon.WORK -> "▣"
        ProjectIcon.SCHOOL -> "◆"
        ProjectIcon.HOME -> "⌂"
        ProjectIcon.STAR -> "★"
    }

private val PROJECT_COLORS = listOf(
    0xFF6750A4,
    0xFF006A6A,
    0xFF386A20,
    0xFFBA1A1A,
    0xFF7D5260,
)
