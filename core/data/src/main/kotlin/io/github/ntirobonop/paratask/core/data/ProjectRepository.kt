package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import kotlinx.coroutines.flow.Flow

enum class ProjectMove {
    UP,
    DOWN,
}

interface ProjectRepository {
    fun observeActiveProjects(): Flow<List<Project>>

    fun observeArchivedProjects(): Flow<List<Project>>

    fun observeProject(id: ProjectId): Flow<Project?>

    suspend fun createProject(
        name: String,
        color: Long,
        icon: ProjectIcon,
    ): ProjectId

    suspend fun updateProject(project: Project)

    suspend fun setArchived(
        id: ProjectId,
        archived: Boolean,
    )

    suspend fun deleteProject(id: ProjectId)

    suspend fun moveProject(
        id: ProjectId,
        move: ProjectMove,
    )
}
