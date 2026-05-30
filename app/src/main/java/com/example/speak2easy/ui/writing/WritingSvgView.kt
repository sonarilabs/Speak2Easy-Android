package com.example.speak2easy.ui.writing

import android.graphics.Path as AndroidPath
import android.graphics.PathMeasure
import android.graphics.RectF
import android.util.Log
import android.util.Xml
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.core.graphics.PathParser
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import kotlin.math.max
import kotlin.math.min

private const val TAG = "WritingSvg"
private const val DEFAULT_STROKE_DURATION_SECONDS = 0.8f
private const val SVG_STROKE_WIDTH = 128f

/**
 * Renders AnimCJK stroke SVGs directly in Compose.
 *
 * Android WebView was unreliable for these inline SVG payloads: the document loaded
 * successfully but frequently reported a zero-height page and painted nothing. The SVGs
 * have a predictable structure, so we parse the fill paths, clip paths, and animated
 * stroke paths, then draw the same guide/trace behavior on a Canvas.
 */
@Composable
fun WritingSvgView(svg: String, mode: WritingMode, modifier: Modifier = Modifier) {
    val parsed = remember(svg) { parseAnimCjkSvg(svg) }
    val totalSeconds = parsed.totalDurationSeconds
    val progressSeconds = remember(svg, mode) { Animatable(0f) }

    LaunchedEffect(svg, mode, totalSeconds) {
        if (mode == WritingMode.GUIDE && totalSeconds > 0f) {
            progressSeconds.snapTo(0f)
            progressSeconds.animateTo(
                targetValue = totalSeconds,
                animationSpec = tween(
                    durationMillis = (totalSeconds * 1000).toInt(),
                    easing = LinearEasing,
                ),
            )
        } else {
            // In TRACE or FREE_DRAW, we don't need the animation progress for strokes.
            progressSeconds.snapTo(0f)
        }
    }

    Canvas(modifier = modifier) {
        if (parsed.fillPaths.isEmpty() && parsed.strokes.isEmpty()) {
            return@Canvas
        }
        drawParsedSvg(
            svg = parsed,
            mode = mode,
            progressSeconds = progressSeconds.value,
        )
    }
}

private fun DrawScope.drawParsedSvg(
    svg: ParsedAnimCjkSvg,
    mode: WritingMode,
    progressSeconds: Float,
) {
    val viewBox = svg.viewBox
    val scale = min(size.width / viewBox.width(), size.height / viewBox.height())
    val tx = (size.width - viewBox.width() * scale) / 2f - viewBox.left * scale
    val ty = (size.height - viewBox.height() * scale) / 2f - viewBox.top * scale

    withTransform({
        translate(tx, ty)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        // Draw the static guide character shape.
        // TRACE: Constant faint guide.
        // GUIDE: Starts as guide, then stays visible but lighter as strokes animate.
        val guideAlpha = when (mode) {
            WritingMode.TRACE -> 0.40f
            WritingMode.GUIDE -> 0.40f
            else -> 0f
        }

        if (guideAlpha > 0f) {
            drawPath(path = svg.combinedPath, color = Color(0xFFCCCCCC).copy(alpha = guideAlpha))
        }

        // Draw animated strokes (only in GUIDE mode).
        if (mode == WritingMode.GUIDE) {
            svg.strokes.forEach { stroke ->
                val fraction = ((progressSeconds - stroke.delaySeconds) / stroke.durationSeconds).coerceIn(0f, 1f)
                if (fraction <= 0f) return@forEach

                val pathToDraw = if (fraction >= 1f) {
                    stroke.path.path
                } else {
                    stroke.path.androidPath.segment(fraction).asComposePath()
                }

                val drawStroke = {
                    drawPath(
                        path = pathToDraw,
                        color = Color.Black,
                        style = Stroke(width = SVG_STROKE_WIDTH, cap = StrokeCap.Round),
                    )
                }

                if (stroke.clipPath != null) {
                    clipPath(stroke.clipPath) { drawStroke() }
                } else {
                    drawStroke()
                }
            }
        }
    }
}

private fun parseAnimCjkSvg(svg: String): ParsedAnimCjkSvg {
    val parser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(StringReader(svg))
    }
    val pathsById = linkedMapOf<String, ParsedSvgPath>()
    val clipPathTargets = mutableMapOf<String, String>()
    val strokes = mutableListOf<ParsedSvgStroke>()
    var viewBox = RectF(0f, 0f, 1024f, 1024f)
    var currentClipPathId: String? = null

    runCatching {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "svg" -> viewBox = parser.attr("viewBox")?.parseViewBox() ?: viewBox
                        "clipPath" -> currentClipPathId = parser.attr("id")
                        "use" -> {
                            val clipId = currentClipPathId
                            val href = parser.attr("href") ?: parser.attr("xlink:href")
                            if (clipId != null && href != null) {
                                clipPathTargets[clipId] = href.removePrefix("#")
                            }
                        }
                        "path" -> {
                            val id = parser.attr("id")
                            val pathData = parser.attr("d")
                            val clipPathRef = parser.attr("clip-path")?.extractClipPathId()
                            if (pathData != null) {
                                val androidPath = PathParser.createPathFromPathData(pathData)
                                val parsedPath = ParsedSvgPath(androidPath, androidPath.asComposePath())
                                if (id != null) {
                                    pathsById[id] = parsedPath
                                } else if (clipPathRef != null) {
                                    strokes += ParsedSvgStroke(
                                        path = parsedPath,
                                        clipPathId = clipPathRef,
                                        delaySeconds = parser.attr("style")?.parseDelaySeconds() ?: 0f,
                                        durationSeconds = DEFAULT_STROKE_DURATION_SECONDS,
                                    )
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "clipPath") currentClipPathId = null
                }
            }
            event = parser.next()
        }
    }.onFailure { error ->
        Log.e(TAG, "Unable to parse AnimCJK SVG", error)
    }

    val resolvedStrokes = strokes.map { stroke ->
        val clipPath = clipPathTargets[stroke.clipPathId]?.let(pathsById::get)?.path
        stroke.copy(clipPath = clipPath)
    }
    Log.d(TAG, "Parsed SVG: fills=${pathsById.size}, strokes=${resolvedStrokes.size}, total=${resolvedStrokes.maxOfOrNull { it.delaySeconds + it.durationSeconds } ?: 0f}s")

    return ParsedAnimCjkSvg(
        viewBox = viewBox,
        fillPaths = pathsById.values.map { it.path },
        strokes = resolvedStrokes,
    )
}

private fun XmlPullParser.attr(name: String): String? {
    for (i in 0 until attributeCount) {
        if (getAttributeName(i) == name) return getAttributeValue(i)
    }
    return null
}

private fun String.parseViewBox(): RectF? {
    val values = trim().split(Regex("\\s+|,")).mapNotNull { it.toFloatOrNull() }
    if (values.size != 4) return null
    return RectF(values[0], values[1], values[0] + values[2], values[1] + values[3])
}

private fun String.extractClipPathId(): String? =
    substringAfter("url(#", missingDelimiterValue = "")
        .substringBefore(")")
        .takeIf { it.isNotBlank() }

private fun String.parseDelaySeconds(): Float? {
    val delay = Regex("--d\\s*:\\s*([0-9.]+)s").find(this)?.groupValues?.getOrNull(1)
    return delay?.toFloatOrNull()
}

private fun AndroidPath.segment(fraction: Float): AndroidPath {
    val targetLength = totalLength() * fraction.coerceIn(0f, 1f)
    val result = AndroidPath()
    val measure = PathMeasure(this, false)
    var remaining = targetLength
    do {
        val length = measure.length
        val drawLength = min(remaining, length)
        if (drawLength > 0f) measure.getSegment(0f, drawLength, result, true)
        remaining = max(0f, remaining - length)
    } while (remaining > 0f && measure.nextContour())
    return result
}

private fun AndroidPath.totalLength(): Float {
    val measure = PathMeasure(this, false)
    var total = 0f
    do {
        total += measure.length
    } while (measure.nextContour())
    return total
}

private data class ParsedAnimCjkSvg(
    val viewBox: RectF,
    val fillPaths: List<Path>,
    val strokes: List<ParsedSvgStroke>,
) {
    val totalDurationSeconds: Float = strokes.maxOfOrNull { it.delaySeconds + it.durationSeconds } ?: 0f

    /** A single path combining all fills to avoid intersection alpha artifacts. */
    val combinedPath: Path by lazy {
        Path().apply {
            fillPaths.forEach { addPath(it) }
        }
    }
}

private data class ParsedSvgPath(
    val androidPath: AndroidPath,
    val path: Path,
)

private data class ParsedSvgStroke(
    val path: ParsedSvgPath,
    val clipPathId: String,
    val delaySeconds: Float,
    val durationSeconds: Float,
    val clipPath: Path? = null,
)
