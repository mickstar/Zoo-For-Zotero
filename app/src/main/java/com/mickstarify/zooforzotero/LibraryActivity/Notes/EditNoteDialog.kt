package com.mickstarify.zooforzotero.LibraryActivity.Notes

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import com.mickstarify.zooforzotero.R
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk27.coroutines.onClick


class EditNoteDialog {

    fun show(
        context: Context?,
        noteText: String,
        onEditNoteChangeListener: onEditNoteChangeListener
    ) {
        if (context == null) {
            Log.e("zotero", "got null context on create Note")
            return
        }
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val dialogBuilder = AlertDialog.Builder(context).create()
        val inflater = context.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_add_note, null)

        val cancelButton = dialogView.findViewById<Button>(R.id.buttonCancel)
        val submitButton = dialogView.findViewById<Button>(R.id.buttonSubmit)
        val editTextNote = dialogView.findViewById<EditText>(R.id.edit_note)

        editTextNote.setText(noteText)

        cancelButton.onClick {
            onEditNoteChangeListener.onCancel()
            imm.hideSoftInputFromWindow(editTextNote.getWindowToken(), 0);
            dialogBuilder.dismiss()
        }

        submitButton.onClick {
            onEditNoteChangeListener.onSubmit(editTextNote.text.toString())
            imm.hideSoftInputFromWindow(editTextNote.getWindowToken(), 0);
            dialogBuilder.dismiss()
        }


        dialogBuilder.setView(dialogView)

        //not letting user dismiss dialog because otherwise the keyboard stays and it's a pain to
        //dismiss it. (need to find currentFocusedView, etc)
        dialogBuilder.setCanceledOnTouchOutside(false)

        dialogBuilder.show()

        editTextNote.requestFocus()
        // Shows the softkeyboard.
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }
}

interface onEditNoteChangeListener {
    fun onCancel()
    fun onSubmit(noteText: String)
}