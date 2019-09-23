package com.mickstarify.zooforzotero.LibraryActivity.ItemView

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note

val ARG_NOTE = "note"

class ItemNoteEntry : Fragment() {
    private var note: Note? = null
    private var listener: OnNoteInteractionListener? = null
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

        view.setOnClickListener({
            showNote()
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
                                    0 -> showNote()
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

    private fun showNote() {
        Log.d("zotero", "note clicked")
        AlertDialog.Builder(this.context)
            .setTitle("Note")
            .setMessage(Html.fromHtml(note?.note))
            .setPositiveButton("Dismiss", { _, _ -> })
            .show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    fun onAttachToParentFragment(parentFragment: Fragment?) {
        if (parentFragment == null) {
            return
        }
        if (parentFragment is OnNoteInteractionListener) {
            listener = parentFragment
        } else {
            throw RuntimeException(parentFragment.toString() + " must implement OnNoteInteractionListener")
        }
    }

    interface OnNoteInteractionListener {
        fun deleteNote(note: Note)
        fun editNote(note: Note)
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
