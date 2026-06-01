package com.sonari.speak2easy.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.domain.model.User
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme
import com.sonari.speak2easy.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Settings — iOS-style grouped sections (App Settings · Account · Help & Feedback · Legal),
 * each in its own rounded card with a small uppercase label above and inline dividers between rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onReportBug: () -> Unit) {
    val container = LocalAppContainer.current
    val c = SonariTheme.colors
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory(container.preferences))
    val theme by themeViewModel.theme.collectAsStateWithLifecycle()
    val hapticsEnabled by container.preferences.hapticsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val notificationsEnabled by container.preferences.notificationsEnabled.collectAsStateWithLifecycle(initialValue = false)
    val reminderMinute by container.preferences.reminderMinuteOfDay.collectAsStateWithLifecycle(initialValue = 20 * 60)
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val haptics = container.hapticsManager
    val snackbarHostState = remember { SnackbarHostState() }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetPasswordConfirm by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    fun openAppNotificationSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showTimePicker = true
        } else {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Notifications are off. Enable them in system settings.",
                    actionLabel = "OPEN",
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) openAppNotificationSettings()
            }
            scope.launch { container.preferences.setNotificationsEnabled(false) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "SETTINGS",
            style = SonariFonts.monoLarge,
            color = c.textPrimary,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
        )

        ProfileSection(user = container.authRepository.currentUser)

        // APP SETTINGS — theme, notifications, haptics
        Spacer(Modifier.height(24.dp))
        SectionLabel("APP SETTINGS")
        SectionCard {
            IconToggleRow(
                icon = if (theme.isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                title = "Theme",
                subtitle = theme.displayName,
                checked = theme.isDark,
                onChange = { themeViewModel.toggle() },
            )
            RowDivider()
            IconToggleRow(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = if (notificationsEnabled) "Daily at ${formatTime(reminderMinute)}" else "Off",
                checked = notificationsEnabled,
                onChange = { wantOn ->
                    if (wantOn) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS,
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (granted) showTimePicker = true
                            else permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            showTimePicker = true
                        }
                    } else {
                        scope.launch { container.preferences.setNotificationsEnabled(false) }
                    }
                },
                onRowTap = if (notificationsEnabled) ({ showTimePicker = true }) else null,
            )
            RowDivider()
            IconToggleRow(
                icon = Icons.Filled.Vibration,
                title = "Haptics",
                subtitle = if (hapticsEnabled) "Vibration feedback on" else "Vibration feedback off",
                checked = hapticsEnabled,
                onChange = { enabled -> scope.launch { container.preferences.setHapticsEnabled(enabled) } },
            )
        }

        // ACCOUNT — reset password (email users only), sign out, delete account
        Spacer(Modifier.height(24.dp))
        SectionLabel("ACCOUNT")
        SectionCard {
            val signedInUser = container.authRepository.currentUser
            val showResetPassword = signedInUser?.authProvider == "email" && !signedInUser.email.isNullOrBlank()
            if (showResetPassword) {
                IconActionRow(
                    icon = Icons.Filled.VpnKey,
                    title = "Reset Password",
                    subtitle = "Change your password",
                    trailing = TrailingChevron,
                ) {
                    haptics.playSelection()
                    showResetPasswordConfirm = true
                }
                RowDivider()
            }
            IconActionRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                titleColor = c.error,
                iconTint = c.error,
            ) {
                haptics.playSelection()
                showSignOutConfirm = true
            }
            RowDivider()
            IconActionRow(
                icon = Icons.Filled.DeleteOutline,
                title = "Delete Account",
                titleColor = c.error,
                iconTint = c.error,
                trailing = TrailingChevron,
            ) {
                haptics.playSelection()
                showDeleteConfirm = true
            }
        }

        // HELP & FEEDBACK — single in-app form
        Spacer(Modifier.height(24.dp))
        SectionLabel("HELP & FEEDBACK")
        SectionCard {
            IconActionRow(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                title = "Help & Feedback",
                subtitle = "Report issues or send suggestions",
                trailing = TrailingChevron,
            ) {
                haptics.playSelection()
                onReportBug()
            }
        }

        // LEGAL — Privacy + Terms in the system browser
        Spacer(Modifier.height(24.dp))
        SectionLabel("LEGAL")
        SectionCard {
            IconActionRow(
                icon = Icons.Filled.PrivacyTip,
                title = "Privacy Policy",
                trailing = TrailingExternal,
            ) { uriHandler.openUri("https://sonarilabs.ai/privacy") }
            RowDivider()
            IconActionRow(
                icon = Icons.AutoMirrored.Filled.Article,
                title = "Terms of Use",
                trailing = TrailingExternal,
            ) { uriHandler.openUri("https://sonarilabs.ai/terms") }
        }

        Spacer(Modifier.height(16.dp))
        VersionFooter()
        SnackbarHost(hostState = snackbarHostState)
    }

    // --- Dialogs ---

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

    if (showResetPasswordConfirm) {
        val email = container.authRepository.currentUser?.email.orEmpty()
        AlertDialog(
            onDismissRequest = { showResetPasswordConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showResetPasswordConfirm = false
                    scope.launch {
                        val result = container.authRepository.forgotPassword(email)
                        val message = when (result) {
                            is com.sonari.speak2easy.data.auth.AuthResult.Success ->
                                result.message?.takeIf { it.isNotBlank() } ?: "Password reset email sent."
                            is com.sonari.speak2easy.data.auth.AuthResult.Error -> result.message
                        }
                        snackbarHostState.showSnackbar(message)
                    }
                }) { Text("Send", color = c.accent) }
            },
            dismissButton = {
                TextButton(onClick = { showResetPasswordConfirm = false }) { Text("Cancel", color = c.textSecondary) }
            },
            title = { Text("Send password reset email?", style = SonariFonts.monoMedium, color = c.textPrimary) },
            text = {
                Text(
                    "We'll email reset instructions to ${maskEmail(email)}.",
                    style = SonariFonts.monoSmall,
                    color = c.textSecondary,
                )
            },
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
                            if (result is com.sonari.speak2easy.data.auth.AuthResult.Error) {
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

// region — Section building blocks

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = SonariFonts.monoCaption,
        color = SonariTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    val c = SonariTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(14.dp)),
    ) {
        content()
    }
}

@Composable
private fun RowDivider() {
    val c = SonariTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)  // align with text, not icon
            .height(1.dp)
            .background(c.border),
    )
}

@Composable
private fun IconToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    onRowTap: (() -> Unit)? = null,
) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onRowTap != null) it.clickable(onClick = onRowTap) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SonariFonts.monoSmall, color = c.textPrimary)
            Text(subtitle, style = SonariFonts.monoTiny, color = c.textTertiary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.buttonText,
                checkedTrackColor = c.accent,
                uncheckedTrackColor = c.surfaceSecondary,
            ),
        )
    }
}

private enum class TrailingKind { None, Chevron, External }
private val TrailingChevron = TrailingKind.Chevron
private val TrailingExternal = TrailingKind.External

@Composable
private fun IconActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    titleColor: Color? = null,
    iconTint: Color? = null,
    trailing: TrailingKind = TrailingKind.None,
    onClick: () -> Unit,
) {
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint ?: c.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = SonariFonts.monoSmall, color = titleColor ?: c.textPrimary)
            subtitle?.let { Text(it, style = SonariFonts.monoTiny, color = c.textTertiary) }
        }
        when (trailing) {
            TrailingKind.Chevron -> Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(20.dp),
            )
            TrailingKind.External -> Icon(
                Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(16.dp),
            )
            TrailingKind.None -> {}
        }
    }
}

// endregion

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

/** Rounded card showing who is signed in — initials avatar, display name, masked email, provider chip, signup date. */
@Composable
private fun ProfileSection(user: User?) {
    if (user == null) return
    val c = SonariTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surfacePrimary)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        InitialsAvatar(user)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            Text(
                user.displayName?.takeIf { it.isNotBlank() } ?: "Speak2Easy learner",
                style = SonariFonts.monoMedium,
                color = c.textPrimary,
            )
            user.email?.let {
                Text(maskEmail(it), style = SonariFonts.monoSmall, color = c.textSecondary)
            }
            val provider = user.authProvider
            val memberSince = user.createdAt?.let { formatMemberSince(it) }?.takeIf { it.isNotEmpty() }
            if (provider != null || memberSince != null) {
                // Bottom row: "Member since X" on the left, provider chip on the right.
                // SpaceBetween pins the chip to the trailing edge; an empty Spacer fills
                // the leading slot if there's no member-since value so the chip still
                // right-aligns cleanly.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    memberSince?.let {
                        Text("Member since $it", style = SonariFonts.monoTiny, color = c.textTertiary)
                    } ?: Spacer(Modifier.size(0.dp))
                    provider?.let { ProviderChip(it) }
                }
            }
        }
    }
}

/** Circular badge with the user's initials on the accent color — mirrors iOS profile avatar. */
@Composable
private fun InitialsAvatar(user: User) {
    val c = SonariTheme.colors
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(c.accent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsFor(user),
            style = SonariFonts.monoMedium,
            color = c.buttonText,
        )
    }
}

/** "Akshay Singh" → "AS"; "alice@example.com" → "A"; otherwise "?". */
private fun initialsFor(user: User): String {
    val name = user.displayName?.trim().orEmpty()
    if (name.isNotEmpty()) {
        val parts = name.split(Regex("\\s+")).filter { it.isNotEmpty() }
        return parts.take(2).joinToString("") { it.first().uppercase() }
    }
    val email = user.email?.trim().orEmpty()
    if (email.isNotEmpty()) return email.first().uppercase()
    return "?"
}

/** Small pill — "Google" / "Email" — color-tinted so the provider reads at a glance. */
@Composable
private fun ProviderChip(provider: String) {
    val c = SonariTheme.colors
    val label = provider.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.surfaceSecondary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = SonariFonts.monoTiny, color = c.textSecondary)
    }
}

/** "alice@example.com" → "al****@example.com". Local part is truncated to 2 visible chars. */
private fun maskEmail(email: String): String {
    val at = email.indexOf('@')
    if (at <= 0) return email
    val local = email.substring(0, at)
    val domain = email.substring(at)
    val visible = local.take(2)
    val masked = "*".repeat((local.length - visible.length).coerceAtLeast(1))
    return "$visible$masked$domain"
}

/** Parses ISO-8601 createdAt to "MMM yyyy". Returns "" on any parse failure (chip hides). */
private fun formatMemberSince(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))
}.getOrDefault("")

/** Two-line centered footer: version on top, company line below. */
@Composable
private fun VersionFooter() {
    val c = SonariTheme.colors
    val context = LocalContext.current
    val versionLine = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
                info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            "Speak2Easy v${info.versionName ?: "?"} ($code)"
        }.getOrDefault("Speak2Easy")
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            versionLine,
            style = SonariFonts.monoTiny,
            color = c.textTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            "Built by Sonari Inc",
            style = SonariFonts.monoTiny,
            color = c.textTertiary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
