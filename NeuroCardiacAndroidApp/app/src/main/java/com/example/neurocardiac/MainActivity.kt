package com.example.neurocardiac

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.neurocardiac.ui.theme.NeuroCardiacTheme
import kotlin.jvm.java

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chatDao = AppDatabase.getDatabase(applicationContext).chatDao()
        enableEdgeToEdge()
        setContent {
            NeuroCardiacTheme {
                val viewModelFactory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(AIChatViewModel::class.java)) {
                            return AIChatViewModel(chatDao) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel class")
                    }
                }
                val chatViewModel: AIChatViewModel = viewModel(factory = viewModelFactory)
                AIChatScreen(viewModel = chatViewModel)
            }
        }
    }
}