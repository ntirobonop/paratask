package io.github.ntirobonop.paratask.core.model

import java.time.Instant

enum class ProjectIcon {
    LIST,
    WORK,
    SCHOOL,
    HOME,
    STAR,
}

data class Project(
    val id: ProjectId,
    val name: String,
    val color: Long,
    val icon: ProjectIcon,
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null,
    val sortOrder: Long = 0,
) {
    init {
        require(name.isNotBlank()) { "Project name must not be blank" }
    }
}
