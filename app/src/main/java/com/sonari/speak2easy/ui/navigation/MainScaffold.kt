package com.sonari.speak2easy.ui.navigation

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
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sonari.speak2easy.di.LocalAppContainer
import com.sonari.speak2easy.ui.chart.ChartScreen
import com.sonari.speak2easy.ui.feedback.FeedbackCategory
import com.sonari.speak2easy.ui.feedback.FeedbackScreen
import com.sonari.speak2easy.ui.lessons.LessonsScreen
import com.sonari.speak2easy.ui.practice.PracticePlan
import com.sonari.speak2easy.ui.practice.PracticeScreen
import com.sonari.speak2easy.ui.progress.ProgressScreen
import com.sonari.speak2easy.ui.progress.SessionDetailScreen
import com.sonari.speak2easy.ui.settings.SettingsScreen
import com.sonari.speak2easy.ui.writing.KanaStrokeCounts
import com.sonari.speak2easy.ui.writing.WritingHomeScreen
import com.sonari.speak2easy.ui.writing.WritingPracticeScreen
import com.sonari.speak2easy.ui.theme.SonariFonts
import com.sonari.speak2easy.ui.theme.SonariTheme

private const val PRACTICE_ROUTE = "practice"
private const val SESSION_DETAIL_ROUTE = "session-detail"
private const val WRITING_PRACTICE_ROUTE = "writing-practice"
private const val FEEDBACK_ROUTE = "feedback"

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
                LessonsScreen(
                    onStartPractice = { source, options ->
                        pendingPractice = PracticePlan(source, options)
                        navController.navigate(PRACTICE_ROUTE)
                    },
                    onReportBug = { navController.navigate(feedbackRoute("lessons")) },
                )
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
            composable(AppTab.SETTINGS.route) {
                SettingsScreen(onReportBug = { navController.navigate(feedbackRoute("settings")) })
            }
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
                route = "$FEEDBACK_ROUTE?source={source}",
                arguments = listOf(navArgument("source") { type = NavType.StringType; defaultValue = "unknown" }),
            ) { entry ->
                val source = entry.arguments?.getString("source") ?: "unknown"
                // Settings often comes from the explicit Help & Report row → preselect General;
                // anywhere else (Lessons header) → keep the same default.
                val initial = FeedbackCategory.GENERAL
                FeedbackScreen(
                    initialCategory = initial,
                    sourceScreen = source,
                    onDismiss = { navController.popBackStack() },
                )
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
                // Prev/Next replace the current writing entry rather than stacking — otherwise
                // pressing Next 10 times leaves 10 entries on the back stack and Exit needs 10
                // taps to leave the writing flow. popUpTo with inclusive=true drops the current
                // entry before pushing the new one, so the back stack stays one-deep.
                val replaceCurrent: NavOptionsBuilder.() -> Unit = {
                    popUpTo("$WRITING_PRACTICE_ROUTE/{charset}/{character}") { inclusive = true }
                    launchSingleTop = true
                }
                WritingPracticeScreen(
                    charset = charset,
                    character = character,
                    previousCharacter = previous,
                    nextCharacter = next,
                    onPrevious = previous?.let { { navController.navigate(writingRoute(charset, it), replaceCurrent) } },
                    onNext = next?.let { { navController.navigate(writingRoute(charset, it), replaceCurrent) } },
                    onExit = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun writingRoute(charset: String, character: String): String =
    "$WRITING_PRACTICE_ROUTE/${Uri.encode(charset)}/${Uri.encode(character)}"

private fun feedbackRoute(source: String): String =
    "$FEEDBACK_ROUTE?source=${Uri.encode(source)}"
