package com.example.tender

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yuyakaido.android.cardstackview.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

data class YelpResponse(
    val businesses: List<Business>
)

data class Business(
    val name: String,
    val alias : String,
    val location: Location,
    val rating: Double,  // Assuming rating is a double
    val image_url: String,  // URL to the business image
    var reviews: List<Review>,// Assuming a list of reviews
    val photos: List<String>,
    val coordinates: Coordinates
)
data class Review(
    val user: User,
    val text: String,
    val rating: Int
)

data class User(
    val name: String,
    val image_url: String
)
data class Location(
    val address1: String,
    val city: String,
    val zip_code: String
)

data class  Coordinates(
    val latitude: Double,
    val longitude: Double
)
class MainActivity : AppCompatActivity(), CardStackListener {
    private val client = OkHttpClient()
    private val apiKey = "ZfWb7NOfgmB3jlHdqQtcxW-XlFPkVTggHBL8Ddr5S2s_4mAhCecID02p_np2D2Rz7C03nA01ZUxhdYFd_qMPPb_O2I3fsuJlapLfShGTGCYNuW2NpVKwE2TvxTRUZXYx"

    private lateinit var cardStackView: CardStackView
    private lateinit var adapter: BusinessCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = BusinessCardAdapter(emptyList()) { business ->
            fetchBusinessDetails(business.alias)
        }

        cardStackView = findViewById(R.id.card_stack_view)
        cardStackView.adapter = adapter  // Set the adapter to the CardStackView
        setupCardStackView()

        val testData = listOf(
            Pair("coffee", "New York City")
            // Add more test data if needed
        )

        testData.forEach { (term, location) ->
            CoroutineScope(Dispatchers.IO).launch {
                val yelpData = getYelpData(term, location)
                withContext(Dispatchers.Main) {
                    yelpData?.businesses?.let { businesses ->
                        adapter.updateData(businesses)
                    }
                }
            }
        }

        findViewById<Button>(R.id.btnLike).setOnClickListener { swipeCard(Direction.Right) }
        findViewById<Button>(R.id.btnDislike).setOnClickListener { swipeCard(Direction.Left) }
        findViewById<Button>(R.id.btnSuperLike).setOnClickListener { swipeCard(Direction.Top) }
    }

    private fun setupCardStackView() {
        val manager = CardStackLayoutManager(this, this)
        manager.setStackFrom(StackFrom.None)
        manager.setVisibleCount(3)
        manager.setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
        manager.setDirections(Direction.FREEDOM)
        cardStackView.layoutManager = manager
    }

    private fun swipeCard(direction: Direction) {
        val setting = SwipeAnimationSetting.Builder()
            .setDirection(direction)
            .build()
        (cardStackView.layoutManager as CardStackLayoutManager).setSwipeAnimationSetting(setting)
        cardStackView.swipe()
    }

    private suspend fun getYelpData(term: String, location: String): YelpResponse? {
        val url = "https://api.yelp.com/v3/businesses/search?term=$term&location=$location"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("YelpData", "Response: $responseBody")
                Gson().fromJson(responseBody, YelpResponse::class.java)
            } else {
                Log.e("YelpData", "Failed to get data: ${response.message}")
                null
            }
        }
    }

    private fun fetchBusinessDetails(businessId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val businessDetails = getYelpBusinessDetails(businessId)
            val reviews = getBusinessReviews(businessId)

            withContext(Dispatchers.Main) {
                businessDetails?.let { business ->
                    business.reviews = reviews
                    BusinessDetailsFragment.newInstance(business).show(supportFragmentManager, "businessDetails")
                }
            }
        }
    }

    private suspend fun getYelpBusinessDetails(businessId: String): Business? {
        val businessDetailsUrl = "https://api.yelp.com/v3/businesses/$businessId"
        val businessDetailsRequest = Request.Builder()
            .url(businessDetailsUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()


        // Execute business details request
        val businessDetailsResponse = client.newCall(businessDetailsRequest).execute()
        val business = if (businessDetailsResponse.isSuccessful) {
            Gson().fromJson(businessDetailsResponse.body?.string(), Business::class.java)
        } else {
            Log.e("YelpBusinessDetails", "Failed to get business details: ${businessDetailsResponse.message}")
            return null
        }


        return business
    }

    private suspend fun getBusinessReviews(businessId: String): List<Review> {
        val url = "https://api.yelp.com/v3/businesses/$businessId/reviews"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            val reviewsResponse = Gson().fromJson(response.body?.string(), Business::class.java)
            reviewsResponse.reviews
        } else {
            Log.e("YelpBusinessReviews", "Failed to get reviews: ${response.message}")
            emptyList()
        }
    }
    // CardStackListener methods
    override fun onCardSwiped(direction: Direction?) {
        // Handle card swipe event
    }
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardRewound() {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}
}
