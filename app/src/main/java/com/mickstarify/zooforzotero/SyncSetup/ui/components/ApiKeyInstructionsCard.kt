package com.mickstarify.zooforzotero.SyncSetup.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun ApiKeyInstructionsCard() {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = stringResource(R.string.api_key_instructions_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.api_key_instructions_steps),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val linkText = buildAnnotatedString {
                append("Open ")
                pushStringAnnotation(
                    tag = "URL",
                    annotation = "https://www.zotero.org/settings/security#applications"
                )
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.Medium
                    )
                ) {
                    append("Zotero Security Settings")
                }
                pop()
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClickableText(
                    text = linkText,
                    style = MaterialTheme.typography.bodyMedium,
                    onClick = { offset ->
                        linkText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )
                Spacer(modifier = Modifier.padding(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Launch,
                    contentDescription = "Open external link",
                    modifier = Modifier.padding(start = 4.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Preview
@Composable
private fun ApiKeyInstructionsCardPreview() {
    ZoteroTheme {
        ApiKeyInstructionsCard()
    }
}

@Preview
@Composable
private fun ApiKeyInstructionsCardDarkPreview() {
    ZoteroTheme(darkTheme = true) {
        ApiKeyInstructionsCard()
    }
}