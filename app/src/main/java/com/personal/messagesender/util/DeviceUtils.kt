package com.personal.messagesender.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

object DeviceUtils {

    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device"
        val model = Build.MODEL.replace(" ", "_").lowercase(Locale.getDefault())
        return "${model}_${androidId.takeLast(6)}"
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        val model = Build.MODEL
        return if (model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))) {
            model.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            "$manufacturer $model"
        }
    }
}
