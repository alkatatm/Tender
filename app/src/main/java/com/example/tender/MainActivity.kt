package com.example.tender

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackView
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val apiKey = "ZfWb7NOfgmB3jlHdqQtcxW-XlFPkVTggHBL8Ddr5S2s_4mAhCecID02p_np2D2Rz7C03nA01ZUxhdYFd_qMPPb_O2I3fsuJlapLfShGTGCYNuW2NpVKwE2TvxTRUZXYx"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Test cases
        val testData = listOf(
            Pair("coffee", "New York City"),
            Pair("Italian restaurant", "Chicago"),
            Pair("gym", "Los Angeles"),
            Pair("bookstore", "Seattle"),
            Pair("bakery", "Paris"),
            Pair("sushi", "Tokyo"),
            Pair("nightlife", "Las Vegas"),
            Pair("art gallery", "London"),
            Pair("spa", "Bali"),
            Pair("vegan restaurant", "San Francisco")
        )

        // Loop through each test case and call the getYelpData function asynchronously
        testData.forEach { (term, location) ->
            CoroutineScope(Dispatchers.IO).launch {
                val result = getYelpData(term, location)
                withContext(Dispatchers.Main) {
                    println("Searching for $term in $location:")
                    println(result)
                    println("--------------------------------------------------")
                }
            }
        }
    }

    private suspend fun getYelpData(term: String, location: String): String {
        val url = "https://api.yelp.com/v3/businesses/search?term=$term&location=$location"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .build()

        return client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string() ?: "No response body"
            } else {
                "Failed to get data: ${response.message}"
            }
        }
    }
}
