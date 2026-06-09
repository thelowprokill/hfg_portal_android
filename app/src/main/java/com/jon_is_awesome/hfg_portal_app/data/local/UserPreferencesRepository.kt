package com.jon_is_awesome.hfg_portal_app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val TEMPERATURE_UNIT = stringPreferencesKey("temperature_unit")
    }

    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "User" }
    val currencySymbol: Flow<String> = context.dataStore.data.map { it[CURRENCY_SYMBOL] ?: "$" }
    val temperatureUnit: Flow<String> = context.dataStore.data.map { it[TEMPERATURE_UNIT] ?: "fahrenheit" }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun updateCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[CURRENCY_SYMBOL] = symbol }
    }

    suspend fun updateTemperatureUnit(unit: String) {
        context.dataStore.edit { it[TEMPERATURE_UNIT] = unit }
    }
}
