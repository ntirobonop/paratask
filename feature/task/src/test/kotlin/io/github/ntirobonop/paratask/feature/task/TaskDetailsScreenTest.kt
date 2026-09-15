package io.github.ntirobonop.paratask.feature.task

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ntirobonop.paratask.core.model.TaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TaskDetailsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `task fields are displayed and editable`() {
        var changedTitle = ""
        composeRule.setContent {
            MaterialTheme {
                TaskDetailsScreen(
                    uiState = readyState(),
                    snackbarHostState = SnackbarHostState(),
                    onTitleChange = { changedTitle = it },
                    onDescriptionChange = {},
                    onCompletedChange = {},
                    onBack = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Купить продукты").assertIsDisplayed()
        composeRule.onNodeWithText("Молоко").assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextReplacement("Купить хлеб")

        composeRule.runOnIdle {
            assertEquals("Купить хлеб", changedTitle)
        }
    }

    @Test
    fun `completion checkbox reports both states`() {
        var completed = false
        var uiState by mutableStateOf(readyState())
        composeRule.setContent {
            MaterialTheme {
                TaskDetailsScreen(
                    uiState = uiState,
                    snackbarHostState = SnackbarHostState(),
                    onTitleChange = {},
                    onDescriptionChange = {},
                    onCompletedChange = { completed = it },
                    onBack = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Выполнить задачу").performClick()
        composeRule.runOnIdle {
            assertTrue(completed)
            uiState = uiState.copy(isCompleted = true)
        }

        composeRule.onNodeWithContentDescription("Вернуть задачу").performClick()
        composeRule.runOnIdle { assertFalse(completed) }
    }

    @Test
    fun `delete action is available from overflow menu`() {
        var deleted = false
        composeRule.setContent {
            MaterialTheme {
                TaskDetailsScreen(
                    uiState = readyState(),
                    snackbarHostState = SnackbarHostState(),
                    onTitleChange = {},
                    onDescriptionChange = {},
                    onCompletedChange = {},
                    onBack = {},
                    onDelete = { deleted = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Действия задачи").performClick()
        composeRule.onNodeWithText("Удалить").performClick()

        composeRule.runOnIdle { assertTrue(deleted) }
    }

    @Test
    fun `missing task shows return action`() {
        composeRule.setContent {
            MaterialTheme {
                TaskDetailsScreen(
                    uiState = TaskDetailsUiState(
                        taskId = TASK_ID,
                        isLoading = false,
                        taskMissing = true,
                    ),
                    snackbarHostState = SnackbarHostState(),
                    onTitleChange = {},
                    onDescriptionChange = {},
                    onCompletedChange = {},
                    onBack = {},
                    onDelete = {},
                )
            }
        }

        composeRule.onNodeWithText("Задача не найдена").assertIsDisplayed()
        composeRule.onNodeWithText("Вернуться во Входящие").assertIsDisplayed()
    }

    private fun readyState() = TaskDetailsUiState(
        taskId = TASK_ID,
        isLoading = false,
        title = "Купить продукты",
        description = "Молоко",
    )

    private companion object {
        val TASK_ID = TaskId("task-1")
    }
}
