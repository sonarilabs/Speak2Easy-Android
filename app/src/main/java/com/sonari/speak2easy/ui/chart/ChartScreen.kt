package com.sonari.speak2easy.ui.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.data.remote.dto.ContentItem
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.components.QuickGuideSheet
import com.sonari.speak2easy.ui.lessons.JapaneseScript
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@Composable
fun ChartScreen() {
    val container = LocalAppContainer.current
    val viewModel: ChartViewModel = viewModel(
        factory = ChartViewModel.Factory(container.lessonRepository, container.applicationContext),
    )
    val state = viewModel.state
    val c = SonariTheme.colors
    val haptics = container.hapticsManager

    var showGuide by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(c.background).padding(horizontal = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("KANA CHART", style = SonariFonts.monoLarge, color = c.textPrimary)
            Spacer(Modifier.size(8.dp))
            // Info icon → Quick Guide sheet explaining Gojuon (50 sounds).
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        haptics.playSelection()
                        showGuide = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = "About the chart", tint = c.accent, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        ScriptToggle(state.script) { viewModel.selectScript(it) }
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(color = c.accent, modifier = Modifier.align(Alignment.Center))
                state.error != null -> Text(state.error ?: "", style = SonariFonts.monoSmall, color = c.error, modifier = Modifier.align(Alignment.Center))
                // Adaptive columns: ~5 cells across on a phone (80dp × 5 ≈ 400dp), more on
                // tablets where Fixed(5) used to give absurd 350-dp tiles. Min size is bigger
                // than Writing's grid (64dp) so Chart cells stay visually distinct.
                else -> LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 80.dp), modifier = Modifier.fillMaxSize()) {
                    items(state.items, key = { it.contentId }) { item ->
                        KanaCell(item) {
                            haptics.playSelection()
                            viewModel.speak(item.contentText)
                        }
                    }
                }
            }
        }
    }

    if (showGuide) {
        QuickGuideSheet(
            title = "Gojūon (五十音)",
            body = "The \"fifty sounds\" — the core set of characters in Japanese phonetic scripts. " +
                "They're organized in a grid with consonant rows (k, s, t, n…) and vowel columns " +
                "(a, i, u, e, o). This is the foundation of Japanese pronunciation.\n\n" +
                "Tap any kana to hear it pronounced.",
            onDismiss = { showGuide = false },
        )
    }
}

@Composable
private fun ScriptToggle(selected: JapaneseScript, onSelect: (JapaneseScript) -> Unit) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfaceSecondary)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        JapaneseScript.entries.forEach { script ->
            val active = script == selected
            Text(
                text = script.displayName.uppercase(),
                style = SonariFonts.monoCaption,
                color = if (active) c.buttonText else c.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) c.accent else Color.Transparent)
                    .clickable { onSelect(script) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun KanaCell(item: ContentItem, onTap: () -> Unit) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(c.cardBackground)
            .border(1.dp, c.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onTap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(item.contentText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Text(item.romanization, style = SonariFonts.monoTiny, color = c.textSecondary)
    }
}
