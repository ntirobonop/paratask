package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.TaskDao
import io.github.ntirobonop.paratask.core.database.TaskEntity
import io.github.ntirobonop.paratask.core.database.ProjectDao
import io.github.ntirobonop.paratask.core.database.SectionDao
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.SectionId
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultTaskRepository(
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao? = null,
    private val sectionDao: SectionDao? = null,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> TaskId = TaskId::random,
) : TaskRepository {
    override fun observeInbox(): Flow<List<Task>> =
        taskDao.observeInbox().map { tasks -> tasks.map(TaskEntity::toDomain) }

    override fun observeToday(date: LocalDate): Flow<List<Task>> =
        taskDao.observeToday(date.toString()).map { tasks -> tasks.map(TaskEntity::toDomain) }

    override fun observeTasksInDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>> {
        require(!endDate.isBefore(startDate)) { "End date must not be before start date" }
        return taskDao.observeTasksInDateRange(
            startDate = startDate.toString(),
            endDate = endDate.toString(),
        ).map { tasks -> tasks.map(TaskEntity::toDomain) }
    }

    override fun observeProjectTasks(projectId: ProjectId): Flow<List<Task>> =
        taskDao.observeProjectTasks(projectId.value)
            .map { tasks -> tasks.map(TaskEntity::toDomain) }

    override fun observeTask(id: TaskId): Flow<Task?> =
        taskDao.observeTask(id.value).map { task -> task?.toDomain() }

    override suspend fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
    ): TaskId = createTask(
        title = title,
        description = description,
        dueDate = dueDate,
        projectId = null,
    )

    override suspend fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
        projectId: ProjectId?,
    ): TaskId = createTask(
        title = title,
        description = description,
        dueDate = dueDate,
        projectId = projectId,
        sectionId = null,
    )

    override suspend fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
        projectId: ProjectId?,
        sectionId: SectionId?,
    ): TaskId {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Task title must not be blank" }
        validateAssignment(projectId = projectId, sectionId = sectionId)

        val id = idFactory()
        val now = clock.instant()
        taskDao.insertTask(
            TaskEntity(
                id = id.value,
                title = normalizedTitle,
                description = description.trim(),
                dueDate = dueDate?.toString(),
                dueTime = null,
                projectId = projectId?.value,
                sectionId = sectionId?.value,
                parentTaskId = null,
                isCompleted = false,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
                completedAt = null,
                deletedAt = null,
                sortOrder = now.toEpochMilli(),
            ),
        )
        return id
    }

    override suspend fun updateTask(task: Task) {
        require(task.title.isNotBlank()) { "Task title must not be blank" }
        val current = taskDao.getTask(task.id.value) ?: error("Task does not exist")
        val projectChanged = current.projectId != task.projectId?.value
        var normalizedSectionId = task.sectionId
        if (projectChanged && normalizedSectionId != null) {
            val section = sectionDao?.getSection(normalizedSectionId.value)
            if (section?.projectId != task.projectId?.value) normalizedSectionId = null
        }
        validateAssignment(
            projectId = task.projectId,
            sectionId = normalizedSectionId,
            requireActiveProject = projectChanged,
        )
        taskDao.updateTask(
            task.copy(
                title = task.title.trim(),
                description = task.description.trim(),
                sectionId = normalizedSectionId,
                updatedAt = clock.instant(),
            ).toEntity(),
        )
    }

    override suspend fun setCompleted(
        id: TaskId,
        completed: Boolean,
    ) {
        val now = clock.instant().toEpochMilli()
        taskDao.setCompleted(
            id = id.value,
            completed = completed,
            completedAt = now.takeIf { completed },
            updatedAt = now,
        )
    }

    override suspend fun setDeleted(
        id: TaskId,
        deleted: Boolean,
    ) {
        val now = clock.instant().toEpochMilli()
        taskDao.setDeleted(
            id = id.value,
            deletedAt = now.takeIf { deleted },
            updatedAt = now,
        )
    }

    private suspend fun validateAssignment(
        projectId: ProjectId?,
        sectionId: SectionId?,
        requireActiveProject: Boolean = true,
    ) {
        require(projectId != null || sectionId == null) { "Inbox tasks cannot have a section" }
        if (projectId == null) return
        val project = projectDao?.getProject(projectId.value)
        require(project != null && (!requireActiveProject || !project.isArchived)) {
            "Tasks can only be assigned to an active project"
        }
        if (sectionId != null) {
            val section = sectionDao?.getSection(sectionId.value)
            require(section != null && section.projectId == projectId.value) {
                "Task section must belong to its project"
            }
        }
    }
}

internal fun TaskEntity.toDomain(): Task = Task(
    id = TaskId(id),
    title = title,
    description = description,
    dueDate = dueDate?.let(LocalDate::parse),
    dueTime = dueTime?.let(LocalTime::parse),
    projectId = projectId?.let(::ProjectId),
    sectionId = sectionId?.let(::SectionId),
    parentTaskId = parentTaskId?.let(::TaskId),
    isCompleted = isCompleted,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    completedAt = completedAt?.let(Instant::ofEpochMilli),
    deletedAt = deletedAt?.let(Instant::ofEpochMilli),
    sortOrder = sortOrder,
)

internal fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id.value,
    title = title,
    description = description,
    dueDate = dueDate?.toString(),
    dueTime = dueTime?.toString(),
    projectId = projectId?.value,
    sectionId = sectionId?.value,
    parentTaskId = parentTaskId?.value,
    isCompleted = isCompleted,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    completedAt = completedAt?.toEpochMilli(),
    deletedAt = deletedAt?.toEpochMilli(),
    sortOrder = sortOrder,
)
