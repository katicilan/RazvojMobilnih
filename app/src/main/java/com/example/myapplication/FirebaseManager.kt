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
                val bestPPT = doc.getDouble("bestPPT") ?: 0.0
                Player(name = name, matchesPlayed = matches, bestPPT = bestPPT)
            }
            onPlayersChanged(playersList)
        }
    }

    // POPRAVLJENO: Prima String i sama kreira objekt za bazu
    fun addNewPlayer(playerName: String, onComplete: (Boolean) -> Unit) {
        val newPlayerMap = mapOf(
            "name" to playerName,
            "matchesPlayed" to 0,
            "bestPPT" to 0.0
        )
        playersCollection.document(playerName).set(newPlayerMap)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // DODANO/POPRAVLJENO: Funkcija koju GameViewModel uporno traži
    fun updatePlayerStats(playerName: String, currentPPT: Double, onRecordBroken: (String, Double) -> Unit) {
        val docRef = playersCollection.document(playerName)
        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val currentMatches = document.getLong("matchesPlayed")?.toInt() ?: 0
                val previousBestPPT = document.getDouble("bestPPT") ?: 0.0

                val updatedMatches = currentMatches + 1
                val isNewRecord = currentPPT > previousBestPPT
                val finalBestPPT = if (isNewRecord) currentPPT else previousBestPPT

                docRef.update(
                    "matchesPlayed", updatedMatches,
                    "bestPPT", finalBestPPT
                ).addOnSuccessListener {
                    if (isNewRecord) {
                        onRecordBroken(playerName, finalBestPPT)
                    }
                }
            }
        }
    }
    fun incrementGamesPlayed(playerName: String) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        // Pretpostavka da ti se kolekcija zove "players", a dokument nosi ime igrača
        val playerDocRef = db.collection("players").document(playerName)

        playerDocRef.update("gamesPlayed", com.google.firebase.firestore.FieldValue.increment(1))
            .addOnFailureListener { e ->
                // Logiraj grešku ako ažuriranje ne uspije
            }
    }
}