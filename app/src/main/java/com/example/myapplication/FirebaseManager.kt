package com.example.myapplication

import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private val db = FirebaseFirestore.getInstance()
    private val playersCollection = db.collection("players")

    fun listenToPlayers(onPlayersChanged: (List<Player>) -> Unit) {
        playersCollection.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val playersList = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: ""
                val matches = doc.getLong("matchesPlayed")?.toInt() ?: 0
                val won = doc.getLong("matchesWon")?.toInt() ?: 0
                val bestPPT = doc.getDouble("bestPPT") ?: 0.0

                Player(name = name, matchesPlayed = matches, matchesWon = won, bestPPT = bestPPT)
            }
            onPlayersChanged(playersList)
        }
    }

    fun addNewPlayer(playerName: String, onComplete: (Boolean) -> Unit) {
        val newPlayerMap = mapOf(
            "name" to playerName,
            "matchesPlayed" to 0,
            "matchesWon" to 0,
            "bestPPT" to 0.0
        )
        playersCollection.document(playerName).set(newPlayerMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updatePlayerStats(
        playerName: String,
        currentPPT: Double,
        isWinner: Boolean,
        onRecordBroken: (String, Double) -> Unit
    ) {
        val docRef = playersCollection.document(playerName)
        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentMatches = document.getLong("matchesPlayed")?.toInt() ?: 0
                val currentWon = document.getLong("matchesWon")?.toInt() ?: 0
                val previousBestPPT = document.getDouble("bestPPT") ?: 0.0

                val updatedMatches = currentMatches + 1
                val updatedWon = if (isWinner) currentWon + 1 else currentWon
                val isNewRecord = currentPPT > previousBestPPT
                val finalBestPPT = if (isNewRecord) currentPPT else previousBestPPT

                docRef.update(
                    "matchesPlayed", updatedMatches,
                    "matchesWon", updatedWon,
                    "bestPPT", finalBestPPT
                ).addOnSuccessListener {
                    if (isNewRecord) {
                        onRecordBroken(playerName, finalBestPPT)
                    }
                }
            }
        }
    }
}