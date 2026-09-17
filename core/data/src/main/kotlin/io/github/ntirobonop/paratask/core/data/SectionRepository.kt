package io.github.ntirobonop.paratask.core.data

import io.github.ntirobonop.paratask.core.model.ProjectId
import io.github.ntirobonop.paratask.core.model.Section
import io.github.ntirobonop.paratask.core.model.SectionId
import kotlinx.coroutines.flow.Flow

interface SectionRepository {
    fun observeAllSections(): Flow<List<Section>>

    fun observeSections(projectId: ProjectId): Flow<List<Section>>

    fun observeSection(id: SectionId): Flow<Section?>

    suspend fun createSection(projectId: ProjectId, name: String): SectionId

    suspend fun renameSection(id: SectionId, name: String)

    suspend fun deleteSection(id: SectionId)
}
