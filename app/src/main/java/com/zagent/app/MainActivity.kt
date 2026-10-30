package com.zagent.app

import com.zagent.app.update.UpdateHost
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.zagent.app.ui.screens.ChatScreen
import com.zagent.app.ui.screens.SettingsScreen
import com.zagent.app.ui.theme.ZAgentTheme
import com.zagent.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ZAgentTheme {
            UpdateHost()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (viewModel.isSettingsOpen) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onDismiss = { viewModel.isSettingsOpen = false }
                        )
                    } else {
                        ChatScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
