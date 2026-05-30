package com.example.speak2easy.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.speak2easy.di.LocalAppContainer
import com.example.speak2easy.ui.chart.ChartScreen
import com.example.speak2easy.ui.lessons.LessonsScreen
import com.example.speak2easy.ui.practice.PracticePlan
import com.example.speak2easy.ui.practice.PracticeScreen
import com.example.speak2easy.ui.progress.ProgressScreen
import com.example.speak2easy.ui.progress.SessionDetailScreen
import com.example.speak2easy.ui.settings.SettingsScreen
import com.example.speak2easy.ui.writing.KanaStrokeCounts
import com.example.speak2easy.ui.writing.WritingHomeScreen
import com.example.speak2easy.ui.writing.WritingPracticeScreen
import com.example.speak2easy.ui.theme.SonariFonts
import com.example.speak2easy.ui.theme.SonariTheme

private const val PRACTICE_ROUTE = "practice"
private const val SESSION_DETAIL_ROUTE = "session-detail"
private const val WRITING_PRACTICE_ROUTE = "writing-practice"

/**
 * Main bottom-nav scaffold (Lessons / Progress / Chart / Settings). A full-screen
 * practice destination is pushed over the bar when a session starts; the bottom bar
 * hides for any non-tab route.
 */
@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val colors = SonariTheme.colors
    val haptics = LocalAppContainer.current.hapticsManager
    val tabs = AppTab.entries

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    var pendingPractice by remember { mutableStateOf<PracticePlan?>(null) }

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = colors.surfacePrimary) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                haptics.playSelection()
                                if (currentRoute != tab.route) {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, style = SonariFonts.monoTiny) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                indicatorColor = colors.surfaceSecondary,
                                unselectedIconColor = colors.textTertiary,
                                unselectedTextColor = colors.textTertiary,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppTab.LESSONS.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppTab.LESSONS.route) {
                LessonsScreen(onStartPractice = { source, options ->
                    pendingPractice = PracticePlan(source, options)
                    navController.navigate(PRACTICE_ROUTE)
                })
            }
            composable(AppTab.PROGRESS.route) {
                ProgressScreen(onSessionClick = { sessionId ->
                    navController.navigate("$SESSION_DETAIL_ROUTE/$sessionId")
                })
            }
            composable(AppTab.WRITING.route) {
                WritingHomeScreen(onSelect = { charset, character ->
                    navController.navigate(writingRoute(charset, character))
                })
            }
            composable(AppTab.CHART.route) { ChartScreen() }
            composable(AppTab.SETTINGS.route) { SettingsScreen() }
            composable(PRACTICE_ROUTE) {
                PracticeScreen(plan = pendingPractice, onExit = { navController.popBackStack() })
            }
            composable(
                route = "$SESSION_DETAIL_ROUTE/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("sessionId").orEmpty()
                SessionDetailScreen(sessionId = id, onBack = { navController.popBackStack() })
            }
            composable(
                route = "$WRITING_PRACTICE_ROUTE/{charset}/{character}",
                arguments = listOf(
                    navArgument("charset") { type = NavType.StringType },
                    navArgument("character") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val charset = backStackEntry.arguments?.getString("charset").orEmpty()
                val character = backStackEntry.arguments?.getString("character").orEmpty()
                val previous = KanaStrokeCounts.previous(charset, character)
                val next = KanaStrokeCounts.next(charset, character)
                WritingPracticeScreen(
                    charset = charset,
                    character = character,
                    previousCharacter = previous,
                    nextCharacter = next,
                    onPrevious = previous?.let { { navController.navigate(writingRoute(charset, it)) } },
                    onNext = next?.let { { navController.navigate(writingRoute(charset, it)) } },
                    onExit = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun writingRoute(charset: String, character: String): String =
    "$WRITING_PRACTICE_ROUTE/${Uri.encode(charset)}/${Uri.encode(character)}"
