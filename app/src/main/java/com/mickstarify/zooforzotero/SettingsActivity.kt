package com.mickstarify.zooforzotero

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.mickstarify.zooforzotero.ZoteroAPI.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroAPI.STORAGE_ACCESS_REQUEST

class SettingsActivity : AppCompatActivity() {
    lateinit var myStorageManager: AttachmentStorageManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        myStorageManager = AttachmentStorageManager(this)
    }

    fun openStoragePicker() {
        myStorageManager.askUserForPath(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("zotero", "got back result request=$requestCode result=$resultCode data=$data")
        when (requestCode) {
            STORAGE_ACCESS_REQUEST -> {
                myStorageManager.setStorage(data?.dataString)
            }
        }
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val preferenceManager = PreferenceManager(context!!)
            val username = findPreference<EditTextPreference>("webdav_username")
            username?.summary = if (preferenceManager.getWebDAVUsername() == "") {
                "No Username set."
            } else {
                preferenceManager.getWebDAVUsername()
            }
            val address = findPreference<EditTextPreference>("webdav_address")
            address?.summary = preferenceManager.getWebDAVAddress()
            val password = findPreference<EditTextPreference>("webdav_password")
            password?.summary = if (preferenceManager.getWebDAVPassword() == "") {
                "No password set."
            } else {
                "***********"
            }

            // check to see if user has ever setup webDAV before.
            if (preferenceManager.getWebDAVAddress() == "") {
                val pref = findPreference<SwitchPreference>("use_webdav")
                pref?.isEnabled = false
                pref?.summary = "Please use the Setup WebDAV option from the Library view."
                // rather them do it in the activity as that will check the connection.
            }
        }

        override fun onResume() {
            super.onResume()
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            Log.d("zotero", "sharedpreference change ${key}")
            when (key) {
                "attachment_sync_location" -> {
                    if (sharedPreferences?.getString(
                            "attachment_sync_location",
                            "null"
                        ) == "CUSTOM"
                    ) {
                        (this.activity as SettingsActivity).openStoragePicker()
                    }
                }
            }
        }
    }
}