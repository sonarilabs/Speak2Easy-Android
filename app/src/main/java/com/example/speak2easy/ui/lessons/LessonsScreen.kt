package com.example.speak2easy.ui.lessons

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speak2easy.di.LocalAppContainer
import com.example.speak2easy.model.LessonCategory
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme
import kotlinx.coroutines.launch

@Composable
fun LessonsScreen(onStartPractice: (PracticeSource, PracticeOptions) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: LessonsViewModel = viewModel(factory = LessonsViewModel.Factory(container.lessonRepository))
    val state = viewModel.state
    val c = SonariTheme.colors
    val haptics = container.hapticsManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by container.networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)
    var selected by remember { mutableStateOf<PracticeSource?>(null) }
    var pendingStart by remember { mutableStateOf<Pair<PracticeSource, PracticeOptions>?>(null) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingStart
        pendingStart = null
        if (granted && pending != null) {
            onStartPractice(pending.first, pending.second)
        } else if (!granted) {
            scope.launch { snackbarHostState.showSnackbar("Mic access is needed to practice speaking.") }
        }
    }

    fun preflightAndStart(source: PracticeSource, options: PracticeOptions) {
        if (!isOnline) {
            scope.launch { snackbarHostState.showSnackbar("No internet connection — can't start a lesson.") }
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            onStartPractice(source, options)
        } else {
            pendingStart = source to options
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val currentEmpty = when (state.category) {
        LessonCategory.WORDS -> state.wordSections.isEmpty()
        LessonCategory.HIRAGANA -> state.hiraganaUnlocked.isEmpty()
        LessonCategory.KATAKANA -> state.katakanaUnlocked.isEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("LESSONS", style = SonariFonts.monoLarge, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp))
            Spacer(Modifier.height(12.dp))
            CategorySelector(state.category) { viewModel.selectCategory(it) }
            Spacer(Modifier.height(4.dp))
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && currentEmpty -> {
                    CircularProgressIndicator(color = c.accent, modifier = Modifier.align(Alignment.Center))
                }
                state.error != null && currentEmpty -> {
                    Text(
                        state.error ?: "",
                        style = SonariFonts.monoSmall,
                        color = c.error,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                else -> LessonsGrid(state) { source ->
                    haptics.playSelection()
                    selected = source
                }
            }
        }
    }

    selected?.let { source ->
        LessonOptionsSheet(
            source = source,
            onDismiss = { selected = null },
            onStart = { options ->
                selected = null
                preflightAndStart(source, options)
            },
        )
    }

    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.fillMaxWidth())
}

/**
 * Two-column grid for lesson tiles; full-width spans for section headers and word-group rows.
 * Single top-level [LazyVerticalGrid] avoids nested-lazy crashes and keeps one scrollable surface.
 */
@Composable
private fun LessonsGrid(state: LessonsUiState, onSelect: (PracticeSource) -> Unit) {
    val c = SonariTheme.colors
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.category == LessonCategory.WORDS) {
            val accent = c.accentFor(LessonCategory.WORDS)
            state.wordSections.forEach { section ->
                item(key = "section-${section.title}", span = { GridItemSpan(2) }) {
                    SectionHeader(section.title, section.subtitle)
                }
                items(section.groups, key = { it.groupLabel }) { group ->
                    WordGroupCard(group, accent, enabled = state.wordsAccessible) {
                        onSelect(PracticeSource.WordGroup(group))
                    }
                }
            }
        } else {
            val hira = state.category == LessonCategory.HIRAGANA
            val groups = if (hira) LessonData.hiraganaLessons else LessonData.katakanaLessons
            val progress = if (hira) state.hiraganaProgress else state.katakanaProgress
            val unlocked = if (hira) state.hiraganaUnlocked else state.katakanaUnlocked
            val accent = c.accentFor(state.category)
            val script = if (hira) JapaneseScript.HIRAGANA else JapaneseScript.KATAKANA
            groups.forEach { group ->
                item(key = "section-${script.value}-${group.title}", span = { GridItemSpan(2) }) {
                    SectionHeader(group.title, group.subtitle)
                }
                items(group.lessons, key = { "${script.value}-${it.number}" }) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        accent = accent,
                        progress = progress[lesson.progressKey] ?: 0f,
                        unlocked = unlocked[lesson.progressKey] ?: false,
                    ) { onSelect(PracticeSource.Lesson(lesson, script)) }
                }
            }
        }
    }
}
