package com.example.speak2easy.di

import androidx.compose.runtime.staticCompositionLocalOf

/** Exposes the app's [AppContainer] to composables without prop-drilling. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
