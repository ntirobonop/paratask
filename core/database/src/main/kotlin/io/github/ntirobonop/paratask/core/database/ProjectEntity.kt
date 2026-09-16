package io.github.ntirobonop.paratask.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["is_archived", "deleted_at", "sort_order"]),
    ],
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: Long,
    val icon: String,
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Long,
)
