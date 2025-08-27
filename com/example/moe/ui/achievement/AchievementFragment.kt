package com.example.moe.ui.achievement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.moe.R
import com.example.moe.ui.memory.MemoryManager
import com.example.moe.ui.option.Option
import com.example.moe.ui.utils.Utils
import com.google.gson.reflect.TypeToken

data class Achievement(
    val name: String,
    val condition: Int,
    var isAchieved: Boolean,
    val conditionType: String,
    val detail: String
)

class AchievementFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_achievement, container, false)
        val achievementListLayout = root.findViewById<LinearLayout>(R.id.achievement_list)

        val memoryManager = MemoryManager(requireContext())
        val memoryData = memoryManager.loadMemoryData()
        val mostReps = memoryManager.getMostReps()
        val maxContinuousDays = Utils.calculateMaxContinuousDays(memoryData)

        // mostRepsを除外したデータを作成
        val filteredMemoryData = memoryData.filterKeys { it != "mostReps" }

        val typeToken = object : TypeToken<List<Achievement>>() {}
        val achievements = Utils.loadDataFromFile(requireContext(), "achievements.json", typeToken)

        var lastConditionType: String? = null

        if (achievements != null) {
            for (achievement in achievements) {

                if (achievement.conditionType != lastConditionType) {
                    val dividerTextView = TextView(context)
                    dividerTextView.text = when (achievement.conditionType) {
                        "MOST_REPS" -> "----最高回数----"
                        "DAILY_COUNT" -> "----1日----"
                        "OBJECTIVE" -> "----目標----"
                        "CONTINUOUS" -> "----連続----"
                        "TOTAL" -> "----累計----"
                        "COMPLETE" -> "----完全制覇----"
                        else -> "----その他----"
                    }
                    dividerTextView.textSize = 18f
                    dividerTextView.setTextColor(resources.getColor(android.R.color.white))
                    dividerTextView.setPadding(0, 16, 0, 16)

                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.gravity = android.view.Gravity.CENTER
                    dividerTextView.layoutParams = layoutParams
                    dividerTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER

                    achievementListLayout.addView(dividerTextView)
                    lastConditionType = achievement.conditionType
                }

                val achievementView = LayoutInflater.from(context).inflate(R.layout.item_achievement, achievementListLayout, false)
                val nameTextView = achievementView.findViewById<TextView>(R.id.achievement_name)
                val detailTextView = achievementView.findViewById<TextView>(R.id.achievement_detail)
                val statusIcon = achievementView.findViewById<ImageView>(R.id.achievement_status_icon)
                val statusText = achievementView.findViewById<TextView>(R.id.achievement_status_text)

                nameTextView.text = achievement.name
                detailTextView.text = achievement.detail

                // mostRepsを除外して、条件ごとの値を計算
                val currentValue = when (achievement.conditionType) {
                    "DAILY_COUNT" -> filteredMemoryData.values.maxOrNull() ?: 0 // mostRepsを除外
                    "CONTINUOUS" -> maxContinuousDays
                    "OBJECTIVE" -> filteredMemoryData.values.count { it >= Option.OBJECTIVE } // mostRepsを除外
                    "TOTAL" -> filteredMemoryData.values.sum() // mostRepsを除外
                    "COMPLETE" -> {
                        val totalAchievements = achievements.size
                        val achievedCount = achievements.count { it.isAchieved }
                        val progressPercentage = (achievedCount * 100) / totalAchievements
                        progressPercentage
                    }
                    else -> mostReps
                }

                // 状態の表示部分
                if (achievement.isAchieved) {
                    statusIcon.visibility = View.VISIBLE
                    statusText.visibility = View.GONE
                } else {
                    statusIcon.visibility = View.GONE
                    statusText.visibility = View.VISIBLE
                    statusText.text = when (achievement.conditionType) {
                        "COMPLETE" -> "${currentValue}%"
                        else -> "${currentValue}/${achievement.condition}"
                    }
                }

                achievementListLayout.addView(achievementView)
            }
        }

        return root
    }
}
