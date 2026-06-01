package com.sonari.speak2easy.ui.practice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
import android.view.WindowManager
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.components.Mascot
import com.sonari.speak2easy.ui.components.MascotImage
import com.sonari.speak2easy.ui.lessons.PracticeOptions
import com.sonari.speak2easy.ui.lessons.PracticeSource
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

/** What to practice + how. Set by the Lessons options sheet, consumed by the session. */
data class PracticePlan(val source: PracticeSource, val options: PracticeOptions)

@Composable
fun PracticeScreen(plan: PracticePlan?, onExit: () -> Unit) {
    val c = SonariTheme.colors

    if (plan == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }

    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: PracticeViewModel = viewModel(
        factory = PracticeViewModel.Factory(
            plan,
            container.practiceRepository,
            container.lessonRepository,
            container.applicationContext,
            container.hapticsManager,
        ),
    )
    val state = viewModel.state

    var resumeMicAfterPermission by remember { mutableStateOf(false) }
    BackHandler {
        if (state.phase != PracticePhase.COMPLETED || !state.isSavingCompletion) {
            onExit()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted)
        if (granted && resumeMicAfterPermission) viewModel.onManualMicTap()
        resumeMicAfterPermission = false
    }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
        if (!granted) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Keep the screen awake for the whole session — reading kana while waiting to record
    // shouldn't drop the display. Cleared automatically when leaving Practice.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val isOnline by container.networkMonitor.isConnected.collectAsStateWithLifecycle(initialValue = true)
    // Single confirmation dialog reused by both the top EXIT entry and the bottom EXIT LESSON.
    var showExitConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!isOnline) OfflineBanner()
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.phase) {
                    PracticePhase.LOADING -> CircularProgressIndicator(color = c.accent, modifier = Modifier.align(Alignment.Center))
                    PracticePhase.ERROR -> ErrorContent(state.errorMessage, onExit)
                    PracticePhase.COMPLETED -> CompletionContent(
                        state = state,
                        onExit = onExit,
                        onRetrySave = viewModel::retryCompletionSave,
                    )
                    PracticePhase.ACTIVE -> ActiveContent(
                        state = state,
                        onExitRequest = { showExitConfirm = true },
                        onPrev = viewModel::goPrev,
                        onPlay = viewModel::onManualSpeakerTap,
                        onRevealHint = viewModel::revealHint,
                        onSkip = viewModel::onSkip,
                        onRecordToggle = {
                            if (state.isRecording) {
                                viewModel.onManualMicTap()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    viewModel.onPermissionResult(true)
                                    viewModel.onManualMicTap()
                                } else {
                                    resumeMicAfterPermission = true
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    )
                }

                if (state.showResult && state.lastResult != null) {
                    ResultOverlay(state.lastResult!!)
                }
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showExitConfirm = false
                    onExit()
                }) { Text("Exit", color = c.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExitConfirm = false }) {
                    Text("Stay", color = c.textSecondary)
                }
            },
            title = { Text("Exit this lesson?", style = SonariFonts.monoMedium, color = c.textPrimary) },
            text = {
                Text(
                    "Your progress so far in this session won't be saved.",
                    style = SonariFonts.monoSmall,
                    color = c.textSecondary,
                )
            },
            containerColor = c.surfacePrimary,
        )
    }
}

/** Thin warning bar pinned to the top of Practice when there's no network. */
@Composable
private fun OfflineBanner() {
    val c = SonariTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.error.copy(alpha = 0.18f))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "NO INTERNET CONNECTION",
            style = SonariFonts.monoCaption,
            color = c.error,
        )
    }
}

@Composable
private fun ErrorContent(message: String?, onExit: () -> Unit) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message ?: "Something went wrong.", style = SonariFonts.monoSmall, color = c.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.buttonText)) {
            Text("BACK", style = SonariFonts.monoSmall)
        }
    }
}

@Composable
private fun ActiveContent(
    state: PracticeUiState,
    onExitRequest: () -> Unit,
    onPrev: () -> Unit,
    onPlay: () -> Unit,
    onRevealHint: () -> Unit,
    onSkip: () -> Unit,
    onRecordToggle: () -> Unit,
) {
    val c = SonariTheme.colors
    val item = state.currentItem ?: return
    val showHint = !state.focusMode || state.hintRevealed
    val canGoPrev = state.currentIndex > 0

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        // Top row: EXIT on first item (confirms), PREV after — counter centered, SKIP on the right.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = if (canGoPrev) onPrev else onExitRequest)
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            ) {
                if (canGoPrev) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous", tint = c.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("PREV", style = SonariFonts.monoCaption, color = c.accent)
                } else {
                    Icon(Icons.Filled.Close, contentDescription = "Exit", tint = c.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("EXIT", style = SonariFonts.monoCaption, color = c.accent)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("${state.currentIndex + 1} / ${state.total}", style = SonariFonts.monoCaption, color = c.textSecondary)
            Spacer(Modifier.weight(1f))
            Text(
                "SKIP",
                style = SonariFonts.monoCaption,
                color = c.textSecondary,
                modifier = Modifier.clickable(onClick = onSkip).padding(vertical = 6.dp, horizontal = 4.dp),
            )
        }
        LinearProgressIndicator(
            progress = { if (state.total == 0) 0f else (state.currentIndex + 1).toFloat() / state.total },
            color = c.accent,
            trackColor = c.surfaceSecondary,
            // Material 3 1.3+ adds a stop-indicator dot at the trailing end by default — drop it.
            drawStopIndicator = {},
            gapSize = 0.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            // Auto-shrink so multi-char words like あたたかい fit on a single line.
            Text(
                item.character,
                fontSize = characterFontSize(item.character),
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
            )
            if (item.isWord && item.hiraganaReading != null && item.hiraganaReading != item.character) {
                Text(item.hiraganaReading, style = SonariFonts.monoMedium, color = c.textSecondary, modifier = Modifier.padding(top = 6.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Small speaker button sits between the character and the hints (matches iOS).
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(c.surfacePrimary)
                    .border(1.5.dp, c.accent, CircleShape)
                    .clickable(enabled = !state.isSubmitting && (!state.isRecording || state.isHandsFree), onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Play pronunciation",
                    tint = if (state.isPlaying) c.accent.copy(alpha = 0.6f) else c.accent,
                    modifier = Modifier.size(20.dp),
                )
            }

            if (showHint) {
                Spacer(Modifier.height(20.dp))
                Text("ROMAJI", style = SonariFonts.monoCaption, color = c.textSecondary)
                Text(
                    item.romanization,
                    style = SonariFonts.monoLarge,
                    color = c.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                )

                if (item.isWord && item.englishTranslation != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("MEANING", style = SonariFonts.monoCaption, color = c.textSecondary)
                    Text(
                        item.englishTranslation,
                        style = SonariFonts.monoSmall,
                        color = c.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                    )
                } else {
                    Spacer(Modifier.height(16.dp))
                    Text("PRONUNCIATION", style = SonariFonts.monoCaption, color = c.textSecondary)
                    Text(
                        item.guide,
                        style = SonariFonts.monoSmall,
                        color = c.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onRevealHint) {
                    Text("SHOW HINT", style = SonariFonts.monoCaption, color = c.accent)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // TAP TO RECORD + mic button, centered.
        Text(
            if (state.isRecording) "RECORDING…" else "TAP TO RECORD",
            style = SonariFonts.monoCaption,
            color = c.textSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RecordButton(state = state, onClick = onRecordToggle)
        }

        // Drop the EXIT LESSON closer to the bottom of the safe area (above the gesture nav).
        Spacer(Modifier.height(40.dp))

        // Box-based outline matches the rest of the app's button style (Welcome SIGN IN, etc.)
        // — Material 3's OutlinedButton has its own pill radius + border weight that looked off.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, c.error.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .clickable(onClick = onExitRequest),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Close, contentDescription = null, tint = c.error, modifier = Modifier.size(16.dp))
            Spacer(Modifier.size(8.dp))
            Text("EXIT LESSON", style = SonariFonts.monoSmall, color = c.error)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Auto-fit font size for the kana/word so multi-character entries stay on one line. */
private fun characterFontSize(text: String): androidx.compose.ui.unit.TextUnit = when {
    text.length <= 1 -> 120.sp
    text.length == 2 -> 96.sp
    text.length == 3 -> 72.sp
    text.length == 4 -> 56.sp
    text.length <= 6 -> 44.sp
    text.length <= 8 -> 36.sp
    else -> 28.sp
}

@Composable
private fun RecordButton(state: PracticeUiState, onClick: () -> Unit) {
    val c = SonariTheme.colors
    val bg = when {
        state.isRecording -> c.error
        else -> c.accent
    }
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(enabled = !state.isSubmitting, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isSubmitting -> CircularProgressIndicator(color = c.buttonText, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            state.isRecording -> Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = c.buttonText, modifier = Modifier.size(34.dp))
            else -> Icon(Icons.Filled.Mic, contentDescription = "Record", tint = c.buttonText, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun ResultOverlay(result: PracticeResultUi) {
    val c = SonariTheme.colors
    val accent = if (result.isCorrect) c.success else c.error
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(if (result.isCorrect) "✓" else "✕", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(
                if (result.isCorrect) "Correct!" else "Try again",
                style = SonariFonts.monoLarge,
                color = accent,
                modifier = Modifier.padding(top = 8.dp),
            )
            val detail = result.feedback ?: result.transcribed?.let { "Heard: $it" }
            if (detail != null) {
                Text(detail, style = SonariFonts.monoSmall, color = c.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }
}

@Composable
private fun CompletionContent(
    state: PracticeUiState,
    onExit: () -> Unit,
    onRetrySave: () -> Unit,
) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        MascotImage(mascot = Mascot.Cheering, height = 160.dp)
        Spacer(Modifier.height(12.dp))
        Text("SESSION COMPLETE", style = SonariFonts.monoCaption, color = c.textSecondary)
        Text("${state.accuracyPercent}%", fontSize = 72.sp, fontWeight = FontWeight.Black, color = c.accent, modifier = Modifier.padding(top = 8.dp))
        Text("${state.firstTryCorrect} / ${state.total} first try", style = SonariFonts.monoSmall, color = c.textSecondary)

        if (state.needsPractice.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text("NEEDS PRACTICE", style = SonariFonts.monoCaption, color = c.textSecondary, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            state.needsPractice.forEach { item ->
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Text(
                        item.character,
                        style = SonariFonts.monoLarge,
                        color = c.textPrimary,
                        fontSize = if (item.character.length > 5) 24.sp else 32.sp
                    )
                    Text(
                        item.romanization,
                        style = SonariFonts.monoSmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        if (state.isSavingCompletion) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = c.accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(8.dp))
                Text("Saving progress...", style = SonariFonts.monoTiny, color = c.textSecondary)
            }
            Spacer(Modifier.height(12.dp))
        } else if (state.completionErrorMessage != null) {
            Text(
                state.completionErrorMessage,
                style = SonariFonts.monoTiny,
                color = c.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = if (state.completionErrorMessage != null) onRetrySave else onExit,
            enabled = !state.isSavingCompletion,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.buttonText),
        ) {
            Text(
                when {
                    state.isSavingCompletion -> "SAVING"
                    state.completionErrorMessage != null -> "RETRY SAVE"
                    else -> "DONE"
                },
                style = SonariFonts.monoSmall,
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
