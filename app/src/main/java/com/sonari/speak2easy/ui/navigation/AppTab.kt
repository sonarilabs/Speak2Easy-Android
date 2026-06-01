package com.sonari.speak2easy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** Five bottom-nav tabs — Chart sits before Writing so the more familiar reference tab leads. */
enum class AppTab(val route: String, val label: String, val icon: ImageVector) {
    LESSONS("lessons", "Lessons", Icons.Filled.School),
    PROGRESS("progress", "Progress", Icons.Filled.BarChart),
    CHART("chart", "Chart", Icons.Filled.GridView),
    WRITING("writing", "Writing", Icons.Filled.Edit),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
}
