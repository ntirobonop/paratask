package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.TaskDao
import io.github.ntirobonop.paratask.core.database.TaskEntity
import io.github.ntirobonop.paratask.core.database.ProjectDao
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
    ): TaskId {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Task title must not be blank" }
        validateNewAssignment(projectId)

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
                sectionId = null,
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
        if (current.projectId != task.projectId?.value) {
            validateNewAssignment(task.projectId)
        }
        taskDao.updateTask(
            task.copy(
                title = task.title.trim(),
                description = task.description.trim(),
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

    private suspend fun validateNewAssignment(projectId: ProjectId?) {
        if (projectId == null) return
        val project = projectDao?.getProject(projectId.value)
        require(project != null && !project.isArchived) {
            "Tasks can only be assigned to an active project"
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
