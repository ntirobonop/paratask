package io.github.ntirobonop.paratask

import android.app.Application
import io.github.ntirobonop.paratask.core.data.DefaultTaskRepository
import io.github.ntirobonop.paratask.core.data.DefaultProjectRepository
import io.github.ntirobonop.paratask.core.data.ProjectRepository
import io.github.ntirobonop.paratask.core.data.DefaultSectionRepository
import io.github.ntirobonop.paratask.core.data.SectionRepository
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.database.AppDatabase
import io.github.ntirobonop.paratask.core.database.createAppDatabase

class ParaTaskApplication : Application() {
    private val database: AppDatabase by lazy { createAppDatabase(this) }

    val taskRepository: TaskRepository by lazy {
        DefaultTaskRepository(
            taskDao = database.taskDao(),
            projectDao = database.projectDao(),
            sectionDao = database.sectionDao(),
        )
    }

    val projectRepository: ProjectRepository by lazy {
        DefaultProjectRepository(database.projectDao())
    }

    val sectionRepository: SectionRepository by lazy {
        DefaultSectionRepository(
            sectionDao = database.sectionDao(),
            projectDao = database.projectDao(),
        )
    }
}
