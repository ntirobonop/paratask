package io.github.ntirobonop.paratask.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "Task ID must not be blank" }
    }

    companion object {
        fun random(): TaskId = TaskId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class ProjectId(val value: String) {
    init {
        require(value.isNotBlank()) { "Project ID must not be blank" }
    }

    companion object {
        fun random(): ProjectId = ProjectId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class SectionId(val value: String) {
    init {
        require(value.isNotBlank()) { "Section ID must not be blank" }
    }

    companion object {
        fun random(): SectionId = SectionId(UUID.randomUUID().toString())
    }
}

data class Task(
    val id: TaskId,
    val title: String,
    val description: String = "",
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val projectId: ProjectId? = null,
    val sectionId: SectionId? = null,
    val parentTaskId: TaskId? = null,
    val isCompleted: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val completedAt: Instant? = null,
    val deletedAt: Instant? = null,
    val sortOrder: Long = 0,
)
