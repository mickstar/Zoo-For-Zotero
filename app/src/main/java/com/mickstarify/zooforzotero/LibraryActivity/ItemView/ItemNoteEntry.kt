package com.mickstarify.zooforzotero.LibraryActivity.ItemView

import android.app.AlertDialog
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteInteractionListener
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteView
import com.mickstarify.zooforzotero.LibraryActivity.ViewModels.LibraryListViewModel
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note

val ARG_NOTE = "note"

class ItemNoteEntry() : Fragment() {
    private var note: Note? = null
    private var listener: NoteInteractionListener? = null
    lateinit var libraryViewModel: LibraryListViewModel

    val noteKey: String by lazy { arguments?.getString("noteKey") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        libraryViewModel =
            ViewModelProvider(requireActivity()).get(LibraryListViewModel::class.java)

        val noteText = requireView().findViewById<TextView>(R.id.textView_note)

        if (noteKey == "") {
            return
        }

        libraryViewModel.getOnItemClicked().observe(viewLifecycleOwner) { item ->
            note = item?.notes?.filter { it.key == noteKey }?.firstOrNull()

            val htmlText = stripHtml(note?.note ?: "")
            noteText.text = htmlText
            val noteView = NoteView(requireContext(), note!!, listener!!)

            requireView().setOnClickListener {
                noteView.show()
            }

            requireView().setOnLongClickListener(object : View.OnLongClickListener {
                override fun onLongClick(dialog: View?): Boolean {
                    AlertDialog.Builder(this@ItemNoteEntry.context)
                        .setTitle("Note")
                        .setItems(
                            arrayOf("View Note", "Edit Note", "Delete Note")
                        ) { _dialog, item ->
                            when (item) {
                                0 -> noteView.show()
                                1 -> editNote()
                                2 -> deleteNote()
                            }
                        }.show()
                    return true
                }

            })
        }


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
        fun newInstance(noteKey: String): ItemNoteEntry {
            val fragment = ItemNoteEntry()
            val args = Bundle().apply {
                putString("noteKey", noteKey)
            }
            fragment.arguments = args
            return fragment
        }

        fun newInstance() = ItemNoteEntry()
    }
}
