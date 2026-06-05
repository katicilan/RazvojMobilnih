package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSelectionScreen(
    mode: DartMode,
    viewModel: GameViewModel,
    onLetsDartClicked: (List<String>) -> Unit
) {
    var newPlayerName by remember { mutableStateOf("") }
    val selectedPlayers = remember { mutableStateListOf<String>() }
    var isInputVisible by remember { mutableStateOf(false) }

    val avaliablePlayers = viewModel.firebasePlayers

    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. POZADINSKA SLIKA (S 50% PROZIRNOSTI) ---
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = "Pozadina",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, // Rasteže sliku preko cijelog ekrana bez izobličenja
            alpha = 0.7f // Postavlja prozirnost na točno 50%
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Odaberi igrače za mod: ${mode.name}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(avaliablePlayers) { player ->
                    val isSelected = selectedPlayers.contains(player.name)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (isSelected) {
                                selectedPlayers.remove(player.name)
                            } else {
                                selectedPlayers.add(player.name)
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = player.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Odigrano: ${player.matchesPlayed} | Najbolji PPT: ${player.bestPPT}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    if (isSelected) {
                                        selectedPlayers.remove(player.name)
                                    } else {
                                        selectedPlayers.add(player.name)
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isInputVisible) {
                        Button(
                            onClick = { isInputVisible = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Prikaži unos"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Dodaj novog igrača")
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlayerName,
                                    onValueChange = { newPlayerName = it },
                                    label = { Text("Ime novog igrača") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // POPRAVLJENO: Šaljemo samo String (newPlayerName) jer tvoj Firebase to očekuje!
                                IconButton(
                                    onClick = {
                                        if (newPlayerName.isNotBlank()) {
                                            FirebaseManager.addNewPlayer(newPlayerName) { success ->
                                                if (success) {
                                                    newPlayerName = ""
                                                    isInputVisible = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Potvrdi"
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = {
                                        isInputVisible = false
                                        newPlayerName = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Odustani"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onLetsDartClicked(selectedPlayers.toList()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPlayers.isNotEmpty()
            ) {
                Text(text = "LET'S DART! 🎯")
            }
        }
    }
}