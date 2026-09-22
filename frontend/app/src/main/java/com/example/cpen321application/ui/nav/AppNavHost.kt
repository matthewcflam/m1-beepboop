package com.example.cpen321application.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cpen321application.ui.home.HomeScreen
import com.example.cpen321application.ui.live.LiveUpdatesScreen
import com.example.cpen321application.ui.login.LoginScreen
import com.example.cpen321application.ui.timer.TimerScreen

object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
    const val LIVE = "live"
    const val TIMER = "timer"
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onLoginClick = { navController.navigate(Routes.LOGIN) },
                onLiveUpdatesClick = { navController.navigate(Routes.LIVE) },
                onTimerClick = { navController.navigate(Routes.TIMER) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LIVE) {
            LiveUpdatesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TIMER) {
            TimerScreen(onBack = { navController.popBackStack() })
        }
    }
}
