package com.example.durian

import android.content.Context
import android.widget.Button

class ButtonExtend(context: Context): Button(context) {
    var isPushing: Boolean = false
        set(value) {
            field = value
        }
        get() {
            return field
        }
}