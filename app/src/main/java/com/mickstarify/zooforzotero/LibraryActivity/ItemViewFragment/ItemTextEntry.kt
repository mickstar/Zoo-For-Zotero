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

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_LABEL = "label"
private const val ARG_CONTENT = "content"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ItemTextEntry.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ItemTextEntry.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ItemTextEntry : Fragment() {
    // TODO: Rename and change types of parameters
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
//            contentView.text = content
        }

        return view
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ItemTextEntry.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ItemTextEntry().apply {
                arguments = Bundle().apply {
                    putString(ARG_LABEL, param1)
                    putString(ARG_CONTENT, param2)
                }
            }
    }
}
