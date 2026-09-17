package io.github.ntirobonop.paratask.feature.projects

import io.github.ntirobonop.paratask.core.data.ProjectMove
import io.github.ntirobonop.paratask.core.data.ProjectRepository
import io.github.ntirobonop.paratask.core.data.SectionRepository
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.Section
import io.github.ntirobonop.paratask.core.model.SectionId
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal class ProjectTestRepository : ProjectRepository, SectionRepository, TaskRepository {
    val project = Project(ProjectId("project"), "Учёба", 0xFF6750A4, ProjectIcon.SCHOOL,
        createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
    val section = Section(SectionId("section"), project.id, "В работе", Instant.EPOCH, Instant.EPOCH)
    val sections = MutableStateFlow(listOf(section))
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    var failSectionWrites = false
    private var nextId = 0

    override fun observeActiveProjects(): Flow<List<Project>> = flowOf(listOf(project))
    override fun observeArchivedProjects(): Flow<List<Project>> = flowOf(emptyList())
    override fun observeProject(id: ProjectId): Flow<Project?> = flowOf(project.takeIf { it.id == id })
    override suspend fun createProject(name: String, color: Long, icon: ProjectIcon): ProjectId = error("Not used")
    override suspend fun updateProject(project: Project) = Unit
    override suspend fun setArchived(id: ProjectId, archived: Boolean) = Unit
    override suspend fun deleteProject(id: ProjectId) = Unit
    override suspend fun moveProject(id: ProjectId, move: ProjectMove) = Unit

    override fun observeAllSections(): Flow<List<Section>> = sections
    override fun observeSections(projectId: ProjectId): Flow<List<Section>> = sections.map { values ->
        values.filter { it.projectId == projectId }
    }
    override fun observeSection(id: SectionId): Flow<Section?> = sections.map { values ->
        values.firstOrNull { it.id == id }
    }
    override suspend fun createSection(projectId: ProjectId, name: String): SectionId {
        check(!failSectionWrites)
        val created = section.copy(id = SectionId("created-${nextId++}"), projectId = projectId, name = name.trim())
        sections.value += created
        return created.id
    }
    override suspend fun renameSection(id: SectionId, name: String) {
        check(!failSectionWrites)
        sections.value = sections.value.map { if (it.id == id) it.copy(name = name.trim()) else it }
    }
    override suspend fun deleteSection(id: SectionId) {
        check(!failSectionWrites)
        tasks.value = tasks.value.map { if (it.sectionId == id) it.copy(sectionId = null) else it }
        sections.value = sections.value.filterNot { it.id == id }
    }

    override fun observeInbox(): Flow<List<Task>> = tasks.map { values -> values.filter { it.projectId == null } }
    override fun observeToday(date: LocalDate): Flow<List<Task>> = tasks.map { values -> values.filter { it.dueDate == date } }
    override fun observeTasksInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> =
        tasks.map { values -> values.filter { task -> task.dueDate?.let { it in startDate..endDate } == true } }
    override fun observeProjectTasks(projectId: ProjectId): Flow<List<Task>> = tasks.map { values ->
        values.filter { it.projectId == projectId && !it.isCompleted && it.deletedAt == null }
    }
    override fun observeTask(id: TaskId): Flow<Task?> = tasks.map { values -> values.firstOrNull { it.id == id } }
    override suspend fun createTask(title: String, description: String, dueDate: LocalDate?): TaskId =
        createTask(title, description, dueDate, null, null)
    override suspend fun createTask(
        title: String, description: String, dueDate: LocalDate?, projectId: ProjectId?, sectionId: SectionId?,
    ): TaskId {
        val task = Task(TaskId("task-${nextId++}"), title, description, dueDate,
            projectId = projectId, sectionId = sectionId, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
        tasks.value += task
        return task.id
    }
    override suspend fun updateTask(task: Task) {
        tasks.value = tasks.value.map { if (it.id == task.id) task else it }
    }
    override suspend fun setCompleted(id: TaskId, completed: Boolean) {
        updateTask(requireNotNull(tasks.value.firstOrNull { it.id == id }).copy(isCompleted = completed))
    }
    override suspend fun setDeleted(id: TaskId, deleted: Boolean) {
        updateTask(requireNotNull(tasks.value.firstOrNull { it.id == id }).copy(deletedAt = Instant.EPOCH.takeIf { deleted }))
    }
}
