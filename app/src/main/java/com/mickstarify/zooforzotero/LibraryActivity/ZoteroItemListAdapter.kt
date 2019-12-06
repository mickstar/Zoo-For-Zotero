package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Database.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item

class ZoteroItemListAdapter(val context: Context, var list: List<ListEntry>) : BaseAdapter() {

    fun updateItems(newList: List<ListEntry>) {
        list = newList
        this.notifyDataSetChanged()
    }

    override fun getView(i : Int, _convertView : View?, parent : ViewGroup?): View {
        val entry = getItem(i)
        // Check if an existing view is being reused, otherwise inflate the view
        val convertView: View = if (_convertView == null) {
            LayoutInflater.from(context).inflate(R.layout.zotero_list_item_adapter, parent, false)
        } else {
            _convertView
        }

        if (entry.isItem()) {
            initItem(entry.getItem(), convertView)
        } else {
            initCollection(entry.getCollection(), convertView)
        }

        return convertView
    }

    fun initItem(item: Item, view: View) {
        val lbl_title: TextView = view.findViewById(R.id.txt_title) as TextView
        val author = view.findViewById<TextView>(R.id.txt_author)
        val img: ImageView = view.findViewById(R.id.imageView) as ImageView
        var icon: Int = R.drawable.treeitem_2x
        when (item.getValue("itemType")) {
            "note" -> {
                icon = R.drawable.treeitem_note_2x
            }
            "book" -> {
                icon = R.drawable.treeitem_book_2x
            }
            "bookSection" -> {
                icon = R.drawable.treeitem_booksection_2x
            }
            "journalArticle" -> {
                icon = R.drawable.treeitem_journalarticle_2x
            }
            "magazineArticle" -> {
                icon = R.drawable.treeitem_magazinearticle_2x
            }
            "newspaperArticle" -> {
                icon = R.drawable.treeitem_newspaperarticle_2x
            }
            "thesis" -> {
                icon = R.drawable.treeitem_thesis
            }
            "letter" -> {
                icon = R.drawable.treeitem_letter_2x
            }
            "manuscript" -> {
                icon = R.drawable.treeitem_manuscript
            }
            "interview" -> {
                icon = R.drawable.treeitem_interview_2x
            }
            "film" -> {
                icon = R.drawable.treeitem_film_2x
            }
            "artwork" -> {
                icon = R.drawable.treeitem_artwork_2x
            }
            "webpage" -> {
                icon = R.drawable.treeitem_webpage_48px
            }
            "attachment" -> {
                icon = R.drawable.treeitem_attachment_web_link_2x
            }
            "report" -> {
                icon = R.drawable.treeitem_report_2x
            }
            "bill" -> {
                icon = R.drawable.treeitem_bill_2x
            }
            "case" -> {
                icon = R.drawable.treeitem_case
            }
            "hearing" -> {
                icon = R.drawable.treeitem_hearing
            }
            "patent" -> {
                icon = R.drawable.treeitem_patent
            }
            "statute" -> {
                icon = R.drawable.treeitem_statute
            }
            "email" -> {
                icon = R.drawable.treeitem_email
            }
            "map" -> {
                icon = R.drawable.treeitem_map
            }
            "blogPost" -> {
                icon = R.drawable.treeitem_blogpost
            }
            "instantMessage" -> {
                icon = R.drawable.treeitem_instantmessage_2x
            }
            "forumPost" -> {
                icon = R.drawable.treeitem_forumpost_2x
            }
            "audioRecording" -> {
                icon = R.drawable.treeitem_audiorecording_2x
            }
            "presentation" -> {
                icon = R.drawable.treeitem_presentation
            }
            "videoRecording" -> {
                icon = R.drawable.treeitem_videorecording
            }
            "tvBroadcast" -> {
                icon = R.drawable.treeitem_tvbroadcast
            }
            "radioBroadcast" -> {
                icon = R.drawable.treeitem_radiobroadcast
            }
            "podcast" -> {
                icon = R.drawable.treeitem_podcast
            }
            "computerProgram" -> {
                icon = R.drawable.treeitem_computerprogram_2x
            }
            "conferencePaper" -> {
                icon = R.drawable.treeitem_conferencepaper
            }
            "document" -> {
                icon = R.drawable.treeitem_webpage_gray_48px
            }
            "encyclopediaArticle" -> {
                icon = R.drawable.treeitem_encyclopediaarticle
            }
            "dictionaryEntry" -> {
                icon = R.drawable.treeitem_dictionaryentry
            }
        }
        img.setImageResource(icon)
        if (item.getItemType() == "note") {
            val note = item.data["note"]
            if (note == null) {
                lbl_title.text = "Unknown note"
            } else {
                // we want to subjugate longer strings with ellipses
                val noteSubstring = note.take(20)
                lbl_title.text = if (noteSubstring == note) {
                    note
                } else {
                    "${noteSubstring}..."
                }
            }
            lbl_title.text = item.data["note"] ?: "Note"
        }
        lbl_title.text = item.getTitle()
        author.text = item.getAuthor()
    }

    fun initCollection(collection: Collection, view: View) {
        val lbl_title: TextView = view.findViewById(R.id.txt_title) as TextView
        val author = view.findViewById<TextView>(R.id.txt_author)
        val img: ImageView = view.findViewById(R.id.imageView) as ImageView
        img.setImageResource(R.drawable.treesource_collection_2x)
        lbl_title.text = collection.name
        author.text = "Subcollection"
    }

    override fun getItem(position: Int): ListEntry {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
//        return getItem(position).
        return 0
    }

    override fun getCount(): Int {
        return list.size
    }

}