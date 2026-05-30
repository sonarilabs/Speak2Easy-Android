package com.example.speak2easy.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speak2easy.data.remote.dto.PracticeSessionSummary
import com.example.speak2easy.di.LocalAppContainer
import com.example.speak2easy.ui.lessons.formatWordGroupTitle
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

@Composable
fun ProgressScreen(onSessionClick: (String) -> Unit = {}) {
    val container = LocalAppContainer.current
    val viewModel: ProgressViewModel = viewModel(
        factory = ProgressViewModel.Factory(container.progressRepository, container.authRepository),
    )
    val state = viewModel.state
    val c = SonariTheme.colors
    val haptics = container.hapticsManager

    Column(modifier = Modifier.fillMaxSize().background(c.background).padding(horizontal = 20.dp)) {
        Text("PROGRESS", style = SonariFonts.monoLarge, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = c.accent, modifier = Modifier.align(Alignment.Center))
            }
            return
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            val stats = state.progress?.stats
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("ACCURACY", "${(stats?.avgAccuracy ?: 0.0).toInt()}%", c.accent, Modifier.weight(1f))
                StatCard("STREAK", "${state.progress?.streak?.streakDays ?: 0}", c.accentSecondary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("SESSIONS", "${state.sessions.size}", c.success, Modifier.weight(1f))
                StatCard("ATTEMPTS", "${stats?.totalAttempts ?: 0}", c.accent, Modifier.weight(1f))
            }

            Spacer(Modifier.height(28.dp))
            Text("RECENT SESSIONS", style = SonariFonts.monoCaption, color = c.textSecondary)
            Spacer(Modifier.height(8.dp))

            if (state.sessions.isEmpty()) {
                Text(
                    "Complete a lesson to see your sessions here.",
                    style = SonariFonts.monoSmall,
                    color = c.textTertiary,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                state.sessions.forEach { summary ->
                    SessionRow(summary) {
                        haptics.playSelection()
                        onSessionClick(summary.sessionId)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    val c = SonariTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = SonariFonts.monoCaption, color = c.textSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

/**
 * iOS-parity row: accuracy ring on the left, title + "mastered + duration" in the middle,
 * relative-time label on the right.
 */
@Composable
private fun SessionRow(session: PracticeSessionSummary, onClick: () -> Unit) {
    val c = SonariTheme.colors
    val accuracy = ((session.accuracyRate ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val ringColor = if (accuracy >= 1f) c.success else c.accent
    val correct = session.uniqueCorrect ?: session.correctCount ?: 0
    val total = session.uniqueTotal ?: session.totalItems ?: 0
    val durationLabel = formatDuration(session.durationSeconds ?: 0)
    val timeLabel = relativeTimeLabel(session.endedAt ?: session.startedAt)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
            CircularProgressIndicator(
                progress = { accuracy },
                color = ringColor,
                trackColor = c.surfaceSecondary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(56.dp),
            )
            Text(
                "${(accuracy * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                sessionTitle(session),
                style = SonariFonts.monoSmall,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$correct/$total mastered  $durationLabel",
                style = SonariFonts.monoTiny,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (timeLabel.isNotEmpty()) {
            Text(timeLabel, style = SonariFonts.monoTiny, color = c.textTertiary)
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s}s"
}

/** "now" / "Xm ago" / "Xh ago" / "Xd ago" / "MMM d" — mirrors the iOS relative format. */
private fun relativeTimeLabel(iso: String?): String {
    if (iso.isNullOrEmpty()) return ""
    return try {
        val instant = Instant.parse(iso)
        val diff = Duration.between(instant, Instant.now()).seconds
        when {
            diff < 60 -> "now"
            diff < 3600 -> "${diff / 60}m ago"
            diff < 86400 -> "${diff / 3600}h ago"
            diff < 7 * 86400 -> "${diff / 86400}d ago"
            else -> LocalDate.ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (_: Exception) {
        ""
    }
}

private fun sessionTitle(session: PracticeSessionSummary): String {
    session.groupLabel?.takeIf { it.isNotEmpty() }?.let { return formatWordGroupTitle(it) }
    session.lessonNumber?.let { num ->
        val script = session.lessonCharsetDisplayName?.substringBefore(" ")
        return if (!script.isNullOrEmpty()) "$script · Lesson $num" else "Lesson $num"
    }
    session.lessonCharsetDisplayName?.takeIf { it.isNotEmpty() }?.let { return it }
    return if (session.sessionType == "drill") "Vocab Practice" else session.sessionType?.replaceFirstChar(Char::uppercaseChar) ?: "Practice"
}
