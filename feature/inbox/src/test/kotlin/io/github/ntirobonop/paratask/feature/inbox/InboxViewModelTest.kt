package io.github.ntirobonop.paratask.feature.inbox

import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `tasks from repository are exposed after loading`() = runTest {
        val task = task(id = "one", title = "Купить продукты")
        val repository = FakeTaskRepository(initialTasks = listOf(task))

        val viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(task), viewModel.uiState.value.tasks)
    }

    @Test
    fun `create task ignores blank title and forwards a valid draft`() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = InboxViewModel(repository)

        viewModel.createTask(title = "   ", description = "ignored")
        viewModel.createTask(title = "Купить продукты", description = "Молоко")
        advanceUntilIdle()

        assertEquals(
            listOf("Купить продукты" to "Молоко"),
            repository.createdDrafts,
        )
        assertEquals("Купить продукты", viewModel.uiState.value.tasks.single().title)
    }

    @Test
    fun `complete emits undo event and undo restores completion state`() = runTest {
        val taskId = TaskId("one")
        val repository = FakeTaskRepository(initialTasks = listOf(task(id = taskId.value)))
        val viewModel = InboxViewModel(repository)
        val event = async { viewModel.events.first() }

        viewModel.completeTask(taskId)
        advanceUntilIdle()

        assertEquals(listOf(taskId to true), repository.completionChanges)
        assertEquals(InboxUiEvent.TaskCompleted(taskId), event.await())
        assertTrue(viewModel.uiState.value.tasks.isEmpty())

        viewModel.undoCompletion(taskId)
        advanceUntilIdle()

        assertEquals(
            listOf(taskId to true, taskId to false),
            repository.completionChanges,
        )
        assertEquals(listOf(taskId), viewModel.uiState.value.tasks.map(Task::id))
    }
}

private class FakeTaskRepository(
    initialTasks: List<Task> = emptyList(),
) : TaskRepository {
    private val allTasks = initialTasks.associateBy(Task::id).toMutableMap()
    private val inbox = MutableStateFlow(activeTasks())
    val createdDrafts = mutableListOf<Pair<String, String>>()
    val completionChanges = mutableListOf<Pair<TaskId, Boolean>>()

    override fun observeInbox(): Flow<List<Task>> = inbox

    override fun observeTask(id: TaskId): Flow<Task?> =
        inbox.map { tasks -> tasks.firstOrNull { task -> task.id == id } }

    override suspend fun createTask(
        title: String,
        description: String,
    ): TaskId {
        createdDrafts += title to description
        val id = TaskId("created-${createdDrafts.size}")
        allTasks[id] = task(id = id.value, title = title, description = description)
        publish()
        return id
    }

    override suspend fun updateTask(task: Task) {
        allTasks[task.id] = task
        publish()
    }

    override suspend fun setCompleted(
        id: TaskId,
        completed: Boolean,
    ) {
        completionChanges += id to completed
        allTasks[id] = checkNotNull(allTasks[id]).copy(isCompleted = completed)
        publish()
    }

    override suspend fun setDeleted(
        id: TaskId,
        deleted: Boolean,
    ) {
        allTasks[id] = checkNotNull(allTasks[id]).copy(
            deletedAt = NOW.takeIf { deleted },
        )
        publish()
    }

    private fun publish() {
        inbox.value = activeTasks()
    }

    private fun activeTasks(): List<Task> =
        allTasks.values.filter { task ->
            task.projectId == null && task.deletedAt == null && !task.isCompleted
        }
}

private val NOW: Instant = Instant.parse("2026-09-15T12:00:00Z")

private fun task(
    id: String,
    title: String = "Задача",
    description: String = "",
): Task = Task(
    id = TaskId(id),
    title = title,
    description = description,
    createdAt = NOW,
    updatedAt = NOW,
)
