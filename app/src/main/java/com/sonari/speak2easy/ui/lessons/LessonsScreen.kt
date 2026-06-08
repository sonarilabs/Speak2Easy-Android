package com.sonari.speak2easy.ui.lessons

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.model.LessonCategory
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme
import com.sonari.speak2easy.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

@Composable
fun LessonsScreen(
    onStartPractice: (PracticeSource, PracticeOptions) -> Unit,
    onReportBug: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: LessonsViewModel = viewModel(
        factory = LessonsViewModel.Factory(container.lessonRepository),
    )
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory(container.preferences))
    val theme by themeViewModel.theme.collectAsStateWithLifecycle()
    val state = viewModel.state
    val c = SonariTheme.colors
    val haptics = container.hapticsManager
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by container.networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)

    // Re-fetch the current category whenever the user lands on Lessons (tab switch, returning
    // from a practice session, etc.). The unlock flag in the response is computed server-side, so
    // a stale snapshot was why "lesson 2 didn't unlock" after the user passed 80% on lesson 1.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.reloadCurrent()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
        LessonCategory.SENTENCES -> state.sentenceLessons.isEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("LESSONS", style = SonariFonts.monoLarge, color = c.textPrimary, modifier = Modifier.weight(1f))
                HeaderIconButton(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = "Help & Report",
                ) {
                    haptics.playSelection()
                    onReportBug()
                }
                Spacer(Modifier.width(8.dp))
                HeaderIconButton(
                    icon = if (theme.isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                    contentDescription = "Toggle theme",
                ) {
                    haptics.playSelection()
                    themeViewModel.toggle()
                }
            }
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
                    // iOS semantics: a word group is locked only when the API explicitly
                    // returns isUnlocked = false for it. Missing key = treat as unlocked.
                    // Category-level gate (`wordsAccessible`) still overrides — if the user
                    // hasn't unlocked the Words category at all, every group is disabled.
                    val groupUnlocked = state.wordGroupUnlocked[group.groupLabel] != false
                    WordGroupCard(
                        group = group,
                        accent = accent,
                        enabled = state.wordsAccessible && groupUnlocked,
                        progress = state.wordGroupProgress[group.groupLabel] ?: 0f,
                    ) { onSelect(PracticeSource.WordGroup(group)) }
                }
            }
        } else if (state.category == LessonCategory.SENTENCES) {
            val accent = c.accentFor(LessonCategory.SENTENCES)
            item(key = "section-sentences", span = { GridItemSpan(2) }) {
                SectionHeader("Sentences", "Speak whole sentences — gradually getting harder")
            }
            items(state.sentenceLessons, key = { "sentence-${it.lessonNumber}" }) { lp ->
                LessonCard(
                    lesson = LessonInfo(
                        number = lp.lessonNumber,
                        title = SentenceCurriculum.titleFor(lp.lessonNumber),
                        subtitle = "${lp.totalItems} sentence" + if (lp.totalItems == 1) "" else "s",
                        characters = emptyList(),
                        apiLessonNumber = lp.lessonNumber,
                    ),
                    accent = accent,
                    progress = lp.progress,
                    unlocked = lp.isUnlocked,
                ) {
                    onSelect(
                        PracticeSource.SentenceLesson(
                            lessonNumber = lp.lessonNumber,
                            lessonTitle = SentenceCurriculum.titleFor(lp.lessonNumber),
                            sentenceCount = lp.totalItems,
                        ),
                    )
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

/** 40x40 rounded square icon button used in the Lessons header — mirrors iOS's surface chips. */
@Composable
private fun HeaderIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    val c = SonariTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.textPrimary, modifier = Modifier.size(20.dp))
    }
}
