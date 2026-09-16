package io.github.ntirobonop.paratask.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, ProjectEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun projectDao(): ProjectDao
}

fun createAppDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "paratask.db",
    ).addMigrations(MIGRATION_1_2)
        .build()

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS projects (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                color INTEGER NOT NULL,
                icon TEXT NOT NULL,
                is_archived INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                deleted_at INTEGER,
                sort_order INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_projects_is_archived_deleted_at_sort_order
            ON projects (is_archived, deleted_at, sort_order)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tasks_new (
                id TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                due_date TEXT,
                due_time TEXT,
                project_id TEXT,
                section_id TEXT,
                parent_task_id TEXT,
                is_completed INTEGER NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                completed_at INTEGER,
                deleted_at INTEGER,
                sort_order INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(project_id) REFERENCES projects(id)
                    ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO tasks_new (
                id, title, description, due_date, due_time, project_id, section_id,
                parent_task_id, is_completed, created_at, updated_at, completed_at,
                deleted_at, sort_order
            )
            SELECT
                id, title, description, due_date, due_time,
                CASE
                    WHEN project_id IN (SELECT id FROM projects) THEN project_id
                    ELSE NULL
                END,
                section_id, parent_task_id, is_completed, created_at, updated_at,
                completed_at, deleted_at, sort_order
            FROM tasks
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE tasks")
        db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_tasks_project_id_is_completed_deleted_at
            ON tasks (project_id, is_completed, deleted_at)
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_tasks_parent_task_id
            ON tasks (parent_task_id)
            """.trimIndent(),
        )
    }
}
