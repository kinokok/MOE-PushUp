package com.example.moe.ui.theme

import android.app.AlertDialog
import android.os.Bundle
import android.app.Dialog
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.example.moe.R
import com.example.moe.ui.memory.MemoryManager
import com.example.moe.ui.utils.Utils

class ResultDialog : DialogFragment() {

    interface OnDialogCloseListener {
        fun onDialogClose()
    }

    private var listener: OnDialogCloseListener? = null

    fun setOnDialogCloseListener(listener: OnDialogCloseListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val res = arguments?.getString("res") ?: "0"
        val time = arguments?.getString("time") ?: "0"
        val message = arguments?.getString("message")
        val phraseMessage = arguments?.getString("phraseMessage")

        val builder = AlertDialog.Builder(this.requireContext())
        val inflater = this.requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_result, null)
        val coin = (res.toInt() * 10).toString()

        val button = dialogView.findViewById<ImageButton>(R.id.positive_button)
        val msg = dialogView.findViewById<TextView>(R.id.dialog_msg)
        msg.text = "$phraseMessage\n$message\n${coin.trim()}コインを獲得しました！"

        button.setOnClickListener {
            val memoryManager = MemoryManager(requireContext())
            memoryManager.saveTapData(res.trim().toInt())

            listener?.onDialogClose()
            dismiss()
        }

        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)

        return dialog
    }

    companion object {
        fun newInstance(res: String, time: String, message: String, phraseMessage: String): ResultDialog {
            val fragment = ResultDialog()
            val args = Bundle()
            args.putString("res", res)
            args.putString("time", time)
            args.putString("message", message)
            args.putString("phraseMessage", phraseMessage)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onResume() {
        super.onResume()
        Utils.setDimAmount(dialog?.window, 0.7f)
    }
}
