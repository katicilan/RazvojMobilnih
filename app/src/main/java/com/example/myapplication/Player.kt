package com.example.myapplication

data class Player(
    val id: String = "",
    val name: String = "",
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val currentPPT: Double = 0.0,
    val bestPPT: Double = 0.0
)
