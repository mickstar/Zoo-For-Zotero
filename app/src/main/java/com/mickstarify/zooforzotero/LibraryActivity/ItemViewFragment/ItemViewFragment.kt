package com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.resources.TextAppearance
import com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment.ItemAuthorsEntry.*
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Creator

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import kotlinx.android.synthetic.main.fragment_item_main.*
import java.util.*

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ItemViewFragment.OnListFragmentInteractionListener] interface.
 */
class ItemViewFragment: BottomSheetDialogFragment(), OnFragmentInteractionListener{
    override fun onFragmentInteraction(uri: Uri) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var item : Item
    private lateinit var attachments : List<Item>
    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            item = it.getParcelable<Item>(ARG_ITEM)!!
            attachments = it.getParcelableArrayList<Item>(ARG_ATTACHMENTS)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_main, container, false)

        val toolbar : Toolbar? = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).title = item.getTitle()

        val layout : LinearLayout = view.findViewById(R.id.item_fragment_scrollview_ll_layout)

        addTextEntry("Item Type", item.data["itemType"]?:"Unknown")
        addTextEntry("title", item.getTitle())
        if (item.creators.isNotEmpty()){
            this.addCreators(item.creators)
        }
        else{
            this.addCreators(listOf(Creator("Author", "", "")))
        }
        for ((key,value) in item.data){
            if (key != "itemType" && key != "title"){
                addTextEntry(key,value)
            }
        }
        val attachmentsLayout = view.findViewById<LinearLayout>(R.id.item_fragment_scrollview_ll_attachments)
        attachmentsLayout.addView(TextView(context).apply {
            this.text = "Attachments"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.setTextAppearance(android.R.style.TextAppearance_DeviceDefault_Large)
            }
        })
        this.addAttachments(attachments)

        return view
    }

    private fun addAttachments(attachments: List<Item>){
        val fmt = this.childFragmentManager.beginTransaction()
        for (attachment in attachments) {
            fmt.add(
                R.id.item_fragment_scrollview_ll_attachments,
                ItemAttachmentEntry.newInstance(attachment)
            )
        }
        fmt.commit()
    }

    private fun addTextEntry(label: String, content : String) {
        val fmt = this.childFragmentManager.beginTransaction()
        fmt.add(R.id.item_fragment_scrollview_ll_layout,
            ItemTextEntry.newInstance(label, content) as Fragment)
        fmt.commit()
    }

    private fun addCreators(creators : List<Creator>) {
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
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
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
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: Item?)
    }

    companion object {
        const val ARG_ITEM = "item"
        const val ARG_ATTACHMENTS = "attachments"

        @JvmStatic
        fun newInstance(item : Item, attachments : List<Item>) =
            ItemViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM, item)
                    putParcelableArrayList(ARG_ATTACHMENTS, ArrayList(attachments))
                }
            }
    }
}
