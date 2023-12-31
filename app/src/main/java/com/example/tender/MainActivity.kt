package com.example.tender

import LoginFragment
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.stringPreferencesKey
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.yuyakaido.android.cardstackview.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request


data class YelpResponse(
    val businesses: List<Business>
)

data class Business(
    val name: String,
    val alias : String,
    val location: Location,
    val rating: Double,
    val image_url: String,
    var reviews: List<Review>,
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
    private val apiKey = "API_KEY"
    private var defaultSearch = "food"
    private var defaultLocation = "Fullerton"
    private lateinit var cardStackView: CardStackView
    private lateinit var adapter: BusinessCardAdapter
    private lateinit var dbHelper: DBHelper
    private lateinit var likeButton: LottieAnimationView
    private lateinit var superlikeButton: LottieAnimationView
    private lateinit var dislikeButton: LottieAnimationView
    private lateinit var menubutton: LottieAnimationView
    private lateinit var textview: TextView
    private var isDataLoaded = false
    private var currentSwipedPosition = 0
    private lateinit var viewModel: TenderViewModel

    private var mGoogleSignInClient: GoogleSignInClient? = null
    companion object {
        val SEARCH_TERM_KEY = stringPreferencesKey("searchTerm")
        val LOCATION_KEY = stringPreferencesKey("location")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Install the splash screen
        installSplashScreen()
        setContentView(R.layout.activity_main)
        Log.d("MainActivity", "onCreate started")

        viewModel = ViewModelProvider(this).get(TenderViewModel::class.java)

        // Check if data is already available and use it
        if (savedInstanceState != null) {
            viewModel.search_term = savedInstanceState.getString("searchTerm", "defaultSearchValue") ?: "defaultSearchValue"
            viewModel.location_term = savedInstanceState.getString("location", "defaultLocationValue") ?: "defaultLocationValue"
        }

//        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        dbHelper = DBHelper(this)
        adapter = BusinessCardAdapter(emptyList()) { business ->
            fetchBusinessDetails(business.alias)
        }
        setupCardStackView()
        setupFABAndButtons()
        if (savedInstanceState == null) {
            refreshData(defaultSearch,defaultLocation)
            Log.d("MainActivity", "RefreshingData")
        } else {
            restoreInstanceState(savedInstanceState)
            Log.d("MainActivity", "RestoreInstance")
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
        else
            refreshData(defaultSearch,defaultLocation)

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("searchTerm", viewModel.search_term)
        outState.putString("location", viewModel.location_term)
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
        setupCardStackView()
        loadDataStoreAndRefreshData()
        setupFABAndButtons()

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
        val intent: Intent = intent
        val name: String? = intent.getStringExtra("name")
        likeButton = findViewById(R.id.btnLike)
        textview = findViewById(R.id.tv_name)
        superlikeButton = findViewById(R.id.btnSuperLike)
        dislikeButton = findViewById(R.id.btnDislike)
        menubutton = findViewById(R.id.floatingActionButton)
        textview.setText(name)
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


    private fun refreshData(searchTerm: String, location: String) {
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

    fun updateSearchCriteriaDataStore(search: String, location: String) {
        CoroutineScope(Dispatchers.IO).launch {
            DataStoreManager.getInstance(this@MainActivity).edit { preferences ->
                preferences[SEARCH_TERM_KEY] = search
                preferences[LOCATION_KEY] = location
            }


            refreshData(search, location)
        }
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

    private fun getUserId(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        return account?.id
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
                R.id.action_choice3 -> {
                    showLoginFragment()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun handleSignOut() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null) {
            // Sign out the user
            GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_SIGN_IN)
                .signOut()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Clear user data from SharedPreferences
                        val sharedPreferences = getSharedPreferences("userdata", Context.MODE_PRIVATE)
                        val editor: SharedPreferences.Editor = sharedPreferences.edit()
                        editor.clear()
                        editor.apply()

                        // Start SplashActivity and finish current activity
                        val intent = Intent(this@MainActivity, SplashActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Handle sign-out failure if needed
                        Log.e("MainActivity", "Sign-out failed", task.exception)
                    }
                }
        } else {
            // Handle the case when no account is signed in
            Log.d("MainActivity", "No account signed in")
        }
    }

    private fun showSearchLocationFragment() {
        val fragment = SearchLocationFragment()
        fragment.show(supportFragmentManager, "SearchLocationFragment")
    }

    private fun showLikedDislikedFragment() {
        val fragment = LikedDislikedFragment()
        fragment.show(supportFragmentManager, "LikedDislikedFragment")
    }

    private fun showLoginFragment() {
        val fragment = LoginFragment()
        fragment.show(supportFragmentManager, "LoginFragment")
    }

    private fun loadDataStoreAndRefreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = DataStoreManager.getInstance(this@MainActivity).data.first()
            val search = viewModel.search_term
            val location = viewModel.location_term

            withContext(Dispatchers.Main) {
                updateSearchCriteriaDataStore(search, location) // This will refresh the data with the stored preferences
            }
        }
    }
//    }
    override fun onBackPressed() {
        if (!isUserSignedIn()) {
            super.onBackPressed()
        }
    }
    private fun isUserSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(this) != null
    }
}
