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

    var joinedRooms: List<String>
        get() = prefs.getString(KEY_JOINED_ROOMS, null)
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) {
            prefs.edit().putString(KEY_JOINED_ROOMS, value.joinToString(SEP)).apply()
        }

    var activeRoom: String?
        get() = prefs.getString(KEY_ACTIVE_ROOM, null)
        set(value) {
            prefs.edit().putString(KEY_ACTIVE_ROOM, value).apply()
        }

    init {
        // Migrate the legacy single-room key into the joined-rooms set.
        val legacy = prefs.getString(KEY_LEGACY_INVITE_CODE, null)
        if (!legacy.isNullOrBlank()) {
            val current = joinedRooms
            if (legacy !in current) {
                joinedRooms = current + legacy
            }
            if (activeRoom == null) {
                activeRoom = legacy
            }
            prefs.edit().remove(KEY_LEGACY_INVITE_CODE).apply()
        }
    }

    companion object {
        private const val PREFS = "drawshare_session"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_JOINED_ROOMS = "joined_rooms"
        private const val KEY_ACTIVE_ROOM = "active_room"
        private const val KEY_LEGACY_INVITE_CODE = "last_invite_code"
        private const val SEP = ","
    }
}
