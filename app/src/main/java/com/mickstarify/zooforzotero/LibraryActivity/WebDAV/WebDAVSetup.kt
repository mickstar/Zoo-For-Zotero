package com.mickstarify.zooforzotero.LibraryActivity.WebDAV

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.textfield.TextInputLayout
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.WebdavAuthMode
import com.mickstarify.zooforzotero.ZoteroAPI.Webdav
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.CompletableObserver
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers

class WebDAVSetup : AppCompatActivity() {
    lateinit var preferenceManager: PreferenceManager
    lateinit var linearLayourAdvancedConfig: LinearLayout

    lateinit var textInputLayout_connectTimeout: TextInputLayout
    lateinit var textInputLayout_readTimeout: TextInputLayout
    lateinit var textInputLayout_writeTimeout: TextInputLayout

    lateinit var textInputLayout_host: TextInputLayout
    lateinit var textInputLayout_user: TextInputLayout
    lateinit var textInputLayout_password: TextInputLayout

    lateinit var textInputLayout_auth_mode: TextInputLayout

    lateinit var checkBox_verifySSL: CheckBox
    lateinit var checkBox_formatAddress: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_dav_setup)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        preferenceManager = PreferenceManager(this)

        textInputLayout_connectTimeout = findViewById(R.id.textInputLayout_webdav_connect_timeout)
        textInputLayout_readTimeout = findViewById(R.id.textInputLayout_webdav_read_timeout)
        textInputLayout_writeTimeout = findViewById(R.id.textInputLayout_webdav_write_timeout)

        textInputLayout_host = findViewById(R.id.textInputLayout_webdav_host)
        textInputLayout_user = findViewById(R.id.textInputLayout_webdav_user)
        textInputLayout_password = findViewById(R.id.textInputLayout_webdav_password)

        checkBox_verifySSL = findViewById(R.id.checkBox_Verify_ssl)
        checkBox_formatAddress = findViewById(R.id.checkBox_add_zotero_host)

        textInputLayout_auth_mode = findViewById(R.id.TextInputLayout_webdav_auth_mode)

        val authModeAdapter = ArrayAdapter<String>(
            this, R.layout.webdav_auth_mode_list_item, listOf(
                "AUTOMATIC",
                "BASIC",
                "DIGEST"
            )
        )
        (textInputLayout_auth_mode.editText as AutoCompleteTextView).apply {
            setAdapter(authModeAdapter)

            this.listSelection = when (preferenceManager.getWebDAVAuthMode()) {
                WebdavAuthMode.AUTOMATIC -> 0
                WebdavAuthMode.BASIC -> 1
                WebdavAuthMode.DIGEST -> 2
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val authMode = when (position) {
                        0 -> WebdavAuthMode.AUTOMATIC
                        1 -> WebdavAuthMode.BASIC
                        2 -> WebdavAuthMode.DIGEST
                        else -> WebdavAuthMode.AUTOMATIC
                    }
                    Log.d("Webdav", "Setting auth mode to ${authMode}")
                    preferenceManager.setWebDAVAuthMode(authMode)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                }
            }
        }

        loadConfig()

        val submitButton = findViewById<Button>(R.id.button_webdav_connect)

        submitButton.setOnClickListener {
            saveConfig()

            val address = textInputLayout_host.editText?.text.toString()
            val username = textInputLayout_user.editText?.text.toString()
            val password = textInputLayout_password.editText?.text.toString()
            if (address == "") {
                destroyWebDAVAuthentication()
            } else {
                makeConnection(address, username, password)
            }
        }

        val advancedConfigButton: Button = findViewById(R.id.button_webdav_advanced_config)
        linearLayourAdvancedConfig = findViewById(R.id.linearLayout_advanced_config)
        advancedConfigButton.setOnClickListener {
            toggleAdvancedConfig()
        }
    }

    private fun saveConfig() {
        var host = textInputLayout_host.editText!!.text.toString()
        val username = textInputLayout_user.editText!!.text.toString()
        val password = textInputLayout_password.editText!!.text.toString()
        val verifySSL: Boolean = checkBox_verifySSL.isChecked
        val formatAddress = checkBox_formatAddress.isChecked
        if (formatAddress) {
            host = formatAddress(host)
            textInputLayout_host.editText!!.setText(host)
        }
        val connectTimeout: Long
        try {
            connectTimeout = textInputLayout_connectTimeout.editText!!.text.toString().toLong()
            if (connectTimeout < 0) {
                throw(NumberFormatException("Must be positive"))
            }
        } catch (e: NumberFormatException) {
            textInputLayout_connectTimeout.error = "Error invalid number. ${e.message}"
            Toast.makeText(this, "Error invalid config.", Toast.LENGTH_SHORT).show()
            return
        }

        val readTimeout: Long
        try {
            readTimeout = textInputLayout_readTimeout.editText!!.text.toString().toLong()
            if (readTimeout < 0L) {
                throw(NumberFormatException("Must be positive"))
            }
        } catch (e: NumberFormatException) {
            textInputLayout_readTimeout.error = "Error invalid number. ${e.message}"
            Toast.makeText(this, "Error invalid config.", Toast.LENGTH_SHORT).show()
            return
        }

        val writeTimeout: Long
        try {
            writeTimeout = textInputLayout_writeTimeout.editText!!.text.toString().toLong()
            if (writeTimeout < 0) {
                throw(NumberFormatException("Must be positive"))
            }
        } catch (e: NumberFormatException) {
            textInputLayout_writeTimeout.error = "Error invalid number. ${e.message}"
            Toast.makeText(this, "Error invalid config.", Toast.LENGTH_SHORT).show()
            return
        }

        preferenceManager.setWebDAVAuthentication(host, username = username, password = password)
        preferenceManager.setVerifySSLForWebDAV(verifySSL)
        preferenceManager.setWebDAVAddZoteroToUrl(formatAddress)
        preferenceManager.setWebDAVConnectTimeout(connectTimeout)
        preferenceManager.setWebDAVReadTimeout(readTimeout)
        preferenceManager.setWebDAVWriteTimeout(writeTimeout)
        // auth mode gets written at time of select, no need to write here.


    }

    private fun loadConfig() {
        textInputLayout_host.editText?.setText(preferenceManager.getWebDAVAddress())
        textInputLayout_user.editText?.setText(preferenceManager.getWebDAVUsername())
        textInputLayout_password.editText?.setText(preferenceManager.getWebDAVPassword())

        checkBox_verifySSL.isChecked = preferenceManager.getVerifySSLForWebDAV()
        checkBox_formatAddress.isChecked = preferenceManager.getWebDAVAddZoteroToUrl()

        textInputLayout_connectTimeout.editText!!.setText(
            preferenceManager.getWebDAVConnectTimeout().toString()
        )
        textInputLayout_readTimeout.editText!!.setText(
            preferenceManager.getWebDAVReadTimeout().toString()
        )
        textInputLayout_writeTimeout.editText!!.setText(
            preferenceManager.getWebDAVWriteTimeout().toString()
        )
    }

    private fun toggleAdvancedConfig() {
        val showing = linearLayourAdvancedConfig.visibility == View.VISIBLE

        val transition = AutoTransition()
        transition.setDuration(500)
        transition.addTarget(R.id.linearLayout_advanced_config)

        TransitionManager.beginDelayedTransition(linearLayourAdvancedConfig, transition)
        if (showing) {
            linearLayourAdvancedConfig.visibility = View.GONE
        } else {
            linearLayourAdvancedConfig.visibility = View.VISIBLE
        }
    }

    fun formatAddress(address: String): String {
        var mAddress = address.trim()
        if (mAddress == "") {
            return ""
        }
        mAddress = if (!mAddress.uppercase().startsWith("HTTP")) {
            "https://" + mAddress
        } else {
            mAddress.trim()
        }
        mAddress = if (mAddress.endsWith("/zotero")) {
            mAddress
        } else {
            if (mAddress.endsWith("/")) { // so we don't get server.com//zotero
                mAddress + "zotero"
            } else {
                mAddress + "/zotero"
            }
        }
        return mAddress
    }

    fun allowInsecureSSL(): Boolean {
        return !findViewById<CheckBox>(R.id.checkBox_Verify_ssl).isChecked
    }

    fun makeConnection(address: String, username: String, password: String) {
        val webDav = Webdav(preferenceManager)
        startProgressDialog()
        Completable.fromAction {
            var status = false // default to false incase we get an exception
            var hadAuthenticationError = false
            var errorMessage = "unset"
            status = webDav.testConnection()
            if (status == false) {
                throw Exception("Unspecified error.")
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : CompletableObserver {
                override fun onSubscribe(d: Disposable) {
                }

                override fun onComplete() {
                    setWebDAVAuthentication(address, username, password)
                    hideProgressDialog()
                }

                override fun onError(e: Throwable) {
                    notifyFailed("Error setting up webdav, message: $e")
                    hideProgressDialog()
                }

            })
    }

    var progressDialog: ProgressDialog? = null
    fun startProgressDialog() {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        progressDialog?.setTitle("Connecting to WebDAV Server")
        progressDialog?.setMessage("We are testing your connection to the WebDAV Server.")
        progressDialog?.isIndeterminate = true
        progressDialog?.show()
    }

    fun hideProgressDialog() {
        progressDialog?.hide()
        progressDialog = null
    }

    fun notifyFailed(message: String = "") {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Error connecting to webDAV server")
        if (message == "") {
            alertDialog.setMessage(
                "There was an error connecting to the webDAV server." +
                        "Please verify the address, username and password."
            )
        } else {
            alertDialog.setMessage(message)
        }
        alertDialog.setPositiveButton("Ok") { _, _ -> {} }
        alertDialog.show()
    }

    fun setWebDAVAuthentication(address: String, username: String, password: String) {
        preferenceManager.setWebDAVAuthentication(address, username, password)
        preferenceManager.setWebDAVEnabled(true)
        Toast.makeText(this, "Successfully added WebDAV.", Toast.LENGTH_SHORT).show()
    }

    fun destroyWebDAVAuthentication() {
        preferenceManager.destroyWebDAVAuthentication()
        preferenceManager.setWebDAVEnabled(false)
        this.finish()
    }
}
