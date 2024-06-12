package com.adventure.tapper.utils

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

fun isAccessibilityServiceEnabled(
    context: Context,
    service: Class<out AccessibilityService>
): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    if (enabledServices.isNullOrEmpty()) {
        return false
    }
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        if (componentName.equals(
                ComponentName(context, service).flattenToString(),
                ignoreCase = true
            )
        ) {
            return true
        }
    }
    return false
}
