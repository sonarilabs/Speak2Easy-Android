package com.sonari.speak2easy.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.data.auth.AuthState
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.onboarding.OnboardingRoot
import com.sonari.speak2easy.ui.paywall.PaywallScreen
import com.sonari.speak2easy.ui.paywall.PaywallViewModel
import com.sonari.speak2easy.ui.splash.SplashScreen

private enum class RootStage { SPLASH, ONBOARDING, PAYWALL, MAIN }

/**
 * Top-level auth + entitlement gate, mirroring iOS `ContentView`.
 * Unknown → Splash; Unauthenticated / PendingVerification / not-onboarded → Onboarding;
 * Authenticated + onboarding complete + premium → the main tab scaffold.
 */
@Composable
fun AppRoot() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val authState by container.authRepository.authState.collectAsStateWithLifecycle()

    val stage = when (val s = authState) {
        AuthState.Unknown -> RootStage.SPLASH
        AuthState.Unauthenticated -> RootStage.ONBOARDING
        is AuthState.PendingVerification -> RootStage.ONBOARDING
        is AuthState.Authenticated -> when {
            !s.user.onboardingCompleted -> RootStage.ONBOARDING
            s.user.subscriptionTier.hasPremiumAccess() -> RootStage.MAIN
            else -> RootStage.PAYWALL
        }
    }

    val authenticated = authState as? AuthState.Authenticated
    LaunchedEffect(authenticated?.user?.userId, authenticated?.user?.onboardingCompleted) {
        if (authenticated?.user?.onboardingCompleted == true) {
            runCatching { container.subscriptionRepository.getStatus() }
        }
    }

    Crossfade(targetState = stage, animationSpec = tween(durationMillis = 300), label = "root-stage") { current ->
        when (current) {
            RootStage.SPLASH -> SplashScreen()
            RootStage.ONBOARDING -> OnboardingRoot()
            RootStage.PAYWALL -> {
                val paywallViewModel: PaywallViewModel = viewModel(
                    factory = PaywallViewModel.Factory(
                        context = context,
                        authRepository = container.authRepository,
                        subscriptionRepository = container.subscriptionRepository,
                    ),
                )
                PaywallScreen(paywallViewModel)
            }
            RootStage.MAIN -> MainScaffold()
        }
    }
}

private fun String?.hasPremiumAccess(): Boolean =
    this != null && isNotBlank() && !equals("free", ignoreCase = true)
