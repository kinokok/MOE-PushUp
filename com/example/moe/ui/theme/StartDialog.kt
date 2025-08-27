package com.example.moe.ui.theme

import android.app.AlertDialog
import android.os.Bundle
import android.app.Dialog
import android.media.Image
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.moe.R
import com.example.moe.ui.option.Option
import com.example.moe.ui.play.PlayFragment
import com.example.moe.ui.utils.Utils

class StartDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(this.requireContext())
        val inflater = this.requireActivity().layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_start, null)

        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
        val okButton = dialogView.findViewById<ImageButton>(R.id.positive_button)
        val inCheckBox = dialogView.findViewById<CheckBox>(R.id.in_checkbox)

        // rep_counter
        val repEditText = dialogView.findViewById<EditText>(R.id.rep_edit_text)
        val repInButton = dialogView.findViewById<ImageButton>(R.id.rep_in_button)
        val repDeButton = dialogView.findViewById<ImageButton>(R.id.rep_de_button)
        repEditText.setKeyListener(null)

        // time_counter
        val timeEditText = dialogView.findViewById<EditText>(R.id.time_edit_text)
        val timeInButton = dialogView.findViewById<ImageButton>(R.id.time_in_button)
        val timeDeButton = dialogView.findViewById<ImageButton>(R.id.time_de_button)
        timeEditText.setKeyListener(null)

        // `Option.OBJECTIVE`の初期値を`repEditText`に設定
        repEditText.setText(Option.OBJECTIVE.toString())

        val minFilter = Utils.createMinFilter(5)
        repEditText.filters = arrayOf(minFilter)

        repInButton.setOnClickListener {
            Utils.updateNumber(repEditText, 5)
        }

        repDeButton.setOnClickListener {
            Utils.updateNumber(repEditText, -5)
        }

        Utils.setLongPressListener(repInButton, repEditText, 5)
        Utils.setLongPressListener(repDeButton, repEditText, -5)

        val timeMinFilter = Utils.createMinFilter(30)
        timeEditText.filters = arrayOf(timeMinFilter)

        timeInButton.setOnClickListener {
            Utils.updateNumber(timeEditText, 30)
        }

        timeDeButton.setOnClickListener {
            Utils.updateNumber(timeEditText, -30)
        }

        Utils.setLongPressListener(timeInButton, timeEditText, 30)
        Utils.setLongPressListener(timeDeButton, timeEditText, -30)

        closeButton.setOnClickListener {
            dismiss()
        }

        // okButton設定
        okButton.setOnClickListener {
            val timeLimit: Int
            val repLimit: Int
            if (inCheckBox.isChecked) {
                // チェックされている場合
                timeLimit = 0
                repLimit = 0
            } else {
                // チェックされていない場合
                timeLimit = timeEditText.text.toString().toIntOrNull() ?: 0
                repLimit = repEditText.text.toString().toIntOrNull() ?: 0
            }
            navigateToNextFragment(timeLimit, repLimit)
            dismiss()
        }

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false) // ここでオーバーレイ領域をタップしても閉じないようにする

        return dialog
    }

    private fun navigateToNextFragment(timeLimit: Int, repLimit: Int) {
        val playFragment = PlayFragment.newInstance(timeLimit, repLimit)
        parentFragmentManager.beginTransaction()
            .replace(R.id.host_fragment_activity_main, playFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        Utils.setDimAmount(dialog?.window, 0.7f)
    }
}

