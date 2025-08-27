package com.example.moe.ui.option

import android.content.Context
import com.example.moe.ui.utils.SharedPreferencesUtil
import org.json.JSONObject

object Option {
    var OBJECTIVE = 20
    var NAME = "Anonymous"
    var THEME = "e"
    var COIN = 5000
    var SENSOR = 0

    fun saveOptions(context: Context) {
        val data = JSONObject().apply {
            put("OBJECTIVE", OBJECTIVE)
            put("NAME", NAME)
            put("THEME", THEME)
            put("COIN", COIN)
            put("SENSOR", SENSOR)
        }
        SharedPreferencesUtil.saveData(context, data)
    }

    fun loadOptions(context: Context) {
        val data = SharedPreferencesUtil.loadData(context)
        data?.let {
            OBJECTIVE = it.optInt("OBJECTIVE", 20)
            NAME = it.optString("NAME", "Anonymous")
            THEME = it.optString("THEME", "e")
            COIN = it.optInt("COIN", 0)
            SENSOR = it.optInt("SENSOR", 0)
        }
    }
}
