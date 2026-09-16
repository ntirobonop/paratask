package io.github.ntirobonop.paratask.core.model

import java.time.Instant

data class Section(
    val id: SectionId,
    val projectId: ProjectId,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val sortOrder: Long = 0,
) {
    init {
        require(name.isNotBlank()) { "Section name must not be blank" }
    }
}
