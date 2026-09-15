package io.github.ntirobonop.paratask.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class TaskDetailsUiState(
    val taskId: TaskId,
    val isLoading: Boolean = true,
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false,
    val hasPendingChanges: Boolean = false,
    val taskMissing: Boolean = false,
)

sealed interface TaskDetailsUiEvent {
    data object Close : TaskDetailsUiEvent

    data class Deleted(val taskId: TaskId) : TaskDetailsUiEvent

    data class ShowMessage(val message: String) : TaskDetailsUiEvent
}

class TaskDetailsViewModel(
    private val taskRepository: TaskRepository,
    private val taskId: TaskId,
    private val autosaveDelayMillis: Long = AUTOSAVE_DELAY_MILLIS,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskDetailsUiState(taskId = taskId))
    val uiState: StateFlow<TaskDetailsUiState> = _uiState.asStateFlow()

    private val eventChannel = Channel<TaskDetailsUiEvent>(capacity = Channel.BUFFERED)
    val events: Flow<TaskDetailsUiEvent> = eventChannel.receiveAsFlow()

    private var currentTask: Task? = null
    private var editRevision = 0L
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            taskRepository.observeTask(taskId)
                .catch {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        taskMissing = true,
                    )
                    eventChannel.send(TaskDetailsUiEvent.ShowMessage("Не удалось загрузить задачу"))
                }
                .collect(::applyPersistedTask)
        }
    }

    fun updateTitle(title: String) {
        editRevision += 1
        _uiState.value = _uiState.value.copy(
            title = title,
            hasPendingChanges = true,
        )
        scheduleAutosave()
    }

    fun updateDescription(description: String) {
        editRevision += 1
        _uiState.value = _uiState.value.copy(
            description = description,
            hasPendingChanges = true,
        )
        scheduleAutosave()
    }

    fun setCompleted(completed: Boolean) {
        saveJob?.cancel()
        viewModelScope.launch {
            if (hasValidPendingChanges() && !persistPendingChanges(editRevision)) return@launch
            runCatching {
                taskRepository.setCompleted(id = taskId, completed = completed)
            }.onFailure {
                eventChannel.send(TaskDetailsUiEvent.ShowMessage("Не удалось изменить задачу"))
            }
        }
    }

    fun deleteTask() {
        saveJob?.cancel()
        viewModelScope.launch {
            runCatching {
                taskRepository.setDeleted(id = taskId, deleted = true)
            }.onSuccess {
                eventChannel.send(TaskDetailsUiEvent.Deleted(taskId))
            }.onFailure {
                eventChannel.send(TaskDetailsUiEvent.ShowMessage("Не удалось удалить задачу"))
            }
        }
    }

    fun navigateBack() {
        saveJob?.cancel()
        viewModelScope.launch {
            when {
                hasValidPendingChanges() && !persistPendingChanges(editRevision) -> return@launch
                _uiState.value.hasPendingChanges -> discardPendingChanges()
            }
            eventChannel.send(TaskDetailsUiEvent.Close)
        }
    }

    private fun applyPersistedTask(task: Task?) {
        currentTask = task
        if (task == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                taskMissing = true,
            )
            return
        }

        val state = _uiState.value
        val keepDraft = state.hasPendingChanges
        _uiState.value = state.copy(
            isLoading = false,
            title = if (keepDraft) state.title else task.title,
            description = if (keepDraft) state.description else task.description,
            isCompleted = task.isCompleted,
            taskMissing = false,
        )
    }

    private fun scheduleAutosave() {
        saveJob?.cancel()
        if (_uiState.value.title.isBlank()) return

        val revision = editRevision
        saveJob = viewModelScope.launch {
            delay(autosaveDelayMillis)
            persistPendingChanges(revision)
        }
    }

    private suspend fun persistPendingChanges(revision: Long): Boolean {
        val task = currentTask ?: return false
        val state = _uiState.value
        if (!state.hasPendingChanges || state.title.isBlank()) return false

        return runCatching {
            taskRepository.updateTask(
                task.copy(
                    title = state.title,
                    description = state.description,
                ),
            )
        }.onSuccess {
            if (editRevision == revision) {
                _uiState.value = _uiState.value.copy(hasPendingChanges = false)
            }
        }.onFailure {
            eventChannel.send(TaskDetailsUiEvent.ShowMessage("Не удалось сохранить изменения"))
        }.isSuccess
    }

    private fun hasValidPendingChanges(): Boolean =
        _uiState.value.hasPendingChanges && _uiState.value.title.isNotBlank()

    private fun discardPendingChanges() {
        val task = currentTask ?: return
        _uiState.value = _uiState.value.copy(
            title = task.title,
            description = task.description,
            hasPendingChanges = false,
        )
    }

    companion object {
        private const val AUTOSAVE_DELAY_MILLIS = 500L

        fun factory(
            taskRepository: TaskRepository,
            taskId: TaskId,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TaskDetailsViewModel(
                    taskRepository = taskRepository,
                    taskId = taskId,
                )
            }
        }
    }
}
