package com.sonari.speak2easy.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonari.speak2easy.ui.auth.AuthViewModel
import com.sonari.speak2easy.ui.components.Mascot
import com.sonari.speak2easy.ui.components.MascotImage
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

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
        // Mascot + supporting copy, centered so the page reads as a real flow step rather than a
        // bare alert (which is how it felt without the visual anchor).
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            MascotImage(mascot = Mascot.Thinking, height = 140.dp)
            Text(
                "Almost there! Verifying your email keeps your progress safe across devices.",
                style = SonariFonts.monoSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
