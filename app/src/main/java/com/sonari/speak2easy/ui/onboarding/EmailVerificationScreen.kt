package com.sonari.speak2easy.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonari.speak2easy.R
import com.sonari.speak2easy.data.auth.AuthState
import com.sonari.speak2easy.ui.auth.AuthViewModel
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@Composable
fun EmailVerificationScreen(viewModel: AuthViewModel) {
    val c = SonariTheme.colors
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val email = (authState as? AuthState.PendingVerification)?.user?.email

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.background)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.email_verification_mascot),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "CHECK YOUR INBOX",
                style = SonariFonts.monoLarge,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                email?.let { "We sent a verification link to $it." }
                    ?: "We sent a verification link to your email.",
                style = SonariFonts.monoSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "After tapping the link, come back here to continue.",
                style = SonariFonts.monoCaption,
                color = c.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ui.errorMessage?.let {
                VerificationStatus(text = it, color = c.error)
            }
            ui.infoMessage?.let {
                VerificationStatus(text = it, color = c.success)
            }
            SonariPrimaryButton(
                text = "CONTINUE",
                loading = ui.isLoading,
                onClick = viewModel::refreshVerificationStatus,
            )
            SonariSecondaryButton("RESEND EMAIL", onClick = viewModel::resendVerification)
            TextButton(onClick = viewModel::signOut) {
                Text("Use a different account", style = SonariFonts.monoCaption, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun VerificationStatus(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = SonariFonts.monoCaption,
        color = color,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
