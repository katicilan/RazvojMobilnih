package com.example.myapplication

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CricketGameScreen(
    playerName: String, // Drži parametar kako bi MainActivity prošla bez greške
    viewModel: GameViewModel,
    onGameFinished: () -> Unit
) {
    val activePlayer = viewModel.getCurrentPlayer()
    val playerPoints = viewModel.playersScores[activePlayer] ?: 0
    val playerHits = viewModel.cricketStatus[activePlayer] ?: mapOf()

    val context = LocalContext.current
    var backPressedTime by remember { mutableStateOf(0L) }

    // --- LOGIKA ZA BACK GUMB (1 KLIK = UKLANJANJE ZADNJEG HIT-A, 2 KLIKA = MODES) ---
    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < 2000) {
            // Dvostruki klik -> povratak na glavni izbornik modova
            onGameFinished()
        } else {
            // Jedan klik -> Poništavanje zadnje strelice preko handleBackAction() iz ViewModela
            val handled = viewModel.handleBackAction()
            if (!handled) {
                backPressedTime = currentTime
                Toast.makeText(context, "Pritisnite ponovo za povratak na glavni izbornik", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Dijalog za pobjedu (bilo prekidom u 15. rundi ili zatvaranjem svih polja)
    if (viewModel.winnerName != null) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = "Kraj utakmice! 🎯", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Pobjednik: ${viewModel.winnerName}!", fontSize = 18.sp) },
            confirmButton = {
                Button(onClick = { onGameFinished() }) {
                    Text("Završi meč")
                }
            }
        )
    }

    // Glavni Box kako bi slika bila u pozadini, a komponente preko nje
    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. POZADINSKA SLIKA SA 50% PROZIRNOSTI ---
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = "Pozadina",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.5f // Točno 50% prozirnosti
        )

        // --- 2. GLAVNI SADRŽAJ EKRANA ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- PRIKAZ BROJA RUNDE NA SAMOM VRHU ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Runda: ${viewModel.currentRound} / 15", // Promijenjeno na 15 jer kriket traje 15 rundi
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE65100), // Narančasta boja za vidljivost preko pozadine
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- INFO O IGRAČU I BODOVIMA ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "IGRAČ", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    Text(text = activePlayer, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Bodovi", fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    Text(text = playerPoints.toString(), fontSize = 26.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TABLICA SEKTORA (20 - 15 + BULL) ---
            val sectors = listOf("20", "19", "18", "17", "16", "15", "BULL")

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                sectors.forEach { sector ->
                    val hits = playerHits[sector] ?: 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f) // Blago prozirna kartica da se vidi wallpaper
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Prikaz kvačica (hits)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(3) { index ->
                                    val isHit = index < hits
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Hit",
                                        tint = if (isHit) Color(0xFF4CAF50) else Color.LightGray.copy(alpha = 0.5f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Gumb za unos pogotka u sektor
                            Button(
                                onClick = { viewModel.handleCricketSectorClick(sector, context) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (hits >= 3) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.width(100.dp)
                            ) {
                                Text(
                                    text = sector,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- GUMB ZA SLJEDEĆEG IGRAČA ---
            Button(
                onClick = { viewModel.nextCricketPlayer(context) }, // POPRAVLJENO: Prolijeđen context parametar!
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SLJEDEĆI IGRAČ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}