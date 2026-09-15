package io.github.ntirobonop.paratask.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE project_id IS NULL
          AND deleted_at IS NULL
          AND is_completed = 0
        ORDER BY sort_order ASC, created_at ASC
        """,
    )
    fun observeInbox(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun observeTask(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTask(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity): Int

    @Query(
        """
        UPDATE tasks
        SET is_completed = :completed,
            completed_at = :completedAt,
            updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun setCompleted(
        id: String,
        completed: Boolean,
        completedAt: Long?,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE tasks
        SET deleted_at = :deletedAt,
            updated_at = :updatedAt
        WHERE id = :id
        """,
    )
    suspend fun setDeleted(
        id: String,
        deletedAt: Long?,
        updatedAt: Long,
    ): Int
}
