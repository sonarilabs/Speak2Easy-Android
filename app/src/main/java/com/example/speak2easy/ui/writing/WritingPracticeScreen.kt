package com.example.speak2easy.ui.writing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speak2easy.di.LocalAppContainer
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

@Composable
fun WritingPracticeScreen(
    charset: String,
    character: String,
    previousCharacter: String? = null,
    nextCharacter: String? = null,
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onExit: () -> Unit,
) {
    BackHandler { onExit() }
    val container = LocalAppContainer.current
    val viewModel: WritingPracticeViewModel = viewModel(
        factory = WritingPracticeViewModel.Factory(character, charset, container.strokeRepository, container.hapticsManager),
    )
    val state = viewModel.state
    val c = SonariTheme.colors
    val expectedStrokes = remember(character) { KanaStrokeCounts.countFor(character) ?: 0 }
    val romanization = remember(character) { KanaStrokeCounts.romanizationFor(character) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var canvasSizePx by remember { mutableStateOf(IntArray(2) { 1 }) }
    val scrollState = rememberScrollState()

    // Auto-scroll to result when it appears.
    LaunchedEffect(state.result) {
        if (state.result != null) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
    ) {
        // Header: EXIT chevron + target character.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { showExitConfirm = true }.padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Exit", tint = c.accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("EXIT", style = SonariFonts.monoCaption, color = c.accent)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (romanization != null) "$character · $romanization" else character,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Spacer(Modifier.weight(1f))
            // Counter slot kept empty so the header stays symmetrical with PracticeScreen.
            Spacer(Modifier.size(40.dp))
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavButton(
                label = "Previous",
                leading = true,
                enabled = previousCharacter != null && onPrevious != null,
                onClick = { onPrevious?.invoke() },
                modifier = Modifier.weight(1f),
            )
            NavButton(
                label = "Next",
                leading = false,
                enabled = nextCharacter != null && onNext != null,
                onClick = { onNext?.invoke() },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            character,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(16.dp))

        ModeRow(state.mode, onChange = viewModel::setMode)

        Spacer(Modifier.height(16.dp))

        // Square drawing surface — white background regardless of theme (matches iOS).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .onSizeChanged { canvasSizePx = intArrayOf(it.width, it.height) },
            contentAlignment = Alignment.Center,
        ) {
            if (state.mode != WritingMode.GUIDE) {
                WritingGuideLines(modifier = Modifier.fillMaxSize())
            }
            when {
                state.isLoadingSvg -> CircularProgressIndicator(color = c.accent)
                else -> {
                    val svg = state.svg
                    val needsSvg = state.mode != WritingMode.FREE_DRAW
                    if (needsSvg && !svg.isNullOrEmpty()) {
                        WritingSvgView(svg = svg, mode = state.mode, modifier = Modifier.fillMaxSize().padding(6.dp))
                    } else if (needsSvg) {
                        // SVG fetch failed — surface that explicitly so the user knows.
                        Text(
                            "Stroke guide unavailable.\nTry FREE mode or check your connection.",
                            style = SonariFonts.monoCaption,
                            color = Color(0xFF666666),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    if (state.mode != WritingMode.GUIDE) {
                        WritingCanvas(
                            strokes = viewModel.strokes,
                            enabled = state.result == null && !state.isSubmitting,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            if (state.mode != WritingMode.FREE_DRAW && expectedStrokes > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF4F4F5))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "$expectedStrokes strokes",
                        style = SonariFonts.monoCaption,
                        color = Color(0xFF475569),
                    )
                }
            }
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = SonariFonts.monoCaption, color = c.error)
        }

        Spacer(Modifier.height(16.dp))

        // CLEAR + SUBMIT row (CLEAR hidden in GUIDE mode where there's no canvas).
        if (state.mode != WritingMode.GUIDE) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionButton(
                    label = "CLEAR",
                    color = c.textSecondary,
                    outlined = true,
                    enabled = viewModel.strokes.isNotEmpty() && !state.isSubmitting,
                    onClick = viewModel::clear,
                    modifier = Modifier.weight(1f),
                )
                ActionButton(
                    label = if (state.isSubmitting) "GRADING…" else "SUBMIT",
                    color = c.accent,
                    outlined = false,
                    enabled = viewModel.strokes.isNotEmpty() && !state.isSubmitting && state.result == null,
                    onClick = { viewModel.submit(canvasSizePx[0], canvasSizePx[1]) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        state.result?.let { result ->
            Spacer(Modifier.height(16.dp))
            WritingResultCard(result = result, mode = state.mode, targetCharacter = state.character)
            Spacer(Modifier.height(12.dp))
            ActionButton(
                label = "TRY AGAIN",
                color = c.accent,
                outlined = false,
                enabled = true,
                onClick = viewModel::clear,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onExit() }) { Text("Exit", color = c.error) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Stay", color = c.textSecondary) }
            },
            title = { Text("Exit writing?", style = SonariFonts.monoMedium, color = c.textPrimary) },
            text = { Text("Your current drawing won't be saved.", style = SonariFonts.monoSmall, color = c.textSecondary) },
            containerColor = c.surfacePrimary,
        )
    }
}

@Composable
private fun NavButton(
    label: String,
    leading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SonariTheme.colors
    val alpha = if (enabled) 1f else 0.35f
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.accent.copy(alpha = 0.16f * alpha))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = c.accent.copy(alpha = alpha), modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(4.dp))
        }
        Text(label, style = SonariFonts.monoSmall, color = c.accent.copy(alpha = alpha))
        if (!leading) {
            Spacer(Modifier.size(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = c.accent.copy(alpha = alpha), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun WritingGuideLines(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.5.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx()), 0f)
        val color = Color(0xFFCBD5E1).copy(alpha = 0.7f)
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = stroke,
            pathEffect = dash,
        )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = stroke,
            pathEffect = dash,
        )
    }
}

@Composable
private fun ModeRow(selected: WritingMode, onChange: (WritingMode) -> Unit) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceSecondary)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WritingMode.entries.forEach { mode ->
            val active = mode == selected
            Text(
                text = mode.label,
                style = SonariFonts.monoCaption,
                color = if (active) c.buttonText else c.textSecondary,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) c.accent else Color.Transparent)
                    .clickable { onChange(mode) }
                    .padding(vertical = 10.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    color: Color,
    outlined: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SonariTheme.colors
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .let { base ->
                if (outlined) base.border(1.5.dp, color.copy(alpha = alpha * 0.6f), RoundedCornerShape(12.dp))
                else base.background(color.copy(alpha = alpha))
            }
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = SonariFonts.monoSmall,
            color = if (outlined) color.copy(alpha = alpha) else c.buttonText,
        )
    }
}
