package com.example.myapplication

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun X01GameScreen(
    mode: DartMode,
    viewModel: GameViewModel,
    playerName: String,
    onGameFinished: () -> Unit
) {
    val currentPlayer = viewModel.getCurrentPlayer()
    val currentScore = viewModel.playersScores[currentPlayer] ?: 501
    val winner = viewModel.winnerName
    val isBust = viewModel.isBustTriggered

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backPressedTime by remember { mutableStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            // Dvostruki klik -> povratak na glavni izbornik
            onGameFinished()
        } else {
            // Jedan klik -> izvrši poništavanje strelice
            val handled = viewModel.handleBackAction()
            if (!handled) {
                backPressedTime = currentTime
                Toast.makeText(context, "Pritisnite ponovo za povratak na glavni izbornik", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = "Pozadina",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- GORNJI DIO: Info o igri, Broj Runde i Trenutni igrač ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mod: ${mode.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    // ISPIS BROJA RUNDE
                    Text(
                        text = "Runda: ${viewModel.currentRound} / ${viewModel.getMaxRounds(viewModel.currentTrackedMode)}", // <-- Ovdje čitamo direktno iz ViewModela!
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentPlayer,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // --- SREDIŠNJI DIO: Glavni prikaz bodova ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (winner != null) {
                    Text(
                        text = "KRAJ IGRE! 🏆\nPobjednik: $winner",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF4CAF50),
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onGameFinished) {
                        Text("Završi igru i spremi statistiku")
                    }
                } else {
                    Text(
                        text = if (isBust) "BUST! 💥" else "$currentScore",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                        fontWeight = FontWeight.Black,
                        color = if (isBust) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "PPT: ${viewModel.getPlayerPPT(currentPlayer)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }

            // --- PRIKAZ TRENUTNIH STRELICA ---
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Trenutni krug:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        viewModel.currentDarts.forEach { dartThrow ->
                            Card(
                                modifier = Modifier.size(54.dp),
                                onClick = { viewModel.undoLastDart() },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (dartThrow != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val textToShow = when {
                                        dartThrow == null -> "-"
                                        dartThrow.baseNumber == 0 -> "MISS"
                                        dartThrow.baseNumber == 25 -> if (dartThrow.multiplier == 2) "D-BULL" else "BULL"
                                        dartThrow.multiplier == 3 -> "T${dartThrow.baseNumber}"
                                        dartThrow.multiplier == 2 -> "D${dartThrow.baseNumber}"
                                        else -> "${dartThrow.baseNumber}"
                                    }

                                    Text(
                                        text = textToShow,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dartThrow != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.undoLastDart() },
                        enabled = winner == null && viewModel.currentDarts.any { it != null },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, shape = CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Poništi zadnju strelicu",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // --- DONJI DIO: Tipkovnica i gumb Sljedeći ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconToggleButton(
                        checked = viewModel.isDoubleActive,
                        onCheckedChange = {
                            viewModel.isDoubleActive = it
                            if (it) viewModel.isTripleActive = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DOUBLE (x2)", fontWeight = FontWeight.Bold)
                    }

                    FilledIconToggleButton(
                        checked = viewModel.isTripleActive,
                        onCheckedChange = {
                            viewModel.isTripleActive = it
                            if (it) viewModel.isDoubleActive = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TRIPLE (x3)", fontWeight = FontWeight.Bold)
                    }
                }

                val numbers = (1..20).toList() + listOf(25, 0)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    numbers.chunked(4).forEach { rowNumbers ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowNumbers.forEach { number ->
                                val buttonText = when (number) {
                                    25 -> "BULL"
                                    0 -> "MISS"
                                    else -> "$number"
                                }

                                val isTripleBullBlocked = number == 25 && viewModel.isTripleActive

                                Button(
                                    onClick = { viewModel.handleNumberClick(number,context) },
                                    modifier = Modifier.weight(1f),
                                    enabled = winner == null && !isTripleBullBlocked,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (number == 25) Color(0xFFD32F2F) else if (number == 0) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = if (number == 25 || number == 0) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Text(
                                        text = buttonText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.nextPlayer(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = winner == null && (viewModel.currentDarts.all { it != null } || isBust),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("SLJEDEĆI IGRAČ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                }
            }
        }
    }
}