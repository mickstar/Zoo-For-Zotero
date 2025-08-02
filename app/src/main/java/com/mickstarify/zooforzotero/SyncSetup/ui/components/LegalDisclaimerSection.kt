package com.mickstarify.zooforzotero.SyncSetup.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.mickstarify.zooforzotero.common.Constants
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun LegalDisclaimerSection() {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_frontpage_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

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
    }
}

@Preview
@Composable
private fun LegalDisclaimerSectionPreview() {
    ZoteroTheme {
        LegalDisclaimerSection()
    }
}

@Preview
@Composable
private fun LegalDisclaimerSectionDarkPreview() {
    ZoteroTheme(darkTheme = true) {
        LegalDisclaimerSection()
    }
}