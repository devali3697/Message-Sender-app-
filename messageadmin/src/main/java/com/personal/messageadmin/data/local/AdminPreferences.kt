package com.personal.messageadmin.data.local

import android.content.Context
import android.content.SharedPreferences

class AdminPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var authEmail: String?
        get() = prefs.getString(KEY_AUTH_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_AUTH_EMAIL, value).apply()

    var authPassword: String?
        get() = prefs.getString(KEY_AUTH_PASSWORD, null)
        set(value) = prefs.edit().putString(KEY_AUTH_PASSWORD, value).apply()

    var fcmToken: String?
        get() = prefs.getString(KEY_FCM_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_FCM_TOKEN, value).apply()

    companion object {
        private const val PREF_NAME = "message_admin_prefs"
        private const val KEY_AUTH_EMAIL = "key_admin_auth_email"
        private const val KEY_AUTH_PASSWORD = "key_admin_auth_password"
        private const val KEY_FCM_TOKEN = "key_fcm_token"
    }
}
