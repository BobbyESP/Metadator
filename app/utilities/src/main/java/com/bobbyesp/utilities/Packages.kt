package com.bobbyesp.utilities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object Packages {
    fun isPackageInstalled(context: Context, packageName: String) = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun Intent.launchOrAction(context: Context, action: () -> Unit) {
        if (resolveActivity(context.packageManager) != null) {
            context.startActivity(this)
        } else {
            action()
        }
    }
}