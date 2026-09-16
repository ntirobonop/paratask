package io.github.ntirobonop.paratask.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query(
        """
        SELECT * FROM projects
        WHERE is_archived = 0
          AND deleted_at IS NULL
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    fun observeActiveProjects(): Flow<List<ProjectEntity>>

    @Query(
        """
        SELECT * FROM projects
        WHERE is_archived = 1
          AND deleted_at IS NULL
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    fun observeArchivedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    fun observeProject(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getProject(id: String): ProjectEntity?

    @Query(
        """
        SELECT * FROM projects
        WHERE is_archived = 0
          AND deleted_at IS NULL
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    suspend fun getActiveProjects(): List<ProjectEntity>

    @Query("SELECT COALESCE(MAX(sort_order), -1) FROM projects WHERE deleted_at IS NULL")
    suspend fun getMaxSortOrder(): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity): Int

    @Query(
        """
        UPDATE projects
        SET is_archived = :archived,
            updated_at = :updatedAt
        WHERE id = :id
          AND deleted_at IS NULL
        """,
    )
    suspend fun setArchived(
        id: String,
        archived: Boolean,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE projects
        SET sort_order = :sortOrder,
            updated_at = :updatedAt
        WHERE id = :id
          AND deleted_at IS NULL
        """,
    )
    suspend fun setSortOrder(
        id: String,
        sortOrder: Long,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE tasks
        SET project_id = NULL,
            section_id = NULL,
            updated_at = :updatedAt
        WHERE project_id = :projectId
        """,
    )
    suspend fun clearTaskAssignments(
        projectId: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE projects
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :projectId
        """,
    )
    suspend fun softDeleteProject(
        projectId: String,
        deletedAt: Long,
    ): Int

    @Transaction
    suspend fun deleteProjectAndReturnTasksToInbox(
        projectId: String,
        deletedAt: Long,
    ): Int {
        clearTaskAssignments(projectId = projectId, updatedAt = deletedAt)
        return softDeleteProject(projectId = projectId, deletedAt = deletedAt)
    }

    @Transaction
    suspend fun swapSortOrders(
        firstId: String,
        firstOrder: Long,
        secondId: String,
        secondOrder: Long,
        updatedAt: Long,
    ) {
        setSortOrder(id = firstId, sortOrder = secondOrder, updatedAt = updatedAt)
        setSortOrder(id = secondId, sortOrder = firstOrder, updatedAt = updatedAt)
    }
}
