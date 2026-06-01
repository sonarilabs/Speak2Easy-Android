package com.sonari.speak2easy.ui.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonari.speak2easy.model.LessonCategory
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@Composable
fun CategorySelector(selected: LessonCategory, onSelect: (LessonCategory) -> Unit) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceSecondary)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LessonCategory.entries.forEach { category ->
            val active = category == selected
            val accent = c.accentFor(category)
            Text(
                text = category.displayName.uppercase(),
                style = SonariFonts.monoCaption,
                color = if (active) c.buttonText else c.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) accent else Color.Transparent)
                    .clickable { onSelect(category) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    val c = SonariTheme.colors
    Column(modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)) {
        Text(title.uppercase(), style = SonariFonts.monoCaption, color = c.textSecondary)
        if (subtitle.isNotEmpty()) {
            Text(subtitle, style = SonariFonts.monoTiny, color = c.textTertiary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun LessonCard(
    lesson: LessonInfo,
    accent: Color,
    progress: Float,
    unlocked: Boolean,
    /** Override the lesson's title (e.g. "Hiragana Vowels" vs. bare "Vowels"). */
    displayTitle: String = lesson.title,
    onClick: () -> Unit,
) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.cardBackground)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .let { if (unlocked) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LESSON ${lesson.number}", style = SonariFonts.monoTiny, color = accent)
            Spacer(Modifier.weight(1f))
            if (!unlocked) {
                Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = c.textTertiary, modifier = Modifier.size(16.dp))
            } else if (progress >= 1f) {
                Text("DONE", style = SonariFonts.monoTiny, color = c.success)
            }
        }
        Text(
            displayTitle,
            style = SonariFonts.monoMedium,
            color = if (unlocked) c.textPrimary else c.textTertiary,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            lesson.subtitle,
            style = SonariFonts.monoSmall,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (unlocked && progress > 0f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = accent,
                trackColor = c.surfaceSecondary,
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

/**
 * Vertical card sized to live in a 2-column grid cell. Title gets the full card width
 * (badge moved to the bottom row) so longer titles like "Combined Sounds" fit on one line.
 * Auto-shrinks the title font for the longest entries.
 */
@Composable
fun WordGroupCard(
    group: WordGroupInfo,
    accent: Color,
    enabled: Boolean,
    progress: Float = 0f,
    onClick: () -> Unit,
) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.cardBackground)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(14.dp),
    ) {
        // Title on its own row, full width, single line — shrinks for the longest titles.
        Text(
            group.title,
            fontSize = if (group.title.length > 12) 13.sp else 15.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = if (enabled) c.textPrimary else c.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            group.subtitle,
            style = SonariFonts.monoTiny,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        // Middle row: word count on the left, status pill on the right (lock / DONE / kana badge).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(
                "${group.itemCount} WORDS",
                style = SonariFonts.monoTiny,
                color = if (enabled) accent else c.textTertiary,
                modifier = Modifier.weight(1f),
            )
            when {
                !enabled -> Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = c.textTertiary, modifier = Modifier.size(16.dp))
                progress >= 1f -> Text("DONE", style = SonariFonts.monoTiny, color = c.success)
                else -> Text(group.charsetBadge(), style = SonariFonts.monoTiny, color = accent)
            }
        }
        // Progress bar — same treatment as LessonCard. Only renders when the user has made
        // some progress on this group (matches the backend's `completed_items > 0` signal).
        if (enabled && progress > 0f) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                color = accent,
                trackColor = c.surfaceSecondary,
                drawStopIndicator = {},
                gapSize = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

private fun WordGroupInfo.charsetBadge(): String = when (charset) {
    JapaneseScript.HIRAGANA -> "あ"
    JapaneseScript.KATAKANA -> "ア"
    null -> "語"
}
