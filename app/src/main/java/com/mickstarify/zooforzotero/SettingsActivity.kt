package com.mickstarify.zooforzotero

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
        }
    }
}