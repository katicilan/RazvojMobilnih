package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel

// Pomoćna klasa za pamćenje povijesti kriket pogodaka (kako bi Undo/Back radio)
data class CricketHistoryEntry(
    val player: String,
    val sector: String,
    val gainedPoints: Int,
    val addedHit: Boolean
)

class GameViewModel : ViewModel() {
    var firebasePlayers = mutableStateListOf<Player>()
    var selectedPlayers = mutableStateListOf<String>()
    var playersScores = mutableStateMapOf<String, Int>()
    var currentPlayerIndex by mutableStateOf(0)

    var currentRound by mutableStateOf(1)
    var currentTrackedMode by mutableStateOf(DartMode.MODE_501)

    var currentDarts = mutableStateListOf<DartThrow?>(null, null, null)

    var isDoubleActive by mutableStateOf(false)
    var isTripleActive by mutableStateOf(false)

    var playerTotalPoints = mutableStateMapOf<String, Int>()
    var playerTotalThrows = mutableStateMapOf<String, Int>()

    var winnerName by mutableStateOf<String?>(null)
    var isBustTriggered by mutableStateOf(false)
    private var scoreAtStartOfTurn = 501

    var cricketStatus = mutableStateMapOf<String, Map<String, Int>>()
    // Lista u kojoj pamtimo pogotke unutar trenutne runde za kriket (za Undo)
    private var cricketHistory = mutableStateListOf<CricketHistoryEntry>()

    fun getMaxRounds(mode: DartMode): Int {
        return when (mode) {
            DartMode.MODE_301 -> 15
            DartMode.MODE_501 -> 20
            DartMode.MODE_1001 -> 40
            else -> 15
        }
    }

    fun startNewGame(mode: DartMode, players: List<String>) {
        currentTrackedMode = mode
        selectedPlayers.clear()
        selectedPlayers.addAll(players)
        playersScores.clear()
        playerTotalPoints.clear()
        playerTotalThrows.clear()
        cricketStatus.clear()
        cricketHistory.clear()
        winnerName = null
        isBustTriggered = false
        currentRound = 1

        val startingPoints = when(mode) {
            DartMode.MODE_301 -> 301
            DartMode.CRICKET -> 0
            DartMode.MODE_1001 -> 1001
            else -> 501
        }

        players.forEach { playerName ->
            playersScores[playerName] = startingPoints
            playerTotalPoints[playerName] = 0
            playerTotalThrows[playerName] = 0

            cricketStatus[playerName] = mapOf(
                "20" to 0, "19" to 0, "18" to 0, "17" to 0, "16" to 0, "15" to 0, "BULL" to 0
            )
        }
        currentPlayerIndex = 0
        scoreAtStartOfTurn = startingPoints
        resetDarts()
    }

    fun monitorFirebasePlayers() {
        FirebaseManager.listenToPlayers { players ->
            firebasePlayers.clear()
            firebasePlayers.addAll(players)
        }
    }

    fun getCurrentPlayer(): String {
        return selectedPlayers.getOrNull(currentPlayerIndex) ?: "Gost"
    }

    fun shuffleSelectedPlayers() {
        selectedPlayers.shuffle()
    }

    fun handleNumberClick(number: Int, context: Context) {
        if (winnerName != null || isBustTriggered) return

        val freeDartIndex = currentDarts.indexOfFirst { it == null }
        if (freeDartIndex != -1) {
            val wasDoubleUsed = isDoubleActive
            val multiplier = when {
                isDoubleActive -> 2
                isTripleActive -> 3
                else -> 1
            }

            val throwResult = DartThrow(baseNumber = number, multiplier = multiplier)
            currentDarts[freeDartIndex] = throwResult

            isDoubleActive = false
            isTripleActive = false

            val player = getCurrentPlayer()
            val currentScore = playersScores[player] ?: 501
            val newScore = currentScore - throwResult.score

            if (newScore == 0) {
                if (wasDoubleUsed || (number == 25 && multiplier == 2)) {
                    playersScores[player] = 0
                    winnerName = player
                    finishGameAndUploadStats(context)
                } else {
                    playersScores[player] = scoreAtStartOfTurn
                    isBustTriggered = true
                }
            } else if (newScore < 2) {
                playersScores[player] = scoreAtStartOfTurn
                isBustTriggered = true
            } else {
                playersScores[player] = newScore
            }
        }
    }

    fun undoLastDart() {
        if (winnerName != null) return

        val lastDartIndex = currentDarts.indexOfLast { it != null }
        if (lastDartIndex != -1) {
            val lastThrow = currentDarts[lastDartIndex] ?: return
            val player = getCurrentPlayer()

            val currentScore = if (isBustTriggered) scoreAtStartOfTurn else (playersScores[player] ?: 501)
            playersScores[player] = currentScore + lastThrow.score

            currentDarts[lastDartIndex] = null
            isBustTriggered = false
        }
    }

    fun handleBackAction(): Boolean {
        if (winnerName != null) return false

        // Ako igramo CRICKET mod, radimo undo preko kriket povijesti
        if (currentTrackedMode == DartMode.CRICKET) {
            if (cricketHistory.isNotEmpty()) {
                val lastAction = cricketHistory.removeAt(cricketHistory.size - 1)
                val currentHitsMap = cricketStatus[lastAction.player]?.toMutableMap() ?: return true

                if (lastAction.addedHit) {
                    val currentHits = currentHitsMap[lastAction.sector] ?: 0
                    if (currentHits > 0) {
                        currentHitsMap[lastAction.sector] = currentHits - 1
                        cricketStatus[lastAction.player] = currentHitsMap
                    }
                }

                if (lastAction.gainedPoints > 0) {
                    val currentPoints = playersScores[lastAction.player] ?: 0
                    playersScores[lastAction.player] = maxOf(0, currentPoints - lastAction.gainedPoints)
                }
                return true
            }
            return false
        }

        // Standardno poništavanje za X01 igre
        val lastDartIndex = currentDarts.indexOfLast { it != null }
        if (lastDartIndex != -1) {
            undoLastDart()
            return true
        }
        return false
    }

    fun nextPlayer(context: Context) {
        val player = getCurrentPlayer()
        if (winnerName != null) return

        val turnTotal = currentDarts.filterNotNull().sumOf { it.score }
        if (!isBustTriggered) {
            playerTotalPoints[player] = (playerTotalPoints[player] ?: 0) + turnTotal
        }
        playerTotalThrows[player] = (playerTotalThrows[player] ?: 0) + 1

        resetDarts()
        isBustTriggered = false

        if (selectedPlayers.isNotEmpty()) {
            if (currentPlayerIndex == selectedPlayers.size - 1) {
                val maxRounds = getMaxRounds(currentTrackedMode)
                if (currentRound >= maxRounds) {
                    val bestPlayer = selectedPlayers.minByOrNull { playersScores[it] ?: 501 }
                    winnerName = bestPlayer
                    finishGameAndUploadStats(context)
                    return
                } else {
                    currentRound++
                }
            }
            currentPlayerIndex = (currentPlayerIndex + 1) % selectedPlayers.size
        }
        scoreAtStartOfTurn = playersScores[getCurrentPlayer()] ?: 501
    }

    fun getPlayerPPT(player: String): String {
        val totalPoints = playerTotalPoints[player] ?: 0
        val totalThrows = playerTotalThrows[player] ?: 0
        if (totalThrows == 0) return "0.0"
        val ppt = totalPoints.toDouble() / totalThrows
        return String.format("%.1f", ppt)
    }

    fun handleCricketSectorClick(sector: String, context: Context) {
        if (winnerName != null) return

        val activePlayer = getCurrentPlayer()
        val currentHitsMap = cricketStatus[activePlayer] ?: return
        val currentHits = currentHitsMap[sector] ?: 0
        val updatedHitsMap = currentHitsMap.toMutableMap()

        if (currentHits < 3) {
            updatedHitsMap[sector] = currentHits + 1
            cricketStatus[activePlayer] = updatedHitsMap

            // Spremi u povijest za Undo gumb
            cricketHistory.add(CricketHistoryEntry(activePlayer, sector, gainedPoints = 0, addedHit = true))

            checkCricketWinner(context)
        } else {
            val numericValue = if (sector == "BULL") 25 else sector.toInt()
            val anyOpponentNotClosed = selectedPlayers.any { player ->
                player != activePlayer && (cricketStatus[player]?.get(sector) ?: 0) < 3
            }

            if (anyOpponentNotClosed) {
                val currentPoints = playersScores[activePlayer] ?: 0
                playersScores[activePlayer] = currentPoints + numericValue

                // Spremi u povijest za Undo gumb
                cricketHistory.add(CricketHistoryEntry(activePlayer, sector, gainedPoints = numericValue, addedHit = false))

                checkCricketWinner(context)
            }
        }
    }

    private fun checkCricketWinner(context: Context) {
        val activePlayer = getCurrentPlayer()
        val currentHitsMap = cricketStatus[activePlayer] ?: return
        val allSectorsClosed = currentHitsMap.values.all { it >= 3 }

        if (allSectorsClosed) {
            val activePlayerPoints = playersScores[activePlayer] ?: 0
            val hasHighestPoints = selectedPlayers.all { player ->
                (playersScores[player] ?: 0) <= activePlayerPoints
            }

            if (hasHighestPoints) {
                winnerName = activePlayer
                finishGameAndUploadStats(context)
            }
        }
    }

    fun nextCricketPlayer(context: Context) {
        if (winnerName != null) return

        // Očisti povijest prethodnog igrača jer prelazi red na novu osobu
        cricketHistory.clear()

        if (selectedPlayers.isNotEmpty()) {
            if (currentPlayerIndex == selectedPlayers.size - 1) {
                if (currentRound >= 15) {
                    val bestPlayer = selectedPlayers.maxWithOrNull(compareBy(
                        { player -> cricketStatus[player]?.values?.count { it >= 3 } ?: 0 },
                        { player -> playersScores[player] ?: 0 }
                    ))
                    winnerName = bestPlayer
                    finishGameAndUploadStats(context)
                    return
                } else {
                    currentRound++
                }
            }
            currentPlayerIndex = (currentPlayerIndex + 1) % selectedPlayers.size
        }
    }

    private fun resetDarts() {
        currentDarts[0] = null
        currentDarts[1] = null
        currentDarts[2] = null
        isDoubleActive = false
        isTripleActive = false
    }

    fun finishGameAndUploadStats(context: Context) {
        selectedPlayers.forEach { playerName ->
            val finalPPTString = getPlayerPPT(playerName)
            val finalPPT = finalPPTString.toDoubleOrNull() ?: 0.0

            val isThisPlayerWinner = (playerName == winnerName)

            FirebaseManager.updatePlayerStats(playerName, finalPPT, isThisPlayerWinner) { name, newRecord ->
                sendLocalNotification(context, playerName = name, pptValue = newRecord)
            }
        }
    }

    private fun sendLocalNotification(context: Context, playerName: String, pptValue: Double) {
        val channelId = "dart_records"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Rekordi", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.star_on)
            .setContentTitle("Novi osobni rekord! 🎯")
            .setContentText("Igrač $playerName je ostvario svoj najbolji PPT: $pptValue!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

data class DartThrow(
    val baseNumber: Int,
    val multiplier: Int
) {
    val score: Int get() = baseNumber * multiplier
}