package io.github.ntirobonop.paratask

import android.app.Application
import io.github.ntirobonop.paratask.core.data.DefaultTaskRepository
import io.github.ntirobonop.paratask.core.data.TaskRepository
import io.github.ntirobonop.paratask.core.database.AppDatabase
import io.github.ntirobonop.paratask.core.database.createAppDatabase

class ParaTaskApplication : Application() {
    private val database: AppDatabase by lazy { createAppDatabase(this) }

    val taskRepository: TaskRepository by lazy {
        DefaultTaskRepository(database.taskDao())
    }
}
