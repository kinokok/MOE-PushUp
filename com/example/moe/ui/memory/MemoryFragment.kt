package com.example.moe.ui.memory

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moe.R
import com.example.moe.ui.option.Option
import com.example.moe.ui.utils.Utils
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class MemoryFragment : Fragment() {

    private lateinit var memoryManager: MemoryManager
    private lateinit var barChart: BarChart
    private var isInitialSetupDone = false // 初回のみ表示範囲を設定するためのフラグ

    private lateinit var tvTotal: TextView
    private lateinit var tvMax: TextView
    private lateinit var tvMin: TextView
    private lateinit var tvAvg: TextView
    private lateinit var todayTextView: TextView
    private lateinit var yesterdayTextView: TextView // 昨日のデータを表示するTextView
    private lateinit var beforeYesterdayTextView: TextView // 一昨日のデータを表示するTextView

    private lateinit var detailButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_memory, container, false)

        barChart = view.findViewById(R.id.barChart)
        tvTotal = view.findViewById(R.id.tvTotal)
        tvMax = view.findViewById(R.id.tvMax)
        tvMin = view.findViewById(R.id.tvMin)
        tvAvg = view.findViewById(R.id.tvAvg)
        todayTextView = view.findViewById(R.id.today) // 今日の回数を表示するTextView
        yesterdayTextView = view.findViewById(R.id.yesterday) // 昨日の回数を表示するTextView
        beforeYesterdayTextView = view.findViewById(R.id.beforeYesterday) // 一昨日の回数を表示するTextView
        detailButton = view.findViewById(R.id.detail_button)

        detailButton.setOnClickListener {
            findNavController().navigate(R.id.navigation_memory_calendar)
        }

        memoryManager = MemoryManager(requireContext())
        displayBarChart()

        return view
    }

    private fun displayBarChart() {
        val jsonString = memoryManager.loadMemoryData()
        val dataMap: Map<String, Int> = jsonString

        val filteredDataMap = dataMap.filterKeys { it != "mostReps" }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayOfWeekFormat = SimpleDateFormat("E", Locale.getDefault())

        val todayCalendar = Calendar.getInstance()
        val todayDate = inputFormat.format(todayCalendar.time) // 今日の日付を取得
        var todayReps = 0 // 今日の回数を格納する変数

        // 昨日と一昨日のカレンダーを設定
        val yesterdayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayDate = inputFormat.format(yesterdayCalendar.time) // 昨日の日付を取得
        var yesterdayReps = 0 // 昨日の回数を格納する変数

        val beforeYesterdayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -2)
        }
        val beforeYesterdayDate = inputFormat.format(beforeYesterdayCalendar.time) // 一昨日の日付を取得
        var beforeYesterdayReps = 0 // 一昨日の回数を格納する変数

        val last7DaysCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }

        filteredDataMap.entries.forEachIndexed { index, entry ->
            val date = inputFormat.parse(entry.key)
            val calendar = Calendar.getInstance().apply {
                if (date != null) {
                    time = date
                }
            }

            // 今日、昨日、一昨日のデータか確認し、それぞれの回数を保存する
            when (entry.key) {
                todayDate -> todayReps = entry.value
                yesterdayDate -> yesterdayReps = entry.value
                beforeYesterdayDate -> beforeYesterdayReps = entry.value
            }

            val label = if (calendar.after(last7DaysCalendar)) {
                when {
                    calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR)
                            && calendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR) -> "今日"
                    calendar.get(Calendar.YEAR) == yesterdayCalendar.get(Calendar.YEAR)
                            && calendar.get(Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(Calendar.DAY_OF_YEAR) -> "昨日"
                    else -> dayOfWeekFormat.format(calendar.time)
                }
            } else {
                val month = calendar.get(Calendar.MONTH) + 1
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                "$month/$day"
            }

            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            labels.add(label)
        }

        // 今日、昨日、一昨日の回数をそれぞれのTextViewにセット
        Utils.setFormattedText(todayTextView, "$todayReps", "回", 30f, 20f)
        Utils.setFormattedText(yesterdayTextView, "$yesterdayReps", "回", 30f, 20f)
        Utils.setFormattedText(beforeYesterdayTextView, "$beforeYesterdayReps", "回", 30f, 20f)

        // その他の統計情報をセット (合計、最大、最小、平均など)
        val last7DaysData = entries.takeLast(7).map { it.y.toInt() }
        val total = last7DaysData.sum()
        val max = last7DaysData.maxOrNull() ?: 0
        val min = last7DaysData.minOrNull() ?: 0
        val avg = if (last7DaysData.isNotEmpty()) last7DaysData.average() else 0.0

        val valueSize = 16f
        val unitSize = 10f

        // 統計情報をTextViewに表示
        Utils.setFormattedText(tvTotal, "$total", "回", 35f, 25f)
        Utils.setFormattedText(tvMax, "$max", "回", valueSize, unitSize)
        Utils.setFormattedText(tvMin, "$min", "回", valueSize, unitSize)
        Utils.setFormattedText(tvAvg, "%.1f".format(avg), "回", valueSize, unitSize)

        val dataSet = BarDataSet(entries, "")
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }
        dataSet.valueTextSize = 20f
        dataSet.valueTypeface = ResourcesCompat.getFont(requireContext(), R.font.azuki_font)
        dataSet.valueTextColor = Color.WHITE

        val customBlueColor = Color.parseColor("#0083FF")
        val defaultGrayColor = 0xFF888888.toInt()

        dataSet.colors = entries.map {
            if (it.y >= Option.OBJECTIVE) customBlueColor else defaultGrayColor
        }

        val barData = BarData(dataSet)
        barChart.data = barData

        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.setLabelCount(7, false)
        xAxis.textSize = 12f
        xAxis.textColor = Color.WHITE

        val leftAxis = barChart.axisLeft
        leftAxis.setDrawGridLines(false)
        leftAxis.setDrawLabels(false)
        leftAxis.axisMinimum = 0f

        val maxValue = filteredDataMap.values.maxOrNull()?.toFloat() ?: 0f
        leftAxis.axisMaximum = maxValue + maxValue * 0.1f

        barChart.axisRight.isEnabled = false
        barChart.isScaleYEnabled = false
        barChart.isScaleXEnabled = false

        if (!isInitialSetupDone) {
            val dataSize = entries.size.toFloat()
            val visibleRange = if (dataSize < 7) dataSize else 7f
            barChart.setVisibleXRangeMaximum(visibleRange)
            barChart.moveViewToX(dataSize - visibleRange)
            isInitialSetupDone = true
        }

        val typeface = ResourcesCompat.getFont(requireContext(), R.font.azuki_font)
        barChart.setNoDataTextTypeface(typeface)
        barChart.setNoDataTextColor(Color.WHITE)
        xAxis.typeface = typeface
        barChart.axisLeft.typeface = typeface
        barChart.axisRight.typeface = typeface
        barData.setValueTypeface(typeface)

        barChart.renderer = RoundedBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler)

        barChart.legend.isEnabled = false
        barChart.description.isEnabled = false
        barChart.invalidate()
    }

}
