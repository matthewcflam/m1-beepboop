package com.example.cpen321application.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onLoginClick: () -> Unit,
    onLiveUpdatesClick: () -> Unit,
    onTimerClick: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                Text("Login + Server")
            }
            Button(onClick = onLiveUpdatesClick, modifier = Modifier.fillMaxWidth()) {
                Text("Live Updates")
            }
            Button(onClick = onTimerClick, modifier = Modifier.fillMaxWidth()) {
                Text("Timer")
            }
        }
    }
}
