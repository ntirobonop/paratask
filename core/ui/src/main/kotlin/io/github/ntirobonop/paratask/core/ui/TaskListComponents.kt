package io.github.ntirobonop.paratask.core.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TaskDraft(
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val projectId: ProjectId? = null,
)

enum class TaskListDestination {
    INBOX,
    TODAY,
    UPCOMING,
    BROWSE,
}

@Composable
fun TaskListContent(
    tasks: List<Task>,
    isLoading: Boolean,
    emptyTitle: String,
    emptyMessage: String,
    onCompleteTask: (TaskId) -> Unit,
    onOpenTask: (TaskId) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    showDueDate: Boolean = true,
) {
    when {
        isLoading -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        tasks.isEmpty() -> Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            EmptyTaskList(title = emptyTitle, message = emptyMessage)
        }

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            items(items = tasks, key = { task -> task.id.value }) { task ->
                TaskRow(
                    task = task,
                    onCompleteTask = onCompleteTask,
                    onOpenTask = onOpenTask,
                    showDueDate = showDueDate,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onCompleteTask: (TaskId) -> Unit,
    onOpenTask: (TaskId) -> Unit,
    showDueDate: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenTask(task.id) }
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
            if (showDueDate) {
                task.dueDate?.let { dueDate ->
                    Text(
                        text = formatTaskDate(dueDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskComposerSheet(
    initialDueDate: LocalDate?,
    onDismissRequest: () -> Unit,
    onCreateTask: (TaskDraft) -> Unit,
    initialProjectId: ProjectId? = null,
    projects: List<Project> = emptyList(),
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var dueDateText by rememberSaveable { mutableStateOf(initialDueDate?.toString()) }
    var projectIdText by rememberSaveable { mutableStateOf(initialProjectId?.value) }
    val dueDate = dueDateText?.let(LocalDate::parse)
    val projectId = projectIdText?.let(::ProjectId)
    val titleFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val createTask = {
        if (title.isNotBlank()) {
            onCreateTask(
                TaskDraft(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    projectId = projectId,
                ),
            )
        }
    }

    ModalBottomSheet(onDismissRequest = onDismissRequest) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Описание") },
                minLines = 2,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { createTask() }),
            )
            TaskDateField(
                dueDate = dueDate,
                onDateChange = { dueDateText = it?.toString() },
            )
            TaskProjectField(
                projectId = projectId,
                projects = projects,
                onProjectChange = { projectIdText = it?.value },
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
fun TaskProjectField(
    projectId: ProjectId?,
    projects: List<Project>,
    onProjectChange: (ProjectId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selectedName = projects.firstOrNull { project -> project.id == projectId }?.name
        ?: "Входящие"

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .semantics { contentDescription = "Выбрать проект" }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Проект", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = selectedName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Входящие") },
                onClick = {
                    onProjectChange(null)
                    expanded = false
                },
            )
            projects.forEach { project ->
                DropdownMenuItem(
                    text = { Text(project.name) },
                    onClick = {
                        onProjectChange(project.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun TaskDateField(
    dueDate: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Дата", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = dueDate?.let(::formatTaskDate) ?: "Не выбрана",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showDatePicker) {
        TaskDatePickerDialog(
            dueDate = dueDate,
            onDateChange = {
                onDateChange(it)
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDatePickerDialog(
    dueDate: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dueDate?.toUtcEpochMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateChange(millis.toLocalDateUtc())
                    }
                },
                enabled = datePickerState.selectedDateMillis != null,
            ) {
                Text("Готово")
            }
        },
        dismissButton = {
            Row {
                if (dueDate != null) {
                    TextButton(onClick = { onDateChange(null) }) {
                        Text("Очистить")
                    }
                }
                TextButton(onClick = onDismissRequest) {
                    Text("Отмена")
                }
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun ParaTaskBottomNavigation(
    selectedDestination: TaskListDestination,
    onDestinationSelected: (TaskListDestination) -> Unit,
) {
    NavigationBar {
        TaskListDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination,
                onClick = { onDestinationSelected(destination) },
                enabled = true,
                icon = { Text(if (selectedDestination == destination) "●" else "○") },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun EmptyTaskList(
    title: String,
    message: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatTaskDate(date: LocalDate): String = DATE_FORMATTER.format(date)

private fun LocalDate.toUtcEpochMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

private val TaskListDestination.label: String
    get() = when (this) {
        TaskListDestination.INBOX -> "Входящие"
        TaskListDestination.TODAY -> "Сегодня"
        TaskListDestination.UPCOMING -> "Предстоящие"
        TaskListDestination.BROWSE -> "Обзор"
    }

private val DATE_FORMATTER = DateTimeFormatter.ofPattern(
    "d MMMM yyyy",
    Locale.forLanguageTag("ru"),
)
