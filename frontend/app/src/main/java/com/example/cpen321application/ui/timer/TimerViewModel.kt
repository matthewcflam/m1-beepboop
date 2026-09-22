package com.example.cpen321application.ui.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TimerPhase { STOPPED, RUNNING, PAUSED, FINISHED }

data class TimerUiState(
    val minutesInput: Int = 0,
    val secondsInput: Int = 0,
    val remainingMs: Long = 0,
    val totalMs: Long = 0,
    val phase: TimerPhase = TimerPhase.STOPPED,
    val showSurprise: Boolean = false
)

private const val TICK_MS = 100L

class TimerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState

    private var tickJob: Job? = null
    private var targetElapsedRealtime: Long = 0L
    private var pausedRemainingMs: Long = 0L

    fun setMinutes(minutes: Int) {
        _uiState.update { it.copy(minutesInput = minutes.coerceIn(0, 99)) }
    }

    fun setSeconds(seconds: Int) {
        _uiState.update { it.copy(secondsInput = seconds.coerceIn(0, 59)) }
    }

    fun start() {
        val state = _uiState.value
        val totalMs = (state.minutesInput * 60L + state.secondsInput) * 1000L
        if (totalMs <= 0L) return

        targetElapsedRealtime = SystemClock.elapsedRealtime() + totalMs
        _uiState.update {
            it.copy(
                totalMs = totalMs,
                remainingMs = totalMs,
                phase = TimerPhase.RUNNING,
                showSurprise = false
            )
        }
        runTicker()
    }

    fun pauseOrResume() {
        when (_uiState.value.phase) {
            TimerPhase.RUNNING -> {
                tickJob?.cancel()
                pausedRemainingMs =
                    (targetElapsedRealtime - SystemClock.elapsedRealtime()).coerceAtLeast(0)
                _uiState.update { it.copy(phase = TimerPhase.PAUSED, remainingMs = pausedRemainingMs) }
            }
            TimerPhase.PAUSED -> {
                targetElapsedRealtime = SystemClock.elapsedRealtime() + pausedRemainingMs
                _uiState.update { it.copy(phase = TimerPhase.RUNNING) }
                runTicker()
            }
            else -> Unit
        }
    }

    fun reset() {
        tickJob?.cancel()
        _uiState.update {
            it.copy(
                remainingMs = 0,
                totalMs = 0,
                phase = TimerPhase.STOPPED,
                showSurprise = false
            )
        }
    }

    fun dismissSurprise() {
        _uiState.update { it.copy(showSurprise = false) }
    }

    private fun runTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val remaining = (targetElapsedRealtime - SystemClock.elapsedRealtime())
                    .coerceAtLeast(0)
                _uiState.update { it.copy(remainingMs = remaining) }
                if (remaining <= 0L) {
                    _uiState.update { it.copy(phase = TimerPhase.FINISHED, showSurprise = true) }
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    override fun onCleared() {
        tickJob?.cancel()
    }
}
