package com.mickstarify.zooforzotero.LibraryActivity.ItemView


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mickstarify.zooforzotero.LibraryActivity.ItemView.ItemViewFragment.OnItemFragmentInteractionListener
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import kotlinx.android.synthetic.main.fragment_item_text_entry.view.*


class MyItemRecyclerViewAdapter(
    private val itemObject : Item,
    private val mListener: OnItemFragmentInteractionListener?
) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

//    private val mOnClickListener: View.OnClickListener

    init {
//        mOnClickListener = View.OnClickListener { v ->
//            val item = v.tag as DummyItem
//            // Notify the active callbacks interface (the activity, if the fragment is attached to
//            // one) that an item has been selected.
//            mListener?.onListFragmentInteraction(item)
//        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_item_text_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (position){
            1 -> {
                holder.mIdView.text = "Title: "
                holder.mContentView.text = itemObject.getTitle()
            }
            2 -> {
            }
        }

//        with(holder.mView) {
//            tag = "hello"
//            setOnClickListener(mOnClickListener)
//        }
    }

    override fun getItemCount(): Int {
        return itemObject.data.size + 3
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_label
        val mContentView: TextView = mView.item_content

        override fun toString(): String {
            return super.toString() + " '" + mContentView.text + "'"
        }
    }
}
