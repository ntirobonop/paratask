package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeInbox(): Flow<List<Task>>

    fun observeTask(id: TaskId): Flow<Task?>

    suspend fun createTask(
        title: String,
        description: String = "",
    ): TaskId

    suspend fun updateTask(task: Task)

    suspend fun setCompleted(
        id: TaskId,
        completed: Boolean,
    )

    suspend fun deleteTask(id: TaskId)
}
