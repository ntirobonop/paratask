package io.github.ntirobonop.paratask.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {
    @Query(
        """
        SELECT * FROM sections
        WHERE deleted_at IS NULL
        ORDER BY project_id ASC, sort_order ASC, created_at ASC
        """,
    )
    fun observeAllSections(): Flow<List<SectionEntity>>

    @Query(
        """
        SELECT * FROM sections
        WHERE project_id = :projectId
          AND deleted_at IS NULL
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    fun observeSections(projectId: String): Flow<List<SectionEntity>>

    @Query("SELECT * FROM sections WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    fun observeSection(id: String): Flow<SectionEntity?>

    @Query("SELECT * FROM sections WHERE id = :id AND deleted_at IS NULL LIMIT 1")
    suspend fun getSection(id: String): SectionEntity?

    @Query(
        """
        SELECT COALESCE(MAX(sort_order), -1) FROM sections
        WHERE project_id = :projectId
          AND deleted_at IS NULL
        """,
    )
    suspend fun getMaxSortOrder(projectId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSection(section: SectionEntity)

    @Update
    suspend fun updateSection(section: SectionEntity): Int

    @Query(
        """
        UPDATE tasks
        SET section_id = NULL,
            updated_at = :updatedAt
        WHERE section_id = :sectionId
        """,
    )
    suspend fun clearTaskAssignments(sectionId: String, updatedAt: Long)

    @Query(
        """
        UPDATE sections
        SET deleted_at = :deletedAt,
            updated_at = :deletedAt
        WHERE id = :sectionId
          AND deleted_at IS NULL
        """,
    )
    suspend fun softDeleteSection(sectionId: String, deletedAt: Long): Int

    @Transaction
    suspend fun deleteSectionAndClearTasks(sectionId: String, deletedAt: Long): Int {
        clearTaskAssignments(sectionId = sectionId, updatedAt = deletedAt)
        return softDeleteSection(sectionId = sectionId, deletedAt = deletedAt)
    }
}
