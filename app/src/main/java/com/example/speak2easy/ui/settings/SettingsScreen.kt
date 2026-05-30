package com.example.speak2easy.ui.settings

import android.Manifest
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speak2easy.di.LocalAppContainer
import com.example.speak2easy.ui.components.Mascot
import com.example.speak2easy.ui.components.MascotCallout
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme
import com.example.speak2easy.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

/** Settings: theme, haptics, notifications, legal links, sign out, delete account. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val container = LocalAppContainer.current
    val c = SonariTheme.colors
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory(container.preferences))
    val theme by themeViewModel.theme.collectAsStateWithLifecycle()
    val hapticsEnabled by container.preferences.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val notificationsEnabled by container.preferences.notificationsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val reminderMinute by container.preferences.reminderMinuteOfDay.collectAsStateWithLifecycle(initialValue = 20 * 60)
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showTimePicker = true
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Enable notifications in system settings to set a daily reminder.")
            }
            scope.launch { container.preferences.setNotificationsEnabled(false) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("SETTINGS", style = SonariFonts.monoLarge, color = c.textPrimary, modifier = Modifier.padding(top = 16.dp))

        SettingRow("Theme") {
            OutlinedButton(onClick = themeViewModel::toggle) {
                Text(theme.displayName, style = SonariFonts.monoCaption, color = c.accent)
            }
        }

        SettingRow("Haptics") {
            Switch(
                checked = hapticsEnabled,
                onCheckedChange = { enabled -> scope.launch { container.preferences.setHapticsEnabled(enabled) } },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = c.buttonText,
                    checkedTrackColor = c.accent,
                    uncheckedTrackColor = c.surfaceSecondary,
                ),
            )
        }

        NotificationsRow(
            enabled = notificationsEnabled,
            subtitle = if (notificationsEnabled) "Daily at ${formatTime(reminderMinute)}" else "Off",
            onToggle = { wantOn ->
                if (wantOn) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Pre-API 33: permission is implicit. Go straight to time picker.
                        showTimePicker = true
                    }
                } else {
                    scope.launch { container.preferences.setNotificationsEnabled(false) }
                }
            },
            onTapWhenOn = { showTimePicker = true },
        )

        // Legal links — open in the system browser (mirrors WelcomeScreen).
        LinkRow("Privacy Policy") { uriHandler.openUri("https://sonarilabs.ai/privacy") }
        LinkRow("Terms of Service") { uriHandler.openUri("https://sonarilabs.ai/terms") }

        Spacer(Modifier.size(28.dp))

        MascotCallout(
            mascot = Mascot.HappyWave,
            title = "Keep your streak warm",
            body = "Small daily reps beat marathon sessions. A reminder helps.",
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Filled accent button reads clearly as an action (the surfaceSecondary version
        // looked disabled on the dark theme).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(bottom = 0.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.accent)
                .clickable { showSignOutConfirm = true },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("SIGN OUT", style = SonariFonts.monoSmall, color = c.buttonText)
        }

        Spacer(Modifier.size(12.dp))

        // Destructive action — outlined red, matches EXIT LESSON style.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, c.error.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .clickable { showDeleteConfirm = true },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DELETE ACCOUNT", style = SonariFonts.monoSmall, color = c.error)
        }

        Spacer(Modifier.size(16.dp))

        SnackbarHost(hostState = snackbarHostState)
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = reminderMinute / 60,
            initialMinute = reminderMinute % 60,
            is24Hour = false,
        )
        LaunchedEffect(pickerState.hour, pickerState.minute) {
            container.hapticsManager.playSelection()
        }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        container.preferences.setReminderMinuteOfDay(pickerState.hour * 60 + pickerState.minute)
                        container.preferences.setNotificationsEnabled(true)
                    }
                    showTimePicker = false
                }) {
                    Text("Save", color = c.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel", color = c.textSecondary) }
            },
            title = { Text("Daily reminder", style = SonariFonts.monoMedium, color = c.textPrimary) },
            text = { TimePicker(state = pickerState) },
            containerColor = c.surfacePrimary,
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutConfirm = false
                    container.authRepository.signOut()
                }) { Text("Sign out", color = c.error) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel", color = c.textSecondary) }
            },
            title = { Text("Sign out?", style = SonariFonts.monoMedium, color = c.textPrimary) },
            text = { Text("You'll be returned to the welcome screen. Your progress stays on the server.", style = SonariFonts.monoSmall, color = c.textSecondary) },
            containerColor = c.surfacePrimary,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirm = false },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {
                        isDeleting = true
                        scope.launch {
                            val result = container.authRepository.deleteAccount()
                            isDeleting = false
                            showDeleteConfirm = false
                            if (result is com.example.speak2easy.data.auth.AuthResult.Error) {
                                snackbarHostState.showSnackbar(result.message)
                            }
                        }
                    },
                ) { Text(if (isDeleting) "Deleting…" else "Delete", color = c.error) }
            },
            dismissButton = {
                TextButton(enabled = !isDeleting, onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = c.textSecondary)
                }
            },
            title = { Text("Delete your account?", style = SonariFonts.monoMedium, color = c.error) },
            text = {
                Text(
                    "This permanently removes your account, sessions, and progress. " +
                        "We can't undo this.",
                    style = SonariFonts.monoSmall,
                    color = c.textSecondary,
                )
            },
            containerColor = c.surfacePrimary,
        )
    }
}

@Composable
private fun SettingRow(label: String, trailing: @Composable () -> Unit) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = SonariFonts.monoSmall, color = c.textPrimary)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun NotificationsRow(
    enabled: Boolean,
    subtitle: String,
    onToggle: (Boolean) -> Unit,
    onTapWhenOn: () -> Unit,
) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .let { if (enabled) it.clickable(onClick = onTapWhenOn) else it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Notifications", style = SonariFonts.monoSmall, color = c.textPrimary)
            Text(subtitle, style = SonariFonts.monoTiny, color = c.textTertiary)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.buttonText,
                checkedTrackColor = c.accent,
                uncheckedTrackColor = c.surfaceSecondary,
            ),
        )
    }
}

/** External link row — label on the left, "open" glyph on the right, tap opens in browser. */
@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = SonariFonts.monoSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun formatTime(minuteOfDay: Int): String {
    val h24 = minuteOfDay / 60
    val m = minuteOfDay % 60
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    val ampm = if (h24 < 12) "AM" else "PM"
    return "%d:%02d %s".format(h12, m, ampm)
}
