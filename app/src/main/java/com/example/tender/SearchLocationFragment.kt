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

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
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

        val getLocationButton = view.findViewById<Button>(R.id.btnGetCurrentLocation)
        getLocationButton.setOnClickListener {
            getLastLocation()
        }
        etSearch.setOnEditorActionListener(editorActionListener)
        etLocation.setOnEditorActionListener(editorActionListener)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val search = view.findViewById<EditText>(R.id.etSearch).text.toString()
            val location = view.findViewById<EditText>(R.id.etLocation).text.toString()
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
    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null) {
                        if (addresses.isNotEmpty()) {
                            val cityName = addresses[0].locality
                            view?.findViewById<EditText>(R.id.etLocation)?.setText(cityName)
                        }
                    }
                }
            }

        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLastLocation()
                } else {
                    // Permission was denied. Handle the functionality without location access.
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
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
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}