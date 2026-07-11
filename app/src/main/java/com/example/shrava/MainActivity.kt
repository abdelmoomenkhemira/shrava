package com.example.shrava

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.shrava.ui.screens.SplashScreen
import com.example.shrava.ui.navigation.ShravaNavGraph
import com.example.shrava.ui.theme.ShravaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val showSplash = mutableStateOf(true)

        setContent {
            ShravaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (showSplash.value) {
                        SplashScreen(
                            onSplashFinished = { showSplash.value = false }
                        )
                    } else {
                        ShravaNavGraph()
                    }
                }
            }
        }
    }
}
