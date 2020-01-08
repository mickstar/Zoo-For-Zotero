package com.mickstarify.zooforzotero.AttachmentManager

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.mickstarify.zooforzotero.R

import kotlinx.android.synthetic.main.activity_attachment_manager.*

class AttachmentManager : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attachment_manager)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }



}
