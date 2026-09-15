package io.github.ntirobonop.paratask.feature.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    onAddTask: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Входящие") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Text(text = "+", fontSize = 28.sp)
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Text("●") },
                    label = { Text("Входящие") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Text("○") },
                    label = { Text("Сегодня") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Text("○") },
                    label = { Text("Предстоящие") },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Text("○") },
                    label = { Text("Обзор") },
                )
            }
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            InboxEmptyState()
        }
    }
}

@Composable
private fun InboxEmptyState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Входящие пусты",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Добавьте задачу, чтобы ничего не забыть",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InboxScreenPreview() {
    MaterialTheme {
        InboxScreen()
    }
}
