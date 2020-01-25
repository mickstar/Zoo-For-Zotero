package com.mickstarify.zooforzotero.LibraryActivity.Notes

import android.app.AlertDialog
import android.content.Context
import android.text.Html
import android.util.Log
import android.widget.TextView
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note

class NoteView(val context: Context, val note: Note, val listener: NoteInteractionListener) {
    init {
    }

    fun show() {
        val dialog = AlertDialog.Builder(context)
            .setTitle("Note")
            .setMessage(Html.fromHtml(note.note))
            .setPositiveButton("Dismiss", { _, _ -> })
            .setNeutralButton("Edit", { _, _ -> showNoteEditingDialog() })
            .show()
        try{
            val textView = dialog.window?.decorView?.findViewById<TextView>(android.R.id.message)
            textView?.setTextIsSelectable(true)
        } catch (e: Exception){
            Log.e("zotero", "error setting text selectable in note, $e")
        }
    }

    fun showNoteEditingDialog() {
        EditNoteDialog()
            .show(context, note.note, object :
                onEditNoteChangeListener {
                override fun onCancel() {
                }

                override fun onSubmit(noteText: String) {
                    note.note = noteText
                    listener.editNote(note)

                }

            })
    }
}