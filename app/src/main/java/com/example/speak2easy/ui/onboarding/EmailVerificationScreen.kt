package com.example.speak2easy.ui.onboarding

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.speak2easy.ui.auth.AuthViewModel
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

@Composable
fun EmailVerificationScreen(viewModel: AuthViewModel) {
    val c = SonariTheme.colors
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    OnboardingScaffold(
        title = "Verify your email",
        subtitle = "Tap the link we emailed you, then continue.",
        footer = {
            ui.errorMessage?.let { Text(it, style = SonariFonts.monoCaption, color = c.error) }
            ui.infoMessage?.let { Text(it, style = SonariFonts.monoCaption, color = c.success) }
            SonariPrimaryButton(
                text = "I'VE VERIFIED — CONTINUE",
                loading = ui.isLoading,
                onClick = viewModel::refreshVerificationStatus,
            )
            SonariSecondaryButton("RESEND EMAIL", onClick = viewModel::resendVerification)
            TextButton(onClick = viewModel::signOut) {
                Text("Sign out", style = SonariFonts.monoCaption, color = c.textSecondary)
            }
        },
    ) {
        Text(
            "Almost there! Verifying your email keeps your progress safe across devices.",
            style = SonariFonts.monoSmall,
            color = c.textSecondary,
        )
    }
}
