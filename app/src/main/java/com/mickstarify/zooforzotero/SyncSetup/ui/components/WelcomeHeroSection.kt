package com.mickstarify.zooforzotero.SyncSetup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme

@Composable
fun WelcomeHeroSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // App icon/logo placeholder
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = "Zoo for Zotero Logo",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to Zoo for Zotero",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview
@Composable
fun WelcomeHeroSectionPreview() {
    ZoteroTheme {
        WelcomeHeroSection()
    }
}

@Preview
@Composable
fun WelcomeHeroSectionDarkPreview() {
    ZoteroTheme(darkTheme = true) {
        WelcomeHeroSection()
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeHeroSectionCompactPreview() {
    ZoteroTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            WelcomeHeroSection()
        }
    }
}