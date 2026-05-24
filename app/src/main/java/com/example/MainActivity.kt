package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.FitRepository
import com.example.ui.FitAppContent
import com.example.ui.FitViewModel
import com.example.ui.FitViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core persistence initialize
        val database = AppDatabase.getDatabase(this)
        val repository = FitRepository(database.fitDao())

        // ViewModel and state factory binding
        val factory = FitViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[FitViewModel::class.java]

        setContent {
            MyApplicationTheme {
                FitAppContent(viewModel = viewModel)
            }
        }
    }
}
