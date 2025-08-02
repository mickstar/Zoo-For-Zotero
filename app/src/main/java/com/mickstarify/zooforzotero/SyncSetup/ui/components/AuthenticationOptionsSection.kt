package com.mickstarify.zooforzotero.SyncSetup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.SyncSetup.SyncOption
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun AuthenticationOptionsSection(
    selectedOption: SyncOption,
    onOptionSelected: (SyncOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        AuthOptionCard(
            title = "Web Authentication",
            description = "Sign in through your browser",
            icon = Icons.Outlined.Language,
            selected = selectedOption == SyncOption.ZoteroAPI,
            onClick = {
                onOptionSelected(SyncOption.ZoteroAPI)
            }
        )

        AuthOptionCard(
            title = "API Key",
            description = "Enter your API key manually",
            icon = Icons.Outlined.Key,
            selected = selectedOption == SyncOption.ZoteroAPIManual,
            onClick = {
                onOptionSelected(SyncOption.ZoteroAPIManual)
            }
        )
    }
}

@Preview
@Composable
private fun AuthenticationOptionsSectionPreview() {
    ZoteroTheme {
        AuthenticationOptionsSection(
            selectedOption = SyncOption.ZoteroAPI,
            onOptionSelected = {}
        )
    }
}

@Preview
@Composable
private fun AuthenticationOptionsSectionApiSelectedPreview() {
    ZoteroTheme {
        AuthenticationOptionsSection(
            selectedOption = SyncOption.ZoteroAPIManual,
            onOptionSelected = {}
        )
    }
}

@Preview
@Composable
private fun AuthenticationOptionsSectionDarkPreview() {
    ZoteroTheme(darkTheme = true) {
        AuthenticationOptionsSection(
            selectedOption = SyncOption.ZoteroAPI,
            onOptionSelected = {}
        )
    }
}