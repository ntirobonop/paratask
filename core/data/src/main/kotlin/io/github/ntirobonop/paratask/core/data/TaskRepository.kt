package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.model.ProjectId
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface TaskRepository {
    fun observeInbox(): Flow<List<Task>>

    fun observeToday(date: LocalDate): Flow<List<Task>>

    fun observeTasksInDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>>

    fun observeProjectTasks(projectId: ProjectId): Flow<List<Task>> = emptyFlow()

    fun observeTask(id: TaskId): Flow<Task?>

    suspend fun createTask(
        title: String,
        description: String = "",
        dueDate: LocalDate? = null,
    ): TaskId

    suspend fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
        projectId: ProjectId?,
    ): TaskId {
        require(projectId == null) { "This repository does not support project assignment" }
        return createTask(title = title, description = description, dueDate = dueDate)
    }

    suspend fun updateTask(task: Task)

    suspend fun setCompleted(
        id: TaskId,
        completed: Boolean,
    )

    suspend fun setDeleted(
        id: TaskId,
        deleted: Boolean,
    )
}
