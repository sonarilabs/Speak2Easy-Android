package com.sonari.speak2easy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.navigation.AppRoot
import com.sonari.speak2easy.ui.theme.Speak2EasyTheme
import com.sonari.speak2easy.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as Speak2EasyApp).container
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = ThemeViewModel.Factory(container.preferences),
            )
            val theme by themeViewModel.theme.collectAsStateWithLifecycle()
            Speak2EasyTheme(darkTheme = theme.isDark) {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    AppRoot()
                }
            }
        }
    }
}
