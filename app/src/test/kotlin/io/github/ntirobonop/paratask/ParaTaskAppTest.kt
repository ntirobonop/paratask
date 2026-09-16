package io.github.ntirobonop.paratask

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.data.ProjectMove
import io.github.ntirobonop.paratask.core.data.ProjectRepository
import io.github.ntirobonop.paratask.core.data.SectionRepository
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.Section
import io.github.ntirobonop.paratask.core.model.SectionId
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ParaTaskAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `task can be opened edited closed and reopened`() {
        val repository = FakeAppTaskRepository(task())
        composeRule.setContent {
            MaterialTheme {
                ParaTaskApp(
                    taskRepository = repository,
                    projectRepository = FakeAppProjectRepository(),
                    sectionRepository = FakeAppSectionRepository(),
                )
            }
        }

        composeRule.onNodeWithText("Купить продукты").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0]
            .performTextReplacement("Купить хлеб")
        composeRule.onNodeWithContentDescription("Назад").performClick()

        composeRule.onNodeWithText("Купить хлеб").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("Купить хлеб", repository.task(TASK_ID)?.title)
        }

        composeRule.onNodeWithText("Купить хлеб").performClick()
        composeRule.onNodeWithText("Купить хлеб").assertIsDisplayed()
    }

    @Test
    fun `deleted task can be restored from snackbar`() {
        val repository = FakeAppTaskRepository(task())
        composeRule.setContent {
            MaterialTheme {
                ParaTaskApp(
                    taskRepository = repository,
                    projectRepository = FakeAppProjectRepository(),
                    sectionRepository = FakeAppSectionRepository(),
                )
            }
        }

        composeRule.onNodeWithText("Купить продукты").performClick()
        composeRule.onNodeWithContentDescription("Действия задачи").performClick()
        composeRule.onNodeWithText("Удалить").performClick()

        composeRule.onNodeWithText("Задача удалена").assertIsDisplayed()
        composeRule.onNodeWithText("Отменить").performClick()
        composeRule.onNodeWithText("Купить продукты").assertIsDisplayed()
        composeRule.runOnIdle {
            assertNull(repository.task(TASK_ID)?.deletedAt)
        }
    }

    @Test
    fun `Today navigation shows tasks due today`() {
        val today = LocalDate.parse("2026-09-16")
        val repository = FakeAppTaskRepository(task().copy(dueDate = today))
        composeRule.setContent {
            MaterialTheme {
                ParaTaskApp(
                    taskRepository = repository,
                    projectRepository = FakeAppProjectRepository(),
                    sectionRepository = FakeAppSectionRepository(),
                    today = today,
                )
            }
        }

        composeRule.onNodeWithText("Сегодня").performClick()

        composeRule.onNodeWithText("Купить продукты").assertIsDisplayed()
    }

    @Test
    fun `Upcoming navigation shows current week and tasks due today`() {
        val today = LocalDate.parse("2026-09-16")
        val repository = FakeAppTaskRepository(task().copy(dueDate = today))
        composeRule.setContent {
            MaterialTheme {
                ParaTaskApp(
                    taskRepository = repository,
                    projectRepository = FakeAppProjectRepository(),
                    sectionRepository = FakeAppSectionRepository(),
                    today = today,
                )
            }
        }

        composeRule.onNodeWithText("Предстоящие").performClick()

        composeRule.onNodeWithContentDescription(
            "среда, 16 сентября, задач: 1, выбрано",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Купить продукты").assertIsDisplayed()

        composeRule.onNodeWithText("Купить продукты").performClick()
        composeRule.onNodeWithContentDescription("Назад").performClick()

        composeRule.onNodeWithContentDescription(
            "среда, 16 сентября, задач: 1, выбрано",
        ).assertIsDisplayed()
    }
}

private class FakeAppSectionRepository : SectionRepository {
    override fun observeAllSections(): Flow<List<Section>> = MutableStateFlow(emptyList())

    override fun observeSections(projectId: ProjectId): Flow<List<Section>> =
        MutableStateFlow(emptyList())

    override fun observeSection(id: SectionId): Flow<Section?> = MutableStateFlow(null)

    override suspend fun createSection(projectId: ProjectId, name: String): SectionId =
        error("Not used")

    override suspend fun renameSection(id: SectionId, name: String) = Unit

    override suspend fun deleteSection(id: SectionId) = Unit
}

private class FakeAppProjectRepository : ProjectRepository {
    private val projects = MutableStateFlow<List<Project>>(emptyList())

    override fun observeActiveProjects(): Flow<List<Project>> = projects

    override fun observeArchivedProjects(): Flow<List<Project>> =
        projects.map { values -> values.filter(Project::isArchived) }

    override fun observeProject(id: ProjectId): Flow<Project?> =
        projects.map { values -> values.firstOrNull { project -> project.id == id } }

    override suspend fun createProject(
        name: String,
        color: Long,
        icon: ProjectIcon,
    ): ProjectId = error("Not used")

    override suspend fun updateProject(project: Project) = Unit

    override suspend fun setArchived(id: ProjectId, archived: Boolean) = Unit

    override suspend fun deleteProject(id: ProjectId) = Unit

    override suspend fun moveProject(id: ProjectId, move: ProjectMove) = Unit
}

private class FakeAppTaskRepository(initialTask: Task) : TaskRepository {
    private val tasks = MutableStateFlow(mapOf(initialTask.id to initialTask))

    override fun observeInbox(): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { task ->
            task.projectId == null && task.deletedAt == null && !task.isCompleted
        }
    }

    override fun observeToday(date: LocalDate): Flow<List<Task>> = tasks.map { values ->
        values.values.filter { task ->
            task.dueDate == date && task.deletedAt == null && !task.isCompleted
        }
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
        tasks.value = tasks.value + (task.id to task)
    }

    override suspend fun setCompleted(id: TaskId, completed: Boolean) {
        tasks.value = tasks.value + (
            id to checkNotNull(tasks.value[id]).copy(isCompleted = completed)
        )
    }

    override suspend fun setDeleted(id: TaskId, deleted: Boolean) {
        tasks.value = tasks.value + (
            id to checkNotNull(tasks.value[id]).copy(deletedAt = NOW.takeIf { deleted })
        )
    }

    fun task(id: TaskId): Task? = tasks.value[id]
}

private val TASK_ID = TaskId("task-1")
private val NOW: Instant = Instant.parse("2026-09-15T12:00:00Z")

private fun task(): Task = Task(
    id = TASK_ID,
    title = "Купить продукты",
    description = "Молоко",
    createdAt = NOW,
    updatedAt = NOW,
)
