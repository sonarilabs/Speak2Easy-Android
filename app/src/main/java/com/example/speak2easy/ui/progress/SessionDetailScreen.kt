package com.example.speak2easy.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speak2easy.data.remote.dto.PracticeAttemptDetail
import com.example.speak2easy.data.remote.dto.SkippedItem
import com.example.speak2easy.di.LocalAppContainer
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

/**
 * Per-session breakdown: correct/incorrect/skipped buckets (deduped first-attempt-wins).
 * Tapping an incorrect row opens a drill sheet with the feedback text.
 * Mirrors iOS `Views/Progress/SessionDetailView.swift`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(sessionId: String, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: SessionDetailViewModel = viewModel(
        factory = SessionDetailViewModel.Factory(container.progressRepository, sessionId),
    )
    val state = viewModel.state
    val c = SonariTheme.colors
    val haptics = container.hapticsManager
    var drillTarget by remember { mutableStateOf<PracticeAttemptDetail?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(8.dp))
            Text("BACK", style = SonariFonts.monoCaption, color = c.accent)
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = c.accent)
            }
            state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(state.error!!, style = SonariFonts.monoSmall, color = c.error, modifier = Modifier.padding(24.dp))
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                ) {
                    item { HeaderCard(accuracyPercent = state.accuracyPercent, totalItems = state.total) }
                    item { Spacer(Modifier.height(16.dp)) }
                    item { StatsPills(correct = state.correct.size, incorrect = state.incorrect.size, skipped = state.skipped.size) }

                    if (state.correct.isNotEmpty()) {
                        item { Spacer(Modifier.height(24.dp)) }
                        item { SectionLabel("CORRECT (${state.correct.size})", c.success) }
                        items(state.correct, key = { "c-${it.attemptId}" }) { attempt ->
                            AttemptRow(attempt, accent = c.success, glyph = "✓", onClick = null)
                        }
                    }

                    if (state.incorrect.isNotEmpty()) {
                        item { Spacer(Modifier.height(24.dp)) }
                        item { SectionLabel("INCORRECT (${state.incorrect.size})", c.error) }
                        items(state.incorrect, key = { "x-${it.attemptId}" }) { attempt ->
                            AttemptRow(attempt, accent = c.error, glyph = "✕", onClick = {
                                haptics.playSelection()
                                drillTarget = attempt
                            })
                        }
                    }

                    if (state.skipped.isNotEmpty()) {
                        item { Spacer(Modifier.height(24.dp)) }
                        item { SectionLabel("SKIPPED (${state.skipped.size})", c.textSecondary) }
                        items(state.skipped, key = { "s-${it.contentId}" }) { skip ->
                            SkippedRow(skip)
                        }
                    }
                }
            }
        }
    }

    drillTarget?.let { target ->
        ModalBottomSheet(onDismissRequest = { drillTarget = null }, containerColor = c.surfacePrimary) {
            IncorrectDrillContent(target)
        }
    }
}

@Composable
private fun HeaderCard(accuracyPercent: Int, totalItems: Int) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
            CircularProgressIndicator(
                progress = { (accuracyPercent / 100f).coerceIn(0f, 1f) },
                color = c.accent,
                trackColor = c.surfaceSecondary,
                strokeWidth = 8.dp,
                modifier = Modifier.size(96.dp),
            )
            Text("$accuracyPercent%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = c.textPrimary)
        }
        Spacer(Modifier.size(20.dp))
        Column {
            Text("FIRST-TRY ACCURACY", style = SonariFonts.monoCaption, color = c.textSecondary)
            Text("$totalItems items", style = SonariFonts.monoMedium, color = c.textPrimary, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun StatsPills(correct: Int, incorrect: Int, skipped: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val c = SonariTheme.colors
        StatPill("Correct", correct, c.success, Modifier.weight(1f))
        StatPill("Incorrect", incorrect, c.error, Modifier.weight(1f))
        if (skipped > 0) StatPill("Skipped", skipped, c.textSecondary, Modifier.weight(1f))
    }
}

@Composable
private fun StatPill(label: String, value: Int, accent: Color, modifier: Modifier = Modifier) {
    val c = SonariTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 14.dp),
    ) {
        Text("$value", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent)
        Text(label.uppercase(), style = SonariFonts.monoCaption, color = c.textSecondary, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun SectionLabel(label: String, accent: Color) {
    Text(
        label,
        style = SonariFonts.monoCaption,
        color = accent,
        modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun AttemptRow(attempt: PracticeAttemptDetail, accent: Color, glyph: String, onClick: (() -> Unit)?) {
    val c = SonariTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(attempt.contentText ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(attempt.romanization ?: "", style = SonariFonts.monoSmall, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            attempt.feedback?.takeIf { it.isNotBlank() && onClick != null }?.let {
                Text(it, style = SonariFonts.monoTiny, color = c.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Text(glyph, style = SonariFonts.monoLarge, color = accent)
    }
}

@Composable
private fun SkippedRow(item: SkippedItem) {
    val c = SonariTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(item.contentText ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Spacer(Modifier.size(12.dp))
        Text(item.romanization ?: "", style = SonariFonts.monoSmall, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text("↷", style = SonariFonts.monoLarge, color = c.textTertiary)
    }
}

@Composable
private fun IncorrectDrillContent(attempt: PracticeAttemptDetail) {
    val c = SonariTheme.colors
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text("WHAT YOU SAID", style = SonariFonts.monoCaption, color = c.error)
        Spacer(Modifier.height(12.dp))
        Text(attempt.contentText ?: "", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
        attempt.romanization?.let {
            Text(it, style = SonariFonts.monoMedium, color = c.textSecondary, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(20.dp))
        attempt.feedback?.takeIf { it.isNotBlank() }?.let {
            Text("FEEDBACK", style = SonariFonts.monoCaption, color = c.textSecondary)
            Text(it, style = SonariFonts.monoSmall, color = c.textPrimary, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(16.dp))
        }
        attempt.matchScore?.let {
            Text("MATCH SCORE", style = SonariFonts.monoCaption, color = c.textSecondary)
            Text("${(it * 100).toInt()}%", style = SonariFonts.monoLarge, color = c.accent, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(16.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}
