package com.example.maszyna_stanow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.maszyna_stanow.ui.screens.MainScreen
import com.example.maszyna_stanow.ui.theme.Maszyna_stanowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Maszyna_stanowTheme {
                MainScreen()
            }
        }
    }
}
