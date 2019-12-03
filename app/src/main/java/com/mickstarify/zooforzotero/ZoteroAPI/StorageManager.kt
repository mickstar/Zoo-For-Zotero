package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import android.content.Intent
import com.mickstarify.zooforzotero.SettingsActivity

const val STORAGE_ACCESS_REQUEST = 1  // The request code

class StorageManager(val context: Context) {

    init {

    }

    fun getAccess(activity: SettingsActivity) {
        val intent = Intent()
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.startActivityForResult(intent, STORAGE_ACCESS_REQUEST)
    }
}