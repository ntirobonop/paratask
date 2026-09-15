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
import kotlinx.coroutines.launch

@Composable
fun ParaTaskApp(taskRepository: TaskRepository) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var selectedTaskId by rememberSaveable { mutableStateOf<String?>(null) }

    val taskId = selectedTaskId?.let(::TaskId)
    if (taskId == null) {
        InboxRoute(
            taskRepository = taskRepository,
            snackbarHostState = snackbarHostState,
            onOpenTask = { selectedTaskId = it.value },
        )
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
