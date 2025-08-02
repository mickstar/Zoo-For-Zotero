package com.mickstarify.zooforzotero.SyncSetup.ui.screens

import android.content.Intent
import android.net.Uri
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

        // Welcome Hero Section
        WelcomeHeroSection()

        // Authentication Options
        AuthenticationOptionsSection(
            selectedOption = state.selectedSyncOption,
            onOptionSelected = { option ->
                onEvent(SyncSetupViewModel.Event.SelectSyncOption(option))
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Disclaimer
        Text(
            text = stringResource(R.string.app_frontpage_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Legal Agreement Text
        val annotatedText = buildAnnotatedString {
            append(stringResource(R.string.legal_agreement_prefix))

            pushStringAnnotation(tag = "terms", annotation = Constants.Legal.TERMS_OF_USE_URL)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(stringResource(R.string.terms_of_use))
            }
            pop()

            append(stringResource(R.string.legal_agreement_middle))

            pushStringAnnotation(tag = "privacy", annotation = Constants.Legal.PRIVACY_POLICY_URL)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(stringResource(R.string.privacy_policy))
            }
            pop()
        }

        ClickableText(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = { offset ->
                annotatedText.getStringAnnotations(tag = "terms", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    }
                annotatedText.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    }
            }
        )

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

    // API Key Dialog (Legacy - should be removed once navigation is fully implemented)
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
            state = SyncSetupViewModel.State(showApiKeyDialog = true),
            onEvent = {}
        )
    }
}