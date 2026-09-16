package io.github.ntirobonop.paratask.feature.inbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ntirobonop.paratask.core.model.Task
import io.github.ntirobonop.paratask.core.model.TaskId
import io.github.ntirobonop.paratask.core.ui.TaskDraft
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class InboxScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `empty Inbox displays empty state`() {
        composeRule.setContent {
            MaterialTheme {
                InboxScreen(
                    uiState = InboxUiState(isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {},
                    onDismissQuickAdd = {},
                    onCreateTask = {},
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToToday = {},
                )
            }
        }

        composeRule.onNodeWithText("Входящие пусты").assertIsDisplayed()
        composeRule.onNodeWithText("Добавьте задачу, чтобы ничего не забыть")
            .assertIsDisplayed()
    }

    @Test
    fun `FAB opens focused Quick Add and creates entered draft`() {
        var createdDraft: TaskDraft? = null
        composeRule.setContent {
            var showQuickAdd by remember { mutableStateOf(false) }
            MaterialTheme {
                InboxScreen(
                    uiState = InboxUiState(isLoading = false),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = showQuickAdd,
                    onAddTask = { showQuickAdd = true },
                    onDismissQuickAdd = { showQuickAdd = false },
                    onCreateTask = { draft ->
                        createdDraft = draft
                        showQuickAdd = false
                    },
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToToday = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Добавить задачу").performClick()
        composeRule.onNodeWithText("Не выбрана").assertExists()
        composeRule.onAllNodes(hasSetTextAction())[0]
            .assertIsFocused()
            .performTextInput("Купить продукты")
        composeRule.onAllNodes(hasSetTextAction())[1]
            .performTextInput("Молоко")
        composeRule.onNodeWithText("Создать").performClick()

        composeRule.runOnIdle {
            assertEquals(
                TaskDraft(title = "Купить продукты", description = "Молоко"),
                createdDraft,
            )
        }
    }

    @Test
    fun `task checkbox requests completion`() {
        var completedTaskId: TaskId? = null
        val taskId = TaskId("task")
        composeRule.setContent {
            MaterialTheme {
                InboxScreen(
                    uiState = InboxUiState(
                        tasks = listOf(
                            Task(
                                id = taskId,
                                title = "Купить продукты",
                                createdAt = NOW,
                                updatedAt = NOW,
                            ),
                        ),
                        isLoading = false,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {},
                    onDismissQuickAdd = {},
                    onCreateTask = {},
                    onCompleteTask = { completedTaskId = it },
                    onOpenTask = {},
                    onNavigateToToday = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Выполнить задачу Купить продукты")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(taskId, completedTaskId)
        }
    }

    @Test
    fun `task row opens selected task`() {
        val taskId = TaskId("task")
        var openedTaskId: TaskId? = null
        composeRule.setContent {
            MaterialTheme {
                InboxScreen(
                    uiState = InboxUiState(
                        tasks = listOf(
                            Task(
                                id = taskId,
                                title = "Купить продукты",
                                createdAt = NOW,
                                updatedAt = NOW,
                            ),
                        ),
                        isLoading = false,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {},
                    onDismissQuickAdd = {},
                    onCreateTask = {},
                    onCompleteTask = {},
                    onOpenTask = { openedTaskId = it },
                    onNavigateToToday = {},
                )
            }
        }

        composeRule.onNodeWithText("Купить продукты").performClick()

        composeRule.runOnIdle {
            assertEquals(taskId, openedTaskId)
        }
    }

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-15T12:00:00Z")
    }
}
