package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.ProjectDao
import io.github.ntirobonop.paratask.core.database.ProjectEntity
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultProjectRepository(
    private val projectDao: ProjectDao,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> ProjectId = ProjectId::random,
) : ProjectRepository {
    override fun observeActiveProjects(): Flow<List<Project>> =
        projectDao.observeActiveProjects().map { projects -> projects.map(ProjectEntity::toDomain) }

    override fun observeArchivedProjects(): Flow<List<Project>> =
        projectDao.observeArchivedProjects()
            .map { projects -> projects.map(ProjectEntity::toDomain) }

    override fun observeProject(id: ProjectId): Flow<Project?> =
        projectDao.observeProject(id.value).map { project -> project?.toDomain() }

    override suspend fun createProject(
        name: String,
        color: Long,
        icon: ProjectIcon,
    ): ProjectId {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Project name must not be blank" }
        val id = idFactory()
        val now = clock.instant()
        projectDao.insertProject(
            ProjectEntity(
                id = id.value,
                name = normalizedName,
                color = color,
                icon = icon.name,
                isArchived = false,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
                deletedAt = null,
                sortOrder = projectDao.getMaxSortOrder() + 1,
            ),
        )
        return id
    }

    override suspend fun updateProject(project: Project) {
        val normalizedName = project.name.trim()
        require(normalizedName.isNotEmpty()) { "Project name must not be blank" }
        val current = projectDao.getProject(project.id.value)
            ?: error("Project does not exist")
        projectDao.updateProject(
            current.copy(
                name = normalizedName,
                color = project.color,
                icon = project.icon.name,
                updatedAt = clock.instant().toEpochMilli(),
            ),
        )
    }

    override suspend fun setArchived(
        id: ProjectId,
        archived: Boolean,
    ) {
        check(projectDao.setArchived(id.value, archived, clock.millis()) == 1) {
            "Project does not exist"
        }
    }

    override suspend fun deleteProject(id: ProjectId) {
        check(
            projectDao.deleteProjectAndReturnTasksToInbox(
                projectId = id.value,
                deletedAt = clock.millis(),
            ) == 1,
        ) {
            "Project does not exist"
        }
    }

    override suspend fun moveProject(
        id: ProjectId,
        move: ProjectMove,
    ) {
        val projects = projectDao.getActiveProjects()
        val currentIndex = projects.indexOfFirst { project -> project.id == id.value }
        check(currentIndex >= 0) { "Project does not exist or is not active" }
        val targetIndex = when (move) {
            ProjectMove.UP -> currentIndex - 1
            ProjectMove.DOWN -> currentIndex + 1
        }
        if (targetIndex !in projects.indices) return
        val current = projects[currentIndex]
        val target = projects[targetIndex]
        projectDao.swapSortOrders(
            firstId = current.id,
            firstOrder = current.sortOrder,
            secondId = target.id,
            secondOrder = target.sortOrder,
            updatedAt = clock.millis(),
        )
    }
}

internal fun ProjectEntity.toDomain(): Project = Project(
    id = ProjectId(id),
    name = name,
    color = color,
    icon = ProjectIcon.valueOf(icon),
    isArchived = isArchived,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deletedAt = deletedAt?.let(Instant::ofEpochMilli),
    sortOrder = sortOrder,
)
