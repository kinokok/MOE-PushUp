package com.example.moe.ui.theme

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.moe.R
import com.example.moe.ui.option.Option
import com.example.moe.ui.utils.Utils

class StoreDialog(private val onPositiveClick: () -> Unit, private val itemName: String) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_store, null)

        val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
        val positiveButton = dialogView.findViewById<ImageButton>(R.id.positive_button)
        val noPositiveButton = dialogView.findViewById<ImageButton>(R.id.no_positive_button)
        val negativeButton = dialogView.findViewById<ImageButton>(R.id.negative_button)
        val dialogText = dialogView.findViewById<TextView>(R.id.store_text)
        val storeTitle = dialogView.findViewById<TextView>(R.id.store_title)

        // Set texts for title and content
        storeTitle.text = "${itemName.trim()}"
        dialogText.text = "${itemName.trim()}を5000Gで購入しますか?"

        if (Option.COIN < 5000) {
            noPositiveButton.visibility = View.VISIBLE
        } else {
            noPositiveButton.visibility = View.GONE
        }

        closeButton.setOnClickListener {
            dismiss()
        }

        positiveButton.setOnClickListener {
            if (Option.COIN >= 5000) {
                Option.COIN -= 5000
                onPositiveClick()
                dismiss()
            }
        }

        negativeButton.setOnClickListener {
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
