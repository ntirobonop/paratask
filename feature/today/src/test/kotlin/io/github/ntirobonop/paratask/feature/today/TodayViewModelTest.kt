package io.github.ntirobonop.paratask.feature.today

import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TodayViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `only tasks for today are exposed`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTodayRepository(
            listOf(task(id = "today", dueDate = TODAY), task(id = "tomorrow", dueDate = TOMORROW)),
        )

        val viewModel = TodayViewModel(repository, today = TODAY)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf(TaskId("today")), viewModel.uiState.value.tasks.map(Task::id))
    }

    @Test
    fun `contextual draft date is forwarded to repository`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTodayRepository()
            val viewModel = TodayViewModel(repository, today = TODAY)

            viewModel.createTask(
                title = "Купить продукты",
                description = "Молоко",
                dueDate = TOMORROW,
            )
            advanceUntilIdle()

            assertEquals(TOMORROW, repository.createdTasks.single().dueDate)
        }

    @Test
    fun `task leaves Today when its date changes`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskId = TaskId("today")
            val repository = FakeTodayRepository(listOf(task(id = taskId.value, dueDate = TODAY)))
            val viewModel = TodayViewModel(repository, today = TODAY)
            advanceUntilIdle()

            repository.updateTask(checkNotNull(repository.task(taskId)).copy(dueDate = TOMORROW))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.tasks.isEmpty())
        }

    @Test
    fun `completion removes task and undo restores it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskId = TaskId("today")
            val repository = FakeTodayRepository(listOf(task(id = taskId.value, dueDate = TODAY)))
            val viewModel = TodayViewModel(repository, today = TODAY)
            advanceUntilIdle()

            viewModel.completeTask(taskId)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.tasks.isEmpty())
            assertEquals(TodayUiEvent.TaskCompleted(taskId), viewModel.events.first())

            viewModel.undoCompletion(taskId)
            advanceUntilIdle()

            assertEquals(listOf(taskId), viewModel.uiState.value.tasks.map(Task::id))
        }
}

private class FakeTodayRepository(initialTasks: List<Task> = emptyList()) : TaskRepository {
    private val tasks = MutableStateFlow(initialTasks.associateBy(Task::id))
    val createdTasks = mutableListOf<Task>()

    override fun observeInbox(): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { it.projectId == null && it.deletedAt == null && !it.isCompleted }
    }

    override fun observeToday(date: LocalDate): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { it.dueDate == date && it.deletedAt == null && !it.isCompleted }
    }

    override fun observeTasksInDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { task ->
            task.dueDate != null &&
                task.dueDate in startDate..endDate &&
                task.deletedAt == null &&
                !task.isCompleted
        }
    }

    override fun observeTask(id: TaskId): Flow<Task?> = tasks.map { it[id] }

    override suspend fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
    ): TaskId {
        val id = TaskId("created-${createdTasks.size + 1}")
        val task = task(id = id.value, title = title, description = description, dueDate = dueDate)
        createdTasks += task
        tasks.value = tasks.value + (id to task)
        return id
    }

    override suspend fun updateTask(task: Task) {
        tasks.value = tasks.value + (task.id to task)
    }

    override suspend fun setCompleted(id: TaskId, completed: Boolean) {
        tasks.value = tasks.value + (id to checkNotNull(tasks.value[id]).copy(isCompleted = completed))
    }

    override suspend fun setDeleted(id: TaskId, deleted: Boolean) {
        tasks.value = tasks.value + (
            id to checkNotNull(tasks.value[id]).copy(deletedAt = NOW.takeIf { deleted })
        )
    }

    fun task(id: TaskId): Task? = tasks.value[id]
}

private val NOW: Instant = Instant.parse("2026-09-16T09:00:00Z")
private val TODAY: LocalDate = LocalDate.parse("2026-09-16")
private val TOMORROW: LocalDate = TODAY.plusDays(1)

private fun task(
    id: String,
    title: String = "Задача",
    description: String = "",
    dueDate: LocalDate? = null,
): Task = Task(
    id = TaskId(id),
    title = title,
    description = description,
    dueDate = dueDate,
    createdAt = NOW,
    updatedAt = NOW,
)
