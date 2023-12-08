package com.example.tender

import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

object DataStoreManager {
    private const val DATASTORE_NAME = "AppPreferences"

    // Lazy initialization of the DataStore
    private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

    fun getInstance(context: Context): DataStore<Preferences> {
        return context.applicationContext.dataStore
    }
}
