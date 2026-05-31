package com.example.data

import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: Flow<List<ProxyProfile>> = profileDao.getAllProfiles()
    val selectedProfile: Flow<ProxyProfile?> = profileDao.getSelectedProfile()

    suspend fun getProfileById(id: Long): ProxyProfile? {
        return profileDao.getProfileById(id)
    }

    suspend fun insertProfile(profile: ProxyProfile): Long {
        return profileDao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: ProxyProfile) {
        profileDao.deleteProfile(profile)
    }

    suspend fun deleteProfileById(id: Long) {
        profileDao.deleteProfileById(id)
    }

    suspend fun selectProfile(id: Long) {
        profileDao.selectProfile(id)
    }

    suspend fun updateLatency(id: Long, latency: Int) {
        profileDao.updateLatency(id, latency)
    }
}
