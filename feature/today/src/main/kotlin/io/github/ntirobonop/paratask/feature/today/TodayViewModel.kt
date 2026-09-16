package io.github.ntirobonop.paratask.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.model.ProjectId
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val date: LocalDate,
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface TodayUiEvent {
    data class TaskCompleted(val taskId: TaskId) : TodayUiEvent

    data class ShowMessage(val message: String) : TodayUiEvent
}

class TodayViewModel(
    private val taskRepository: TaskRepository,
    val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val eventChannel = Channel<TodayUiEvent>(capacity = Channel.BUFFERED)
    val events: Flow<TodayUiEvent> = eventChannel.receiveAsFlow()

    val uiState = taskRepository.observeToday(today)
        .map { tasks -> TodayUiState(date = today, tasks = tasks, isLoading = false) }
        .catch {
            emit(TodayUiState(date = today, isLoading = false))
            eventChannel.send(TodayUiEvent.ShowMessage("Не удалось загрузить задачи"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TodayUiState(date = today),
        )

    fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
        projectId: ProjectId? = null,
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            runCatching {
                taskRepository.createTask(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    projectId = projectId,
                )
            }.onFailure {
                eventChannel.send(TodayUiEvent.ShowMessage("Не удалось создать задачу"))
            }
        }
    }

    fun completeTask(taskId: TaskId) {
        viewModelScope.launch {
            runCatching {
                taskRepository.setCompleted(id = taskId, completed = true)
            }.onSuccess {
                eventChannel.send(TodayUiEvent.TaskCompleted(taskId))
            }.onFailure {
                eventChannel.send(TodayUiEvent.ShowMessage("Не удалось выполнить задачу"))
            }
        }
    }

    fun undoCompletion(taskId: TaskId) {
        viewModelScope.launch {
            runCatching {
                taskRepository.setCompleted(id = taskId, completed = false)
            }.onFailure {
                eventChannel.send(TodayUiEvent.ShowMessage("Не удалось вернуть задачу"))
            }
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            today: LocalDate = LocalDate.now(),
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TodayViewModel(taskRepository = taskRepository, today = today) }
        }
    }
}
