package io.github.ntirobonop.paratask.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
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
import io.github.ntirobonop.paratask.core.model.Section
import io.github.ntirobonop.paratask.core.model.SectionId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TaskSectionComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun inboxDoesNotOfferASectionSelector() {
        composeRule.setContent {
            MaterialTheme {
                TaskSectionField(null, null, listOf(FIRST_SECTION), onSectionChange = {})
            }
        }

        composeRule.onNodeWithText("—").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Выбрать секцию").assertIsNotEnabled()
    }

    @Test
    fun selectorOnlyOffersNonDeletedSectionsOfSelectedProject() {
        var selected: SectionId? = null
        composeRule.setContent {
            MaterialTheme {
                TaskSectionField(
                    FIRST_PROJECT.id,
                    null,
                    listOf(FIRST_SECTION, SECOND_SECTION, FIRST_SECTION.copy(
                        id = SectionId("deleted"), name = "Удалённая", deletedAt = Instant.EPOCH,
                    )),
                    onSectionChange = { selected = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Выбрать секцию").performClick()
        composeRule.onNodeWithText(SECOND_SECTION.name).assertDoesNotExist()
        composeRule.onNodeWithText("Удалённая").assertDoesNotExist()
        composeRule.onNodeWithText(FIRST_SECTION.name).performClick()
        composeRule.runOnIdle { assertEquals(FIRST_SECTION.id, selected) }
    }

    @Test
    fun changingComposerProjectClearsSectionAndOffersNewProjectSections() {
        var draft: TaskDraft? = null
        composeRule.setContent {
            MaterialTheme {
                TaskComposerSheet(
                    initialDueDate = null,
                    initialProjectId = FIRST_PROJECT.id,
                    initialSectionId = FIRST_SECTION.id,
                    projects = listOf(FIRST_PROJECT, SECOND_PROJECT),
                    sections = listOf(FIRST_SECTION, SECOND_SECTION),
                    onDismissRequest = {},
                    onCreateTask = { draft = it },
                )
            }
        }
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Задача")
        composeRule.onNodeWithContentDescription("Выбрать проект").performScrollTo().performClick()
        composeRule.onNodeWithText(SECOND_PROJECT.name).performClick()
        composeRule.onNodeWithText("Создать").performScrollTo().performClick()
        composeRule.runOnIdle {
            assertEquals(SECOND_PROJECT.id, draft?.projectId)
            assertNull(draft?.sectionId)
        }

        composeRule.onNodeWithContentDescription("Выбрать секцию").performScrollTo().performClick()
        composeRule.onNodeWithText(FIRST_SECTION.name).assertDoesNotExist()
        composeRule.onNodeWithText(SECOND_SECTION.name).performClick()
        composeRule.onNodeWithText("Создать").performScrollTo().performClick()
        composeRule.runOnIdle { assertEquals(SECOND_SECTION.id, draft?.sectionId) }
    }

    @Test
    fun archivedProjectNameIsDisplayedButCannotBeNewlySelected() {
        val archived = FIRST_PROJECT.copy(isArchived = true)
        composeRule.setContent {
            MaterialTheme {
                TaskProjectField(archived.id, listOf(archived, SECOND_PROJECT), {})
            }
        }
        composeRule.onNodeWithText(archived.name).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Выбрать проект").performClick()
        composeRule.onNodeWithText(SECOND_PROJECT.name).assertIsDisplayed()
        // Only the existing selected-value row, never a menu item, can match the archived name.
        assertEquals(1, composeRule.onAllNodes(hasText(archived.name) and hasClickAction()).fetchSemanticsNodes().size)
    }

    private companion object {
        val FIRST_PROJECT = Project(ProjectId("one"), "Учёба", 0xFF6750A4, ProjectIcon.SCHOOL,
            createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH)
        val SECOND_PROJECT = FIRST_PROJECT.copy(id = ProjectId("two"), name = "Дом")
        val FIRST_SECTION = Section(SectionId("first"), FIRST_PROJECT.id, "В работе", Instant.EPOCH, Instant.EPOCH)
        val SECOND_SECTION = FIRST_SECTION.copy(id = SectionId("second"), projectId = SECOND_PROJECT.id, name = "Покупки")
    }
}
