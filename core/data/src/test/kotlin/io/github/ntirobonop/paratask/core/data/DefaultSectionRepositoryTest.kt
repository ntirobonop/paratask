package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.ProjectDao
import io.github.ntirobonop.paratask.core.database.ProjectEntity
import io.github.ntirobonop.paratask.core.database.SectionDao
import io.github.ntirobonop.paratask.core.database.SectionEntity
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.SectionId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSectionRepositoryTest {
    private val instant = Instant.parse("2026-09-16T12:00:00Z")
    private val projectId = ProjectId("project-1")
    private val sectionId = SectionId("section-1")
    private val projectDao = SectionTestProjectDao(project(projectId.value))
    private val sectionDao = FakeSectionDao()
    private val repository = DefaultSectionRepository(
        sectionDao = sectionDao,
        projectDao = projectDao,
        clock = Clock.fixed(instant, ZoneOffset.UTC),
        idFactory = { sectionId },
    )

    @Test
    fun createSectionNormalizesAndPersistsInput() = runTest {
        val createdId = repository.createSection(projectId, "  В работе  ")

        val section = repository.observeSection(createdId).first()
        requireNotNull(section)
        assertEquals(sectionId, createdId)
        assertEquals("В работе", section.name)
        assertEquals(projectId, section.projectId)
        assertEquals(instant, section.createdAt)
        assertEquals(0L, section.sortOrder)
    }

    @Test
    fun createSectionRejectsBlankNameAndArchivedProject() {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.createSection(projectId, "   ") }
        }

        projectDao.projects.value = listOf(project(projectId.value).copy(isArchived = true))
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.createSection(projectId, "Готово") }
        }
    }

    @Test
    fun renameAndDeletePreserveSectionLifecycleRules() = runTest {
        repository.createSection(projectId, "Черновик")

        repository.renameSection(sectionId, "  Готово  ")
        assertEquals("Готово", repository.observeSection(sectionId).first()?.name)

        repository.deleteSection(sectionId)
        assertEquals(listOf(sectionId.value), sectionDao.clearedAssignments)
        assertTrue(repository.observeSections(projectId).first().isEmpty())
    }
}

private class FakeSectionDao : SectionDao {
    private val sections = MutableStateFlow<List<SectionEntity>>(emptyList())
    val clearedAssignments = mutableListOf<String>()

    override fun observeAllSections(): Flow<List<SectionEntity>> = sections.map { values ->
        values.filter { it.deletedAt == null }.sortedWith(
            compareBy(SectionEntity::projectId, SectionEntity::sortOrder, SectionEntity::createdAt),
        )
    }

    override fun observeSections(projectId: String): Flow<List<SectionEntity>> =
        sections.map { values ->
            values.filter { it.projectId == projectId && it.deletedAt == null }
                .sortedWith(compareBy(SectionEntity::sortOrder, SectionEntity::createdAt))
        }

    override fun observeSection(id: String): Flow<SectionEntity?> = sections.map { values ->
        values.firstOrNull { it.id == id && it.deletedAt == null }
    }

    override suspend fun getSection(id: String): SectionEntity? =
        sections.value.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getMaxSortOrder(projectId: String): Long =
        sections.value.filter { it.projectId == projectId && it.deletedAt == null }
            .maxOfOrNull(SectionEntity::sortOrder) ?: -1

    override suspend fun insertSection(section: SectionEntity) {
        check(sections.value.none { it.id == section.id })
        sections.value += section
    }

    override suspend fun updateSection(section: SectionEntity): Int = replace(section.id) { section }

    override suspend fun clearTaskAssignments(sectionId: String, updatedAt: Long) {
        clearedAssignments += sectionId
    }

    override suspend fun softDeleteSection(sectionId: String, deletedAt: Long): Int =
        replace(sectionId) { section ->
            section.copy(deletedAt = deletedAt, updatedAt = deletedAt)
        }

    private fun replace(id: String, transform: (SectionEntity) -> SectionEntity): Int {
        var changed = 0
        sections.value = sections.value.map { section ->
            if (section.id == id && section.deletedAt == null) {
                changed += 1
                transform(section)
            } else {
                section
            }
        }
        return changed
    }
}

private class SectionTestProjectDao(initialProject: ProjectEntity) : ProjectDao {
    val projects = MutableStateFlow(listOf(initialProject))

    override fun observeActiveProjects(): Flow<List<ProjectEntity>> = projects.map { values ->
        values.filter { !it.isArchived && it.deletedAt == null }
    }

    override fun observeArchivedProjects(): Flow<List<ProjectEntity>> = projects.map { values ->
        values.filter { it.isArchived && it.deletedAt == null }
    }

    override fun observeProject(id: String): Flow<ProjectEntity?> = projects.map { values ->
        values.firstOrNull { it.id == id && it.deletedAt == null }
    }

    override suspend fun getProject(id: String): ProjectEntity? =
        projects.value.firstOrNull { it.id == id && it.deletedAt == null }

    override suspend fun getActiveProjects(): List<ProjectEntity> =
        projects.value.filter { !it.isArchived && it.deletedAt == null }

    override suspend fun getMaxSortOrder(): Long =
        projects.value.maxOfOrNull(ProjectEntity::sortOrder) ?: -1

    override suspend fun insertProject(project: ProjectEntity) = error("Not used")

    override suspend fun updateProject(project: ProjectEntity): Int = error("Not used")

    override suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long): Int =
        error("Not used")

    override suspend fun setSortOrder(id: String, sortOrder: Long, updatedAt: Long): Int =
        error("Not used")

    override suspend fun clearTaskAssignments(projectId: String, updatedAt: Long) =
        error("Not used")

    override suspend fun softDeleteProject(projectId: String, deletedAt: Long): Int =
        error("Not used")

    override suspend fun softDeleteProjectSections(projectId: String, deletedAt: Long) =
        error("Not used")
}

private fun project(id: String) = ProjectEntity(
    id = id,
    name = id,
    color = 0,
    icon = "LIST",
    isArchived = false,
    createdAt = 10,
    updatedAt = 10,
    deletedAt = null,
    sortOrder = 0,
)
