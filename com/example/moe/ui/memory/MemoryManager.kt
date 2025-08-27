package com.example.moe.ui.memory

import android.content.Context
import com.example.moe.ui.achievement.AchievementManager
import com.example.moe.ui.utils.Utils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MemoryManager(private val context: Context) {

    private val fileName = "memory.json"
    private var tapDataMap = mutableMapOf<String, Int>()
    private var mostReps: Int = 0
    private val achievementManager = AchievementManager(context)

    init {
        val data = loadDataFromFile()
        tapDataMap = data.first.toMutableMap()
        mostReps = data.second
        checkAndAddDate()
        achievementManager.checkAchievements() // 初期ロード時に実績チェック
    }

    private fun loadDataFromFile(): Pair<Map<String, Int>, Int> {
        val dataMap: Map<String, Any>? = Utils.loadDataFromFile(context, fileName, object : TypeToken<Map<String, Any>>() {})

        if (dataMap != null) {
            val repsDataMap = dataMap.filterKeys { it != "mostReps" && it != "achievements" }.mapValues {
                (it.value as Double).toInt()
            }
            val mostReps = (dataMap["mostReps"] as? Double)?.toInt() ?: 0
            return Pair(repsDataMap, mostReps)
        }
        return Pair(mutableMapOf(), 0)
    }

    private fun saveDataToFile(data: Map<String, Int>, mostReps: Int) {
        val dataWithMostReps = data.toMutableMap().apply {
            put("mostReps", mostReps)
        }
        Utils.saveDataToFile(context, fileName, dataWithMostReps)
    }

    private fun checkAndAddDate() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateFormat.format(Date())

        val lastDate = tapDataMap.keys.maxOrNull()

        if (lastDate != null && lastDate != today) {
            var date = dateFormat.parse(lastDate) ?: return
            val currentDate = dateFormat.parse(today) ?: return

            while (date.before(currentDate)) {
                date = Date(date.time + (1000 * 60 * 60 * 24))
                val formattedDate = dateFormat.format(date)
                if (!tapDataMap.containsKey(formattedDate)) {
                    tapDataMap[formattedDate] = 0
                }
            }
        }

        if (!tapDataMap.containsKey(today)) {
            tapDataMap[today] = 0
        }

        saveDataToFile(tapDataMap, mostReps)
    }

    fun saveTapData(vararg values: Int) {
        checkAndAddDate()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val maxValue = values.maxOrNull() ?: 0

        tapDataMap[today] = tapDataMap.getOrDefault(today, 0) + maxValue

        if (maxValue > mostReps) {
            mostReps = maxValue
        }

        saveDataToFile(tapDataMap, mostReps)
        achievementManager.checkAchievements() // データが更新されたときに実績をチェック
    }

    fun getMostReps(): Int {
        return mostReps
    }

    fun loadMemoryData(): Map<String, Int> {
        return Utils.loadDataFromFile(context, fileName, object : TypeToken<Map<String, Int>>() {}) ?: emptyMap()
    }
}
