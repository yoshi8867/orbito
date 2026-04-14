package com.yoshi0311.orbito.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yoshi0311.orbito.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                var gameStarted by rememberSaveable { mutableStateOf(false) }

                if (gameStarted) {
                    GameScreen(modifier = Modifier.fillMaxSize())
                } else {
                    StartScreen(
                        onStart = { gameStarted = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
