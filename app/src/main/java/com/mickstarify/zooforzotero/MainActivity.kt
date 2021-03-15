package com.mickstarify.zooforzotero

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mickstarify.zooforzotero.LibraryActivity.LibraryActivity
import com.mickstarify.zooforzotero.SyncSetup.AuthenticationStorage
import com.mickstarify.zooforzotero.SyncSetup.SyncSetupView


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (intent != null){
            if (intent.action == Intent.ACTION_SEND){
                if (intent.type == "text/plain"){
                    handleTextIntent(intent)
                    finish()
                    return
                }
            }
        }

        val auth = AuthenticationStorage(this)
        val intent = if (auth.hasCredentials()) {
            Intent(this, LibraryActivity::class.java)
        } else {
            Intent(this, SyncSetupView::class.java)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun handleTextIntent(intent: Intent){
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Log.d("zotero", "got intent with text $it")

            val url = "https://www.zotero.org/save?q=${it}"
            val webIntent = Intent(Intent.ACTION_VIEW)
            webIntent.data = Uri.parse(url)

            startActivity(webIntent)
        }
    }
}
