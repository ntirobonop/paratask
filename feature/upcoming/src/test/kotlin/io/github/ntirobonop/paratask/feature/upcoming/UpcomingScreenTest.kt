package io.github.ntirobonop.paratask.feature.upcoming

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
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class UpcomingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `week strip exposes boundaries selection and task indicators`() {
        composeRule.setContent {
            MaterialTheme {
                UpcomingScreen(
                    uiState = readyState(),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {},
                    onDismissQuickAdd = {},
                    onCreateTask = {},
                    onSelectDate = {},
                    onPreviousWeek = {},
                    onNextWeek = {},
                    onToday = {},
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToInbox = {},
                    onNavigateToToday = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("понедельник, 14 сентября, задач: 0")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "среда, 16 сентября, задач: 1, выбрано",
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("воскресенье, 20 сентября, задач: 0")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Купить продукты").assertIsDisplayed()
    }

    @Test
    fun `week and day actions are forwarded`() {
        var selectedDate: LocalDate? = null
        var previous = false
        var next = false
        var today = false
        composeRule.setContent {
            MaterialTheme {
                UpcomingScreen(
                    uiState = readyState(),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = false,
                    onAddTask = {},
                    onDismissQuickAdd = {},
                    onCreateTask = {},
                    onSelectDate = { selectedDate = it },
                    onPreviousWeek = { previous = true },
                    onNextWeek = { next = true },
                    onToday = { today = true },
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToInbox = {},
                    onNavigateToToday = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("четверг, 17 сентября, задач: 0")
            .performClick()
        composeRule.onNodeWithContentDescription("Предыдущая неделя").performClick()
        composeRule.onNodeWithContentDescription("Следующая неделя").performClick()
        composeRule.onNodeWithContentDescription("Перейти к сегодняшней дате").performClick()

        composeRule.runOnIdle {
            assertEquals(TODAY.plusDays(1), selectedDate)
            assertEquals(true, previous)
            assertEquals(true, next)
            assertEquals(true, today)
        }
    }

    @Test
    fun `Quick Add starts with selected date`() {
        var createdDraft: TaskDraft? = null
        composeRule.setContent {
            var showQuickAdd by remember { mutableStateOf(false) }
            MaterialTheme {
                UpcomingScreen(
                    uiState = readyState(),
                    snackbarHostState = SnackbarHostState(),
                    showQuickAdd = showQuickAdd,
                    onAddTask = { showQuickAdd = true },
                    onDismissQuickAdd = { showQuickAdd = false },
                    onCreateTask = {
                        createdDraft = it
                        showQuickAdd = false
                    },
                    onSelectDate = {},
                    onPreviousWeek = {},
                    onNextWeek = {},
                    onToday = {},
                    onCompleteTask = {},
                    onOpenTask = {},
                    onNavigateToInbox = {},
                    onNavigateToToday = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Добавить задачу").performClick()
        composeRule.onNodeWithText("16 сентября 2026").assertExists()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Купить продукты")
        composeRule.onNodeWithText("Создать").performClick()

        composeRule.runOnIdle { assertEquals(TODAY, createdDraft?.dueDate) }
    }

    private fun readyState() = UpcomingUiState(
        today = TODAY,
        selectedDate = TODAY,
        weekStart = MONDAY,
        weekTasks = listOf(
            Task(
                id = TaskId("today"),
                title = "Купить продукты",
                dueDate = TODAY,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        ),
        isLoading = false,
    )

    private companion object {
        val NOW: Instant = Instant.parse("2026-09-16T09:00:00Z")
        val TODAY: LocalDate = LocalDate.parse("2026-09-16")
        val MONDAY: LocalDate = LocalDate.parse("2026-09-14")
    }
}
