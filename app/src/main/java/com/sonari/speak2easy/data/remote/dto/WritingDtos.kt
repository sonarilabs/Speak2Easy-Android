package com.sonari.speak2easy.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Mirrors iOS `WritingStrokeData.swift`. `SonariJson` (snake_case naming strategy)
 * handles the `canvas_size` ↔ `canvasSize` / `overall_score` ↔ `overallScore` mappings.
 */

@Serializable
data class StrokePoint(
    val x: Double,
    val y: Double,
    val t: Double = 0.0,           // ms since the stroke started
    val p: Double = 1.0,           // 0..1 pressure — defaults to 1 since fingers don't report pressure
)

@Serializable
data class Stroke(val points: List<StrokePoint>)

@Serializable
data class EvaluationRequest(
    val target: String,
    val mode: String,              // "guide" | "trace" | "freedraw"
    val canvasSize: List<Double>,  // [512.0, 512.0]
    val strokes: List<Stroke>,
)

@Serializable
data class ComponentScore(
    // Backend currently uses `ok` on stroke_count; `pass` is reserved for the future.
    val ok: Boolean? = null,
    val pass: Boolean? = null,
    val correct: Boolean? = null,
    val isCorrect: Boolean? = null,
    val match: Boolean? = null,
    val matches: Boolean? = null,
    val recognizedCorrect: Boolean? = null,
    val score: Double? = null,
    val confidence: Double? = null,
    val probability: Double? = null,
    val expected: Int? = null,
    val got: Int? = null,
    val expectedLabel: String? = null,
    val gotLabel: String? = null,
    val target: String? = null,
    val predicted: String? = null,
    val prediction: String? = null,
    val recognized: String? = null,
    val topGuess: String? = null,
    val topPrediction: String? = null,
    val label: String? = null,
    val guess: String? = null,
) {
    /** Numeric score-like value; classifier commonly reports confidence instead of score. */
    val scoreValue: Double?
        get() = score ?: confidence ?: probability

    /** Best recognized label from the classifier-shaped response variants. */
    val recognizedLabel: String?
        get() = listOfNotNull(gotLabel, predicted, prediction, recognized, topGuess, topPrediction, label, guess)
            .firstOrNull { it.isNotBlank() }

    /** Either of the truthy fields the backend may emit. */
    val passed: Boolean?
        get() = passedFor()

    fun passedFor(targetCharacter: String? = null): Boolean? {
        val explicit = ok ?: pass ?: correct ?: isCorrect ?: match ?: matches ?: recognizedCorrect
        if (explicit != null) return explicit

        if (expected != null && got != null) return expected == got

        val expectedText = targetCharacter ?: target ?: expectedLabel
        val recognizedText = recognizedLabel
        if (!expectedText.isNullOrBlank() && !recognizedText.isNullOrBlank()) {
            return recognizedText.trim() == expectedText.trim()
        }

        return scoreValue?.let { it >= 0.7 }
    }
}

@Serializable
data class EvaluationComponents(
    val strokeCount: ComponentScore? = null,
    val strokeOrder: ComponentScore? = null,
    // Backend key is `stroke_shape_dtw`, not `stroke_shape` — snake-case stays in sync via the field name.
    val strokeShapeDtw: ComponentScore? = null,
    val classifier: ComponentScore? = null,
)

@Serializable
data class EvaluationResponse(
    val pass: Boolean = false,
    val overallScore: Double = 0.0,
    val components: EvaluationComponents = EvaluationComponents(),
    // Server returns a list of short hints (e.g. ["Excellent stroke shapes.", "Top guess was 'お'."]).
    val feedback: List<String> = emptyList(),
)
