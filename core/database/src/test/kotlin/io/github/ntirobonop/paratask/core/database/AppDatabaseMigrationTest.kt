package io.github.ntirobonop.paratask.core.database

import android.content.Context
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
class AppDatabaseMigrationTest {
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
    fun migration1To2PreservesTasksAndAddsProjectForeignKey() = runTest {
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL(CREATE_V1_TASKS)
            database.execSQL(
                """
                INSERT INTO tasks (
                    id, title, description, due_date, due_time, project_id, section_id,
                    parent_task_id, is_completed, created_at, updated_at, completed_at,
                    deleted_at, sort_order
                ) VALUES (
                    'task', 'Сохранённая задача', '', NULL, NULL, 'legacy-project', NULL,
                    NULL, 0, 10, 10, NULL, NULL, 10
                )
                """.trimIndent(),
            )
            database.version = 1
        }

        val migrated = createAppDatabase(context)
        val task = migrated.taskDao().getTask("task")

        assertEquals("Сохранённая задача", task?.title)
        assertNull(task?.projectId)
        assertEquals(emptyList<ProjectEntity>(), migrated.projectDao().observeActiveProjects().first())

        val foreignKeys = migrated.openHelper.readableDatabase
            .query("PRAGMA foreign_key_list(tasks)")
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.getString(cursor.getColumnIndexOrThrow("table")))
                    }
                }
            }
        assertEquals(listOf("projects"), foreignKeys)
        migrated.close()
    }

    private companion object {
        const val DATABASE_NAME = "paratask.db"
        val CREATE_V1_TASKS = """
            CREATE TABLE IF NOT EXISTS tasks (
                id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                due_date TEXT,
                due_time TEXT,
                project_id TEXT,
                section_id TEXT,
                parent_task_id TEXT,
                is_completed INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                completed_at INTEGER,
                deleted_at INTEGER,
                sort_order INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
        """.trimIndent()
    }
}
