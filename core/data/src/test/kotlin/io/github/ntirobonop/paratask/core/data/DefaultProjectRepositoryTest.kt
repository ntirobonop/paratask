package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.ProjectDao
import io.github.ntirobonop.paratask.core.database.ProjectEntity
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultProjectRepositoryTest {
    private val instant = Instant.parse("2026-09-16T12:00:00Z")
    private val projectId = ProjectId("project-1")
    private val dao = FakeProjectDao()
    private val repository = DefaultProjectRepository(
        projectDao = dao,
        clock = Clock.fixed(instant, ZoneOffset.UTC),
        idFactory = { projectId },
    )

    @Test
    fun createProjectNormalizesAndPersistsInput() = runTest {
        val createdId = repository.createProject(
            name = "  Университет  ",
            color = 0xFF6750A4,
            icon = ProjectIcon.SCHOOL,
        )

        val project = repository.observeProject(createdId).first()
        requireNotNull(project)
        assertEquals(projectId, createdId)
        assertEquals("Университет", project.name)
        assertEquals(ProjectIcon.SCHOOL, project.icon)
        assertEquals(instant, project.createdAt)
        assertFalse(project.isArchived)
    }

    @Test
    fun createProjectRejectsBlankName() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.createProject("   ", 0, ProjectIcon.LIST) }
        }
    }

    @Test
    fun archiveRemovesProjectFromActiveListAndCanBeUndone() = runTest {
        repository.createProject("Дом", 1, ProjectIcon.HOME)

        repository.setArchived(projectId, archived = true)
        assertTrue(repository.observeActiveProjects().first().isEmpty())
        assertEquals(listOf(projectId), repository.observeArchivedProjects().first().map { it.id })

        repository.setArchived(projectId, archived = false)
        assertEquals(listOf(projectId), repository.observeActiveProjects().first().map { it.id })
    }

    @Test
    fun deleteSoftDeletesProjectAndClearsTaskAssignments() = runTest {
        repository.createProject("Дом", 1, ProjectIcon.HOME)

        repository.deleteProject(projectId)

        assertTrue(repository.observeActiveProjects().first().isEmpty())
        assertEquals(listOf(projectId.value), dao.clearedAssignments)
        assertEquals(listOf(projectId.value), dao.deletedProjectSections)
        assertEquals(instant.toEpochMilli(), dao.allProjects().single().deletedAt)
    }

    @Test
    fun moveProjectSwapsAdjacentSortOrders() = runTest {
        val ids = listOf(ProjectId("first"), ProjectId("second"), ProjectId("third"))
        var nextId = 0
        val movingRepository = DefaultProjectRepository(
            projectDao = dao,
            clock = Clock.fixed(instant, ZoneOffset.UTC),
            idFactory = { ids[nextId++] },
        )
        ids.forEachIndexed { index, _ ->
            movingRepository.createProject("Project $index", index.toLong(), ProjectIcon.LIST)
        }

        movingRepository.moveProject(ids[1], ProjectMove.UP)

        assertEquals(
            listOf(ids[1], ids[0], ids[2]),
            movingRepository.observeActiveProjects().first().map { it.id },
        )
    }
}

private class FakeProjectDao : ProjectDao {
    private val projects = MutableStateFlow<List<ProjectEntity>>(emptyList())
    val clearedAssignments = mutableListOf<String>()
    val deletedProjectSections = mutableListOf<String>()

    override fun observeActiveProjects(): Flow<List<ProjectEntity>> = projects.map(::active)

    override fun observeArchivedProjects(): Flow<List<ProjectEntity>> = projects.map { values ->
        values.filter { it.isArchived && it.deletedAt == null }.sortedBy(ProjectEntity::sortOrder)
    }

    override fun observeProject(id: String): Flow<ProjectEntity?> = projects.map { values ->
        values.firstOrNull { it.id == id && it.deletedAt == null }
    }

    override suspend fun getProject(id: String): ProjectEntity? =
        projects.value.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getActiveProjects(): List<ProjectEntity> = active(projects.value)

    override suspend fun getMaxSortOrder(): Long =
        projects.value.filter { it.deletedAt == null }.maxOfOrNull(ProjectEntity::sortOrder) ?: -1

    override suspend fun insertProject(project: ProjectEntity) {
        check(projects.value.none { it.id == project.id })
        projects.value += project
    }

    override suspend fun updateProject(project: ProjectEntity): Int = replace(project.id) { project }

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long): Int =
        replace(id) { it.copy(isArchived = archived, updatedAt = updatedAt) }

    override suspend fun setSortOrder(id: String, sortOrder: Long, updatedAt: Long): Int =
        replace(id) { it.copy(sortOrder = sortOrder, updatedAt = updatedAt) }

    override suspend fun clearTaskAssignments(projectId: String, updatedAt: Long) {
        clearedAssignments += projectId
    }

    override suspend fun softDeleteProject(projectId: String, deletedAt: Long): Int =
        replace(projectId) { it.copy(deletedAt = deletedAt, updatedAt = deletedAt) }

    override suspend fun softDeleteProjectSections(projectId: String, deletedAt: Long) {
        deletedProjectSections += projectId
    }

    fun allProjects(): List<ProjectEntity> = projects.value

    private fun active(values: List<ProjectEntity>): List<ProjectEntity> =
        values.filter { !it.isArchived && it.deletedAt == null }.sortedBy(ProjectEntity::sortOrder)

    private fun replace(id: String, transform: (ProjectEntity) -> ProjectEntity): Int {
        var changed = 0
        projects.value = projects.value.map { project ->
            if (project.id == id && project.deletedAt == null) {
                changed += 1
                transform(project)
            } else {
                project
            }
        }
        return changed
    }
}
