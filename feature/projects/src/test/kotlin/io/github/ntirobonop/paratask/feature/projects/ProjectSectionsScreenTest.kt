package io.github.ntirobonop.paratask.feature.projects

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ProjectSectionsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()
    private val repository = ProjectTestRepository()

    @Test
    fun unsectionedTasksPrecedeSectionGroupsWithoutRedundantProjectMarkers() {
        val task = Task(TaskId("plain"), "Без группы", projectId = repository.project.id,
            createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
        repository.tasks.value = listOf(task, task.copy(id = TaskId("grouped"), title = "В группе", sectionId = repository.section.id))
        showProject()

        composeRule.onNodeWithText("Без группы").assertIsDisplayed()
        composeRule.onNodeWithText("В работе · 1").assertIsDisplayed()
        composeRule.onNodeWithText("В группе").assertIsDisplayed()
        val plainTop = composeRule.onNodeWithText("Без группы").fetchSemanticsNode().boundsInRoot.top
        val sectionTop = composeRule.onNodeWithText("В работе · 1").fetchSemanticsNode().boundsInRoot.top
        assertTrue(plainTop < sectionTop)
        composeRule.onNodeWithContentDescription("Проект Учёба").assertDoesNotExist()
    }

    @Test
    fun sectionContextPreselectsProjectAndSectionForNewTask() {
        showProject()
        composeRule.onNodeWithContentDescription("Добавить задачу в секцию В работе").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Новая задача секции")
        composeRule.onNodeWithText("Создать").performScrollTo().performClick()

        composeRule.waitUntil(5_000) { repository.tasks.value.isNotEmpty() }
        composeRule.runOnIdle {
            val created = repository.tasks.value.single()
            assertEquals(repository.project.id, created.projectId)
            assertEquals(repository.section.id, created.sectionId)
        }
        composeRule.onNodeWithText("В работе · 1").assertIsDisplayed()
    }

    @Test
    fun sectionCanBeCreatedRenamedAndDeletedFromProject() {
        showProject()
        composeRule.onNodeWithText("+ Секция").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Запланировано")
        composeRule.onNodeWithText("Сохранить").performClick()
        composeRule.onNodeWithText("Запланировано · 0").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Переименовать секцию Запланировано").performClick()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Позже")
        composeRule.onNodeWithText("Сохранить").performClick()
        composeRule.onNodeWithText("Позже · 0").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Удалить секцию Позже").performClick()
        composeRule.onNodeWithText("Позже · 0").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(listOf(repository.section), repository.sections.value) }
    }

    private fun showProject() {
        composeRule.setContent {
            MaterialTheme {
                ProjectRoute(
                    projectId = repository.project.id,
                    taskRepository = repository,
                    projectRepository = repository,
                    sectionRepository = repository,
                    activeProjects = listOf(repository.project),
                    snackbarHostState = remember { SnackbarHostState() },
                    onBack = {},
                    onOpenTask = {},
                )
            }
        }
        composeRule.onNodeWithText("В работе · ${repository.tasks.value.count { it.sectionId == repository.section.id }}").assertIsDisplayed()
    }
}
