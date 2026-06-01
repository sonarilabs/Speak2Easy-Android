package com.sonari.speak2easy.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sonari.speak2easy.data.prefs.SonariPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTheme(val rawValue: String) {
    LIGHT("light"),
    DARK("dark");

    val isDark: Boolean get() = this == DARK
    val displayName: String get() = name // "LIGHT" / "DARK"
}

/**
 * Owns the dark/light selection, persisted via [SonariPreferences].
 * Mirrors iOS `ThemeManager` (default dark, toggle()).
 */
class ThemeViewModel(private val prefs: SonariPreferences) : ViewModel() {

    val theme: StateFlow<AppTheme> = prefs.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppTheme.DARK)

    fun toggle() {
        viewModelScope.launch {
            prefs.setTheme(if (theme.value.isDark) AppTheme.LIGHT else AppTheme.DARK)
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    class Factory(private val prefs: SonariPreferences) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ThemeViewModel(prefs) as T
    }
}
