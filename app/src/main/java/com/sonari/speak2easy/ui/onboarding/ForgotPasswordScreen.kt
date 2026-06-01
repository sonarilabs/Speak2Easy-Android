package com.sonari.speak2easy.ui.onboarding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonari.speak2easy.ui.auth.AuthViewModel
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

@Composable
fun ForgotPasswordScreen(viewModel: AuthViewModel, onBack: () -> Unit) {
    val c = SonariTheme.colors
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }

    OnboardingScaffold(
        title = "Reset password",
        subtitle = "We'll email you a reset link.",
        onBack = onBack,
        footer = {
            ui.errorMessage?.let { Text(it, style = SonariFonts.monoCaption, color = c.error) }
            ui.infoMessage?.let { Text(it, style = SonariFonts.monoCaption, color = c.success) }
            SonariPrimaryButton(
                text = "SEND RESET LINK",
                loading = ui.isLoading,
                enabled = email.isNotBlank(),
                onClick = { viewModel.forgotPassword(email) },
            )
        },
    ) {
        SonariTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
    }
}
