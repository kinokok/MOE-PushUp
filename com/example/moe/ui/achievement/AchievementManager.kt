package com.example.moe.ui.achievement

import android.content.Context
import com.example.moe.ui.option.Option
import com.example.moe.ui.utils.Utils
import com.google.gson.reflect.TypeToken

class AchievementManager(private val context: Context) {

    private val fileName = "achievements.json"
    private var achievements = mutableListOf<Achievement>()

    init {
        loadAchievements()
    }

    fun checkAchievements() {
        val memoryData = loadMemoryData()
        val mostReps = memoryData["mostReps"] ?: 0
        val maxContinuousDays = Utils.calculateMaxContinuousDays(memoryData)
        val totalSum = memoryData.filterKeys { it != "mostReps" }.values.sum() // mostRepsを除外
        val filteredMemoryData = memoryData.filterKeys { it != "mostReps" }

        var allAchieved = true

        for (achievement in achievements) {
            when (achievement.conditionType) {
                "MOST_REPS" -> {
                    if (mostReps >= achievement.condition && !achievement.isAchieved) {
                        achievement.isAchieved = true
                    }
                }
                "DAILY_COUNT" -> {
                    // mostRepsを除外して判定
                    val achieved = filteredMemoryData.entries.any {
                        it.value >= achievement.condition && !achievement.isAchieved
                    }
                    if (achieved) {
                        achievement.isAchieved = true
                    }
                }
                "CONTINUOUS" -> {
                    if (maxContinuousDays >= achievement.condition && !achievement.isAchieved) {
                        achievement.isAchieved = true
                    }
                }
                "TOTAL" -> {
                    // mostRepsを除外した累計の判定
                    if (totalSum >= achievement.condition && !achievement.isAchieved) {
                        achievement.isAchieved = true
                    }
                }
                "OBJECTIVE" -> {
                    // mostRepsを除外して目標達成数をカウント
                    val objectiveCount = filteredMemoryData.values.count {
                        it >= Option.OBJECTIVE
                    }

                    if (objectiveCount >= achievement.condition && !achievement.isAchieved) {
                        achievement.isAchieved = true
                    }
                }
                "COMPLETE" -> {
                    // 全ての実績が達成されているか確認
                    if (achievements.all { it.isAchieved } && !achievement.isAchieved) {
                        achievement.isAchieved = true
                    }
                }
            }
            if (!achievement.isAchieved && achievement.conditionType != "COMPLETE") {
                allAchieved = false
            }
        }

        if (allAchieved) {
            achievements.find { it.conditionType == "COMPLETE" }?.let {
                it.isAchieved = true
            }
        }

        saveAchievements()
    }

    private fun saveAchievements() {
        Utils.saveDataToFile(context, fileName, achievements)
    }

    private fun loadAchievements() {
        val typeToken = object : TypeToken<MutableList<Achievement>>() {}
        val loadedData = Utils.loadDataFromFile(context, fileName, typeToken)
        if (loadedData != null) {
            achievements = loadedData
        }
    }

    private fun loadMemoryData(): Map<String, Int> {
        val typeToken = object : TypeToken<Map<String, Int>>() {}
        return Utils.loadDataFromFile(context, "memory.json", typeToken) ?: emptyMap()
    }
}
