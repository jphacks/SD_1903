package com.example.durian

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View
import android.widget.RadioButton


fun ShowNoActionDialog(activity: Activity, title: String, layoutId: Int, cancelAble:Boolean = false, setListener: (View, AlertDialog) -> Unit) {
    val view: View = activity.layoutInflater.inflate(layoutId, null)
    val dialog = AlertDialog.Builder(activity).apply {
        setView(view)
        setTitle(title)
        setIcon(R.mipmap.durian_launcher)
        setCancelable(cancelAble)
    }
    val alert = dialog.show()
    setListener(view, alert)
}

fun ShowActionDialog(activity: Activity, title: String, layoutId: Int, cancelAble:Boolean = false, okListener: (View) -> Unit) {
    val view: View = activity.layoutInflater.inflate(layoutId, null)
    AlertDialog.Builder(activity).apply {
        setView(view)
        setTitle(title)
        setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
            okListener(view)
        })
        setIcon(R.mipmap.durian_launcher)
        setCancelable(cancelAble)
        show()
    }
}