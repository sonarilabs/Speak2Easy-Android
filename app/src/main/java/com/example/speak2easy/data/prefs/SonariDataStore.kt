package com.example.speak2easy.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single app-wide DataStore for reactive UI preferences (theme, haptics).
 * Declared once here because DataStore enforces one instance per file name per process.
 */
internal val Context.sonariDataStore: DataStore<Preferences> by preferencesDataStore(name = "sonari_prefs")
