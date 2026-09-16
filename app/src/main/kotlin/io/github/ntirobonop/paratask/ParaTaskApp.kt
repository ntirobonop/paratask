package io.github.ntirobonop.paratask

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.feature.inbox.InboxRoute
import io.github.ntirobonop.paratask.feature.task.TaskDetailsRoute
import io.github.ntirobonop.paratask.feature.today.TodayRoute
import io.github.ntirobonop.paratask.feature.upcoming.UpcomingRoute
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun ParaTaskApp(
    taskRepository: TaskRepository,
    today: LocalDate = LocalDate.now(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDestination by rememberSaveable { mutableStateOf(TopLevelDestination.INBOX.name) }

    val taskId = selectedTaskId?.let(::TaskId)
    if (taskId == null) {
        when (TopLevelDestination.valueOf(selectedDestination)) {
            TopLevelDestination.INBOX -> InboxRoute(
                taskRepository = taskRepository,
                snackbarHostState = snackbarHostState,
                onOpenTask = { selectedTaskId = it.value },
                onNavigateToToday = { selectedDestination = TopLevelDestination.TODAY.name },
                onNavigateToUpcoming = {
                    selectedDestination = TopLevelDestination.UPCOMING.name
                },
            )

            TopLevelDestination.TODAY -> TodayRoute(
                taskRepository = taskRepository,
                snackbarHostState = snackbarHostState,
                onOpenTask = { selectedTaskId = it.value },
                onNavigateToInbox = { selectedDestination = TopLevelDestination.INBOX.name },
                onNavigateToUpcoming = {
                    selectedDestination = TopLevelDestination.UPCOMING.name
                },
                today = today,
            )

            TopLevelDestination.UPCOMING -> UpcomingRoute(
                taskRepository = taskRepository,
                snackbarHostState = snackbarHostState,
                onOpenTask = { selectedTaskId = it.value },
                onNavigateToInbox = { selectedDestination = TopLevelDestination.INBOX.name },
                onNavigateToToday = { selectedDestination = TopLevelDestination.TODAY.name },
                today = today,
            )
        }
    } else {
        TaskDetailsRoute(
            taskRepository = taskRepository,
            taskId = taskId,
            snackbarHostState = snackbarHostState,
            onClose = { selectedTaskId = null },
            onDeleted = { deletedTaskId ->
                selectedTaskId = null
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Задача удалена",
                        actionLabel = "Отменить",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        runCatching {
                            taskRepository.setDeleted(
                                id = deletedTaskId,
                                deleted = false,
                            )
                        }.onFailure {
                            snackbarHostState.showSnackbar("Не удалось восстановить задачу")
                        }
                    }
                }
            },
        )
    }
}

private enum class TopLevelDestination {
    INBOX,
    TODAY,
    UPCOMING,
}
