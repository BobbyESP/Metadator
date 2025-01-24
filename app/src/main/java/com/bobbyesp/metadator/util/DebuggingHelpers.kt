package com.bobbyesp.metadator.util

import com.bobbyesp.metadator.BuildConfig

//execute the code inside if it is a debug release
fun executeIfDebugging(function: () -> Unit) {
    if(BuildConfig.DEBUG) function()
}