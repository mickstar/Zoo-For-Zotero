package com.mickstarify.zooforzotero.SyncSetup.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.SyncSetup.ui.components.AuthenticationOptionsSection
import com.mickstarify.zooforzotero.SyncSetup.ui.components.LegalDisclaimerSection
import com.mickstarify.zooforzotero.SyncSetup.ui.components.WelcomeHeroSection
import com.mickstarify.zooforzotero.SyncSetup.viewmodels.SyncSetupViewModel
import com.mickstarify.zooforzotero.common.Constants
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun SyncSetupLandingScreen(
    state: SyncSetupViewModel.State,
    onEvent: (SyncSetupViewModel.Event) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        WelcomeHeroSection()

        AuthenticationOptionsSection(
            selectedOption = state.selectedSyncOption,
            onOptionSelected = { option ->
                onEvent(SyncSetupViewModel.Event.SelectSyncOption(option))
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        LegalDisclaimerSection()

        Spacer(modifier = Modifier.height(32.dp))

        // Continue Button
        Button(
            onClick = { onEvent(SyncSetupViewModel.Event.ProceedWithSetup) },
            enabled = state.isProceedEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.proceed),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview
@Composable
private fun SyncSetupLandingScreenPreview() {
    ZoteroTheme {
        SyncSetupLandingScreen(
            state = SyncSetupViewModel.State(),
            onEvent = {}
        )
    }
}

@Preview
@Composable
private fun SyncSetupLandingScreenWithApiDialogPreview() {
    ZoteroTheme {
        SyncSetupLandingScreen(
            state = SyncSetupViewModel.State(),
            onEvent = {}
        )
    }
}