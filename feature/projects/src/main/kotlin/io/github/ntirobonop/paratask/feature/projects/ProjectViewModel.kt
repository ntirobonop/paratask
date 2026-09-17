package io.github.ntirobonop.paratask.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ntirobonop.paratask.core.data.ProjectRepository
import io.github.ntirobonop.paratask.core.data.SectionRepository
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.Section
import io.github.ntirobonop.paratask.core.model.SectionId
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectUiState(
    val project: Project? = null,
    val tasks: List<Task> = emptyList(),
    val sections: List<Section> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface ProjectUiEvent {
    data class TaskCompleted(val taskId: TaskId) : ProjectUiEvent

    data class ShowMessage(val message: String) : ProjectUiEvent
}

class ProjectViewModel(
    private val taskRepository: TaskRepository,
    projectRepository: ProjectRepository,
    private val sectionRepository: SectionRepository,
    private val projectId: ProjectId,
) : ViewModel() {
    private val eventChannel = Channel<ProjectUiEvent>(Channel.BUFFERED)
    val events: Flow<ProjectUiEvent> = eventChannel.receiveAsFlow()

    val uiState = combine(
        projectRepository.observeProject(projectId),
        taskRepository.observeProjectTasks(projectId),
        sectionRepository.observeSections(projectId),
    ) { project, tasks, sections ->
        ProjectUiState(
            project = project,
            tasks = tasks,
            sections = sections,
            isLoading = false,
        )
    }.catch {
        eventChannel.send(ProjectUiEvent.ShowMessage("Не удалось загрузить проект"))
        emit(ProjectUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProjectUiState(),
    )

    fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
        selectedProjectId: ProjectId?,
        selectedSectionId: SectionId? = null,
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching {
                taskRepository.createTask(
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    projectId = selectedProjectId,
                    sectionId = selectedSectionId,
                )
            }.onFailure {
                eventChannel.send(ProjectUiEvent.ShowMessage("Не удалось создать задачу"))
            }
        }
    }

    fun createSection(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { sectionRepository.createSection(projectId, name) }
                .onFailure {
                    eventChannel.send(ProjectUiEvent.ShowMessage("Не удалось создать секцию"))
                }
        }
    }

    fun renameSection(id: SectionId, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching { sectionRepository.renameSection(id, name) }
                .onFailure {
                    eventChannel.send(ProjectUiEvent.ShowMessage("Не удалось переименовать секцию"))
                }
        }
    }

    fun deleteSection(id: SectionId) {
        viewModelScope.launch {
            runCatching { sectionRepository.deleteSection(id) }
                .onFailure {
                    eventChannel.send(ProjectUiEvent.ShowMessage("Не удалось удалить секцию"))
                }
        }
    }

    fun completeTask(id: TaskId) {
        viewModelScope.launch {
            runCatching { taskRepository.setCompleted(id, true) }
                .onSuccess { eventChannel.send(ProjectUiEvent.TaskCompleted(id)) }
                .onFailure {
                    eventChannel.send(ProjectUiEvent.ShowMessage("Не удалось выполнить задачу"))
                }
        }
    }

    fun undoCompletion(id: TaskId) {
        viewModelScope.launch {
            runCatching { taskRepository.setCompleted(id, false) }
                .onFailure {
                    eventChannel.send(ProjectUiEvent.ShowMessage("Не удалось вернуть задачу"))
                }
        }
    }

    companion object {
        fun factory(
            taskRepository: TaskRepository,
            projectRepository: ProjectRepository,
            sectionRepository: SectionRepository,
            projectId: ProjectId,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ProjectViewModel(
                    taskRepository = taskRepository,
                    projectRepository = projectRepository,
                    sectionRepository = sectionRepository,
                    projectId = projectId,
                )
            }
        }
    }
}
