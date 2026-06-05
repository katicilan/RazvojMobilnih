package com.example.myapplication

enum class DartMode(val displayName: String) {
    CRICKET("Cricket"),
    MODE_501("501"),
    MODE_301("301"),

    MODE_1001("1001")
}

sealed class Screen(val route: String) {
    object ModeSelection : Screen("mode_selection")
    object PlayerSelection : Screen("player_selection/{mode}") {
        fun createRoute(mode: String) = "player_selection/$mode"
    }
    object GameScreen : Screen("game_screen/{mode}/{player}") {
        fun createRoute(mode: String, player: String) = "game_screen/$mode/$player"
    }
}