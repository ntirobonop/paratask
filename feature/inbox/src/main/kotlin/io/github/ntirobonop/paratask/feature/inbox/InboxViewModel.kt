package io.github.ntirobonop.paratask.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface InboxUiEvent {
    data class TaskCompleted(val taskId: TaskId) : InboxUiEvent

    data class ShowMessage(val message: String) : InboxUiEvent
}

class InboxViewModel(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    private val eventChannel = Channel<InboxUiEvent>(capacity = Channel.BUFFERED)
    val events: Flow<InboxUiEvent> = eventChannel.receiveAsFlow()

    val uiState = taskRepository.observeInbox()
        .map { tasks -> InboxUiState(tasks = tasks, isLoading = false) }
        .catch {
            emit(InboxUiState(isLoading = false))
            eventChannel.send(InboxUiEvent.ShowMessage("Не удалось загрузить задачи"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = InboxUiState(),
        )

    fun createTask(
        title: String,
        description: String,
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            runCatching {
                taskRepository.createTask(title = title, description = description)
            }.onFailure {
                eventChannel.send(InboxUiEvent.ShowMessage("Не удалось создать задачу"))
            }
        }
    }

    fun completeTask(taskId: TaskId) {
        viewModelScope.launch {
            runCatching {
                taskRepository.setCompleted(id = taskId, completed = true)
            }.onSuccess {
                eventChannel.send(InboxUiEvent.TaskCompleted(taskId))
            }.onFailure {
                eventChannel.send(InboxUiEvent.ShowMessage("Не удалось выполнить задачу"))
            }
        }
    }

    fun undoCompletion(taskId: TaskId) {
        viewModelScope.launch {
            runCatching {
                taskRepository.setCompleted(id = taskId, completed = false)
            }.onFailure {
                eventChannel.send(InboxUiEvent.ShowMessage("Не удалось вернуть задачу"))
            }
        }
    }

    companion object {
        fun factory(taskRepository: TaskRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { InboxViewModel(taskRepository) }
        }
    }
}
