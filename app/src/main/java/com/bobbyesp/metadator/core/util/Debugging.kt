package com.bobbyesp.metadator.core.util

import com.bobbyesp.metadator.BuildConfig

// execute the code inside if it is a debug release
fun executeIfDebugging(debugOnlyOperation: () -> Unit) {
  if (BuildConfig.DEBUG) debugOnlyOperation()
}
