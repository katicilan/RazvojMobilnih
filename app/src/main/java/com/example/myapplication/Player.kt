package com.example.myapplication

data class Player(
    val id: String = "",
    val name: String = "",
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0, // <-- DODANO: Novo polje za broj pobjeda
    val currentPPT: Double = 0.0,
    val bestPPT: Double = 0.0
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "matchesPlayed" to matchesPlayed,
            "matchesWon" to matchesWon, // <-- DODANO: Mapiranje za Firebase
            "currentPPT" to currentPPT,
            "bestPPT" to bestPPT
        )
    }
}