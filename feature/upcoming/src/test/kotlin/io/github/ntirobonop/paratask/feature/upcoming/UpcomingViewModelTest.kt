package io.github.ntirobonop.paratask.feature.upcoming

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
class UpcomingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `current ISO week and selected-day tasks are exposed`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeUpcomingRepository(
                listOf(
                    task(id = "today", dueDate = TODAY),
                    task(id = "tomorrow", dueDate = TODAY.plusDays(1)),
                ),
            )
            val viewModel = UpcomingViewModel(repository, today = TODAY)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(MONDAY, state.weekStart)
            assertEquals(MONDAY, state.weekDates.first())
            assertEquals(SUNDAY, state.weekDates.last())
            assertEquals(listOf(TaskId("today")), state.selectedTasks.map(Task::id))
            assertEquals(1, state.taskCountByDate[TODAY])
            assertEquals(1, state.taskCountByDate[TODAY.plusDays(1)])
            assertEquals(listOf(MONDAY to SUNDAY), repository.requestedRanges)
        }

    @Test
    fun `day selection stays in one observation and week navigation preserves weekday`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeUpcomingRepository()
            val viewModel = UpcomingViewModel(repository, today = TODAY)
            advanceUntilIdle()

            viewModel.selectDate(TODAY.plusDays(1))
            advanceUntilIdle()
            assertEquals(TODAY.plusDays(1), viewModel.uiState.value.selectedDate)
            assertEquals(1, repository.requestedRanges.size)

            viewModel.showNextWeek()
            advanceUntilIdle()
            assertEquals(TODAY.plusDays(8), viewModel.uiState.value.selectedDate)
            assertEquals(2, repository.requestedRanges.size)

            viewModel.showToday()
            advanceUntilIdle()
            assertEquals(TODAY, viewModel.uiState.value.selectedDate)
            assertEquals(MONDAY, viewModel.uiState.value.weekStart)
        }

    @Test
    fun `completion removes task and undo restores it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val taskId = TaskId("today")
            val repository = FakeUpcomingRepository(listOf(task(id = taskId.value, dueDate = TODAY)))
            val viewModel = UpcomingViewModel(repository, today = TODAY)
            advanceUntilIdle()

            viewModel.completeTask(taskId)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.selectedTasks.isEmpty())
            assertEquals(UpcomingUiEvent.TaskCompleted(taskId), viewModel.events.first())

            viewModel.undoCompletion(taskId)
            advanceUntilIdle()

            assertEquals(listOf(taskId), viewModel.uiState.value.selectedTasks.map(Task::id))
        }
}

private class FakeUpcomingRepository(initialTasks: List<Task> = emptyList()) : TaskRepository {
    private val tasks = MutableStateFlow(initialTasks.associateBy(Task::id))
    val requestedRanges = mutableListOf<Pair<LocalDate, LocalDate>>()

    override fun observeInbox(): Flow<List<Task>> = activeTasks()

    override fun observeToday(date: LocalDate): Flow<List<Task>> = activeTasks().map { tasks ->
        tasks.filter { it.dueDate == date }
    }

    override fun observeTasksInDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>> {
        requestedRanges += startDate to endDate
        return activeTasks().map { tasks ->
            tasks.filter { task -> task.dueDate?.let { it in startDate..endDate } == true }
        }
    }

    override fun observeTask(id: TaskId): Flow<Task?> = tasks.map { it[id] }

    override suspend fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
    ): TaskId {
        val id = TaskId("created")
        tasks.value = tasks.value + (
            id to task(
                id = id.value,
                title = title,
                description = description,
                dueDate = dueDate,
            )
        )
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

    private fun activeTasks(): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { task -> task.deletedAt == null && !task.isCompleted }
    }
}

private val NOW: Instant = Instant.parse("2026-09-16T09:00:00Z")
private val TODAY: LocalDate = LocalDate.parse("2026-09-16")
private val MONDAY: LocalDate = LocalDate.parse("2026-09-14")
private val SUNDAY: LocalDate = LocalDate.parse("2026-09-20")

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
