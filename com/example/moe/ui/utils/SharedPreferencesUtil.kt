package com.example.moe.ui.utils

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object SharedPreferencesUtil {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_DATA = "data"

    fun saveData(context: Context, data: JSONObject) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString(KEY_DATA, data.toString())
        editor.apply()
    }

    fun loadData(context: Context): JSONObject? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dataString = sharedPreferences.getString(KEY_DATA, null)
        return if (dataString != null) {
            JSONObject(dataString)
        } else {
            null
        }
    }
}
