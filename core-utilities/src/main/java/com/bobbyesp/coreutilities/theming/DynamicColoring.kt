package com.bobbyesp.coreutilities.theming

import android.os.Build

fun isDynamicColoringSupported(): Boolean {
    return Build.VERSION.SDK_INT >= 31 //Build.VERSION_CODES.S
}