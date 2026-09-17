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
class SectionDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var projectDao: ProjectDao
    private lateinit var sectionDao: SectionDao
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(SECTION_INTEGRITY_CALLBACK)
            .allowMainThreadQueries()
            .build()
        projectDao = database.projectDao()
        sectionDao = database.sectionDao()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sectionsAreIsolatedByProjectAndOrdered() = runTest {
        projectDao.insertProject(project("one"))
        projectDao.insertProject(project("two"))
        sectionDao.insertSection(section("later", "one", 20))
        sectionDao.insertSection(section("first", "one", 10))
        sectionDao.insertSection(section("other", "two", 0))

        assertEquals(
            listOf("first", "later"),
            sectionDao.observeSections("one").first().map(SectionEntity::id),
        )
    }

    @Test
    fun deletingSectionClearsAssignmentsWithoutDeletingTasks() = runTest {
        projectDao.insertProject(project("project"))
        sectionDao.insertSection(section("section", "project", 0))
        taskDao.insertTask(task("task", "project", "section"))

        sectionDao.deleteSectionAndClearTasks("section", deletedAt = 100)

        assertNull(taskDao.getTask("task")?.sectionId)
        assertEquals("project", taskDao.getTask("task")?.projectId)
        assertEquals("task", taskDao.getTask("task")?.id)
        assertEquals(emptyList<SectionEntity>(), sectionDao.observeSections("project").first())
    }

    @Test
    fun freshDatabaseEnforcesSectionIntegrityWithoutRepository() = runTest {
        assertSectionIntegrity(database)
    }

    @Test
    fun sectionDeletionAlsoClearsCompletedAndDeletedTasks() = runTest {
        projectDao.insertProject(project("project"))
        sectionDao.insertSection(section("section", "project", 0))
        taskDao.insertTask(task("completed", "project", "section").copy(
            isCompleted = true,
            completedAt = 20,
        ))
        taskDao.insertTask(task("deleted", "project", "section").copy(deletedAt = 30))

        sectionDao.deleteSectionAndClearTasks("section", 100)

        assertNull(taskDao.getTask("completed")?.sectionId)
        assertNull(taskDao.getTask("deleted")?.sectionId)
        assertEquals(20L, taskDao.getTask("completed")?.completedAt)
        assertEquals(30L, taskDao.getTask("deleted")?.deletedAt)
        assertEquals("project", taskDao.getTask("completed")?.projectId)
        assertEquals("project", taskDao.getTask("deleted")?.projectId)
    }
}

private fun project(id: String) = ProjectEntity(
    id = id,
    name = id,
    color = 0,
    icon = "LIST",
    isArchived = false,
    createdAt = 10,
    updatedAt = 10,
    deletedAt = null,
    sortOrder = 0,
)

private fun section(id: String, projectId: String, sortOrder: Long) = SectionEntity(
    id = id,
    projectId = projectId,
    name = id,
    createdAt = 10,
    updatedAt = 10,
    deletedAt = null,
    sortOrder = sortOrder,
)

private fun task(id: String, projectId: String, sectionId: String) = TaskEntity(
    id = id,
    title = id,
    description = "",
    dueDate = null,
    dueTime = null,
    projectId = projectId,
    sectionId = sectionId,
    parentTaskId = null,
    isCompleted = false,
    createdAt = 10,
    updatedAt = 10,
    completedAt = null,
    deletedAt = null,
    sortOrder = 10,
)
