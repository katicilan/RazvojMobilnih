package com.example.myapplication

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

@Composable
fun PlayerSelectionScreen(
    viewModel: GameViewModel,
    onPlayersSelected: (List<String>) -> Unit
) {
    val context = LocalContext.current
    var newPlayerName by remember { mutableStateOf("") }

    // Pokreni slušanje Firebase igrača kad se ekran učita
    LaunchedEffect(Unit) {
        viewModel.monitorFirebasePlayers()
    }

    val availablePlayers = viewModel.firebasePlayers
    val selected = viewModel.selectedPlayers

    // --- UPORABA SENZORA ZA PROTRESI (SHAKE DETECTOR) ---
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastShakeTime = 0L

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

                if (gForce > 2.2) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastShakeTime > 1000) {
                        lastShakeTime = currentTime

                        if (selected.size > 1) {
                            viewModel.shuffleSelectedPlayers()
                            Toast.makeText(context, "Redoslijed igrača je promiješan! 🎲", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Odaberite barem 2 igrača za miješanje", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // --- KORISNIČKO SUČELJE (UI) ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Odaberi igrače",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Text(
            text = "💡 Protresi mobitel za miješanje redoslijeda!",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
            Button(
                onClick = {
                    if (newPlayerName.isNotBlank()) {
                        FirebaseManager.addNewPlayer(newPlayerName.trim()) { success ->
                            if (success) {
                                Toast.makeText(context, "Igrač dodan!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        newPlayerName = ""
                    }
                },
                modifier = Modifier.height(56.dp)
            ) {
                Text("Dodaj")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availablePlayers) { player ->
                val isChecked = selected.contains(player.name)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!selected.contains(player.name)) selected.add(player.name)
                                } else {
                                    selected.remove(player.name)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                fontSize = 18.sp,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                            )

                            // Izračun postotka pobjeda
                            val winRate = if (player.matchesPlayed > 0) {
                                (player.matchesWon.toDouble() / player.matchesPlayed.toDouble()) * 100
                            } else {
                                0.0
                            }
                            val formattedWinRate = String.format("%.1f", winRate)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "PPT: ${player.bestPPT}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Partije: ${player.matchesPlayed}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Pobjede: $formattedWinRate%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onPlayersSelected(selected.toList()) },
            enabled = selected.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("LET'S DART 🎯", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}