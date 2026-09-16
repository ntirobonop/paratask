package io.github.ntirobonop.paratask.feature.upcoming

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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UpcomingUiState(
    val today: LocalDate,
    val selectedDate: LocalDate,
    val weekStart: LocalDate,
    val weekTasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
) {
    val weekDates: List<LocalDate>
        get() = weekDates(weekStart)

    val selectedTasks: List<Task>
        get() = weekTasks.filter { task -> task.dueDate == selectedDate }

    val taskCountByDate: Map<LocalDate, Int>
        get() = weekTasks
            .mapNotNull(Task::dueDate)
            .groupingBy { it }
            .eachCount()
}

sealed interface UpcomingUiEvent {
    data class TaskCompleted(val taskId: TaskId) : UpcomingUiEvent

    data class ShowMessage(val message: String) : UpcomingUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingViewModel(
    private val taskRepository: TaskRepository,
    val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val selectedDate = MutableStateFlow(today)
    private val eventChannel = Channel<UpcomingUiEvent>(capacity = Channel.BUFFERED)
    val events: Flow<UpcomingUiEvent> = eventChannel.receiveAsFlow()

    private val weekSnapshot = selectedDate
        .map(LocalDate::isoWeekStart)
        .distinctUntilChanged()
        .flatMapLatest(::observeWeek)

    val uiState = combine(selectedDate, weekSnapshot) { selectedDate, snapshot ->
        UpcomingUiState(
            today = today,
            selectedDate = selectedDate,
            weekStart = selectedDate.isoWeekStart(),
            weekTasks = snapshot.tasks.takeIf {
                snapshot.weekStart == selectedDate.isoWeekStart()
            }.orEmpty(),
            isLoading = snapshot.weekStart != selectedDate.isoWeekStart() || snapshot.isLoading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UpcomingUiState(
            today = today,
            selectedDate = today,
            weekStart = today.isoWeekStart(),
        ),
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun showPreviousWeek() {
        selectedDate.value = selectedDate.value.minusWeeks(1)
    }

    fun showNextWeek() {
        selectedDate.value = selectedDate.value.plusWeeks(1)
    }

    fun showToday() {
        selectedDate.value = today
    }

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
                eventChannel.send(UpcomingUiEvent.ShowMessage("Не удалось создать задачу"))
            }
        }
    }

    fun completeTask(taskId: TaskId) {
        viewModelScope.launch {
            runCatching {
                taskRepository.setCompleted(id = taskId, completed = true)
            }.onSuccess {
                eventChannel.send(UpcomingUiEvent.TaskCompleted(taskId))
            }.onFailure {
                eventChannel.send(UpcomingUiEvent.ShowMessage("Не удалось выполнить задачу"))
            }
        }
    }

    fun undoCompletion(taskId: TaskId) {
        viewModelScope.launch {
            runCatching {
                taskRepository.setCompleted(id = taskId, completed = false)
            }.onFailure {
                eventChannel.send(UpcomingUiEvent.ShowMessage("Не удалось вернуть задачу"))
            }
        }
    }

    private fun observeWeek(weekStart: LocalDate): Flow<WeekSnapshot> =
        taskRepository.observeTasksInDateRange(
            startDate = weekStart,
            endDate = weekStart.plusDays(6),
        ).map { tasks ->
            WeekSnapshot(weekStart = weekStart, tasks = tasks, isLoading = false)
        }.catch {
            eventChannel.send(UpcomingUiEvent.ShowMessage("Не удалось загрузить задачи"))
            emit(WeekSnapshot(weekStart = weekStart, isLoading = false))
        }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            today: LocalDate = LocalDate.now(),
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { UpcomingViewModel(taskRepository = taskRepository, today = today) }
        }
    }
}

private data class WeekSnapshot(
    val weekStart: LocalDate,
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)
