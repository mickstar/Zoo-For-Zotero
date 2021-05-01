package com.mickstarify.zooforzotero.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mickstarify.zooforzotero.LibraryActivity.ListEntry
import com.mickstarify.zooforzotero.R

class LibraryListRecyclerViewAdapter(
    val items: List<ListEntry>
) : RecyclerView.Adapter<LibraryListRecyclerViewAdapter.ListEntryViewHolder>() {
    class ListEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.imageView_library_list_image)
        val textView_title = view.findViewById<TextView>(R.id.TextView_library_list_title)
        val textView_author = view.findViewById<TextView>(R.id.TextView_library_list_author)
        val pdfImage = view.findViewById<TextView>(R.id.imageView_attachment_icon)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LibraryListRecyclerViewAdapter.ListEntryViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_screen_list_item, parent, false)

        return ListEntryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: LibraryListRecyclerViewAdapter.ListEntryViewHolder,
        position: Int
    ) {

        val entry = items.get(position)
        if (entry.isItem()) {
            val item = entry.getItem()
            holder.textView_title.text = item.getTitle()
            holder.textView_author.text = item.getAuthor()
        } else {
            val collection = entry.getCollection()
            holder.textView_title.text = collection.name
            holder.pdfImage.visibility = View.GONE
            holder.imageView.setImageResource(R.drawable.treesource_collection_2x)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }


}