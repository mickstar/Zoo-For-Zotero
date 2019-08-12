package com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mickstarify.zooforzotero.R

import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item

/**
 * A fragment representing a list of Items.
 * Activities containing this fragment MUST implement the
 * [ItemViewFragment.OnListFragmentInteractionListener] interface.
 */
class ItemViewFragment: BottomSheetDialogFragment() {

    private lateinit var item : Item
    private var listener: OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            item = it.getParcelable<Item>(ARG_ITEM)!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_main, container, false)
        val titleView = view.findViewById<TextView>(R.id.item_title)
        titleView.text = item.getTitle()

        val layout : LinearLayout = view.findViewById(R.id.item_fragment_scrollview_ll_layout)

        addTextEntry("Item Type", item.data["itemType"]?:"Unknown")
        addTextEntry("title", item.getTitle())

        for ((key,value) in item.data){
            if (key != "itemType" && key != "title"){
                addTextEntry(key,value)
            }
        }

        return view
    }

    private fun addTextEntry(label: String, content : String) {
        val fmt = this.childFragmentManager.beginTransaction()
        fmt.add(R.id.item_fragment_scrollview_ll_layout,
            ItemTextEntry.newInstance(label, content) as Fragment)
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

        @JvmStatic
        fun newInstance(item : Item) =
            ItemViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ITEM, item)
                }
            }
    }
}
