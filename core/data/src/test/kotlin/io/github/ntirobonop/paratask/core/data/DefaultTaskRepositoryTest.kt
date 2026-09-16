package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.TaskDao
import io.github.ntirobonop.paratask.core.database.TaskEntity
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultTaskRepositoryTest {
    private val instant = Instant.parse("2026-09-15T12:00:00Z")
    private val taskId = TaskId("task-1")
    private val dao = FakeTaskDao()
    private val repository = DefaultTaskRepository(
        taskDao = dao,
        clock = Clock.fixed(instant, ZoneOffset.UTC),
        idFactory = { taskId },
    )

    @Test
    fun createTaskNormalizesAndPersistsInput() = runTest {
        val createdId = repository.createTask("  Купить продукты  ", "  Молоко  ")

        val task = repository.observeTask(createdId).first()
        requireNotNull(task)
        assertEquals(taskId, createdId)
        assertEquals("Купить продукты", task.title)
        assertEquals("Молоко", task.description)
        assertEquals(instant, task.createdAt)
        assertFalse(task.isCompleted)
    }

    @Test
    fun createTaskRejectsBlankTitle() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.createTask("   ") }
        }
    }

    @Test
    fun completionRemovesTaskFromInboxAndCanBeUndone() = runTest {
        repository.createTask("Task")

        repository.setCompleted(taskId, completed = true)
        assertTrue(repository.observeInbox().first().isEmpty())
        assertEquals(instant, repository.observeTask(taskId).first()?.completedAt)

        repository.setCompleted(taskId, completed = false)
        assertEquals(listOf(taskId), repository.observeInbox().first().map { it.id })
        assertNull(repository.observeTask(taskId).first()?.completedAt)
    }

    @Test
    fun deletionRemovesTaskFromInboxAndCanBeUndone() = runTest {
        repository.createTask("Task")

        repository.setDeleted(taskId, deleted = true)
        assertTrue(repository.observeInbox().first().isEmpty())
        assertEquals(instant, repository.observeTask(taskId).first()?.deletedAt)

        repository.setDeleted(taskId, deleted = false)
        assertEquals(listOf(taskId), repository.observeInbox().first().map { it.id })
        assertNull(repository.observeTask(taskId).first()?.deletedAt)
    }

    @Test
    fun createTaskPersistsDateAndExposesItInToday() = runTest {
        val date = LocalDate.parse("2026-09-15")

        repository.createTask(title = "Task", dueDate = date)

        assertEquals(date, repository.observeTask(taskId).first()?.dueDate)
        assertEquals(listOf(taskId), repository.observeToday(date).first().map { it.id })
        assertTrue(repository.observeToday(date.plusDays(1)).first().isEmpty())
    }

    @Test
    fun dateRangeMapsInclusiveBoundaries() = runTest {
        val monday = LocalDate.parse("2026-09-14")
        repository.createTask(title = "Task", dueDate = monday)

        assertEquals(
            listOf(taskId),
            repository.observeTasksInDateRange(monday, monday.plusDays(6)).first().map { it.id },
        )
    }

    @Test
    fun dateRangeRejectsReversedBoundaries() {
        val monday = LocalDate.parse("2026-09-14")

        assertThrows(IllegalArgumentException::class.java) {
            repository.observeTasksInDateRange(monday, monday.minusDays(1))
        }
    }
}

private class FakeTaskDao : TaskDao {
    private val tasks = MutableStateFlow<List<TaskEntity>>(emptyList())

    override fun observeInbox(): Flow<List<TaskEntity>> = tasks.map { values ->
        values
            .filter { it.projectId == null && it.deletedAt == null && !it.isCompleted }
            .sortedWith(compareBy(TaskEntity::sortOrder, TaskEntity::createdAt))
    }

    override fun observeToday(dueDate: String): Flow<List<TaskEntity>> = tasks.map { values ->
        values
            .filter { it.dueDate == dueDate && it.deletedAt == null && !it.isCompleted }
            .sortedWith(compareBy(TaskEntity::sortOrder, TaskEntity::createdAt))
    }

    override fun observeTasksInDateRange(
        startDate: String,
        endDate: String,
    ): Flow<List<TaskEntity>> = tasks.map { values ->
        values
            .filter { task ->
                task.dueDate?.let { it >= startDate && it <= endDate } == true &&
                    task.deletedAt == null &&
                    !task.isCompleted
            }
            .sortedWith(
                compareBy(TaskEntity::dueDate, TaskEntity::sortOrder, TaskEntity::createdAt),
            )
    }

    override fun observeTask(id: String): Flow<TaskEntity?> =
        tasks.map { values -> values.firstOrNull { it.id == id } }

    override suspend fun getTask(id: String): TaskEntity? =
        tasks.value.firstOrNull { it.id == id }

    override suspend fun insertTask(task: TaskEntity) {
        check(tasks.value.none { it.id == task.id })
        tasks.value += task
    }

    override suspend fun updateTask(task: TaskEntity): Int = replace(task.id) { task }

    override suspend fun setCompleted(
        id: String,
        completed: Boolean,
        completedAt: Long?,
        updatedAt: Long,
    ): Int = replace(id) { task ->
        task.copy(
            isCompleted = completed,
            completedAt = completedAt,
            updatedAt = updatedAt,
        )
    }

    override suspend fun setDeleted(
        id: String,
        deletedAt: Long?,
        updatedAt: Long,
    ): Int = replace(id) { task ->
        task.copy(deletedAt = deletedAt, updatedAt = updatedAt)
    }

    private fun replace(
        id: String,
        transform: (TaskEntity) -> TaskEntity,
    ): Int {
        var changed = 0
        tasks.value = tasks.value.map { task ->
            if (task.id == id) {
                changed += 1
                transform(task)
            } else {
                task
            }
        }
        return changed
    }
}
