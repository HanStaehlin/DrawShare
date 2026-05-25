package com.example.data

import android.content.Context

class SessionStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_USER_NAME, value).apply()
        }

    var lastInviteCode: String?
        get() = prefs.getString(KEY_INVITE_CODE, null)
        set(value) {
            prefs.edit().putString(KEY_INVITE_CODE, value).apply()
        }

    companion object {
        private const val PREFS = "drawshare_session"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_INVITE_CODE = "last_invite_code"
    }
}
