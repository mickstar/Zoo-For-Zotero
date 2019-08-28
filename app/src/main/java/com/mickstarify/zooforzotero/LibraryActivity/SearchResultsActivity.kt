package com.mickstarify.zooforzotero.LibraryActivity

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SearchResultsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_SEARCH) {
            val query = intent.getStringExtra(SearchManager.QUERY)
            Log.d(packageName, "got query $query")
            val myIntent = Intent(LibraryActivity.ACTION_FILTER)
            myIntent.putExtra(LibraryActivity.EXTRA_QUERY, query)
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(myIntent)
            overridePendingTransition(0, 0) // kills the lame animation
        } else {
            Log.d(packageName, "got intent ${intent.action} query=${intent.dataString}")
        }

        finish()
    }
}
