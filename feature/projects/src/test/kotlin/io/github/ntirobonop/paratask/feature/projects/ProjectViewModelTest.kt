package io.github.ntirobonop.paratask.feature.projects

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectViewModelTest {
    @get:Rule
    val mainDispatcherRule = object : TestWatcher() {
        override fun starting(description: Description) { Dispatchers.setMain(StandardTestDispatcher()) }
        override fun finished(description: Description) { Dispatchers.resetMain() }
    }

    @Test
    fun projectStateReactsToSectionLifecycle() = runTest {
        val repository = ProjectTestRepository()
        val viewModel = ProjectViewModel(repository, repository, repository, repository.project.id)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(repository.project, viewModel.uiState.value.project)

        viewModel.createSection("   ")
        viewModel.createSection("Новая")
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.sections.size)
        val created = viewModel.uiState.value.sections.last()
        viewModel.renameSection(created.id, "Позже")
        advanceUntilIdle()
        assertEquals("Позже", viewModel.uiState.value.sections.last().name)
        viewModel.deleteSection(created.id)
        advanceUntilIdle()
        assertEquals(listOf(repository.section), viewModel.uiState.value.sections)
    }

    @Test
    fun sectionWriteFailureReportsErrorAndKeepsExistingSections() = runTest {
        val repository = ProjectTestRepository().apply { failSectionWrites = true }
        val viewModel = ProjectViewModel(repository, repository, repository, repository.project.id)
        viewModel.deleteSection(repository.section.id)
        advanceUntilIdle()
        assertEquals(ProjectUiEvent.ShowMessage("Не удалось удалить секцию"), viewModel.events.first())
        assertEquals(listOf(repository.section), repository.sections.value)
    }

    @Test
    fun creationCompletionAndUndoKeepSectionAssignment() = runTest {
        val repository = ProjectTestRepository()
        val viewModel = ProjectViewModel(repository, repository, repository, repository.project.id)
        viewModel.createTask("В секции", "Описание", null, repository.project.id, repository.section.id)
        advanceUntilIdle()
        val task = repository.tasks.value.single()
        assertEquals(repository.section.id, task.sectionId)
        viewModel.completeTask(task.id)
        advanceUntilIdle()
        assertEquals(ProjectUiEvent.TaskCompleted(task.id), viewModel.events.first())
        assertTrue(repository.tasks.value.single().isCompleted)
        viewModel.undoCompletion(task.id)
        advanceUntilIdle()
        assertEquals(task, repository.tasks.value.single())
    }
}
