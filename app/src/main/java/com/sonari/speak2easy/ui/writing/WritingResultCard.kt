package com.sonari.speak2easy.ui.writing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonari.speak2easy.data.remote.dto.ComponentScore
import com.sonari.speak2easy.data.remote.dto.EvaluationResponse
import com.sonari.speak2easy.ui.components.Mascot
import com.sonari.speak2easy.ui.components.MascotImage
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@Composable
fun WritingResultCard(
    result: EvaluationResponse,
    mode: WritingMode,
    modifier: Modifier = Modifier,
    targetCharacter: String? = null,
) {
    val c = SonariTheme.colors
    val statusColor = if (result.pass) c.success else c.error

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        ResultMessage(result = result, mode = mode, targetCharacter = targetCharacter)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
                CircularProgressIndicator(
                    progress = { result.overallScore.coerceIn(0.0, 1.0).toFloat() },
                    color = statusColor,
                    trackColor = c.surfaceSecondary,
                    strokeWidth = 8.dp,
                    modifier = Modifier.size(92.dp),
                )
                Text(
                    "${(result.overallScore * 100).toInt()}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.size(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (result.pass) "Nice!" else "Keep practicing",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                )
                result.components.strokeCount?.let { sc ->
                    val expected = sc.expected ?: 0
                    val got = sc.got ?: 0
                    Text("$got/$expected strokes", style = SonariFonts.monoSmall, color = c.textSecondary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = c.divider)
        Spacer(Modifier.height(12.dp))

        val strokeCountPass = result.components.strokeCount?.passedFor(targetCharacter)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            result.components.strokeCount?.let { Breakdown("Stroke count", it, BreakdownKind.STROKE_COUNT) }
            result.components.strokeOrder?.let {
                Breakdown("Stroke order", it, BreakdownKind.STROKE_ORDER, strokeCountPass = strokeCountPass)
            }
            result.components.strokeShapeDtw?.let {
                Breakdown("Shape", it, BreakdownKind.STROKE_SHAPE, strokeCountPass = strokeCountPass)
            }
            result.components.classifier?.let {
                Breakdown("Prediction", it, BreakdownKind.CHARACTER_MATCH, targetCharacter)
            }
        }

        // Spell out the remaining feedback lines below the breakdown.
        if (result.feedback.size > 1) {
            Spacer(Modifier.size(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                result.feedback.drop(1).forEach {
                    Text(it, style = SonariFonts.monoCaption, color = c.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun ResultMessage(result: EvaluationResponse, mode: WritingMode, targetCharacter: String?) {
    val c = SonariTheme.colors
    val statusColor = if (result.pass) c.success else c.error
    val mascot = when {
        result.pass && mode == WritingMode.FREE_DRAW -> Mascot.Success
        result.pass && mode == WritingMode.TRACE -> Mascot.Practicing
        result.pass -> Mascot.Studying
        else -> Mascot.Thinking
    }
    val title = when {
        result.pass && mode == WritingMode.FREE_DRAW -> "FREE DRAW COMPLETE"
        result.pass && mode == WritingMode.TRACE -> "TRACE COMPLETE"
        result.pass -> "WRITING COMPLETE"
        else -> "KEEP PRACTICING"
    }
    val body = when {
        result.pass && !targetCharacter.isNullOrBlank() && mode == WritingMode.FREE_DRAW ->
            "Strong work. You wrote $targetCharacter without the trace guide."
        result.pass && !targetCharacter.isNullOrBlank() ->
            "Strong work. You wrote $targetCharacter clearly."
        result.pass -> "Strong work. Your writing is looking sharp."
        else -> result.feedback.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "Review the notes below and try again."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(statusColor.copy(alpha = 0.06f))
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MascotImage(mascot = mascot, height = 86.dp)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(title, style = SonariFonts.monoCaption, color = statusColor)
            Text(body, style = SonariFonts.monoSmall, color = c.textSecondary)
        }
    }
}

@Composable
private fun Breakdown(
    label: String,
    score: ComponentScore,
    kind: BreakdownKind,
    targetCharacter: String? = null,
    strokeCountPass: Boolean? = null,
) {
    val c = SonariTheme.colors
    val pass = score.breakdownPassed(kind, targetCharacter, strokeCountPass)
    val passColor = if (pass) c.success else c.error
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = if (pass) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            tint = passColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.size(12.dp))
        Text(label, style = SonariFonts.monoMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text(
            score.breakdownValue(kind),
            style = SonariFonts.monoMedium,
            color = if (pass) c.textSecondary else passColor,
        )
    }
}

private enum class BreakdownKind {
    STROKE_COUNT,
    STROKE_ORDER,
    STROKE_SHAPE,
    CHARACTER_MATCH,
}

private fun ComponentScore.breakdownPassed(
    kind: BreakdownKind,
    targetCharacter: String?,
    strokeCountPass: Boolean?,
): Boolean {
    if (strokeCountPass == false && (kind == BreakdownKind.STROKE_ORDER || kind == BreakdownKind.STROKE_SHAPE)) {
        return false
    }

    val explicit = ok ?: pass ?: correct ?: isCorrect ?: match ?: matches ?: recognizedCorrect
    if (explicit != null) return explicit

    return when (kind) {
        BreakdownKind.STROKE_COUNT -> expected != null && got != null && expected == got
        BreakdownKind.STROKE_ORDER -> (scoreValue ?: 0.0) >= 0.9
        BreakdownKind.STROKE_SHAPE -> (scoreValue ?: 0.0) >= 0.8
        BreakdownKind.CHARACTER_MATCH -> passedFor(targetCharacter) == true
    }
}

private fun ComponentScore.breakdownValue(kind: BreakdownKind): String =
    when (kind) {
        BreakdownKind.STROKE_COUNT -> {
            val expectedText = expected?.toString() ?: "?"
            val gotText = got?.toString() ?: "?"
            "$gotText / $expectedText"
        }
        BreakdownKind.CHARACTER_MATCH -> recognizedLabel ?: formattedScore()
        BreakdownKind.STROKE_ORDER,
        BreakdownKind.STROKE_SHAPE -> formattedScore()
    }

private fun ComponentScore.formattedScore(): String =
    scoreValue?.let { "${(it * 100).toInt()}%" } ?: "—"
