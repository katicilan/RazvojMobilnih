package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val gameViewModel: GameViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
        gameViewModel.monitorFirebasePlayers()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = "mode_selection"
                        ) {
                            // 1. Ekran: Odabir moda igre
                            composable("mode_selection") {
                                ModeSelectionScreen(
                                    onModeSelected = { selectedMode ->
                                        navController.navigate("player_selection/${selectedMode.name}")
                                    }
                                )
                            }

                            // 2. Ekran: Odabir igrača
                            composable(
                                route = "player_selection/{modeName}",
                                arguments = listOf(navArgument("modeName") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val modeName = backStackEntry.arguments?.getString("modeName") ?: "MODE_501"
                                val mode = DartMode.valueOf(modeName)

                                PlayerSelectionScreen(
                                    mode = mode,
                                    viewModel = gameViewModel,
                                    onLetsDartClicked = { selectedPlayersList: List<String> ->
                                        gameViewModel.startNewGame(mode, selectedPlayersList)

                                        if (mode == DartMode.CRICKET) {
                                            navController.navigate("cricket_game")
                                        } else {
                                            val playersString = selectedPlayersList.joinToString(separator = ",")
                                            navController.navigate("x01_game/$playersString")
                                        }
                                    }
                                )
                            }

                            // 3. Ekran: X01 Game Screen
                            composable(
                                route = "x01_game/{playerName}",
                                arguments = listOf(navArgument("playerName") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val nameFromRoute = backStackEntry.arguments?.getString("playerName") ?: "Igrač"

                                X01GameScreen(
                                    mode = gameViewModel.currentTrackedMode,
                                    viewModel = gameViewModel,
                                    playerName = nameFromRoute,
                                    onGameFinished = {
                                        // Ovdje više NE POZIVAMO finishGameAndUploadStats!
                                        navController.navigate("mode_selection") {
                                            popUpTo("mode_selection") { inclusive = true }
                                        }
                                    }
                                )
                            }

// --- 4. Ekran: Cricket Game Screen ---
                            composable(route = "cricket_game") {
                                CricketGameScreen(
                                    playerName = "",
                                    viewModel = gameViewModel,
                                    onGameFinished = {
                                        // Ovdje isto NE POZIVAMO finishGameAndUploadStats!
                                        navController.navigate("mode_selection") {
                                            popUpTo("mode_selection") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}