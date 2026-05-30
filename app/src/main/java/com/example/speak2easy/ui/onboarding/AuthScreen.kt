package com.example.speak2easy.ui.onboarding

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.speak2easy.ui.auth.AuthViewModel
import com.example.speak2easy.ui.components.Mascot
import com.example.speak2easy.ui.components.MascotImage
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

enum class AuthMode { SIGN_IN, SIGN_UP }

@Composable
fun AuthScreen(
    mode: AuthMode,
    viewModel: AuthViewModel,
    onToggleMode: () -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit,
    onGoogle: () -> Unit,
) {
    val c = SonariTheme.colors
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val signUp = mode == AuthMode.SIGN_UP

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val emailOk = android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordOk = password.length >= 8
    val matchOk = !signUp || password == confirm
    val formValid = emailOk && passwordOk && matchOk

    Column(modifier = Modifier.fillMaxSize().background(c.background).systemBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("BACK", style = SonariFonts.monoCaption, color = c.accent)
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MascotImage(mascot = Mascot.Thinking, height = 120.dp)
            Spacer(Modifier.height(8.dp))
            Text(if (signUp) "CREATE ACCOUNT" else "WELCOME BACK", style = SonariFonts.monoLarge, color = c.textPrimary)
            Text(
                if (signUp) "Start your Japanese journey" else "Continue your learning",
                style = SonariFonts.monoSmall,
                color = c.textSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(Modifier.height(32.dp))

            // Google — always present (matches iOS). Tapping without a Web Client ID surfaces a message.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, c.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onGoogle),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (signUp) "Sign up with Google" else "Sign in with Google", style = SonariFonts.monoMedium, color = c.textPrimary)
            }

            DividerOr()

            SonariLabeledField("EMAIL ADDRESS", email, { email = it }, "user@example.com", KeyboardType.Email)
            Spacer(Modifier.height(16.dp))
            SonariLabeledField("PASSWORD", password, { password = it }, "min. 8 characters", KeyboardType.Password, isPassword = true)
            if (signUp) {
                Spacer(Modifier.height(16.dp))
                SonariLabeledField("CONFIRM PASSWORD", confirm, { confirm = it }, "re-enter password", KeyboardType.Password, isPassword = true)
                if (password.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    HintRow(passwordOk, "At least 8 characters")
                }
                if (confirm.isNotEmpty() && password != confirm) {
                    HintRow(false, "Passwords do not match")
                }
            }

            ui.errorMessage?.let {
                Text(it, style = SonariFonts.monoCaption, color = c.error, modifier = Modifier.padding(top = 12.dp).fillMaxWidth())
            }
            ui.infoMessage?.let {
                Text(it, style = SonariFonts.monoCaption, color = c.success, modifier = Modifier.padding(top = 12.dp).fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (formValid) c.accent else c.surfaceSecondary)
                    .clickable(enabled = formValid && !ui.isLoading) {
                        if (signUp) viewModel.signUpWithEmail(email, password, null) else viewModel.signInWithEmail(email, password)
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (ui.isLoading) {
                    CircularProgressIndicator(color = c.buttonText, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(if (signUp) "CONTINUE >>" else "SIGN IN >>", style = SonariFonts.monoMedium, color = if (formValid) c.buttonText else c.textTertiary)
                }
            }

            if (!signUp) {
                Spacer(Modifier.height(8.dp))
                Text("FORGOT PASSWORD?", style = SonariFonts.monoCaption, color = c.accent, modifier = Modifier.clickable(onClick = onForgotPassword).padding(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text(
                if (signUp) "Already have an account? Sign in" else "New here? Create an account",
                style = SonariFonts.monoCaption,
                color = c.accent,
                modifier = Modifier.clickable(onClick = onToggleMode).padding(8.dp),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DividerOr() {
    val c = SonariTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(c.border))
        Text("OR", style = SonariFonts.monoCaption, color = c.textTertiary, modifier = Modifier.padding(horizontal = 16.dp))
        Box(Modifier.weight(1f).height(1.dp).background(c.border))
    }
}

@Composable
private fun HintRow(met: Boolean, text: String) {
    val c = SonariTheme.colors
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (met) "✓ " else "• ", style = SonariFonts.monoCaption, color = if (met) c.success else c.textSecondary)
        Text(text, style = SonariFonts.monoCaption, color = if (met) c.textSecondary else c.error)
    }
}
