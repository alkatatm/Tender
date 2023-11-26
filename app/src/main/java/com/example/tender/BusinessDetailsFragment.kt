package com.example.tender

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson

class BusinessDetailsFragment : DialogFragment() {

    private var business: Business? = null
    private lateinit var mapView: MapView

    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels * 0.9).toInt() // 90% of screen width
        val height = (metrics.heightPixels * 0.9).toInt() // 60% of screen height
        dialog?.window?.setLayout(width, height)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("BUSINESS_DETAILS")?.let {
            business = Gson().fromJson(it, Business::class.java)
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_business_details, container, false)
        val reviewsLayout = view.findViewById<LinearLayout>(R.id.layoutReviews)
        val tvBusinessName = view.findViewById<TextView>(R.id.tvBusinessNameDetails)
        val tvBusinessLocation = view.findViewById<TextView>(R.id.tvBusinessLocation)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerImages)
        val yelpStars = view.findViewById<ImageView>(R.id.yelpStars)
        business?.let {
            tvBusinessName.text = it.name
            tvBusinessLocation.text = "${it.location.address1}, ${it.location.city}, ${it.location.zip_code}"
            yelpStars.setImageResource(getStarResource(it.rating))
            // Set up the ViewPager for images
            if (it.photos.isNotEmpty()) {
                val adapter = ImageAdapter(it.photos)
                viewPager.adapter = adapter
            } else {
                // Handle case when there are no photos
            }
        }

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { googleMap ->
            // Ensure business and its coordinates are not null
            val businessCoordinates = business?.coordinates
            if (businessCoordinates != null) {
                val location = LatLng(businessCoordinates.latitude, businessCoordinates.longitude)

                // Add a marker at the business location
                googleMap.addMarker(MarkerOptions().position(location).title(business?.name))

                // Move the camera to the business location
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            } else {
                // Handle the case where business or coordinates are null
                // You might want to log an error or show a message
            }
        }

        business?.reviews?.forEach { review ->
            val reviewView = inflater.inflate(R.layout.review_card, reviewsLayout, false)
            val tvReviewText = reviewView.findViewById<TextView>(R.id.tvReviewText)
            val imgReviewStars = reviewView.findViewById<ImageView>(R.id.imgReviewStars)

            tvReviewText.text = "\"${review.text}\"\n${review.user.name}"
            imgReviewStars.setImageResource(getStarResource(review.rating))

            reviewsLayout.addView(reviewView)
        }
        return view
    }
    private fun formatReviews(reviews: List<Review>): String {
        if (reviews.isNullOrEmpty()) {
            return "No reviews available."  // Return a default message
        }
        return reviews.joinToString("\n\n") { review ->
            "\"${review.text}\" - ${review.user.name}"
        }
    }
    private fun getStarResource(rating: Number): Int {
        return when (rating.toDouble()) {
            1.0 -> R.drawable.stars_regular_1
            1.5 -> R.drawable.stars_regular_1_half
            2.0 -> R.drawable.stars_regular_2
            2.5 -> R.drawable.stars_regular_2_half
            3.0 -> R.drawable.stars_regular_3
            3.5 -> R.drawable.stars_regular_3_half
            4.0 -> R.drawable.stars_regular_4
            4.5 -> R.drawable.stars_regular_4_half
            5.0 -> R.drawable.stars_regular_5
            else -> R.drawable.stars_regular_0 // Default or no rating
        }
    }
    companion object {
        fun newInstance(business: Business): BusinessDetailsFragment {
            val fragment = BusinessDetailsFragment()
            val args = Bundle()
            args.putString("BUSINESS_DETAILS", Gson().toJson(business))
            fragment.arguments = args
            return fragment
        }
    }
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
