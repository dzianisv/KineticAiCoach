package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.MainViewModel
import com.example.ui.screens.ClassResultsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OnboardingChatScreen
import com.example.ui.screens.PaywallScreen
import com.example.ui.screens.PoseTrackerScreen
import com.example.ui.screens.TodaysClassScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val profile by viewModel.userProfile.collectAsState()

    // Render a high-contrast black placeholder to prevent flashing while Room loads the profile
    if (profile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    // Dynamic start destination based on authentication and stats complete status
    val startDestination = if (profile?.isLoggedIn == true) {
        if (profile?.name.isNullOrBlank()) {
            "onboarding"
        } else {
            "dashboard"
        }
    } else {
        "login"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { name, email ->
                    viewModel.signInUser(name, email)
                    navController.navigate("onboarding") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("onboarding") {
            OnboardingChatScreen(
                viewModel = viewModel,
                onOnboardingComplete = {
                    navController.navigate("dashboard") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            val ctx = LocalContext.current
            LaunchedEffect(Unit) {
                (ctx as? Activity)?.let { viewModel.checkPaymentIssues(it) }
            }
            DashboardScreen(
                viewModel = viewModel,
                onStartWorkout = { exerciseName ->
                    navController.navigate("pose_tracker/$exerciseName")
                },
                onStartClass = {
                    if (viewModel.startTodaysClass()) {
                        navController.navigate("todays_class")
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate("onboarding")
                },
                onUpgradeClick = {
                    viewModel.triggerPaywall("about_tab")
                }
            )
        }

        composable(
            route = "pose_tracker/{exerciseName}",
            arguments = listOf(navArgument("exerciseName") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseName = backStackEntry.arguments?.getString("exerciseName") ?: "Squats"
            PoseTrackerScreen(
                exerciseName = exerciseName,
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // --- PRD v2: today's class routes (Lane B) ---
        composable("todays_class") {
            TodaysClassScreen(
                viewModel = viewModel,
                onClassFinished = { classId ->
                    navController.navigate("class_results/$classId") {
                        popUpTo("dashboard")
                    }
                },
                onExit = {
                    navController.popBackStack("dashboard", inclusive = false)
                }
            )
        }

        composable(
            route = "class_results/{classId}",
            arguments = listOf(navArgument("classId") { type = NavType.StringType })
        ) { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId")?.toIntOrNull() ?: 0
            ClassResultsScreen(
                viewModel = viewModel,
                classId = classId,
                onDone = {
                    navController.popBackStack("dashboard", inclusive = false)
                }
            )
        }
        }

        val showPaywall by viewModel.showPaywall.collectAsState()
        if (showPaywall) {
            val proPlans by viewModel.proPlans.collectAsState()
            val billingConnected by viewModel.billingConnected.collectAsState()
            val isPro by viewModel.isPro.collectAsState()
            val trialDaysRemaining by viewModel.trialDaysRemaining.collectAsState()
            val trialExpired by viewModel.trialExpired.collectAsState()
            val activity = LocalContext.current as? Activity
            PaywallScreen(
                proPlans = proPlans,
                isConnected = billingConnected,
                isPro = isPro,
                trialDaysRemaining = trialDaysRemaining,
                trialExpired = trialExpired,
                onSubscribe = { basePlanId ->
                    activity?.let { viewModel.launchPurchase(it, basePlanId) }
                },
                onRestore = { viewModel.restorePurchases() },
                onManageSubscription = {
                    activity?.let {
                        val url = viewModel.manageSubscriptionUrl(it.packageName)
                        it.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                },
                onDismiss = { viewModel.dismissPaywall() }
            )
        }
    }
}
