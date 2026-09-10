package com.example.mazmazradio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.example.mazmazradio.ui.MainScreen
import com.example.mazmazradio.ui.RadioViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RadioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initMediaController(this)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}