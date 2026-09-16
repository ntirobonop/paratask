package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.database.ProjectDao
import io.github.ntirobonop.paratask.core.database.SectionDao
import io.github.ntirobonop.paratask.core.database.SectionEntity
import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.Section
import io.github.ntirobonop.paratask.core.model.SectionId
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultSectionRepository(
    private val sectionDao: SectionDao,
    private val projectDao: ProjectDao,
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> SectionId = SectionId::random,
) : SectionRepository {
    override fun observeAllSections(): Flow<List<Section>> =
        sectionDao.observeAllSections().map { sections -> sections.map(SectionEntity::toDomain) }

    override fun observeSections(projectId: ProjectId): Flow<List<Section>> =
        sectionDao.observeSections(projectId.value)
            .map { sections -> sections.map(SectionEntity::toDomain) }

    override fun observeSection(id: SectionId): Flow<Section?> =
        sectionDao.observeSection(id.value).map { section -> section?.toDomain() }

    override suspend fun createSection(projectId: ProjectId, name: String): SectionId {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Section name must not be blank" }
        val project = projectDao.getProject(projectId.value)
        require(project != null && !project.isArchived) {
            "Sections can only be created in an active project"
        }
        val id = idFactory()
        val now = clock.instant()
        sectionDao.insertSection(
            SectionEntity(
                id = id.value,
                projectId = projectId.value,
                name = normalizedName,
                createdAt = now.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
                deletedAt = null,
                sortOrder = sectionDao.getMaxSortOrder(projectId.value) + 1,
            ),
        )
        return id
    }

    override suspend fun renameSection(id: SectionId, name: String) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Section name must not be blank" }
        val section = sectionDao.getSection(id.value) ?: error("Section does not exist")
        check(
            sectionDao.updateSection(
                section.copy(name = normalizedName, updatedAt = clock.millis()),
            ) == 1,
        ) { "Section does not exist" }
    }

    override suspend fun deleteSection(id: SectionId) {
        check(sectionDao.deleteSectionAndClearTasks(id.value, clock.millis()) == 1) {
            "Section does not exist"
        }
    }
}

internal fun SectionEntity.toDomain(): Section = Section(
    id = SectionId(id),
    projectId = ProjectId(projectId),
    name = name,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deletedAt = deletedAt?.let(Instant::ofEpochMilli),
    sortOrder = sortOrder,
)
