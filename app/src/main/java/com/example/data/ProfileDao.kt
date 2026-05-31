package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM proxy_profiles ORDER BY addedAt DESC")
    fun getAllProfiles(): Flow<List<ProxyProfile>>

    @Query("SELECT * FROM proxy_profiles WHERE isSelected = 1 LIMIT 1")
    fun getSelectedProfile(): Flow<ProxyProfile?>

    @Query("SELECT * FROM proxy_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): ProxyProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProxyProfile): Long

    @Delete
    suspend fun deleteProfile(profile: ProxyProfile)

    @Query("DELETE FROM proxy_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    @Query("UPDATE proxy_profiles SET isSelected = 0 WHERE isSelected = 1")
    suspend fun clearSelected()

    @Query("UPDATE proxy_profiles SET isSelected = 1 WHERE id = :id")
    suspend fun markSelected(id: Long)

    @Transaction
    suspend fun selectProfile(id: Long) {
        clearSelected()
        markSelected(id)
    }

    @Query("UPDATE proxy_profiles SET latency = :latency WHERE id = :id")
    suspend fun updateLatency(id: Long, latency: Int)
}
