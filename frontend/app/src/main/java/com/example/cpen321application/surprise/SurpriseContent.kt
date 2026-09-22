package com.example.cpen321application.surprise

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// How one element tumbles when the timer finishes.
data class FallSpec(
    val delayMs: Long,
    val driftX: Dp,
    val rotationDeg: Float,
    val floorInset: Dp = 0.dp
)

// Timer-finished surprise: when [fallen] flips true the element drops to [floorBottomPx]
// (root coordinates), bounces, lands tilted, and stops accepting input. It never gets back up —
// leaving the screen discards the timer's ViewModel, which is the only way to reset.
fun Modifier.fallToFloor(
    fallen: Boolean,
    floorBottomPx: () -> Float,
    spec: FallSpec
): Modifier = composed {
    val density = LocalDensity.current
    var restingBottom by remember { mutableFloatStateOf(Float.NaN) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(fallen) {
        if (!fallen) return@LaunchedEffect
        // Wait until both the element and the floor have been laid out.
        snapshotFlow { !restingBottom.isNaN() && floorBottomPx() > 0f }.first { it }
        delay(spec.delayMs)

        val insetPx = with(density) { spec.floorInset.toPx() }
        val bouncy = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
        launch { offsetY.animateTo(floorBottomPx() - restingBottom - insetPx, bouncy) }
        launch { offsetX.animateTo(with(density) { spec.driftX.toPx() }, bouncy) }
        launch { rotation.animateTo(spec.rotationDeg, bouncy) }
    }

    this
        // Measured before the graphicsLayer, so the fall itself doesn't skew the measurement.
        .onGloballyPositioned { coords ->
            if (restingBottom.isNaN()) restingBottom = coords.boundsInRoot().bottom
        }
        .graphicsLayer {
            translationX = offsetX.value
            translationY = offsetY.value
            rotationZ = rotation.value
        }
        .pointerInput(fallen) {
            if (!fallen) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
}
