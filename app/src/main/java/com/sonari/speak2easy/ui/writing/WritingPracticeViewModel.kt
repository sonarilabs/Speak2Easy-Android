package com.sonari.speak2easy.ui.writing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.remote.dto.EvaluationRequest
import com.sonari.speak2easy.data.remote.dto.EvaluationResponse
import com.sonari.speak2easy.data.remote.dto.Stroke
import com.sonari.speak2easy.data.remote.dto.StrokePoint
import com.sonari.speak2easy.data.writing.StrokeRepository
import com.sonari.speak2easy.service.HapticsManager
import kotlinx.coroutines.launch
import kotlin.math.max

data class WritingUiState(
    val character: String = "",
    val charset: String = "hiragana",
    val mode: WritingMode = WritingMode.GUIDE,
    val svg: String? = null,        // null = loading; "" = fetch failed (still let user free-draw)
    val isLoadingSvg: Boolean = true,
    val isSubmitting: Boolean = false,
    val result: EvaluationResponse? = null,
    val errorMessage: String? = null,
)

/**
 * Owns the SVG load (via [StrokeRepository] cache) and the submit flow. The drawing
 * strokes themselves live on a `SnapshotStateList` hoisted in the screen so the canvas
 * can append in real time — this VM only reads them when the user taps SUBMIT.
 */
class WritingPracticeViewModel(
    character: String,
    charset: String,
    private val strokeRepo: StrokeRepository,
    private val haptics: HapticsManager,
) : ViewModel() {

    var state by mutableStateOf(WritingUiState(character = character, charset = charset))
        private set

    /** Drawn strokes — exposed mutably to the canvas, read on submit / clear. */
    val strokes: SnapshotStateList<CanvasStroke> = emptyList<CanvasStroke>().toMutableStateList()

    init { loadSvg() }

    fun setMode(mode: WritingMode) {
        if (mode == state.mode) return
        state = state.copy(mode = mode, result = null, errorMessage = null)
        strokes.clear()
    }

    fun clear() {
        strokes.clear()
        state = state.copy(result = null, errorMessage = null)
    }

    fun retryLoad() = loadSvg()

    private fun loadSvg() {
        viewModelScope.launch {
            state = state.copy(isLoadingSvg = true, errorMessage = null)
            try {
                val svg = strokeRepo.getSvg(state.charset, state.character)
                android.util.Log.d("WritingSvg", "Fetched SVG for ${state.charset}/${state.character}: ${svg.length} chars")
                state = state.copy(svg = svg, isLoadingSvg = false)
            } catch (e: Exception) {
                android.util.Log.e("WritingSvg", "SVG fetch failed for ${state.charset}/${state.character}", e)
                // Empty SVG means GUIDE / TRACE will show a blank, but FREE_DRAW still works.
                state = state.copy(svg = "", isLoadingSvg = false, errorMessage = e.message ?: "Couldn't load the stroke guide")
            }
        }
    }

    /**
     * Normalize captured strokes to a 512×512 canvas (iOS parity) and POST to /{charset}/evaluate.
     * [canvasWidthPx] + [canvasHeightPx] are the actual on-screen px of the drawing surface,
     * captured via `Modifier.onSizeChanged` in the screen.
     */
    fun submit(canvasWidthPx: Int, canvasHeightPx: Int) {
        if (strokes.isEmpty() || state.isSubmitting) return
        val side = max(canvasWidthPx, canvasHeightPx).coerceAtLeast(1)
        val scale = CANVAS_API_SIDE / side.toDouble()
        val apiStrokes = strokes.map { s ->
            Stroke(
                points = s.points.map {
                    StrokePoint(
                        x = it.x * scale,
                        y = it.y * scale,
                        t = it.tMs.toDouble(),
                        p = it.pressure.toDouble(),
                    )
                },
            )
        }
        val request = EvaluationRequest(
            target = state.character,
            mode = state.mode.apiValue,
            canvasSize = listOf(CANVAS_API_SIDE, CANVAS_API_SIDE),
            strokes = apiStrokes,
        )
        state = state.copy(isSubmitting = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val resp = strokeRepo.evaluate(state.charset, request)
                if (resp.pass) haptics.playCorrect() else haptics.playIncorrect()
                state = state.copy(isSubmitting = false, result = resp)
            } catch (e: Exception) {
                state = state.copy(isSubmitting = false, errorMessage = e.message ?: "Couldn't grade that drawing.")
            }
        }
    }

    class Factory(
        private val character: String,
        private val charset: String,
        private val strokeRepo: StrokeRepository,
        private val haptics: HapticsManager,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WritingPracticeViewModel(character, charset, strokeRepo, haptics) as T
    }

    private companion object {
        const val CANVAS_API_SIDE = 512.0
    }
}
