package com.mickstarify.zooforzotero.SyncSetup

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivity
import com.mickstarify.zooforzotero.SyncSetup.ui.SyncSetupScreen
import com.mickstarify.zooforzotero.ui.theme.ZoteroTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SyncSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display with visible status bar
        enableEdgeToEdge()

        setContent {
            ZoteroTheme {
                SyncSetupScreen(
                    onNavigateToLibrary = {
                        navigateToLibrary()
                    }
                )
            }
        }
    }

    private fun navigateToLibrary() {
        val intent = Intent(this, LibraryActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Check if sync is already set up and finish if it is
        val auth = AuthenticationStorage(this)
        if (auth.hasCredentials()) {
            navigateToLibrary()
        }
    }
}