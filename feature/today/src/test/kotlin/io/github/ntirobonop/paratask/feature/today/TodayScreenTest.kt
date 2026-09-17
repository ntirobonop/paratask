package io.github.ntirobonop.paratask.feature.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ntirobonop.paratask.core.model.Project
import io.github.ntirobonop.paratask.core.model.ProjectIcon
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.ui.TaskDraft
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TodayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty Today displays contextual empty state`() {
        composeRule.setContent {
            MaterialTheme {
                TodayScreen(
                    uiState = TodayUiState(date = TODAY, isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {},
                    onDismissQuickAdd = {},
                    onCreateTask = {},
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToInbox = {},
                    onNavigateToUpcoming = {},
                )
            }
        }

        composeRule.onNodeWithText("На сегодня задач нет").assertIsDisplayed()
    }

    @Test
    fun `Quick Add starts with today selected`() {
        var createdDraft: TaskDraft? = null
        composeRule.setContent {
            var showQuickAdd by remember { mutableStateOf(false) }
            MaterialTheme {
                TodayScreen(
                    uiState = TodayUiState(date = TODAY, isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = showQuickAdd,
                    onAddTask = { showQuickAdd = true },
                    onDismissQuickAdd = { showQuickAdd = false },
                    onCreateTask = {
                        createdDraft = it
                        showQuickAdd = false
                    },
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToInbox = {},
                    onNavigateToUpcoming = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Добавить задачу").performClick()
        composeRule.onNodeWithText("16 сентября 2026").assertExists()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Купить продукты")
        composeRule.onNodeWithText("Создать").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(TODAY, createdDraft?.dueDate)
        }
    }

    @Test
    fun `Inbox navigation is active`() {
        var inboxSelected = false
        composeRule.setContent {
            MaterialTheme {
                TodayScreen(
                    uiState = TodayUiState(date = TODAY, isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {},
                    onDismissQuickAdd = {},
                    onCreateTask = {},
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToInbox = { inboxSelected = true },
                    onNavigateToUpcoming = {},
                )
            }
        }

        composeRule.onNodeWithText("Входящие").performClick()
        composeRule.runOnIdle { assertEquals(true, inboxSelected) }
    }

    @Test
    fun `task rows identify even archived projects outside a project screen`() {
        val project = Project(
            id = ProjectId("archived"), name = "Учёба", color = 0xFF6750A4,
            icon = ProjectIcon.SCHOOL, isArchived = true,
            createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
        )
        val task = Task(TaskId("project-task"), "Сдать работу", dueDate = TODAY,
            projectId = project.id, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
        composeRule.setContent {
            MaterialTheme {
                TodayScreen(
                    uiState = TodayUiState(date = TODAY, tasks = listOf(task), isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {}, onDismissQuickAdd = {}, onCreateTask = {},
                    onCompleteTask = {}, onOpenTask = {},
                    onNavigateToInbox = {}, onNavigateToUpcoming = {},
                    activeProjects = emptyList(),
                    taskProjects = listOf(project),
                )
            }
        }
        composeRule.onNodeWithContentDescription("Проект Учёба", useUnmergedTree = true).assertIsDisplayed()
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.parse("2026-09-16")
    }
}
