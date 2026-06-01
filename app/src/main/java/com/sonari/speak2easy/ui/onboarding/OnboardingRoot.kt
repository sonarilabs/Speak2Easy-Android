package com.sonari.speak2easy.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.R
import com.sonari.speak2easy.data.auth.AuthState
import com.sonari.speak2easy.di.AppContainer
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.auth.AuthViewModel
import com.sonari.speak2easy.ui.auth.GoogleAuthHelper
import com.sonari.speak2easy.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * Onboarding + auth flow. Routes on auth state: unauthenticated → auth screens;
 * pending verification → verify screen; authenticated but not onboarded → onboarding steps.
 */
@Composable
fun OnboardingRoot() {
    val container = LocalAppContainer.current
    val authState by container.authRepository.authState.collectAsStateWithLifecycle()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(container.authRepository))

    when (authState) {
        is AuthState.PendingVerification -> EmailVerificationScreen(authViewModel)
        is AuthState.Authenticated -> OnboardingStepsFlow(container)
        else -> AuthFlow(authViewModel)
    }
}

private enum class AuthStep { WELCOME, SIGN_IN, SIGN_UP, FORGOT }

@Composable
private fun AuthFlow(viewModel: AuthViewModel) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val webClientId = stringResource(R.string.google_web_client_id)
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(AuthStep.WELCOME) }

    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory(container.preferences))
    val theme by themeViewModel.theme.collectAsStateWithLifecycle()

    val onGoogle: () -> Unit = {
        if (webClientId.isBlank()) {
            viewModel.reportError("Google sign-in isn't set up yet. Add your OAuth Web client ID to res/values/google.xml.")
        } else {
            scope.launch {
                try {
                    val cred = GoogleAuthHelper.signIn(context, webClientId)
                    viewModel.onGoogleCredential(cred.idToken, cred.email, cred.displayName)
                } catch (_: GetCredentialCancellationException) {
                    // user dismissed the chooser — no-op
                } catch (e: Exception) {
                    viewModel.reportError(e.message ?: "Google sign-in failed")
                }
            }
        }
    }

    BackHandler(enabled = step != AuthStep.WELCOME) {
        viewModel.clearMessages()
        step = AuthStep.WELCOME
    }

    when (step) {
        AuthStep.WELCOME -> WelcomeScreen(
            onGetStarted = { viewModel.clearMessages(); step = AuthStep.SIGN_UP },
            onSignIn = { viewModel.clearMessages(); step = AuthStep.SIGN_IN },
            themeLabel = theme.displayName,
            onToggleTheme = themeViewModel::toggle,
        )
        AuthStep.SIGN_IN -> AuthScreen(
            mode = AuthMode.SIGN_IN,
            viewModel = viewModel,
            onToggleMode = { viewModel.clearMessages(); step = AuthStep.SIGN_UP },
            onForgotPassword = { viewModel.clearMessages(); step = AuthStep.FORGOT },
            onBack = { viewModel.clearMessages(); step = AuthStep.WELCOME },
            onGoogle = onGoogle,
            themeLabel = theme.displayName,
            onToggleTheme = themeViewModel::toggle,
        )
        AuthStep.SIGN_UP -> AuthScreen(
            mode = AuthMode.SIGN_UP,
            viewModel = viewModel,
            onToggleMode = { viewModel.clearMessages(); step = AuthStep.SIGN_IN },
            onForgotPassword = {},
            onBack = { viewModel.clearMessages(); step = AuthStep.WELCOME },
            onGoogle = onGoogle,
            themeLabel = theme.displayName,
            onToggleTheme = themeViewModel::toggle,
        )
        AuthStep.FORGOT -> ForgotPasswordScreen(
            viewModel = viewModel,
            onBack = { viewModel.clearMessages(); step = AuthStep.SIGN_IN },
        )
    }
}

private enum class OnbStep { PERSONAL, PREFS }

@Composable
private fun OnboardingStepsFlow(container: AppContainer) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(container.authRepository, container.preferences),
    )
    var step by rememberSaveable { mutableStateOf(OnbStep.PERSONAL) }

    BackHandler(enabled = step == OnbStep.PREFS) { step = OnbStep.PERSONAL }

    when (step) {
        OnbStep.PERSONAL -> PersonalInfoScreen(viewModel = viewModel, onNext = { step = OnbStep.PREFS })
        OnbStep.PREFS -> LearningPreferencesScreen(viewModel = viewModel, onBack = { step = OnbStep.PERSONAL })
    }
}
