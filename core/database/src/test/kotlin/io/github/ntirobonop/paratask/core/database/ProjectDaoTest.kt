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
class ProjectDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SECTION_INTEGRITY_CALLBACK)
            .allowMainThreadQueries()
            .build()
        projectDao = database.projectDao()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun activeAndArchivedProjectsAreSeparatedAndManuallyOrdered() = runTest {
        projectDao.insertProject(project(id = "second", sortOrder = 20))
        projectDao.insertProject(project(id = "first", sortOrder = 10))
        projectDao.insertProject(project(id = "archived", sortOrder = 0, archived = true))

        assertEquals(
            listOf("first", "second"),
            projectDao.observeActiveProjects().first().map(ProjectEntity::id),
        )
        assertEquals(
            listOf("archived"),
            projectDao.observeArchivedProjects().first().map(ProjectEntity::id),
        )
    }

    @Test
    fun deletingProjectReturnsTasksToInboxAtomically() = runTest {
        projectDao.insertProject(project(id = "project", sortOrder = 0))
        taskDao.insertTask(task(id = "task", projectId = "project"))

        assertEquals(emptyList<TaskEntity>(), taskDao.observeInbox().first())
        projectDao.deleteProjectAndReturnTasksToInbox(
            projectId = "project",
            deletedAt = 100,
        )

        assertEquals(listOf("task"), taskDao.observeInbox().first().map(TaskEntity::id))
        assertNull(taskDao.getTask("task")?.projectId)
        assertNull(taskDao.getTask("task")?.sectionId)
        assertNull(projectDao.observeProject("project").first())
    }

    @Test
    fun deletingProjectRemovesItsSectionsAndPreservesEveryTaskState() = runTest {
        val sections = database.sectionDao()
        projectDao.insertProject(project("project", 0))
        projectDao.insertProject(project("other", 1))
        sections.insertSection(SectionEntity("section", "project", "Section", 10, 10, null, 0))
        sections.insertSection(SectionEntity("other-section", "other", "Other", 10, 10, null, 0))
        val active = task("active", "project").copy(sectionId = "section")
        val completed = active.copy(id = "completed", isCompleted = true, completedAt = 20)
        val deleted = active.copy(id = "deleted", deletedAt = 30)
        listOf(active, completed, deleted).forEach { taskDao.insertTask(it) }

        projectDao.deleteProjectAndReturnTasksToInbox("project", 100)

        listOf(active, completed, deleted).forEach { original ->
            assertEquals(
                original.copy(projectId = null, sectionId = null, updatedAt = 100),
                taskDao.getTask(original.id),
            )
        }
        assertEquals(listOf("active"), taskDao.observeInbox().first().map(TaskEntity::id))
        assertEquals(listOf("other-section"), sections.observeAllSections().first().map(SectionEntity::id))
        assertNull(sections.getSection("section"))
        database.openHelper.readableDatabase.query(
            "SELECT deleted_at FROM sections WHERE id = 'section'",
        ).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(100L, cursor.getLong(0))
        }
    }
}

private fun project(
    id: String,
    sortOrder: Long,
    archived: Boolean = false,
): ProjectEntity = ProjectEntity(
    id = id,
    name = id,
    color = 0xFF6750A4,
    icon = "LIST",
    isArchived = archived,
    createdAt = 10,
    updatedAt = 10,
    deletedAt = null,
    sortOrder = sortOrder,
)

private fun task(
    id: String,
    projectId: String?,
): TaskEntity = TaskEntity(
    id = id,
    title = id,
    description = "",
    dueDate = null,
    dueTime = null,
    projectId = projectId,
    sectionId = null,
    parentTaskId = null,
    isCompleted = false,
    createdAt = 10,
    updatedAt = 10,
    completedAt = null,
    deletedAt = null,
    sortOrder = 10,
)
