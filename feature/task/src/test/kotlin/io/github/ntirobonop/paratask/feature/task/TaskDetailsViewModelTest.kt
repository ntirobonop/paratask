package io.github.ntirobonop.paratask.feature.task

import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.SectionId
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskDetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads persisted task`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTaskRepository(task())
        val viewModel = TaskDetailsViewModel(repository, TASK_ID)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Купить продукты", viewModel.uiState.value.title)
        assertEquals("Молоко", viewModel.uiState.value.description)
    }

    @Test
    fun `valid edits are autosaved after debounce`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTaskRepository(task())
        val viewModel = TaskDetailsViewModel(repository, TASK_ID, autosaveDelayMillis = 500)
        advanceUntilIdle()

        viewModel.updateTitle("Купить хлеб")
        advanceTimeBy(499)
        runCurrent()
        assertTrue(repository.updatedTasks.isEmpty())

        advanceTimeBy(1)
        runCurrent()

        assertEquals("Купить хлеб", repository.updatedTasks.single().title)
        assertFalse(viewModel.uiState.value.hasPendingChanges)
    }

    @Test
    fun `due date is autosaved with other task details`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTaskRepository(task())
            val viewModel = TaskDetailsViewModel(repository, TASK_ID, autosaveDelayMillis = 500)
            advanceUntilIdle()

            val dueDate = LocalDate.parse("2026-09-16")
            viewModel.updateDueDate(dueDate)
            advanceTimeBy(500)
            runCurrent()

            assertEquals(dueDate, repository.updatedTasks.single().dueDate)
            assertEquals(dueDate, repository.task(TASK_ID)?.dueDate)
            assertFalse(viewModel.uiState.value.hasPendingChanges)
        }

    @Test
    fun `due date can be cleared by autosave`() = runTest(mainDispatcherRule.testDispatcher) {
        val dueDate = LocalDate.parse("2026-09-16")
        val repository = FakeTaskRepository(task().copy(dueDate = dueDate))
        val viewModel = TaskDetailsViewModel(repository, TASK_ID, autosaveDelayMillis = 500)
        advanceUntilIdle()

        viewModel.updateDueDate(null)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(null, repository.updatedTasks.single().dueDate)
        assertEquals(null, repository.task(TASK_ID)?.dueDate)
    }

    @Test
    fun `section is autosaved with task details`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTaskRepository(task().copy(projectId = PROJECT_ID))
        val viewModel = TaskDetailsViewModel(repository, TASK_ID, autosaveDelayMillis = 500)
        advanceUntilIdle()

        viewModel.updateSection(SECTION_ID)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(SECTION_ID, repository.updatedTasks.single().sectionId)
        assertEquals(SECTION_ID, repository.task(TASK_ID)?.sectionId)
    }

    @Test
    fun `changing project clears incompatible section`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTaskRepository(
                task().copy(projectId = PROJECT_ID, sectionId = SECTION_ID),
            )
            val viewModel = TaskDetailsViewModel(repository, TASK_ID, autosaveDelayMillis = 500)
            advanceUntilIdle()

            val otherProjectId = ProjectId("project-2")
            viewModel.updateProject(otherProjectId)
            advanceTimeBy(500)
            runCurrent()

            assertEquals(otherProjectId, repository.updatedTasks.single().projectId)
            assertEquals(null, repository.updatedTasks.single().sectionId)
        }

    @Test
    fun `reselecting current project preserves section without autosave`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTaskRepository(task().copy(projectId = PROJECT_ID, sectionId = SECTION_ID))
            val viewModel = TaskDetailsViewModel(repository, TASK_ID)
            advanceUntilIdle()

            viewModel.updateProject(PROJECT_ID)
            advanceUntilIdle()

            assertEquals(SECTION_ID, viewModel.uiState.value.sectionId)
            assertFalse(viewModel.uiState.value.hasPendingChanges)
            assertTrue(repository.updatedTasks.isEmpty())
        }

    @Test
    fun `blank title does not replace persisted task`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTaskRepository(task())
        val viewModel = TaskDetailsViewModel(repository, TASK_ID, autosaveDelayMillis = 500)
        advanceUntilIdle()

        viewModel.updateTitle("   ")
        advanceTimeBy(1_000)
        runCurrent()

        assertTrue(repository.updatedTasks.isEmpty())
        assertEquals("Купить продукты", repository.task(TASK_ID)?.title)

        viewModel.navigateBack()
        advanceUntilIdle()

        assertEquals(TaskDetailsUiEvent.Close, viewModel.events.first())
        assertEquals("Купить продукты", viewModel.uiState.value.title)
        assertFalse(viewModel.uiState.value.hasPendingChanges)
    }

    @Test
    fun `back flushes pending valid edit before closing`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTaskRepository(task())
        val viewModel = TaskDetailsViewModel(repository, TASK_ID, autosaveDelayMillis = 10_000)
        advanceUntilIdle()

        viewModel.updateDescription("Хлеб")
        viewModel.navigateBack()
        advanceUntilIdle()

        assertEquals("Хлеб", repository.updatedTasks.single().description)
        assertEquals(TaskDetailsUiEvent.Close, viewModel.events.first())
    }

    @Test
    fun `completion can be set and restored`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTaskRepository(task())
        val viewModel = TaskDetailsViewModel(repository, TASK_ID)
        advanceUntilIdle()

        viewModel.setCompleted(true)
        advanceUntilIdle()
        viewModel.setCompleted(false)
        advanceUntilIdle()

        assertEquals(listOf(true, false), repository.completionChanges)
        assertFalse(viewModel.uiState.value.isCompleted)
    }

    @Test
    fun `delete emits deleted event`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeTaskRepository(task())
        val viewModel = TaskDetailsViewModel(repository, TASK_ID)
        advanceUntilIdle()

        viewModel.deleteTask()
        advanceUntilIdle()

        assertEquals(listOf(true), repository.deletionChanges)
        assertEquals(TaskDetailsUiEvent.Deleted(TASK_ID), viewModel.events.first())
    }

    @Test
    fun `missing task exposes recoverable state`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = TaskDetailsViewModel(FakeTaskRepository(), TASK_ID)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.taskMissing)
    }
}

private class FakeTaskRepository(initialTask: Task? = null) : TaskRepository {
    private val tasks = MutableStateFlow(
        initialTask?.let { mapOf(it.id to it) } ?: emptyMap(),
    )

    val updatedTasks = mutableListOf<Task>()
    val completionChanges = mutableListOf<Boolean>()
    val deletionChanges = mutableListOf<Boolean>()

    override fun observeInbox(): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { it.deletedAt == null && !it.isCompleted }
    }

    override fun observeToday(date: LocalDate): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { it.dueDate == date && it.deletedAt == null && !it.isCompleted }
    }

    override fun observeTasksInDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { task ->
            task.dueDate?.let { it in startDate..endDate } == true &&
                task.deletedAt == null &&
                !task.isCompleted
        }
    }

    override fun observeTask(id: TaskId): Flow<Task?> = tasks.map { it[id] }

    override suspend fun createTask(
        title: String,
        description: String,
        dueDate: LocalDate?,
    ): TaskId =
        error("Not used")

    override suspend fun updateTask(task: Task) {
        updatedTasks += task
        tasks.value = tasks.value + (task.id to task)
    }

    override suspend fun setCompleted(id: TaskId, completed: Boolean) {
        completionChanges += completed
        tasks.value = tasks.value + (id to checkNotNull(tasks.value[id]).copy(isCompleted = completed))
    }

    override suspend fun setDeleted(id: TaskId, deleted: Boolean) {
        deletionChanges += deleted
        tasks.value = tasks.value + (
            id to checkNotNull(tasks.value[id]).copy(deletedAt = NOW.takeIf { deleted })
        )
    }

    fun task(id: TaskId): Task? = tasks.value[id]
}

private val TASK_ID = TaskId("task-1")
private val PROJECT_ID = ProjectId("project-1")
private val SECTION_ID = SectionId("section-1")
private val NOW: Instant = Instant.parse("2026-09-15T12:00:00Z")

private fun task(): Task = Task(
    id = TASK_ID,
    title = "Купить продукты",
    description = "Молоко",
    createdAt = NOW,
    updatedAt = NOW,
)
