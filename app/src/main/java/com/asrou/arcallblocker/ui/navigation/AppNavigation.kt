package com.asrou.arcallblocker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.asrou.arcallblocker.ui.blacklist.BlacklistScreen
import com.asrou.arcallblocker.ui.blockedcalls.BlockedCallsScreen
import com.asrou.arcallblocker.ui.diagnostics.DiagnosticsScreen
import com.asrou.arcallblocker.ui.home.HomeScreen
import com.asrou.arcallblocker.ui.onboarding.OnboardingScreen
import com.asrou.arcallblocker.ui.onboarding.OnboardingViewModel
import com.asrou.arcallblocker.ui.schedule.ScheduleScreen
import com.asrou.arcallblocker.ui.settings.SettingsScreen
import com.asrou.arcallblocker.ui.whitelist.WhitelistScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val BLOCKED_CALLS = "blocked_calls"
    const val BLACKLIST = "blacklist"
    const val WHITELIST = "whitelist"
    const val SCHEDULE = "schedule"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Check if onboarding is completed to determine start destination
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isOnboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()

    val startDestination = if (isOnboardingCompleted) Routes.HOME else Routes.ONBOARDING

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToBlockedCalls = { navController.navigate(Routes.BLOCKED_CALLS) },
                onNavigateToBlacklist = { navController.navigate(Routes.BLACKLIST) },
                onNavigateToWhitelist = { navController.navigate(Routes.WHITELIST) },
                onNavigateToSchedule = { navController.navigate(Routes.SCHEDULE) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.BLOCKED_CALLS) {
            BlockedCallsScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.BLACKLIST) {
            BlacklistScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.WHITELIST) {
            WhitelistScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.SCHEDULE) {
            ScheduleScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDiagnostics = { navController.navigate(Routes.DIAGNOSTICS) }
            )
        }

        composable(Routes.DIAGNOSTICS) {
            DiagnosticsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
