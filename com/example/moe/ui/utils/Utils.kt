package com.example.moe.ui.utils

import android.content.Context
import android.content.res.AssetManager
import android.os.Handler
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.view.MotionEvent
import android.view.Window
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.example.moe.ui.store.ReadmeData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object Utils {
    private var isCounting = false
    private var isProcessing = false
    private val handler = Handler()
    private val gson = Gson()


    fun setDimAmount(window: Window?, dimAmount: Float) {
        window?.let {
            val lp = it.attributes
            lp.dimAmount = dimAmount
            it.attributes = lp
        }
    }

    fun setLongPressListener(button: ImageButton, editText: EditText, increment: Int) {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isProcessing) {
                        isCounting = true
                        isProcessing = true
                        handler.postDelayed(object : Runnable {
                            override fun run() {
                                if (isCounting) {
                                    updateNumber(editText, increment)
                                    handler.postDelayed(this, 100)
                                }
                            }
                        }, 500)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isCounting = false
                    handler.postDelayed({
                        isProcessing = false
                    }, 500)
                }
            }
            false
        }
    }

    fun createMinFilter(minValue: Int): InputFilter {
        return InputFilter { source, start, end, dest, dstart, dend ->
            try {
                val input = (dest.subSequence(0, dstart).toString() +
                        source.subSequence(start, end) +
                        dest.subSequence(dend, dest.length)).toInt()
                if (input >= minValue) {
                    null
                } else {
                    minValue.toString()
                }
            } catch (e: NumberFormatException) {
                ""
            }
        }
    }


    fun updateNumber(editText: EditText, increment: Int) {
        val currentNumber = editText.text.toString().toIntOrNull() ?: 0
        val newNumber = (currentNumber + increment).coerceAtLeast(0)
        editText.setText(newNumber.toString())
    }

    // JSONファイルからデータを読み込むメソッド（内部ストレージにファイルがない場合、assetsからコピーして生成）
    fun <T> loadDataFromFile(context: Context, fileName: String, typeToken: TypeToken<T>): T? {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            val jsonString = file.readText()
            return gson.fromJson(jsonString, typeToken.type)
        } else {
            // Check if file exists in assets
            val assetData = loadDataFromAssetFile(context, fileName, typeToken)
            if (assetData != null) {
                // Save assets data to internal storage
                saveDataToFile(context, fileName, assetData)
                return assetData
            } else {
                // File does not exist in assets; create default data
                val defaultData = createDefaultData()
                // Save the default data to internal storage
                saveDataToFile(context, fileName, defaultData)
                return gson.fromJson(gson.toJson(defaultData), typeToken.type)
            }
        }
    }

    // Create a default data map for the past 7 days and a mostReps key
    private fun createDefaultData(): Map<String, Int> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -6)
        }
        val dataMap = mutableMapOf<String, Int>()

        for (i in 0..6) {
            val date = dateFormat.format(calendar.time)
            dataMap[date] = 0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        dataMap["mostReps"] = 0

        return dataMap
    }


    // assetsフォルダからJSONデータを読み込むメソッド
    fun <T> loadDataFromAssetFile(context: Context, fileName: String, typeToken: TypeToken<T>): T? {
        val assetManager: AssetManager = context.assets
        val gson = Gson()  // Gsonインスタンスをここで作成
        return try {
            assetManager.open(fileName).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val jsonString = reader.readText()
                    gson.fromJson(jsonString, typeToken.type)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // JSONファイルにデータを書き込むメソッド
    fun saveDataToFile(context: Context, fileName: String, data: Any) {
        val file = File(context.filesDir, fileName)
        file.writeText(gson.toJson(data))
    }

    // Achieved ステータスを SharedPreferences に保存
    fun saveAchievedStatus(context: Context, folderName: String, isAchieved: Boolean) {
        val sharedPreferences = context.getSharedPreferences("achieved_status", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(folderName, isAchieved).apply()
    }


    // 連続日数を計算するメソッド
    fun calculateMaxContinuousDays(memoryData: Map<String, Int>): Int {
        var maxContinuousDays = 0
        var currentStreak = 0

        for (value in memoryData.values) {
            if (value > 0) {
                currentStreak++
                maxContinuousDays = maxOf(maxContinuousDays, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        return maxContinuousDays
    }


    //memoryフォントサイズ変更
    fun setFormattedText(textView: TextView, value: String, unit: String, valueSize: Float, unitSize: Float) {
        val formattedText = "$value$unit"
        val spannableString = SpannableString(formattedText)

        spannableString.setSpan(
            AbsoluteSizeSpan(valueSize.toInt(), true),
            0,
            value.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        spannableString.setSpan(
            AbsoluteSizeSpan(unitSize.toInt(), true),
            value.length,
            formattedText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spannableString
    }

}
