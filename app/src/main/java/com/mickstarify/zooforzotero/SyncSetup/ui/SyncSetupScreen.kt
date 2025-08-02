package com.mickstarify.zooforzotero.SyncSetup.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mickstarify.zooforzotero.SyncSetup.ui.screens.SyncSetupApiKeyScreen
import com.mickstarify.zooforzotero.SyncSetup.ui.screens.SyncSetupLandingScreen
import com.mickstarify.zooforzotero.SyncSetup.ui.screens.SyncSetupWebViewScreen
import com.mickstarify.zooforzotero.SyncSetup.viewmodels.SyncSetupViewModel
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

// Navigation Routes
object SyncSetupRoutes {
    const val LANDING = "sync_setup_landing"
    const val WEBVIEW_AUTH = "sync_setup_webview"
    const val API_KEY_ENTRY = "sync_setup_api_key"
}

@Composable
fun SyncSetupScreen(
    onNavigateToLibrary: () -> Unit,
    viewModel: SyncSetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SyncSetupViewModel.Effect.NavigateToZoteroApiSetup -> {
                    navController.navigate(SyncSetupRoutes.WEBVIEW_AUTH)
                }

                is SyncSetupViewModel.Effect.NavigateToApiKeyEntry -> {
                    navController.navigate(SyncSetupRoutes.API_KEY_ENTRY)
                }

                is SyncSetupViewModel.Effect.NavigateToLibrary -> {
                    onNavigateToLibrary()
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = SyncSetupRoutes.LANDING,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(SyncSetupRoutes.LANDING) {
                SyncSetupLandingScreen(
                    state = state,
                    onEvent = viewModel::dispatch
                )
            }

            composable(SyncSetupRoutes.WEBVIEW_AUTH) {
                SyncSetupWebViewScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAuthComplete = {
                        onNavigateToLibrary()
                    }
                )
            }

            composable(SyncSetupRoutes.API_KEY_ENTRY) {
                SyncSetupApiKeyScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToLibrary = {
                        onNavigateToLibrary()
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun SyncSetupScreenPreview() {
    ZoteroTheme {
        SyncSetupScreen(
            onNavigateToLibrary = {}
        )
    }
}