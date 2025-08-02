package com.mickstarify.zooforzotero.LibraryActivity.ItemView

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import java.util.LinkedList

class ShareItemDialog(item: Item) {


    // maps name to a isChecked flag
    val isChecked = HashMap<String, Boolean>()
    val pairs = getShareableParameters(item)

    fun buildShareText(): String {
        var string = ""
        for ((name, value) in pairs) {
            if (isChecked[name] == true) {
                if (string != "") {
                    string += ", "
                }
                string += "$name: ${value.trim()}"
            }
        }
        Log.d("zotero", "built $string")
        return string
    }

    fun show(context: Context?, shareItemListener: onShareItemListener?) {
        if (context == null) {
            Log.e("zotero", "Error, got null context on share item.")
            return
        }

        val dialogBuilder = AlertDialog.Builder(context).create()
        val inflater = LayoutInflater.from(context)
        val dialogView: View = inflater.inflate(R.layout.dialog_share_item, null)

        val previewTextView = dialogView.findViewById<EditText>(R.id.edittext_share_preview)

        val checkboxLayout = dialogView.findViewById<LinearLayout>(R.id.linearlayout_checkbox)
        for ((name, value) in pairs) {
            val checkbox = CheckBox(context)
            this.isChecked[name] = false
            checkbox.setText(name)
            if (name == "Title" || name == "Date" || name == "Author") {
                this.isChecked[name] = true
                checkbox.isChecked = true
            }
            checkbox.setOnClickListener(View.OnClickListener { v ->
                Log.d("zotero", "pressed $name - $value")
                this.isChecked[name] = !this.isChecked[name]!!
                previewTextView.setText(buildShareText())
            })
            checkboxLayout.addView(checkbox)
        }

        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)
        cancelButton.setOnClickListener {
            dialogBuilder.dismiss()
        }

        val submitButton = dialogView.findViewById<Button>(R.id.button_share)
        submitButton.setOnClickListener {
            shareItemListener?.shareItem(previewTextView.text.toString())
        }
        
        previewTextView.setText(buildShareText())
        dialogBuilder.setView(dialogView)
        dialogBuilder.show()
    }

    fun getShareableParameters(item: Item): List<Pair<String, String>> {
        /* Returns a list of (titles, values) for a given item. */

        // we will specify these manually so they appear on the top.
        val pairs = LinkedList<Pair<String, String>>()
        if (item.data.containsKey("title")) {
            pairs.add(Pair("Title", item.data["title"] ?: "none"))
        }
        pairs.add(Pair("Author", item.getAuthor()))
        if (item.data.containsKey("date") && item.data["date"] != "") {
            pairs.add(Pair("Date", item.data["date"] ?: "none"))
        }

        // the rest of the data will be based on what's available.
        for ((name, value) in item.data) {
            if (name != "title" && name != "date" && name != "author" && name != "key") {
                if (value != "") {
                    pairs.add(Pair(name, value))
                }
            }
        }
        return pairs
    }
}