package com.mickstarify.zooforzotero.LibraryActivity.ItemView

import android.app.AlertDialog
import android.content.Context
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            note = it.getParcelable(ARG_NOTE)
        }
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
            Log.d("zotero", "note clicked")
            AlertDialog.Builder(this.context)
                .setTitle("Note")
                .setMessage(Html.fromHtml(note?.note))
                .setPositiveButton("Dismiss", { _, _ -> })
                .show()
        })

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    interface OnNoteInteractionListener {
        fun deleteNote(note: Note)
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
