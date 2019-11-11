package com.mickstarify.zooforzotero.LibraryActivity.ItemView

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Creator
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import java.util.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ItemViewFragment.OnListFragmentInteractionListener] interface.
 */
class ItemViewFragment : BottomSheetDialogFragment(), ItemNoteEntry.OnNoteInteractionListener {
    override fun deleteNote(note: Note) {
        listener?.onNoteDelete(note)
    }

    override fun editNote(note: Note) {
        EditNoteDialog().show(context, note.note, object : onEditNoteChangeListener {
            override fun onCancel() {
            }

            override fun onSubmit(noteText: String) {
                note.note = noteText
                listener?.onNoteEdit(note)

            }

        })
    }

    private lateinit var item: Item
    private lateinit var attachments: List<Item>
    private lateinit var notes: List<Note>
    private var listener: OnItemFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getParcelable<Item>(ARG_ITEM)!!
            attachments = it.getParcelableArrayList<Item>(ARG_ATTACHMENTS)!!
            notes = it.getParcelableArrayList<Note>(ARG_NOTES)!!
        }
        setHasOptionsMenu(true) //so we can set custom menu options
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_library_item_actionbar, menu)
        // idk why these library activity items are being inflated...
        menu.removeItem(R.id.settings)
        menu.removeItem(R.id.search)
        menu.removeItem(R.id.webdav_setup)
        menu.removeItem(R.id.filter_menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_note -> {
                this.showCreateNoteDialog()
            }
            else -> {
                Log.d("zotero", "item fragmenmt menu item clicked")
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showCreateNoteDialog() {
        EditNoteDialog().show(context, "", object : onEditNoteChangeListener {
            override fun onCancel() {
            }

            override fun onSubmit(noteText: String) {
                Log.d("zotero", "got note $noteText")
                val note = Note(noteText, item.ItemKey)
                listener?.onNoteCreate(note)

            }

        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_main, container, false)

        val toolbar: Toolbar? = view.findViewById(R.id.item_fragment_toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).title = item.getTitle()

        addTextEntry("Item Type", item.data["itemType"] ?: "Unknown")
        addTextEntry("title", item.getTitle())
        if (item.creators.isNotEmpty()) {
            this.addCreators(item.creators)
        } else {
            this.addCreators(listOf(Creator("Author", "", "")))
        }
        for ((key, value) in item.data) {
            if (key != "itemType" && key != "title") {
                addTextEntry(key, value)
            }
        }
        val attachmentsLayout =
            view.findViewById<LinearLayout>(R.id.item_fragment_scrollview_ll_attachments)
        attachmentsLayout.addView(TextView(context).apply {
            this.text = "Attachments"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large)
            }
        })
        this.addAttachments(attachments)
        this.populateNotes(notes)

        return view
    }

    private fun populateNotes(notes: List<Note>) {
        if (notes.isEmpty()) {
            return
        }
        val fmt = this.childFragmentManager.beginTransaction()
        for (note in notes) {
            fmt.add(
                R.id.item_fragment_scrollview_ll_notes,
                ItemNoteEntry.newInstance(note)
            )
        }
        fmt.commit()

    }

    private fun addAttachments(attachments: List<Item>) {
        val fmt = this.childFragmentManager.beginTransaction()
        for (attachment in attachments) {
            Log.d("zotero", "adding ${attachment.getTitle()}")
            fmt.add(
                R.id.item_fragment_scrollview_ll_attachments,
                ItemAttachmentEntry.newInstance(attachment)
            )
        }
        fmt.commit()
    }

    private fun addTextEntry(label: String, content: String) {
        val fmt = this.childFragmentManager.beginTransaction()
        fmt.add(
            R.id.item_fragment_scrollview_ll_layout,
            ItemTextEntry.newInstance(label, content) as Fragment
        )
        fmt.commit()
    }

    private fun addCreators(creators: List<Creator>) {
        val fmt = this.childFragmentManager.beginTransaction()
        for (creator in creators) {
            fmt.add(
                R.id.item_fragment_scrollview_ll_layout,
                ItemAuthorsEntry.newInstance(creator) as Fragment
            )
        }
        fmt.commit()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnItemFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnItemFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnItemFragmentInteractionListener {
        fun onListFragmentInteraction(item: Item?)
        fun onNoteCreate(note: Note)
        fun onNoteEdit(note: Note)
        fun onNoteDelete(note: Note)
    }

    companion object {
        const val ARG_ITEM = "item"
        const val ARG_ATTACHMENTS = "attachments"
        const val ARG_NOTES = "notes"

        @JvmStatic
        fun newInstance(item: Item, attachments: List<Item>, notes: List<Note>) =
            ItemViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM, item)
                    putParcelableArrayList(ARG_ATTACHMENTS, ArrayList(attachments))
                    putParcelableArrayList(ARG_NOTES, ArrayList(notes))
                }
            }
    }
}
