package io.github.ntirobonop.paratask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.ntirobonop.paratask.core.designsystem.theme.ParaTaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParaTaskTheme {
                ParaTaskApp(
                    taskRepository = (application as ParaTaskApplication).taskRepository,
                )
            }
        }
    }
}
