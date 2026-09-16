package io.github.ntirobonop.paratask.feature.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.ui.ParaTaskBottomNavigation
import io.github.ntirobonop.paratask.core.ui.TaskComposerSheet
import io.github.ntirobonop.paratask.core.ui.TaskDraft
import io.github.ntirobonop.paratask.core.ui.TaskListContent
import io.github.ntirobonop.paratask.core.ui.TaskListDestination
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun UpcomingRoute(
    taskRepository: TaskRepository,
    snackbarHostState: SnackbarHostState,
    onOpenTask: (TaskId) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToToday: () -> Unit,
    today: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier,
    viewModel: UpcomingViewModel = viewModel(
        factory = UpcomingViewModel.factory(taskRepository, today),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is UpcomingUiEvent.TaskCompleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Задача выполнена",
                        actionLabel = "Отменить",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoCompletion(event.taskId)
                    }
                }

                is UpcomingUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    UpcomingScreen(
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
        onSelectDate = viewModel::selectDate,
        onPreviousWeek = viewModel::showPreviousWeek,
        onNextWeek = viewModel::showNextWeek,
        onToday = viewModel::showToday,
        onCompleteTask = viewModel::completeTask,
        onOpenTask = onOpenTask,
        onNavigateToInbox = onNavigateToInbox,
        onNavigateToToday = onNavigateToToday,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    uiState: UpcomingUiState,
    snackbarHostState: SnackbarHostState,
    showQuickAdd: Boolean,
    onAddTask: () -> Unit,
    onDismissQuickAdd: () -> Unit,
    onCreateTask: (TaskDraft) -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit,
    onCompleteTask: (TaskId) -> Unit,
    onOpenTask: (TaskId) -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Предстоящие") },
                actions = {
                    TextButton(
                        onClick = onToday,
                        modifier = Modifier.semantics {
                            contentDescription = "Перейти к сегодняшней дате"
                        },
                    ) {
                        Text("Сегодня")
                    }
                },
            )
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
                selectedDestination = TaskListDestination.UPCOMING,
                onDestinationSelected = { destination ->
                    when (destination) {
                        TaskListDestination.INBOX -> onNavigateToInbox()
                        TaskListDestination.TODAY -> onNavigateToToday()
                        else -> Unit
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            WeekSelector(
                uiState = uiState,
                onSelectDate = onSelectDate,
                onPreviousWeek = onPreviousWeek,
                onNextWeek = onNextWeek,
            )
            TaskListContent(
                tasks = uiState.selectedTasks,
                isLoading = uiState.isLoading,
                emptyTitle = "На выбранную дату задач нет",
                emptyMessage = "Выберите другой день или создайте новую задачу",
                onCompleteTask = onCompleteTask,
                onOpenTask = onOpenTask,
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding(),
                ),
                modifier = Modifier.weight(1f),
                showDueDate = false,
            )
        }
    }

    if (showQuickAdd) {
        TaskComposerSheet(
            initialDueDate = uiState.selectedDate,
            onDismissRequest = onDismissQuickAdd,
            onCreateTask = onCreateTask,
        )
    }
}

@Composable
private fun WeekSelector(
    uiState: UpcomingUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onPreviousWeek,
                modifier = Modifier.semantics { contentDescription = "Предыдущая неделя" },
            ) {
                Text("‹")
            }
            Text(
                text = formatWeekRange(uiState.weekStart),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(
                onClick = onNextWeek,
                modifier = Modifier.semantics { contentDescription = "Следующая неделя" },
            ) {
                Text("›")
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            uiState.weekDates.forEach { date ->
                DayCell(
                    date = date,
                    selected = date == uiState.selectedDate,
                    isToday = date == uiState.today,
                    taskCount = uiState.taskCountByDate[date] ?: 0,
                    onClick = { onSelectDate(date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    taskCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val description = buildString {
        append(FULL_DATE_FORMATTER.format(date))
        append(", задач: ")
        append(taskCount)
        if (selected) append(", выбрано")
    }

    Surface(
        modifier = modifier
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = DAY_FORMATTER.format(date).uppercase(RU_LOCALE),
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday || selected) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday && !selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (taskCount > 0) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
            } else {
                Box(modifier = Modifier.size(6.dp))
            }
        }
    }
}

private fun formatWeekRange(weekStart: LocalDate): String {
    val weekEnd = weekStart.plusDays(6)
    return if (weekStart.month == weekEnd.month) {
        "${weekStart.dayOfMonth}–${WEEK_END_FORMATTER.format(weekEnd)}"
    } else {
        "${WEEK_START_FORMATTER.format(weekStart)} – ${WEEK_END_FORMATTER.format(weekEnd)}"
    }
}

private val RU_LOCALE = Locale.forLanguageTag("ru")
private val DAY_FORMATTER = DateTimeFormatter.ofPattern("EE", RU_LOCALE)
private val FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, d MMMM", RU_LOCALE)
private val WEEK_START_FORMATTER = DateTimeFormatter.ofPattern("d MMMM", RU_LOCALE)
private val WEEK_END_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", RU_LOCALE)
