package io.github.ntirobonop.paratask.core.database

import android.database.sqlite.SQLiteConstraintException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Exercises the actual SQLite constraints, bypassing repository validation. */
internal suspend fun assertSectionIntegrity(database: AppDatabase) {
    val projects = database.projectDao()
    val sections = database.sectionDao()
    val tasks = database.taskDao()
    val first = ProjectEntity("integrity-a", "A", 0, "LIST", false, 10, 10, null, 0)
    val second = first.copy(id = "integrity-b", name = "B")
    projects.insertProject(first)
    projects.insertProject(second)
    sections.insertSection(SectionEntity("integrity-section", first.id, "Section", 10, 10, null, 0))
    sections.insertSection(SectionEntity("integrity-deleted", first.id, "Deleted", 10, 10, 20, 1))
    val valid = TaskEntity(
        id = "integrity-task",
        title = "Valid assignment",
        description = "",
        dueDate = null,
        dueTime = null,
        projectId = first.id,
        sectionId = "integrity-section",
        parentTaskId = null,
        isCompleted = false,
        createdAt = 10,
        updatedAt = 10,
        completedAt = null,
        deletedAt = null,
        sortOrder = 0,
    )
    tasks.insertTask(valid)

    val invalidAssignments = listOf(
        valid.copy(projectId = second.id),
        valid.copy(projectId = null),
        valid.copy(sectionId = "missing-section"),
        valid.copy(sectionId = "integrity-deleted"),
    )
    invalidAssignments.forEachIndexed { index, invalid ->
        val insertError = runCatching { tasks.insertTask(invalid.copy(id = "invalid-$index")) }
            .exceptionOrNull()
        assertTrue("Invalid insert $index must fail: $insertError", insertError is SQLiteConstraintException)
        val updateError = runCatching { tasks.updateTask(invalid) }.exceptionOrNull()
        assertTrue("Invalid update $index must fail: $updateError", updateError is SQLiteConstraintException)
        assertEquals(valid, tasks.getTask(valid.id))
    }

    val unsectioned = valid.copy(sectionId = null)
    tasks.updateTask(unsectioned)
    assertEquals(unsectioned, tasks.getTask(valid.id))
}
