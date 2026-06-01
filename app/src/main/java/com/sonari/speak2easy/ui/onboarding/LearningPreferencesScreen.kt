package com.sonari.speak2easy.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme
import com.sonari.speak2easy.util.TextSanitizer
import kotlinx.coroutines.launch

@Composable
fun LearningPreferencesScreen(viewModel: OnboardingViewModel, onBack: () -> Unit) {
    val c = SonariTheme.colors
    val form = viewModel.form
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    // Tracks the most recent tile tap so the permission-result callback knows which
    // time to persist (avoids a race if Compose recomposes `form` mid-flight).
    var pendingTime by remember { mutableStateOf<PracticeTime?>(null) }

    // Helper: write the picked tile's lower-bound hour to local prefs. AppContainer
    // watches these via combine{} and arms the AlarmManager — we don't call the
    // ReminderManager directly here.
    fun persistReminder(picked: PracticeTime) {
        scope.launch {
            container.preferences.setReminderMinuteOfDay(picked.reminderMinuteOfDay)
            container.preferences.setNotificationsEnabled(true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val picked = pendingTime
        pendingTime = null
        if (granted && picked != null) persistReminder(picked)
        // Denial: silent — onboarding still completes, user can flip it on later from Settings.
    }

    OnboardingScaffold(
        title = "Your goals",
        subtitle = "Step 2 of 2",
        onBack = onBack,
        footer = {
            viewModel.errorMessage?.let { Text(it, style = SonariFonts.monoCaption, color = c.error) }
            SonariPrimaryButton("FINISH", loading = viewModel.isSubmitting, onClick = viewModel::submit)
        },
    ) {
        // Referral code lives on step 2 (matches iOS layout). Sanitizer caps at 20 chars and
        // upper-cases on each keystroke; backend additionally validates against /^[A-Z0-9]{3,20}$/.
        SonariTextField(form.referralCode, { v ->
            viewModel.update { it.copy(referralCode = TextSanitizer.cleanFreeText(v, 20).uppercase()) }
        }, "Referral code (optional)")

        LabeledDropdown("Japanese level", JapaneseLevel.entries.toList(), form.japaneseLevel, { it.display }) { v ->
            viewModel.update { it.copy(japaneseLevel = v) }
        }
        LabeledDropdown("Learning goal", LearningGoal.entries.toList(), form.learningGoal, { it.display }) { v ->
            viewModel.update { it.copy(learningGoal = v) }
        }
        LabeledDropdown("Daily goal", listOf(5, 10, 15, 20, 30), form.dailyGoalMinutes, { "$it minutes / day" }) { v ->
            viewModel.update { it.copy(dailyGoalMinutes = v) }
        }

        // Preferred Time — 2x2 grid of tappable cards mirroring iOS Onboarding step 2.
        // Tap also triggers the POST_NOTIFICATIONS system prompt (first tap only on API 33+;
        // subsequent taps are silent once the user has granted or denied). The chosen tile's
        // lower-bound hour (6am / noon / 6pm / 8pm) becomes the daily reminder time.
        PreferredTimeGrid(selected = form.preferredPracticeTime) { picked ->
            viewModel.update { it.copy(preferredPracticeTime = picked) }
            val needsRuntimePerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            val alreadyGranted = !needsRuntimePerm ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (alreadyGranted) {
                persistReminder(picked)
            } else {
                pendingTime = picked
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun PreferredTimeGrid(selected: PracticeTime, onSelect: (PracticeTime) -> Unit) {
    val c = SonariTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PREFERRED TIME", style = SonariFonts.monoCaption, color = c.textSecondary)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PreferredTimeCard(PracticeTime.MORNING, Icons.Filled.WbTwilight, selected, onSelect, Modifier.weight(1f))
                PreferredTimeCard(PracticeTime.AFTERNOON, Icons.Filled.WbSunny, selected, onSelect, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PreferredTimeCard(PracticeTime.EVENING, Icons.Filled.Bedtime, selected, onSelect, Modifier.weight(1f))
                PreferredTimeCard(PracticeTime.FLEXIBLE, Icons.Filled.Schedule, selected, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreferredTimeCard(
    option: PracticeTime,
    icon: ImageVector,
    selected: PracticeTime,
    onSelect: (PracticeTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = SonariTheme.colors
    val active = option == selected
    val labelColor = if (active) c.buttonText else c.textPrimary
    val rangeColor = if (active) c.buttonText.copy(alpha = 0.75f) else c.textSecondary
    Column(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) c.accent else c.surfacePrimary)
            .border(1.dp, if (active) c.accent else c.border, RoundedCornerShape(14.dp))
            .clickable { onSelect(option) }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) c.buttonText else c.accent,
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(option.shortLabel, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = labelColor)
        Spacer(Modifier.size(4.dp))
        Text(option.timeRange, style = SonariFonts.monoCaption, color = rangeColor)
    }
}
