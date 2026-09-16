package io.github.ntirobonop.paratask.feature.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.ntirobonop.paratask.core.ui.TaskDraft
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
                )
            }
        }

        composeRule.onNodeWithContentDescription("Добавить задачу").performClick()
        composeRule.onNodeWithText("16 сентября 2026").assertExists()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Купить продукты")
        composeRule.onNodeWithText("Создать").performClick()

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
                )
            }
        }

        composeRule.onNodeWithText("Входящие").performClick()
        composeRule.runOnIdle { assertEquals(true, inboxSelected) }
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.parse("2026-09-16")
    }
}
