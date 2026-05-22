package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Query("SELECT * FROM drawing_messages WHERE inviteCode = :inviteCode ORDER BY timestamp DESC")
    fun getMessagesForRoom(inviteCode: String): Flow<List<DrawingMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: DrawingMessage)

    @Query("UPDATE drawing_messages SET isConfirmedDelivered = 1 WHERE id = :id")
    suspend fun confirmDelivery(id: String)

    @Query("DELETE FROM drawing_messages WHERE inviteCode = :inviteCode")
    suspend fun clearMessagesForRoom(inviteCode: String)

    @Query("SELECT * FROM drawing_messages WHERE isReceived = 1 ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReceivedMessage(): DrawingMessage?
}
