package com.example.cpen321application.ui.timer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.surprise.FallSpec
import com.example.cpen321application.surprise.fallToFloor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onBack: () -> Unit, viewModel: TimerViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val fallen = uiState.phase == TimerPhase.FINISHED
    // Bottom edge of the content area in root coordinates — where fallen elements land.
    var floorBottom by remember { mutableFloatStateOf(0f) }
    val floor = { floorBottom }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == TimerPhase.FINISHED) {
            vibrate(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timer") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .onGloballyPositioned { floorBottom = it.boundsInRoot().bottom }
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (uiState.phase == TimerPhase.STOPPED) {
                DurationPicker(
                    minutes = uiState.minutesInput,
                    seconds = uiState.secondsInput,
                    onMinutesChange = viewModel::setMinutes,
                    onSecondsChange = viewModel::setSeconds
                )
            } else {
                val progress = if (uiState.totalMs > 0) {
                    (uiState.remainingMs.toFloat() / uiState.totalMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .size(160.dp)
                            .fallToFloor(fallen, floor, FallSpec(delayMs = 0, driftX = 0.dp, rotationDeg = -8f)),
                        strokeWidth = 8.dp
                    )
                    // Lands resting inside the bottom of the fallen ring.
                    Text(
                        text = formatRemaining(uiState.remainingMs),
                        style = MaterialTheme.typography.displaySmall,
                        modifier = Modifier.fallToFloor(
                            fallen, floor,
                            FallSpec(delayMs = 120, driftX = 0.dp, rotationDeg = 12f, floorInset = 30.dp)
                        )
                    )
                }
            }

            val canStart = uiState.phase == TimerPhase.STOPPED &&
                (uiState.minutesInput > 0 || uiState.secondsInput > 0)

            when (uiState.phase) {
                TimerPhase.STOPPED, TimerPhase.FINISHED -> {
                    // Stays "enabled" once fallen so it keeps its colour; fallToFloor swallows taps.
                    Button(
                        onClick = { if (!fallen) viewModel.start() },
                        enabled = canStart || fallen,
                        modifier = Modifier.fallToFloor(
                            fallen, floor,
                            FallSpec(delayMs = 60, driftX = (-120).dp, rotationDeg = -15f)
                        )
                    ) { Text("Start") }
                }
                TimerPhase.RUNNING -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = viewModel::pauseOrResume) { Text("Pause") }
                        OutlinedButton(onClick = viewModel::reset) { Text("Reset") }
                    }
                }
                TimerPhase.PAUSED -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedButton(onClick = viewModel::pauseOrResume) { Text("Resume") }
                        OutlinedButton(onClick = viewModel::reset) { Text("Reset") }
                    }
                }
            }

            if (fallen) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fallToFloor(
                        fallen, floor,
                        FallSpec(delayMs = 200, driftX = 120.dp, rotationDeg = 10f)
                    )
                ) { Text("Reset") }
            }
        }
    }
}

@Composable
private fun DurationPicker(
    minutes: Int,
    seconds: Int,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberStepper(label = "min", value = minutes, range = 0..99, onValueChange = onMinutesChange)
        Text(":", style = MaterialTheme.typography.displaySmall)
        NumberStepper(label = "sec", value = seconds, range = 0..59, onValueChange = onSecondsChange)
    }
}

// Vertical (+ / value / −) layout keeps both steppers narrow enough to fit side by side on any phone.
@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        FilledTonalIconButton(
            onClick = { onValueChange((value + 1).coerceIn(range)) },
            enabled = value < range.last,
            modifier = Modifier.size(48.dp)
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displaySmall
        )
        FilledTonalIconButton(
            onClick = { onValueChange((value - 1).coerceIn(range)) },
            enabled = value > range.first,
            modifier = Modifier.size(48.dp)
        ) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun formatRemaining(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(VibratorManager::class.java)
        manager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(400)
        }
    }
}
