package io.github.ntirobonop.paratask.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TaskDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `inbox contains only active root tasks without a project`() = runTest {
        dao.insertTask(task(id = "active"))
        dao.insertTask(task(id = "completed", isCompleted = true, completedAt = 20))
        dao.insertTask(task(id = "deleted", deletedAt = 20))
        dao.insertTask(task(id = "project", projectId = "project-1"))
        dao.insertTask(task(id = "subtask", parentTaskId = "parent"))

        val inbox = dao.observeInbox().first()

        assertEquals(listOf("active", "subtask"), inbox.map(TaskEntity::id))
    }

    @Test
    fun `completion and undo update task atomically`() = runTest {
        dao.insertTask(task(id = "task"))

        dao.setCompleted(
            id = "task",
            completed = true,
            completedAt = 200,
            updatedAt = 200,
        )

        val completed = dao.getTask("task")
        assertEquals(true, completed?.isCompleted)
        assertEquals(200L, completed?.completedAt)
        assertEquals(200L, completed?.updatedAt)
        assertEquals(emptyList<TaskEntity>(), dao.observeInbox().first())

        dao.setCompleted(
            id = "task",
            completed = false,
            completedAt = null,
            updatedAt = 300,
        )

        val restored = dao.getTask("task")
        assertEquals(false, restored?.isCompleted)
        assertNull(restored?.completedAt)
        assertEquals(300L, restored?.updatedAt)
        assertEquals(listOf("task"), dao.observeInbox().first().map(TaskEntity::id))
    }
}

private fun task(
    id: String,
    projectId: String? = null,
    parentTaskId: String? = null,
    isCompleted: Boolean = false,
    completedAt: Long? = null,
    deletedAt: Long? = null,
): TaskEntity = TaskEntity(
    id = id,
    title = id,
    description = "",
    dueDate = null,
    dueTime = null,
    projectId = projectId,
    sectionId = null,
    parentTaskId = parentTaskId,
    isCompleted = isCompleted,
    createdAt = 10,
    updatedAt = 10,
    completedAt = completedAt,
    deletedAt = deletedAt,
    sortOrder = 10,
)
