package com.example.tender

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
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
    private var defaultSearch = "food"
    private var defaultLocation = "Fullerton"
    private lateinit var cardStackView: CardStackView
    private lateinit var adapter: BusinessCardAdapter
    private lateinit var dbHelper: DBHelper
    private lateinit var likeButton: LottieAnimationView
    private lateinit var superlikeButton: LottieAnimationView
    private lateinit var dislikeButton: LottieAnimationView
    private lateinit var menubutton: LottieAnimationView
    private var isDataLoaded = false
    private var currentSwipedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate started")

        dbHelper = DBHelper(this)
        adapter = BusinessCardAdapter(emptyList()) { business ->
            fetchBusinessDetails(business.alias)
        }
        setupCardStackView()
        loadPreferencesAndRefreshData()
        setupFABAndButtons()
        if (savedInstanceState == null) {
            loadPreferencesAndRefreshData()
        } else {
            restoreInstanceState(savedInstanceState)
        }
        Log.d("MainActivity", "onCreate finished")
    }
    private fun restoreInstanceState(savedInstanceState: Bundle) {
        // Restore any needed state here
        defaultSearch = savedInstanceState.getString("defaultSearch", defaultSearch)
        defaultLocation = savedInstanceState.getString("defaultLocation", defaultLocation)
        isDataLoaded = savedInstanceState.getBoolean("isDataLoaded")

        if (isDataLoaded) {
            // Re-fetch data or restore UI state
            refreshBusinesses()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("defaultSearch", defaultSearch)
        outState.putString("defaultLocation", defaultLocation)
        outState.putBoolean("isDataLoaded", isDataLoaded)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Handle configuration change, like orientation
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d("MainActivity", "Landscape mode")
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("MainActivity", "Portrait mode")
        }
        setContentView(R.layout.activity_main)
    }
    private fun setupCardStackView() {
        cardStackView = findViewById<CardStackView>(R.id.card_stack_view).apply {
            adapter = this@MainActivity.adapter
            layoutManager = CardStackLayoutManager(this@MainActivity, this@MainActivity).apply {
                setStackFrom(StackFrom.None)
                setVisibleCount(3)
                setSwipeableMethod(SwipeableMethod.AutomaticAndManual)
                setDirections(Direction.FREEDOM)
            }
        }
        Log.d("MainActivity", "CardStackView setup completed")
    }

    private fun setupFABAndButtons() {
        findViewById<LottieAnimationView>(R.id.floatingActionButton).setOnClickListener { view ->
            showPopupMenu(view)
        }
        likeButton = findViewById(R.id.btnLike)
        superlikeButton = findViewById(R.id.btnSuperLike)
        dislikeButton = findViewById(R.id.btnDislike)
        menubutton = findViewById(R.id.floatingActionButton)
        likeButton.setOnClickListener {
            likeButton.playAnimation()
            swipeCard(Direction.Right) }
        dislikeButton.setOnClickListener {
            dislikeButton.playAnimation()
            swipeCard(Direction.Left) }
        superlikeButton.setOnClickListener {
            superlikeButton.playAnimation()
            swipeCard(Direction.Top) }
        Log.d("MainActivity", "FAB and buttons set up")
    }

    private fun loadPreferencesAndRefreshData() {
        val sharedPrefs = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        defaultSearch = sharedPrefs.getString("searchTerm", defaultSearch) ?: defaultSearch
        defaultLocation = sharedPrefs.getString("location", defaultLocation) ?: defaultLocation
        refreshBusinesses()
        Log.d("MainActivity", "Preferences loaded and data refresh initiated")
    }

    fun updateSearchCriteria(search: String, location: String) {
        // Save the new criteria to preferences
        val editor = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE).edit()
        editor.putString("searchTerm", search)
        editor.putString("location", location)
        editor.apply()

        // Refresh data with new criteria
        refreshData(search, location)
    }
    private fun refreshBusinesses() {
        CoroutineScope(Dispatchers.IO).launch {
            // Fetch liked names first
            val likedNames = dbHelper.getLikedBusinessNames()
            // Then fetch Yelp data with the updated list of liked names
            val yelpData = getYelpData(defaultSearch, defaultLocation)

            withContext(Dispatchers.Main) {
                updateUIWithFetchedData(yelpData, likedNames)
            }
        }
    }

    private fun updateUIWithFetchedData(yelpData: YelpResponse?, likedNames: List<String>) {
        val filteredBusinesses =
            yelpData?.businesses?.filterNot { it.name in likedNames } ?: listOf()
        adapter.updateData(filteredBusinesses)

        Log.d("MainActivity", "UI updated with fetched data")

        findViewById<TextView>(R.id.tvNoMoreRestaurants).visibility = View.VISIBLE
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
                    BusinessDetailsFragment.newInstance(business)
                        .show(supportFragmentManager, "businessDetails")
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
            Log.e(
                "YelpBusinessDetails",
                "Failed to get business details: ${businessDetailsResponse.message}"
            )
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
    // CardStackListener methods
    override fun onCardSwiped(direction: Direction?) {
        if (direction == Direction.Right || direction == Direction.Left) {
            val swipeDirectionLiked = direction == Direction.Right
            val businessAtPosition = adapter.getBusinessAt(currentSwipedPosition)

            businessAtPosition?.let { business ->
                CoroutineScope(Dispatchers.IO).launch {
                    val fullBusinessDetails = getYelpBusinessDetails(business.alias)
                    val reviews = getBusinessReviews(business.alias)

                    fullBusinessDetails?.let {
                        // Update the business object with reviews
                        it.reviews = reviews

                        // Save the complete business details with liked status
                        val likedStatus = if (swipeDirectionLiked) 1 else 0
                        dbHelper.addOrUpdateBusiness(fullBusinessDetails, likedStatus)

                        // Log the action
                        Log.d("MainActivity", "Swiped ${if (swipeDirectionLiked) "liked" else "disliked"}: ${it.name}")
                    } ?: run {
                        Log.e("MainActivity", "Failed to fetch business details for: ${business.name}")
                    }
                }
            }
        }
        Log.d("MainActivity", "onCardSwiped: $direction")
    }



    override fun onCardDragging(direction: Direction?, ratio: Float) {
        val layoutManager = cardStackView.layoutManager as? CardStackLayoutManager
        currentSwipedPosition = layoutManager?.topPosition ?: 0
        Log.d(
            "MainActivity",
            "onCardDragging: $direction, ratio: $ratio, position: $currentSwipedPosition"
        )
    }

    override fun onCardRewound() {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}

    private fun showPopupMenu(view: View) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.floating_action_menu, popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_choice1 -> {
                    showSearchLocationFragment()
                    true
                }

                R.id.action_choice2 -> {
                    showLikedDislikedFragment()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun showSearchLocationFragment() {
        val fragment = SearchLocationFragment()
        // You can add logic here to pass data to the fragment if needed
        fragment.show(supportFragmentManager, "SearchLocationFragment")
    }

    private fun showLikedDislikedFragment() {
        val fragment = LikedDislikedFragment()
        // You can add logic here to pass data to the fragment if needed
        fragment.show(supportFragmentManager, "LikedDislikedFragment")
    }

    private fun refreshData(searchTerm: String, location: String) {
        // Update your data and refresh the view. Example:
        defaultSearch = searchTerm
        defaultLocation = location
        CoroutineScope(Dispatchers.IO).launch {
            val yelpData = getYelpData(defaultSearch, defaultLocation)
            withContext(Dispatchers.Main) {
                yelpData?.businesses?.let { businesses ->
                    adapter.updateData(businesses)
                }
            }
        }
    }
}
