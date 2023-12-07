package com.example.tender

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class SearchLocationFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search_location, container, false)
        loadSavedPreferences(view)

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)

        // Set OnEditorActionListener for etLocation
        val editorActionListener = TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sendBackResult(etSearch.text.toString(), etLocation.text.toString())
                true
            } else {
                false
            }
        }

        etSearch.setOnEditorActionListener(editorActionListener)
        etLocation.setOnEditorActionListener(editorActionListener)

        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val search = etSearch.text.toString()
            val location = etLocation.text.toString()
            sendBackResult(search, location)
        }
        return view
    }

    private fun loadSavedPreferences(view: View) {
        val sharedPrefs = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val savedSearchTerm = sharedPrefs.getString("searchTerm", "")
        val savedLocation = sharedPrefs.getString("location", "")

        view.findViewById<EditText>(R.id.etSearch)?.setText(savedSearchTerm)
        view.findViewById<EditText>(R.id.etLocation)?.setText(savedLocation)
    }

    private fun sendBackResult(searchTerm: String, location: String) {
        val sharedPrefs = requireActivity().getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("searchTerm", searchTerm)
            putString("location", location)
            apply()
        }

        (activity as? MainActivity)?.updateSearchCriteria(searchTerm, location)
        dismiss()
    }
}
