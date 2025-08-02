package com.mickstarify.zooforzotero.SyncSetup.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mickstarify.zooforzotero.SyncSetup.ui.components.ZoteroOAuthWebView
import com.mickstarify.zooforzotero.SyncSetup.viewmodels.ZoteroAccountAuthViewModel
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSetupWebViewScreen(
    onNavigateBack: () -> Unit,
    onAuthComplete: () -> Unit,
    viewModel: ZoteroAccountAuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val effects by viewModel.effects.collectAsState()

    // Start OAuth flow when screen is displayed
    LaunchedEffect(Unit) {
        viewModel.handleEvent(ZoteroAccountAuthViewModel.Event.StartOAuthFlow)
    }

    // Handle effects
    LaunchedEffect(effects) {
        when (effects) {
            is ZoteroAccountAuthViewModel.Effect.NavigateToLibrary -> {
                onAuthComplete()
            }

            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zotero Authentication") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to Zotero API...",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "If this takes too long, try using the API key method instead.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                state.authorizationUrl != null -> {
                    ZoteroOAuthWebView(
                        url = state.authorizationUrl!!,
                        onOAuthCallback = { token, verifier ->
                            viewModel.handleEvent(
                                ZoteroAccountAuthViewModel.Event.HandleOAuthCallback(
                                    token,
                                    verifier
                                )
                            )
                        },
                        onPageFinished = {
                            // Page finished loading
                        },
                        onError = { error ->
                            viewModel.handleEvent(ZoteroAccountAuthViewModel.Event.DismissError)
                        }
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Unable to connect to Zotero",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Please check your internet connection and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                viewModel.handleEvent(ZoteroAccountAuthViewModel.Event.StartOAuthFlow)
                            }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Error dialog
    state.error?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                viewModel.handleEvent(ZoteroAccountAuthViewModel.Event.DismissError)
            },
            title = { Text("Authentication Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.handleEvent(ZoteroAccountAuthViewModel.Event.DismissError)
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview
@Composable
fun SyncSetupWebViewScreenPreview() {
    ZoteroTheme {
        SyncSetupWebViewScreen(
            onNavigateBack = {},
            onAuthComplete = {}
        )
    }
}

@Preview
@Composable
fun SyncSetupWebViewScreenDarkPreview() {
    ZoteroTheme(darkTheme = true) {
        SyncSetupWebViewScreen(
            onNavigateBack = {},
            onAuthComplete = {}
        )
    }
}