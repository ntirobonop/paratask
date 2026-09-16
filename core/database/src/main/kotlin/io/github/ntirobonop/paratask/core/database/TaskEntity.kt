package io.github.ntirobonop.paratask.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id", "project_id"],
            childColumns = ["section_id", "project_id"],
        ),
    ],
    indices = [
        Index(value = ["project_id", "is_completed", "deleted_at"]),
        Index(value = ["section_id", "project_id"]),
        Index(value = ["parent_task_id"]),
    ],
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    @ColumnInfo(name = "due_date")
    val dueDate: String?,
    @ColumnInfo(name = "due_time")
    val dueTime: String?,
    @ColumnInfo(name = "project_id")
    val projectId: String?,
    @ColumnInfo(name = "section_id")
    val sectionId: String?,
    @ColumnInfo(name = "parent_task_id")
    val parentTaskId: String?,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Long,
)
