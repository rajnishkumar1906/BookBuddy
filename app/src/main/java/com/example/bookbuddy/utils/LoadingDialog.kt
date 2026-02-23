package com.example.bookbuddy.utils

import android.app.ProgressDialog
import android.content.Context
import com.example.bookbuddy.R

class LoadingDialog(private val context: Context) {
    private var progressDialog: ProgressDialog? = null

    fun show(message: String = "Loading...") {
        progressDialog = ProgressDialog(context).apply {
            setMessage(message)
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            show()
        }
    }

    fun dismiss() {
        progressDialog?.dismiss()
        progressDialog = null
    }
}