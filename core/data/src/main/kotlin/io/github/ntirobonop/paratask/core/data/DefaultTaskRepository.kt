package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.TaskDao
import io.github.ntirobonop.paratask.core.database.TaskEntity
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
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> TaskId = TaskId::random,
) : TaskRepository {
    override fun observeInbox(): Flow<List<Task>> =
        taskDao.observeInbox().map { tasks -> tasks.map(TaskEntity::toDomain) }

    override fun observeTask(id: TaskId): Flow<Task?> =
        taskDao.observeTask(id.value).map { task -> task?.toDomain() }

    override suspend fun createTask(
        title: String,
        description: String,
    ): TaskId {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Task title must not be blank" }

        val id = idFactory()
        val now = clock.instant()
        taskDao.insertTask(
            TaskEntity(
                id = id.value,
                title = normalizedTitle,
                description = description.trim(),
                dueDate = null,
                dueTime = null,
                projectId = null,
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

    override suspend fun deleteTask(id: TaskId) {
        val now = clock.instant().toEpochMilli()
        taskDao.softDelete(
            id = id.value,
            deletedAt = now,
            updatedAt = now,
        )
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
