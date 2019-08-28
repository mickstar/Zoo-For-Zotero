package com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.mickstarify.zooforzotero.R

private const val ARG_LABEL = "label"
private const val ARG_CONTENT = "content"

class ItemTextEntry : Fragment() {
    private var label: String? = null
    private var content: String? = null
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            label = it.getString(ARG_LABEL)
            content = it.getString(ARG_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_item_text_entry, container, false)
        val labelView = view.findViewById<TextView>(R.id.item_label)
        labelView.text = "${label}:"
        val contentView = view.findViewById<TextView>(R.id.item_content)
        contentView.text = content

        if (label == "url"){
            contentView.linksClickable = true
            contentView.movementMethod = LinkMovementMethod.getInstance()
            Linkify.addLinks(contentView, Linkify.WEB_URLS)
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            listener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance(label: String, content: String) =
            ItemTextEntry().apply {
                arguments = Bundle().apply {
                    putString(ARG_LABEL, label)
                    putString(ARG_CONTENT, content)
                }
            }
    }
}
