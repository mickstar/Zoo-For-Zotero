package com.mickstarify.zooforzotero.SyncSetup.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.SyncSetup.SyncOption
import com.mickstarify.zooforzotero.SyncSetup.viewmodels.SyncSetupViewModel
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun SyncSetupScreen(
    onNavigateToZoteroApiSetup: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    viewModel: SyncSetupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(viewModel.effects) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SyncSetupViewModel.Effect.NavigateToZoteroApiSetup -> {
                    onNavigateToZoteroApiSetup()
                }
                is SyncSetupViewModel.Effect.NavigateToLibrary -> {
                    onNavigateToLibrary()
                }
            }
        }
    }
    
    SyncSetupContent(
        state = state,
        onEvent = viewModel::dispatch
    )
}

@Composable
private fun SyncSetupContent(
    state: SyncSetupViewModel.State,
    onEvent: (SyncSetupViewModel.Event) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.pick_your_cloud_storage_provider),
            style = MaterialTheme.typography.headlineSmall
        )
        
        SyncOptionsSection(
            selectedOption = state.selectedSyncOption,
            onOptionSelected = { option ->
                onEvent(SyncSetupViewModel.Event.SelectSyncOption(option))
            }
        )
        
        Text(
            text = stringResource(R.string.app_frontpage_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { onEvent(SyncSetupViewModel.Event.ProceedWithSetup) },
                enabled = state.isProceedEnabled
            ) {
                Text(stringResource(R.string.proceed))
            }
        }
    }
    
    // API Key Dialog
    if (state.showApiKeyDialog) {
        ApiKeyDialog(
            onSubmit = { apiKey ->
                onEvent(SyncSetupViewModel.Event.SubmitApiKey(apiKey))
            },
            onDismiss = {
                onEvent(SyncSetupViewModel.Event.DismissApiKeyDialog)
            }
        )
    }
}

@Composable
private fun SyncOptionsSection(
    selectedOption: SyncOption,
    onOptionSelected: (SyncOption) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SyncOptionItem(
            text = stringResource(R.string.zotero_account_sync),
            selected = selectedOption == SyncOption.ZoteroAPI,
            onClick = { onOptionSelected(SyncOption.ZoteroAPI) }
        )
        
        SyncOptionItem(
            text = stringResource(R.string.enter_your_zotero_api_key_manually),
            selected = selectedOption == SyncOption.ZoteroAPIManual,
            onClick = { onOptionSelected(SyncOption.ZoteroAPIManual) }
        )
    }
}

@Composable
private fun SyncOptionItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Preview
@Composable
fun SyncSetupScreenPreview() {
    ZoteroTheme {
        SyncSetupContent(
            state = SyncSetupViewModel.State(),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun SyncSetupContentPreview() {
    ZoteroTheme {
        SyncSetupContent(
            state = SyncSetupViewModel.State(),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun SyncOptionsSectionPreview() {
    ZoteroTheme {
        SyncOptionsSection(
            selectedOption = SyncOption.ZoteroAPI,
            onOptionSelected = {}
        )
    }
}

@Preview
@Composable
private fun SyncOptionItemPreview() {
    ZoteroTheme {
        SyncOptionItem(
            text = "Zotero Account Sync",
            selected = true,
            onClick = {}
        )
    }
}

@Composable
private fun ApiKeyDialog(
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter your API Key") },
        text = {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview
@Composable
private fun ApiKeyDialogPreview() {
    ZoteroTheme {
        ApiKeyDialog(
            onSubmit = {},
            onDismiss = {}
        )
    }
}