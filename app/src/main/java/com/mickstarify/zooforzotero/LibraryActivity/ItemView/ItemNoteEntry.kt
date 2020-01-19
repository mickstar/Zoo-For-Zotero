package com.mickstarify.zooforzotero.LibraryActivity.ItemView

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteInteractionListener
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteView
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note

val ARG_NOTE = "note"

class ItemNoteEntry : Fragment() {
    private var note: Note? = null
    private var listener: NoteInteractionListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            note = it.getParcelable(ARG_NOTE)
        }
        onAttachToParentFragment(parentFragment)
    }

    fun stripHtml(html: String): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            return Html.fromHtml(html).toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_item_note_entry, container, false)
        val noteText = view.findViewById<TextView>(R.id.textView_note)
        val htmlText = stripHtml(note?.note ?: "")
        noteText.text = htmlText
        val noteView = NoteView(context!!, note!!, listener!!)

        view.setOnClickListener({
            noteView.show()
        })

        view.setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(dialog: View?): Boolean {
                AlertDialog.Builder(this@ItemNoteEntry.context)
                    .setTitle("Note")
                    .setItems(
                        arrayOf("View Note", "Edit Note", "Delete Note"),
                        object : DialogInterface.OnClickListener {
                            override fun onClick(dialog: DialogInterface?, item: Int) {
                                when (item) {
                                    0 -> noteView.show()
                                    1 -> editNote()
                                    2 -> deleteNote()
                                }
                            }

                        }).show()
                return true
            }

        })

        return view
    }

    private fun editNote() {
        Log.d("zotero", "edit note clicked")
        note?.let {
            listener?.editNote(it)
        }
    }

    private fun deleteNote() {
        Log.d("zotero", "delete note clicked")
        note?.let {
            listener?.deleteNote(it)
        }
    }

    fun onAttachToParentFragment(parentFragment: Fragment?) {
        if (parentFragment == null) {
            return
        }
        if (parentFragment is NoteInteractionListener) {
            listener = parentFragment
        } else {
            throw RuntimeException(parentFragment.toString() + " must implement OnNoteInteractionListener")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(note: Note) =
            ItemNoteEntry().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_NOTE, note)
                }
            }
    }
}
