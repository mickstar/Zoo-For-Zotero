package com.mickstarify.zooforzotero.AttachmentManager

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mickstarify.zooforzotero.R
import kotlinx.android.synthetic.main.activity_attachment_manager.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class AttachmentManager : AppCompatActivity(), Contract.View {
    lateinit var presenter: Contract.Presenter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attachment_manager)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        presenter = AttachmentManagerPresenter(this, this)
    }

    override fun initUI() {
        findViewById<LinearLayout>(R.id.ll_meta_information).visibility = View.INVISIBLE
        findViewById<Button>(R.id.button_download).onClick {
            presenter.pressedDownloadAttachments()
        }
        setDownloadButtonState("Loading Library", false)
    }

    override fun makeToastAlert(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
    }

    override fun createErrorAlert(title: String, message: String, onClick: () -> Unit) {
        val alert = AlertDialog.Builder(this)
        alert.setIcon(R.drawable.ic_error_black_24dp)
        alert.setTitle(title)
        alert.setMessage(message)
        alert.setPositiveButton("Ok") { _, _ -> onClick() }
        try {
            alert.show()
        } catch (exception: WindowManager.BadTokenException) {
            Log.e("zotero", "error cannot show error dialog. ${message}")
        }
    }

    var progressBar: ProgressBar? = null
    override fun showLibraryLoadingAnimation() {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progressBar)
        }

        progressBar?.isIndeterminate = true
        progressBar?.visibility = View.VISIBLE
        progressBar?.isActivated = true

        val textView = findViewById<TextView>(R.id.txt_loading_library_text)
        textView.text = resources.getString(R.string.loading_your_library)
        textView.visibility = View.VISIBLE
    }

    override fun hideLibraryLoadingAnimation() {
        progressBar?.let {
            it.visibility = View.INVISIBLE
            it.isActivated = false
        }
        val textView = findViewById<TextView>(R.id.txt_loading_library_text)
        textView.visibility = View.INVISIBLE
    }

    override fun setDownloadButtonState(text: String, enabled: Boolean) {
        val button = findViewById<Button>(R.id.button_download)
        button.isEnabled = enabled
        button.text = text
        Log.d("zotero", "button state changed. ${button.text} ${button.isEnabled}")
    }

    override fun updateLoadingProgress(message: String, current: Int, total: Int) {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progressBar)
        }
        progressBar?.let {
            it.visibility = View.VISIBLE
            it.isIndeterminate = current == 0
            it.progress = current
            it.max = total
            it.isActivated = true
        }

        val textView = findViewById<TextView>(R.id.txt_loading_library_text)
        textView.visibility = View.VISIBLE
        textView.text = message
    }

    override fun displayAttachmentInformation(
        nLocal: Int,
        sizeLocal: String,
        nRemote: Int
    ) {
        findViewById<TextView>(R.id.txt_number_attachments).text =
            "${nLocal} of ${nRemote} Downloaded"
        findViewById<TextView>(R.id.txt_local_size).text = "${sizeLocal} used"

        findViewById<LinearLayout>(R.id.ll_meta_information).visibility = View.VISIBLE

        //todo add free disk space.
    }


    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        if (presenter.isDownloading()) {
            this.makeToastAlert("Do not exit while download is active. Cancel it first.")
        } else {
            super.onBackPressed()
        }
    }


}
