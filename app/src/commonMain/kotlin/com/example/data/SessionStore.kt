package com.example.data

expect class SessionStore {
    var userName: String?
    var joinedRooms: List<String>
    var activeRoom: String?
}
