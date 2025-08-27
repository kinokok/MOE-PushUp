package com.example.moe.ui.option

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.moe.R
import com.example.moe.databinding.FragmentOptionBinding
import com.example.moe.ui.utils.Utils
import android.app.AlertDialog
import android.widget.SeekBar

class OptionFragment : Fragment() {
    private lateinit var binding: FragmentOptionBinding

    private lateinit var obInButton: ImageButton
    private lateinit var obDeButton: ImageButton
    private lateinit var objectiveText: EditText

    private lateinit var nameText: EditText

    private lateinit var sensorSeekBar: SeekBar
    private lateinit var sensorLabel: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentOptionBinding.inflate(inflater, container, false)
        val view = binding.root

        sensorSeekBar = view.findViewById(R.id.sensor_seekbar)
        sensorLabel = view.findViewById(R.id.sensor_label_end)

        obInButton = view.findViewById(R.id.ob_in_button)
        obDeButton = view.findViewById(R.id.ob_de_button)
        objectiveText = view.findViewById(R.id.objective_text)
        nameText = view.findViewById(R.id.name_text)

        // Switchの状態変化を監視してOption.SENSORとTextViewを更新
        sensorSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Option.SENSOR = progress
                updateSensorLabel()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 必要に応じて実装
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 必要に応じて実装
            }
        })

        val obMinFilter = Utils.createMinFilter(5)
        objectiveText.filters = arrayOf(obMinFilter)
        objectiveText.setKeyListener(null)

        obInButton.setOnClickListener {
            Utils.updateNumber(objectiveText, 5)
            saveObjectiveValue()  // 数値が変わるたびに保存
        }
        obDeButton.setOnClickListener {
            Utils.updateNumber(objectiveText, -5)
            saveObjectiveValue()  // 数値が変わるたびに保存
        }

        Utils.setLongPressListener(obInButton, objectiveText, 5)
        Utils.setLongPressListener(obDeButton, objectiveText, -5)

        nameText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveNameValue()  // フォーカスが外れたときに保存
        }



        setDefaultValue()
        return view
    }

    override fun onResume() {
        setDefaultValue()
        super.onResume()
    }

    private fun setDefaultValue() {
        setInitialValue(nameText, Option.NAME)
        setInitialValue(objectiveText, Option.OBJECTIVE)

        sensorSeekBar.progress = Option.SENSOR
        updateSensorLabel()
    }

    private fun updateSensorLabel() {
        val label = when (Option.SENSOR) {
            0 -> "OFF"
            1 -> "弱"
            2 -> "強"
            else -> "未設定"
        }
        sensorLabel.text = label // sensorLabel は表示用の TextView
    }

    private fun saveObjectiveValue() {
        updateOptionFieldFromInt(objectiveText) { Option.OBJECTIVE = it }
        Option.saveOptions(requireContext())
    }

    private fun saveNameValue() {
        updateOptionFieldFromString(nameText) { Option.NAME = it }
        Option.saveOptions(requireContext())
    }

    private fun setInitialValue(editText: EditText, value: String) {
        editText.setText(value)
    }

    private fun setInitialValue(editText: EditText, value: Int) {
        editText.setText(value.toString())
    }

    private fun updateOptionFieldFromString(textView: EditText, updateAction: (String) -> Unit) {
        val text = textView.text.toString().trim()
        updateAction(text)
    }

    private fun updateOptionFieldFromInt(textView: EditText, updateAction: (Int) -> Unit) {
        textView.text.toString().trim().toIntOrNull()?.let { updateAction(it) }
    }



}