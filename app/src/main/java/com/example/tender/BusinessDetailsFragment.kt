package com.example.tender

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson

class BusinessDetailsFragment : DialogFragment() {

    private var business: Business? = null

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
        val tvBusinessName = view.findViewById<TextView>(R.id.tvBusinessNameDetails)
        val tvBusinessLocation = view.findViewById<TextView>(R.id.tvBusinessLocation)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerImages)
        val yelpStars = view.findViewById<ImageView>(R.id.yelpStars)
        val tvBusinessReviews = view.findViewById<TextView>(R.id.tvBusinessReviews)


        business?.let {
            tvBusinessName.text = it.name
            tvBusinessLocation.text = "${it.location.address1}, ${it.location.city}, ${it.location.zip_code}"
            yelpStars.setImageResource(getStarResource(it.rating))
            tvBusinessReviews.text = formatReviews(it.reviews)

            // Set up the ViewPager for images
            if (it.photos.isNotEmpty()) {
                val adapter = ImageAdapter(it.photos)
                viewPager.adapter = adapter
            } else {
                // Handle case when there are no photos
            }
        }
        return view
    }
    private fun formatReviews(reviews: List<Review>): String {
        if (reviews.isNullOrEmpty()) {
            return "No reviews available."  // Return a default message
        }
        return reviews.joinToString("\n\n") { review ->
            "\"${review.text}\" - ${review.user.name}, ${review.rating} Stars"
        }
    }
    private fun getStarResource(rating: Double): Int {
        return when (rating) {
            1.0 -> R.drawable.stars_regular_0
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
    private fun getSmallStarResource(rating: Double): Int {
        return when (rating) {
            1.0 -> R.drawable.stars_small_0
            1.5 -> R.drawable.stars_small_1_half
            2.0 -> R.drawable.stars_small_2
            2.5 -> R.drawable.stars_small_2_half
            3.0 -> R.drawable.stars_small_3
            3.5 -> R.drawable.stars_small_3_half
            4.0 -> R.drawable.stars_small_4
            4.5 -> R.drawable.stars_small_4_half
            5.0 -> R.drawable.stars_small_5
            else -> R.drawable.stars_small_0 // Default or no rating
        }
    }
    private fun logImageUrls(urls: List<String>) {
        urls.forEach { url ->
            Log.d("BusinessDetailsFragment", "Photo URL: $url")
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
}
