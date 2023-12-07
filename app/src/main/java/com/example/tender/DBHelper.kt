package com.example.tender

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.google.gson.Gson

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "LikeDislikeDatabase"

        private const val TABLE_LIKES = "likes"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_LOCATION = "location"
        private const val KEY_RATING = "rating"
        private const val KEY_IMAGE_URL = "image_url"
        private const val KEY_REVIEWS = "reviews"
        private const val KEY_PHOTOS = "photos"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
        private const val KEY_LIKED = "liked" // 0 for dislike, 1 for like
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_LIKES(
                $KEY_ID INTEGER PRIMARY KEY, 
                $KEY_NAME TEXT, 
                $KEY_LOCATION TEXT, 
                $KEY_RATING REAL,
                $KEY_IMAGE_URL TEXT,
                $KEY_REVIEWS TEXT,
                $KEY_PHOTOS TEXT,
                $KEY_LATITUDE REAL,
                $KEY_LONGITUDE REAL,
                $KEY_LIKED INTEGER
            )
        """
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LIKES")
        onCreate(db)
    }

    fun addOrUpdateBusiness(business: Business, liked: Int) {
        val db = this.writableDatabase
        val reviewsJson = Gson().toJson(business.reviews)
        val photosJson = Gson().toJson(business.photos)
        val values = ContentValues().apply {
            put(KEY_NAME, business.name)
            put(KEY_LOCATION, business.location.address1)
            put(KEY_RATING, business.rating)
            put(KEY_IMAGE_URL, business.image_url)
            put(KEY_REVIEWS, reviewsJson)
            put(KEY_PHOTOS, photosJson)
            put(KEY_LATITUDE, business.coordinates.latitude)
            put(KEY_LONGITUDE, business.coordinates.longitude)
            put(KEY_LIKED, liked)
        }
        val cursor = db.query(TABLE_LIKES, arrayOf(KEY_ID), "$KEY_NAME = ? AND $KEY_LOCATION = ?", arrayOf(business.alias, business.location.address1), null, null, null)
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(KEY_ID)
            if (idIndex != -1) {
                val id = cursor.getInt(idIndex)
                db.update(TABLE_LIKES, values, "$KEY_ID = ?", arrayOf(id.toString()))
            }
        } else {
            db.insert(TABLE_LIKES, null, values)
        }
        cursor.close()
        db.close()
    }


    fun getBusinesses(likedStatus: Int): List<Business> {
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $TABLE_LIKES WHERE $KEY_LIKED = ?"
        val cursor = db.rawQuery(selectQuery, arrayOf(likedStatus.toString()))

        val businesses = mutableListOf<Business>()
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(KEY_NAME)
            val locationIndex = cursor.getColumnIndex(KEY_LOCATION)
            val ratingIndex = cursor.getColumnIndex(KEY_RATING)
            val imageUrlIndex = cursor.getColumnIndex(KEY_IMAGE_URL)
            val reviewsIndex = cursor.getColumnIndex(KEY_REVIEWS)
            val photosIndex = cursor.getColumnIndex(KEY_PHOTOS)
            val latitudeIndex = cursor.getColumnIndex(KEY_LATITUDE)
            val longitudeIndex = cursor.getColumnIndex(KEY_LONGITUDE)

            // Check if indices are valid
            if (nameIndex != -1 && locationIndex != -1 && ratingIndex != -1 && imageUrlIndex != -1 && reviewsIndex != -1 && photosIndex != -1 && latitudeIndex != -1 && longitudeIndex != -1) {
                do {
                    val name = cursor.getString(nameIndex)
                    val location = cursor.getString(locationIndex)
                    val rating = cursor.getDouble(ratingIndex)
                    val imageUrl = cursor.getString(imageUrlIndex)
                    val reviewsJson = cursor.getString(reviewsIndex)
                    val photosJson = cursor.getString(photosIndex)
                    val latitude = cursor.getDouble(latitudeIndex)
                    val longitude = cursor.getDouble(longitudeIndex)

                    val reviews = Gson().fromJson(reviewsJson, Array<Review>::class.java).toList()
                    val photos = Gson().fromJson(photosJson, Array<String>::class.java).toList()

                    businesses.add(Business(name, "", Location(location, "", ""), rating, imageUrl, reviews, photos, Coordinates(latitude, longitude)))
                } while (cursor.moveToNext())
            }
        }
        cursor.close()
        Log.d("DBHelper", "Fetched ${businesses.size} businesses with liked status $likedStatus")
        return businesses
    }

    fun getLikedBusinessNames(): List<String> {
        val db = this.readableDatabase
        val selectQuery = "SELECT $KEY_NAME FROM $TABLE_LIKES WHERE $KEY_LIKED = 1"
        val cursor = db.rawQuery(selectQuery, null)

        val likedNames = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(KEY_NAME)
            if (nameIndex != -1) { // Check if the index is valid
                do {
                    val name = cursor.getString(nameIndex)
                    likedNames.add(name)
                } while (cursor.moveToNext())
            }
        }
        cursor.close()
        return likedNames
    }
}

