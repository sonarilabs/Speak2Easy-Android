package com.sonari.speak2easy.ui.writing

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** One captured user touch sample. Pressure is 1.0 for finger input (Android only reports pressure on stylus). */
data class CanvasPoint(val x: Float, val y: Float, val tMs: Long, val pressure: Float)

/** One stroke = uninterrupted finger-down sequence of [CanvasPoint]s. */
data class CanvasStroke(val points: List<CanvasPoint>)

/**
 * Draws all captured strokes plus the in-progress stroke as the finger moves.
 * The in-progress points live in a [SnapshotStateList] so Compose recomposes the Canvas
 * on every move event — otherwise the user only sees the stroke after lifting their finger.
 */
@Composable
fun WritingCanvas(
    strokes: SnapshotStateList<CanvasStroke>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val current = remember { emptyList<CanvasPoint>().toMutableStateList() }
    val strokeStart = remember { mutableStateOf(0L) }

    Canvas(
        modifier = modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: continue
                    val now = System.currentTimeMillis()
                    val pressure = change.pressure.takeIf { !it.isNaN() } ?: 1f
                    when {
                        change.pressed && !change.previousPressed -> {
                            current.clear()
                            strokeStart.value = now
                            current.add(CanvasPoint(change.position.x, change.position.y, 0L, pressure))
                        }
                        change.pressed -> {
                            current.add(
                                CanvasPoint(
                                    change.position.x,
                                    change.position.y,
                                    now - strokeStart.value,
                                    pressure,
                                ),
                            )
                        }
                        !change.pressed && change.previousPressed -> {
                            if (current.size >= 2) strokes.add(CanvasStroke(current.toList()))
                            current.clear()
                        }
                    }
                    change.consume()
                }
            }
        },
    ) {
        strokes.forEach { drawStroke(it) }
        if (current.size >= 2) drawStroke(CanvasStroke(current.toList()))
    }
}

private fun DrawScope.drawStroke(stroke: CanvasStroke) {
    if (stroke.points.size < 2) return
    val path = Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        for (i in 1 until stroke.points.size) lineTo(stroke.points[i].x, stroke.points[i].y)
    }
    drawPath(
        path = path,
        color = Color.Black,
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
