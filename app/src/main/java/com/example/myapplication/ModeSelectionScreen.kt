package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModeSelectionScreen(onModeSelected: (DartMode) -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {


        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = "Pozadina",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.8f
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "ODABERI MOD IGRE", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 32.dp))

            Button(onClick = { onModeSelected(DartMode.MODE_301) }, modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 4.dp)) {
                Text("301", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Button(onClick = { onModeSelected(DartMode.MODE_501) }, modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 4.dp)) {
                Text("501", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Button(onClick = { onModeSelected(DartMode.MODE_1001) }, modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 4.dp)) {
                Text("1001", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Button(onClick = { onModeSelected(DartMode.CRICKET) }, modifier = Modifier.fillMaxWidth().height(60.dp).padding(vertical = 4.dp)) {
                Text("CRICKET", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}