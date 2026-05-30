package com.example.speak2easy.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.speak2easy.data.auth.AuthState
import com.example.speak2easy.di.LocalAppContainer
import com.example.speak2easy.ui.onboarding.OnboardingRoot
import com.example.speak2easy.ui.splash.SplashScreen

private enum class RootStage { SPLASH, ONBOARDING, MAIN }

/**
 * Top-level auth gate, mirroring iOS `ContentView` — minus the paywall.
 * Unknown → Splash; Unauthenticated / PendingVerification / not-onboarded → Onboarding;
 * Authenticated + onboarding complete → the main tab scaffold.
 */
@Composable
fun AppRoot() {
    val container = LocalAppContainer.current
    val authState by container.authRepository.authState.collectAsStateWithLifecycle()

    val stage = when (val s = authState) {
        AuthState.Unknown -> RootStage.SPLASH
        AuthState.Unauthenticated -> RootStage.ONBOARDING
        is AuthState.PendingVerification -> RootStage.ONBOARDING
        is AuthState.Authenticated -> if (s.user.onboardingCompleted) RootStage.MAIN else RootStage.ONBOARDING
    }

    Crossfade(targetState = stage, animationSpec = tween(durationMillis = 300), label = "root-stage") { current ->
        when (current) {
            RootStage.SPLASH -> SplashScreen()
            RootStage.ONBOARDING -> OnboardingRoot()
            RootStage.MAIN -> MainScaffold()
        }
    }
}
