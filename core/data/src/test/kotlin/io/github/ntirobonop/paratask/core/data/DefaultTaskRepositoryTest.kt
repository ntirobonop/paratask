package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.TaskDao
import io.github.ntirobonop.paratask.core.database.TaskEntity
import io.github.ntirobonop.paratask.core.database.ProjectDao
import io.github.ntirobonop.paratask.core.database.ProjectEntity
import io.github.ntirobonop.paratask.core.database.SectionDao
import io.github.ntirobonop.paratask.core.database.SectionEntity
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.SectionId
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTaskRepositoryTest {
    private val instant = Instant.parse("2026-09-15T12:00:00Z")
    private val taskId = TaskId("task-1")
    private val dao = FakeTaskDao()
    private val repository = DefaultTaskRepository(
        taskDao = dao,
        clock = Clock.fixed(instant, ZoneOffset.UTC),
        idFactory = { taskId },
    )

    @Test
    fun createTaskNormalizesAndPersistsInput() = runTest {
        val createdId = repository.createTask("  Купить продукты  ", "  Молоко  ")

        val task = repository.observeTask(createdId).first()
        requireNotNull(task)
        assertEquals(taskId, createdId)
        assertEquals("Купить продукты", task.title)
        assertEquals("Молоко", task.description)
        assertEquals(instant, task.createdAt)
        assertFalse(task.isCompleted)
    }

    @Test
    fun createTaskRejectsBlankTitle() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.createTask("   ") }
        }
    }

    @Test
    fun completionRemovesTaskFromInboxAndCanBeUndone() = runTest {
        repository.createTask("Task")

        repository.setCompleted(taskId, completed = true)
        assertTrue(repository.observeInbox().first().isEmpty())
        assertEquals(instant, repository.observeTask(taskId).first()?.completedAt)

        repository.setCompleted(taskId, completed = false)
        assertEquals(listOf(taskId), repository.observeInbox().first().map { it.id })
        assertNull(repository.observeTask(taskId).first()?.completedAt)
    }

    @Test
    fun deletionRemovesTaskFromInboxAndCanBeUndone() = runTest {
        repository.createTask("Task")

        repository.setDeleted(taskId, deleted = true)
        assertTrue(repository.observeInbox().first().isEmpty())
        assertEquals(instant, repository.observeTask(taskId).first()?.deletedAt)

        repository.setDeleted(taskId, deleted = false)
        assertEquals(listOf(taskId), repository.observeInbox().first().map { it.id })
        assertNull(repository.observeTask(taskId).first()?.deletedAt)
    }

    @Test
    fun createTaskPersistsDateAndExposesItInToday() = runTest {
        val date = LocalDate.parse("2026-09-15")

        repository.createTask(title = "Task", dueDate = date)

        assertEquals(date, repository.observeTask(taskId).first()?.dueDate)
        assertEquals(listOf(taskId), repository.observeToday(date).first().map { it.id })
        assertTrue(repository.observeToday(date.plusDays(1)).first().isEmpty())
    }

    @Test
    fun dateRangeMapsInclusiveBoundaries() = runTest {
        val monday = LocalDate.parse("2026-09-14")
        repository.createTask(title = "Task", dueDate = monday)

        assertEquals(
            listOf(taskId),
            repository.observeTasksInDateRange(monday, monday.plusDays(6)).first().map { it.id },
        )
    }

    @Test
    fun dateRangeRejectsReversedBoundaries() {
        val monday = LocalDate.parse("2026-09-14")

        assertThrows(IllegalArgumentException::class.java) {
            repository.observeTasksInDateRange(monday, monday.minusDays(1))
        }
    }

    @Test
    fun createTaskPersistsSectionThatBelongsToProject() = runTest {
        val projectId = ProjectId("project-1")
        val sectionId = SectionId("section-1")
        val assignmentRepository = assignmentRepository(
            projects = listOf(project(projectId.value)),
            sections = listOf(section(sectionId.value, projectId.value)),
        )

        assignmentRepository.createTask(
            title = "Task",
            description = "",
            dueDate = null,
            projectId = projectId,
            sectionId = sectionId,
        )

        val task = assignmentRepository.observeTask(taskId).first()
        assertEquals(projectId, task?.projectId)
        assertEquals(sectionId, task?.sectionId)
    }

    @Test
    fun createTaskRejectsSectionFromAnotherProject() {
        val projectId = ProjectId("project-1")
        val otherProjectId = ProjectId("project-2")
        val sectionId = SectionId("section-1")
        val assignmentRepository = assignmentRepository(
            projects = listOf(project(projectId.value), project(otherProjectId.value)),
            sections = listOf(section(sectionId.value, otherProjectId.value)),
        )

        assertThrows(IllegalArgumentException::class.java) {
            runTest {
                assignmentRepository.createTask(
                    title = "Task",
                    description = "",
                    dueDate = null,
                    projectId = projectId,
                    sectionId = sectionId,
                )
            }
        }
    }

    @Test
    fun changingProjectClearsIncompatibleSection() = runTest {
        val firstProjectId = ProjectId("project-1")
        val secondProjectId = ProjectId("project-2")
        val sectionId = SectionId("section-1")
        val assignmentRepository = assignmentRepository(
            projects = listOf(project(firstProjectId.value), project(secondProjectId.value)),
            sections = listOf(section(sectionId.value, firstProjectId.value)),
        )
        assignmentRepository.createTask(
            title = "Task",
            description = "",
            dueDate = null,
            projectId = firstProjectId,
            sectionId = sectionId,
        )

        val task = requireNotNull(assignmentRepository.observeTask(taskId).first())
        assignmentRepository.updateTask(task.copy(projectId = secondProjectId))

        val updated = assignmentRepository.observeTask(taskId).first()
        assertEquals(secondProjectId, updated?.projectId)
        assertNull(updated?.sectionId)
    }

    @Test
    fun editingArchivedTaskPreservesItsExistingProjectAndSection() = runTest {
        val projectId = ProjectId("project")
        val sectionId = SectionId("section")
        val projects = mutableListOf(project(projectId.value))
        val repository = assignmentRepository(projects, listOf(section(sectionId.value, projectId.value)))
        repository.createTask("Task", "", null, projectId, sectionId)
        projects[0] = projects[0].copy(isArchived = true)

        val task = requireNotNull(repository.observeTask(taskId).first())
        repository.updateTask(task.copy(title = "Edited archived task"))

        val updated = requireNotNull(repository.observeTask(taskId).first())
        assertEquals("Edited archived task", updated.title)
        assertEquals(projectId, updated.projectId)
        assertEquals(sectionId, updated.sectionId)
    }

    @Test
    fun assigningNewOrExistingTasksToArchivedProjectIsRejected() = runTest {
        val projectId = ProjectId("archived")
        val repository = assignmentRepository(
            listOf(project(projectId.value).copy(isArchived = true)),
            emptyList(),
        )
        val createError = runCatching {
            repository.createTask("Task", "", null, projectId, null)
        }.exceptionOrNull()
        assertTrue(createError is IllegalArgumentException)

        repository.createTask("Inbox task", "", null)
        val task = requireNotNull(repository.observeTask(taskId).first())
        val updateError = runCatching {
            repository.updateTask(task.copy(projectId = projectId))
        }.exceptionOrNull()
        assertTrue(updateError is IllegalArgumentException)
        assertNull(repository.observeTask(taskId).first()?.projectId)
    }

    @Test
    fun movingTaskToInboxClearsSectionAndRejectsNewInboxSection() = runTest {
        val projectId = ProjectId("project")
        val sectionId = SectionId("section")
        val repository = assignmentRepository(
            listOf(project(projectId.value)),
            listOf(section(sectionId.value, projectId.value)),
        )
        repository.createTask("Task", "", null, projectId, sectionId)
        val task = requireNotNull(repository.observeTask(taskId).first())

        repository.updateTask(task.copy(projectId = null))

        assertNull(repository.observeTask(taskId).first()?.sectionId)
        val error = runCatching { repository.createTask("Invalid", "", null, null, sectionId) }
            .exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    private fun assignmentRepository(
        projects: List<ProjectEntity>,
        sections: List<SectionEntity>,
    ) = DefaultTaskRepository(
        taskDao = dao,
        projectDao = AssignmentProjectDao(projects),
        sectionDao = AssignmentSectionDao(sections),
        clock = Clock.fixed(instant, ZoneOffset.UTC),
        idFactory = { taskId },
    )
}

private class FakeTaskDao : TaskDao {
    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun observeInbox(): Flow<List<TaskEntity>> = tasks.map { values ->
        values
            .filter { it.projectId == null && it.deletedAt == null && !it.isCompleted }
            .sortedWith(compareBy(TaskEntity::sortOrder, TaskEntity::createdAt))
    }

    override fun observeToday(dueDate: String): Flow<List<TaskEntity>> = tasks.map { values ->
        values
            .filter { it.dueDate == dueDate && it.deletedAt == null && !it.isCompleted }
            .sortedWith(compareBy(TaskEntity::sortOrder, TaskEntity::createdAt))
    }

    override fun observeTasksInDateRange(
        startDate: String,
        endDate: String,
    ): Flow<List<TaskEntity>> = tasks.map { values ->
        values
            .filter { task ->
                task.dueDate?.let { it >= startDate && it <= endDate } == true &&
                    task.deletedAt == null &&
                    !task.isCompleted
            }
            .sortedWith(
                compareBy(TaskEntity::dueDate, TaskEntity::sortOrder, TaskEntity::createdAt),
            )
    }

    override fun observeProjectTasks(projectId: String): Flow<List<TaskEntity>> =
        tasks.map { values ->
            values.filter { task ->
                task.projectId == projectId && task.deletedAt == null && !task.isCompleted
            }
        }

    override fun observeTask(id: String): Flow<TaskEntity?> =
        tasks.map { values -> values.firstOrNull { it.id == id } }

    override suspend fun getTask(id: String): TaskEntity? =
        tasks.value.firstOrNull { it.id == id }

    override suspend fun insertTask(task: TaskEntity) {
        check(tasks.value.none { it.id == task.id })
        tasks.value += task
    }

    override suspend fun updateTask(task: TaskEntity): Int = replace(task.id) { task }

    override suspend fun setCompleted(
        id: String,
        completed: Boolean,
        completedAt: Long?,
        updatedAt: Long,
    ): Int = replace(id) { task ->
        task.copy(
            isCompleted = completed,
            completedAt = completedAt,
            updatedAt = updatedAt,
        )
    }

    override suspend fun setDeleted(
        id: String,
        deletedAt: Long?,
        updatedAt: Long,
    ): Int = replace(id) { task ->
        task.copy(deletedAt = deletedAt, updatedAt = updatedAt)
    }

    private fun replace(
        id: String,
        transform: (TaskEntity) -> TaskEntity,
    ): Int {
        var changed = 0
        tasks.value = tasks.value.map { task ->
            if (task.id == id) {
                changed += 1
                transform(task)
            } else {
                task
            }
        }
        return changed
    }
}

private class AssignmentProjectDao(
    private val projects: List<ProjectEntity>,
) : ProjectDao {
    override fun observeActiveProjects(): Flow<List<ProjectEntity>> = MutableStateFlow(projects)

    override fun observeArchivedProjects(): Flow<List<ProjectEntity>> = MutableStateFlow(emptyList())

    override fun observeProject(id: String): Flow<ProjectEntity?> =
        MutableStateFlow(projects.firstOrNull { it.id == id })

    override suspend fun getProject(id: String): ProjectEntity? =
        projects.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getActiveProjects(): List<ProjectEntity> =
        projects.filter { !it.isArchived && it.deletedAt == null }

    override suspend fun getMaxSortOrder(): Long =
        projects.maxOfOrNull(ProjectEntity::sortOrder) ?: -1

    override suspend fun insertProject(project: ProjectEntity) = error("Not used")

    override suspend fun updateProject(project: ProjectEntity): Int = error("Not used")

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long): Int =
        error("Not used")

    override suspend fun setSortOrder(id: String, sortOrder: Long, updatedAt: Long): Int =
        error("Not used")

    override suspend fun clearTaskAssignments(projectId: String, updatedAt: Long) =
        error("Not used")

    override suspend fun softDeleteProject(projectId: String, deletedAt: Long): Int =
        error("Not used")

    override suspend fun softDeleteProjectSections(projectId: String, deletedAt: Long) =
        error("Not used")
}

private class AssignmentSectionDao(
    private val sections: List<SectionEntity>,
) : SectionDao {
    override fun observeAllSections(): Flow<List<SectionEntity>> = MutableStateFlow(sections)

    override fun observeSections(projectId: String): Flow<List<SectionEntity>> =
        MutableStateFlow(sections.filter { it.projectId == projectId })

    override fun observeSection(id: String): Flow<SectionEntity?> =
        MutableStateFlow(sections.firstOrNull { it.id == id })

    override suspend fun getSection(id: String): SectionEntity? =
        sections.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getMaxSortOrder(projectId: String): Long =
        sections.filter { it.projectId == projectId }.maxOfOrNull(SectionEntity::sortOrder) ?: -1

    override suspend fun insertSection(section: SectionEntity) = error("Not used")

    override suspend fun updateSection(section: SectionEntity): Int = error("Not used")

    override suspend fun clearTaskAssignments(sectionId: String, updatedAt: Long) = error("Not used")

    override suspend fun softDeleteSection(sectionId: String, deletedAt: Long): Int =
        error("Not used")
}

private fun project(id: String) = ProjectEntity(
    id = id,
    name = id,
    color = 0,
    icon = "LIST",
    isArchived = false,
    createdAt = 10,
    updatedAt = 10,
    deletedAt = null,
    sortOrder = 0,
)

private fun section(id: String, projectId: String) = SectionEntity(
    id = id,
    projectId = projectId,
    name = id,
    createdAt = 10,
    updatedAt = 10,
    deletedAt = null,
    sortOrder = 0,
)
