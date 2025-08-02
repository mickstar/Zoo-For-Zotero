package com.mickstarify.zooforzotero.SyncSetup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mickstarify.zooforzotero.SyncSetup.ui.components.ApiKeyInstructionsCard
import com.mickstarify.zooforzotero.SyncSetup.viewmodels.ManualApiViewModel
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun SyncSetupApiKeyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: ManualApiViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ManualApiViewModel.Effect.NavigateToLibrary -> {
                    onNavigateToLibrary()
                }

                is ManualApiViewModel.Effect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    SyncSetupApiKeyScreenContent(
        state = state,
        onEvent = viewModel::dispatch
    )
}

@Composable
private fun SyncSetupApiKeyScreenContent(
    state: ManualApiViewModel.State,
    onEvent: (ManualApiViewModel.Event) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Enter API Key",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect to your Zotero account using an API key",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Instructions Card
        ApiKeyInstructionsCard()

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = state.apiKey,
            onValueChange = { onEvent(ManualApiViewModel.Event.ApiKeyChanged(it)) },
            label = { Text("API Key") },
            placeholder = { Text("Enter your Zotero API key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isValidating,
            isError = state.errorMessage != null,
            supportingText = {
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text("The key should be a long string of letters and numbers")
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onEvent(ManualApiViewModel.Event.SubmitApiKey) },
            enabled = state.isSubmitEnabled && !state.isValidating,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (state.isValidating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Validating...")
                }
            } else {
                Text("Continue with API Key")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onEvent(ManualApiViewModel.Event.NavigateBack) },
            enabled = !state.isValidating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview
@Composable
private fun SyncSetupApiKeyScreenPreview() {
    ZoteroTheme {
        SyncSetupApiKeyScreenContent(
            state = ManualApiViewModel.State(),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun SyncSetupApiKeyScreenWithContentPreview() {
    ZoteroTheme {
        SyncSetupApiKeyScreenContent(
            state = ManualApiViewModel.State(
                apiKey = "sample-api-key-12345",
                isSubmitEnabled = true
            ),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun SyncSetupApiKeyScreenValidatingPreview() {
    ZoteroTheme {
        SyncSetupApiKeyScreenContent(
            state = ManualApiViewModel.State(
                apiKey = "validating-key-123",
                isValidating = true,
                isSubmitEnabled = true
            ),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun SyncSetupApiKeyScreenErrorPreview() {
    ZoteroTheme(darkTheme = true) {
        SyncSetupApiKeyScreenContent(
            state = ManualApiViewModel.State(
                apiKey = "invalid-key",
                errorMessage = "Error: Your API key was not found. Please check your key and try again."
            ),
            onEvent = {}
        )
    }
}