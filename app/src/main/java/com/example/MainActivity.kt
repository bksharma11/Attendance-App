package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.AttendXApp
import com.example.ui.AttendXViewModel
import com.example.ui.theme.AttendXTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AttendXViewModel by viewModels {
        AttendXViewModel.provideFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AttendXTheme {
                AttendXApp(viewModel = viewModel)
            }
        }
    }
}
