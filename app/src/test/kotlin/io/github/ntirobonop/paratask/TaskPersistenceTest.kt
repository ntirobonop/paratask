package io.github.ntirobonop.paratask

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.ntirobonop.paratask.core.data.DefaultTaskRepository
import io.github.ntirobonop.paratask.core.database.createAppDatabase
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class TaskPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun `task remains after database is closed and reopened`() = runTest {
        val firstDatabase = createAppDatabase(context)
        val firstRepository = DefaultTaskRepository(
            taskDao = firstDatabase.taskDao(),
            clock = Clock.fixed(NOW, ZoneOffset.UTC),
            idFactory = { TASK_ID },
        )
        firstRepository.createTask("Купить продукты", "Молоко")
        firstDatabase.close()

        val reopenedDatabase = createAppDatabase(context)
        val reopenedRepository = DefaultTaskRepository(reopenedDatabase.taskDao())
        val restoredTask = reopenedRepository.observeInbox().first().single()

        assertEquals(TASK_ID, restoredTask.id)
        assertEquals("Купить продукты", restoredTask.title)
        assertEquals("Молоко", restoredTask.description)
        reopenedDatabase.close()
    }

    private companion object {
        const val DATABASE_NAME = "paratask.db"
        val TASK_ID = TaskId("persisted-task")
        val NOW: Instant = Instant.parse("2026-09-15T12:00:00Z")
    }
}
