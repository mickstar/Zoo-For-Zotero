package com.mickstarify.zooforzotero.LibraryActivity.Notes

import android.app.AlertDialog
import android.content.Context
import android.text.Html
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note

class NoteView(val context: Context, val note: Note, val listener: NoteInteractionListener) {
    init {
    }

    fun show() {
        AlertDialog.Builder(context)
            .setTitle("Note")
            .setMessage(Html.fromHtml(note.note))
            .setPositiveButton("Dismiss", { _, _ -> })
            .setNeutralButton("Edit", {_,_ -> showNoteEditingDialog()})
            .show()
    }

    fun showNoteEditingDialog(){
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