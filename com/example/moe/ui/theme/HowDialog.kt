package com.example.moe.ui.theme

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.moe.R
import com.example.moe.ui.utils.Utils

class HowDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(this.requireContext())
        val inflater = this.requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_how, null)

        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
        val nextButton = dialogView.findViewById<ImageButton>(R.id.next_button)
        val dialogTitle = getString(R.string.title_how)

        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        title.text = dialogTitle

        closeButton.setOnClickListener {
            dismiss()
        }

        //nextButton設定
        nextButton.setOnClickListener {
            dismiss()
        }

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    override fun onResume() {
        super.onResume()
        Utils.setDimAmount(dialog?.window, 0.7f)
    }

}


