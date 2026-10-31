package com.zagent.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.zagent.app.ui.screens.ChatScreen
import com.zagent.app.ui.screens.EditorScreen
import com.zagent.app.ui.screens.FileBrowserScreen
import com.zagent.app.ui.screens.OnboardingScreen
import com.zagent.app.ui.screens.SettingsScreen
import com.zagent.app.ui.screens.SplashScreen
import com.zagent.app.ui.screens.TerminalScreen
import com.zagent.app.ui.theme.ZAgentTheme
import com.zagent.app.ui.theme.Zcolors
import com.zagent.app.ui.viewmodel.MainViewModel
import com.zagent.app.update.UpdateHost

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            val dark = viewModel.themeIsDark
            ZAgentTheme(darkTheme = dark) {
                SurfaceBackground()
                AppRoot(viewModel, systemDark)
            }
        }
    }
}

@Composable
private fun SurfaceBackground() {
    val c = Zcolors
    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
    )
}

@Composable
private fun AppRoot(viewModel: MainViewModel, systemDark: Boolean) {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onDone = { showSplash = false })
        return
    }

    if (!viewModel.onboardingDone) {
        OnboardingScreen(
            currentModel = viewModel.modelInput,
            onModelSelected = { m -> viewModel.saveSettings(viewModel.apiKeyInput, m, viewModel.baseUrlInput, viewModel.autoModeInput) },
            onFolderPicked = { uri -> viewModel.setWorkingFolder(uri) },
            onApiKeySaved = { key -> viewModel.saveSettings(key, viewModel.modelInput, viewModel.baseUrlInput, viewModel.autoModeInput) },
            initialApiKey = viewModel.apiKeyInput,
            onFinished = { viewModel.completeOnboarding() }
        )
        return
    }

    val context = LocalContext.current
    var lastBackPress by remember { mutableStateOf(0L) }
    val canGoBack = viewModel.isSettingsOpen || viewModel.isTerminalOpen || viewModel.isEditorOpen || viewModel.isFileBrowserOpen
    BackHandler(enabled = canGoBack) {
        when {
            viewModel.isSettingsOpen -> viewModel.isSettingsOpen = false
            viewModel.isTerminalOpen -> viewModel.isTerminalOpen = false
            viewModel.isEditorOpen -> viewModel.isEditorOpen = false
            viewModel.isFileBrowserOpen -> viewModel.isFileBrowserOpen = false
        }
    }
    BackHandler(enabled = !canGoBack) {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 2000) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPress = now
            android.widget.Toast.makeText(context, "Appuyez encore pour quitter", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (viewModel.isTerminalOpen) {
            TerminalScreen(
                controller = viewModel.terminal,
                onBack = { viewModel.isTerminalOpen = false }
            )
        } else if (viewModel.isEditorOpen) {
            EditorScreen(
                viewModel = viewModel,
                onBack = { viewModel.isEditorOpen = false }
            )
        } else if (viewModel.isFileBrowserOpen) {
            FileBrowserScreen(
                viewModel = viewModel,
                onBack = { viewModel.isFileBrowserOpen = false }
            )
        } else {
            ChatScreen(
                viewModel = viewModel,
                onOpenSettings = { viewModel.isSettingsOpen = true }
            )
        }

        if (viewModel.isSettingsOpen) {
            SettingsScreenCompat(viewModel)
        }

        UpdateHost()
    }
}

@Composable
private fun SettingsScreenCompat(viewModel: MainViewModel) {
    SettingsScreen(
        viewModel = viewModel,
        onDismiss = { viewModel.isSettingsOpen = false }
    )
}
