package com.mickstarify.zooforzotero

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.mickstarify.zooforzotero.LibraryActivity.WebDAV.WebDAVSetup
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.STORAGE_ACCESS_REQUEST
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    @Inject
    lateinit var myStorageManager: AttachmentStorageManager

    @Inject
    lateinit var myPreferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        
        setupToolbar()
        
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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


            val httpTimeoutPreference: EditTextPreference? = findPreference("http_write_timeout")

            httpTimeoutPreference?.setOnBindEditTextListener { editText ->
                // Ensure the input allows only integers
                editText.inputType = InputType.TYPE_CLASS_NUMBER
            }

            httpTimeoutPreference?.setOnPreferenceChangeListener { _, newValue ->
                // Validate the input to ensure it's a non-negative integer
                val value = newValue.toString()
                val isValid = value.matches(Regex("\\d+")) // Match only non-negative integers
                if (!isValid) {
                    Toast.makeText(context, "Please enter a valid non-negative integer", Toast.LENGTH_SHORT).show()
                }
                isValid
            }

        }

        override fun onResume() {
            super.onResume()
            // Set up a listener whenever a key changes
            preferenceScreen.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)

            this.findPreference<SwitchPreference>("use_webdav")?.isChecked =
                preferenceManager.sharedPreferences!!.getBoolean("use_webdav", false)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.key == "configure_webdav") {
                val intent = Intent(requireContext(), WebDAVSetup::class.java)
                startActivity(intent)
                return true
            } else {
                return super.onPreferenceTreeClick(preference)
            }
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
                "use_webdav" -> {
                    if (sharedPreferences?.getBoolean("use_webdav", false) == true && !(requireActivity() as SettingsActivity).myPreferenceManager.isWebDAVConfigured()) {
                        Toast.makeText(requireContext(), "Please configure webdav first", Toast.LENGTH_SHORT).show()
                        sharedPreferences.edit().putBoolean("use_webdav", false).apply()
                        (requireActivity() as SettingsActivity).updateWebDAVPreferenceUI(false)
                    }
                }
            }
        }
    }

    private fun updateWebDAVPreferenceUI(flag: Boolean) {
        val fragment = supportFragmentManager.findFragmentById(R.id.settings) as SettingsFragment
        fragment.findPreference<SwitchPreference>("use_webdav")?.isChecked = flag
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
    }
}