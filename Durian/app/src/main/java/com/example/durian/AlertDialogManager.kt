package com.example.durian

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.RadioButton



fun ShowNoActionDialog(activity: Activity, layoutId: Int, okListener: (View) -> Unit) {
    val view: View = activity.layoutInflater.inflate(layoutId, null)
    AlertDialog.Builder(activity).apply {
        setView(view)
        setTitle("使い方")
        setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
            okListener(view)
        })
        setIcon(R.mipmap.durian_launcher)
        setCancelable(true)
        show()
    }
}