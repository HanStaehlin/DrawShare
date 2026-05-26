package com.example.data

import platform.Foundation.NSUserDefaults

actual class SessionStore {
    private val prefs = NSUserDefaults.standardUserDefaults

    actual var userName: String?
        get() = prefs.stringForKey(KEY_USER_NAME)
        set(value) {
            if (value != null) prefs.setObject(value, KEY_USER_NAME)
            else prefs.removeObjectForKey(KEY_USER_NAME)
        }

    actual var joinedRooms: List<String>
        get() = prefs.stringForKey(KEY_JOINED_ROOMS)
            ?.split(SEP)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) {
            prefs.setObject(value.joinToString(SEP), KEY_JOINED_ROOMS)
        }

    actual var activeRoom: String?
        get() = prefs.stringForKey(KEY_ACTIVE_ROOM)
        set(value) {
            if (value != null) prefs.setObject(value, KEY_ACTIVE_ROOM)
            else prefs.removeObjectForKey(KEY_ACTIVE_ROOM)
        }

    companion object {
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_JOINED_ROOMS = "joined_rooms"
        private const val KEY_ACTIVE_ROOM = "active_room"
        private const val SEP = ","
    }
}
