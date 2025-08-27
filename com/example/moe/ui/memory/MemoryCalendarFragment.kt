package com.example.moe.ui.memory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moe.R
import com.example.moe.ui.memory.MemoryManager
import com.example.moe.ui.utils.Utils
import java.util.*

class MemoryCalendarFragment : Fragment() {

    private lateinit var backButton: ImageButton
    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateLabel: TextView
    private lateinit var validDates: Set<String>
    private lateinit var memoryData: Map<String, Int>

    private lateinit var mostOne: TextView
    private lateinit var mostTwo: TextView
    private lateinit var mostThree: TextView
    private lateinit var totalLabel: TextView

    private lateinit var memoryManager: MemoryManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_memory_calendar, container, false)

        // MemoryManagerの初期化
        memoryManager = MemoryManager(requireContext())

        // ボタンとカレンダーの初期化
        backButton = view.findViewById(R.id.back_button)
        calendarView = view.findViewById(R.id.calendar_view)
        selectedDateLabel = view.findViewById(R.id.selected_date_label)

        mostOne = view.findViewById(R.id.mostOne)
        mostTwo = view.findViewById(R.id.mostTwo)
        mostThree = view.findViewById(R.id.mostThree)
        totalLabel = view.findViewById(R.id.total)

        // memory.jsonファイルの読み込み
        memoryData = memoryManager.loadMemoryData()
        validDates = memoryData.keys

        // 最高記録の表示
        displayMostRecords()

        // 累計記録の表示
        displayTotalReps()

        // 現在の日付を取得し、カレンダーの最大日付を設定
        val today = Calendar.getInstance()
        calendarView.maxDate = today.timeInMillis

        // カレンダーの日付が選択されたときにラベルに表示
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }

            val correctedMonth = month + 1
            val formattedDate = String.format("%d-%02d-%02d", year, correctedMonth, dayOfMonth)

            if (!validDates.contains(formattedDate)) {
                selectedDateLabel.text = "データなし"
            } else {
                val reps = memoryData[formattedDate]
                selectedDateLabel.text = "Date: $formattedDate, Reps: $reps"
            }
        }

        backButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_memory)
        }

        return view
    }

    private fun displayMostRecords() {
        val sortedRecords = memoryData
            .filterKeys { it != "mostReps" } // Exclude "mostReps"
            .entries
            .sortedByDescending { it.value }
            .take(3)

        val valueSize = 35f
        val unitSize = 25f

        Utils.setFormattedText(mostOne, "${sortedRecords.getOrNull(0)?.value ?: 0}", "回", valueSize, unitSize)
        Utils.setFormattedText(mostTwo, "${sortedRecords.getOrNull(1)?.value ?: 0}", "回", valueSize, unitSize)
        Utils.setFormattedText(mostThree, "${sortedRecords.getOrNull(2)?.value ?: 0}", "回", valueSize, unitSize)
    }


    // 累計記録を表示する関数
    private fun displayTotalReps() {
        val totalReps = memoryData.filterKeys { it != "mostReps" }.values.sum()
        val valueSize = 35f
        val unitSize = 25f

        Utils.setFormattedText(totalLabel, "$totalReps", "回", valueSize, unitSize)
    }

}
