package com.example.cpen321application.ui.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private const val GRID_SIZE = 16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveUpdatesScreen(onBack: () -> Unit, viewModel: LiveUpdatesViewModel = viewModel()) {
    val status by viewModel.status.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Updates") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
            Text(
                text = when (status) {
                    ConnectionStatus.CONNECTING -> "Connecting…"
                    ConnectionStatus.LIVE -> "Live"
                    ConnectionStatus.DISCONNECTED -> "Disconnected — retrying"
                },
                style = MaterialTheme.typography.titleMedium
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(top = 16.dp)
            ) {
                drawPixelGrid(viewModel.cells)
            }
        }
    }
}

private fun DrawScope.drawPixelGrid(cells: List<Color>) {
    val cellSize = size.width / GRID_SIZE
    for (y in 0 until GRID_SIZE) {
        for (x in 0 until GRID_SIZE) {
            val color = cells[y * GRID_SIZE + x]
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(x * cellSize, y * cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
            )
        }
    }
    // Thin grid lines so the empty canvas is visible.
    val gridLineColor = Color.Gray.copy(alpha = 0.3f)
    for (i in 0..GRID_SIZE) {
        drawLine(
            color = gridLineColor,
            start = androidx.compose.ui.geometry.Offset(i * cellSize, 0f),
            end = androidx.compose.ui.geometry.Offset(i * cellSize, size.height)
        )
        drawLine(
            color = gridLineColor,
            start = androidx.compose.ui.geometry.Offset(0f, i * cellSize),
            end = androidx.compose.ui.geometry.Offset(size.width, i * cellSize)
        )
    }
}
