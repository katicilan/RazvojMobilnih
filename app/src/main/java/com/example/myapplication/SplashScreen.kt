package com.example.myapplication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // LaunchedEffect se pokreće čim se ekran iscrta
    LaunchedEffect(Unit) {
        delay(2000) // Čeka tačno 2 sekunde (2000 milisekundi)
        onTimeout() // Preusmjerava na sljedeći ekran
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Pozadinska slika (tvoj wallpaper preko cijelog ekrana)
        Image(
            painter = painterResource(id = R.drawable.wallpaper),
            contentDescription = "Splash Pozadina",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Veliki naziv DARTFLOW na sredini ekrana
        Text(
            text = "DartFlow",
            fontSize = 70.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black, // Bijela boja da se ističe na pozadini
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}