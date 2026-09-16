package io.github.ntirobonop.paratask.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.ntirobonop.paratask.core.data.ProjectMove
import io.github.ntirobonop.paratask.core.data.ProjectRepository
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrowseUiState(
    val activeProjects: List<Project> = emptyList(),
    val archivedProjects: List<Project> = emptyList(),
    val isLoading: Boolean = true,
)

sealed interface BrowseUiEvent {
    data class ShowMessage(val message: String) : BrowseUiEvent
}

class BrowseViewModel(
    private val projectRepository: ProjectRepository,
) : ViewModel() {
    private val eventChannel = Channel<BrowseUiEvent>(Channel.BUFFERED)
    val events: Flow<BrowseUiEvent> = eventChannel.receiveAsFlow()

    val uiState = combine(
        projectRepository.observeActiveProjects(),
        projectRepository.observeArchivedProjects(),
    ) { active, archived ->
        BrowseUiState(
            activeProjects = active,
            archivedProjects = archived,
            isLoading = false,
        )
    }.catch {
        eventChannel.send(BrowseUiEvent.ShowMessage("Не удалось загрузить проекты"))
        emit(BrowseUiState(isLoading = false))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrowseUiState(),
    )

    fun createProject(
        name: String,
        color: Long,
        icon: ProjectIcon,
    ) = launchOperation("Не удалось создать проект") {
        projectRepository.createProject(name = name, color = color, icon = icon)
    }

    fun updateProject(
        project: Project,
        name: String,
        color: Long,
        icon: ProjectIcon,
    ) = launchOperation("Не удалось сохранить проект") {
        projectRepository.updateProject(
            project.copy(name = name, color = color, icon = icon),
        )
    }

    fun setArchived(
        id: ProjectId,
        archived: Boolean,
    ) = launchOperation("Не удалось изменить проект") {
        projectRepository.setArchived(id = id, archived = archived)
    }

    fun deleteProject(id: ProjectId) = launchOperation("Не удалось удалить проект") {
        projectRepository.deleteProject(id)
    }

    fun moveProject(
        id: ProjectId,
        move: ProjectMove,
    ) = launchOperation("Не удалось изменить порядок проектов") {
        projectRepository.moveProject(id = id, move = move)
    }

    private fun launchOperation(
        failureMessage: String,
        operation: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { operation() }.onFailure {
                eventChannel.send(BrowseUiEvent.ShowMessage(failureMessage))
            }
        }
    }

    companion object {
        fun factory(projectRepository: ProjectRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { BrowseViewModel(projectRepository) }
            }
    }
}
