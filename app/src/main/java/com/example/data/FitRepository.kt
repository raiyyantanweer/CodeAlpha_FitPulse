package com.example.data

import kotlinx.coroutines.flow.Flow

class FitRepository(private val fitDao: FitDao) {
    fun getActiveUserFlow(): Flow<User?> = fitDao.getActiveUserFlow()
    
    suspend fun getActiveUser(): User? = fitDao.getActiveUser()
    
    fun getAllUsersFlow(): Flow<List<User>> = fitDao.getAllUsersFlow()
    
    suspend fun insertUser(user: User): Long = fitDao.insertUser(user)
    
    suspend fun updateUser(user: User) = fitDao.updateUser(user)
    
    suspend fun switchUser(userId: Int) = fitDao.switchUser(userId)

    suspend fun clearActiveUsers() = fitDao.clearActiveUsers()

    suspend fun getStepLog(userId: Int, date: String): StepLog? = fitDao.getStepLog(userId, date)
    
    fun getStepLogFlow(userId: Int, date: String): Flow<StepLog?> = fitDao.getStepLogFlow(userId, date)
    
    fun getRecentStepLogsFlow(userId: Int, limit: Int = 7): Flow<List<StepLog>> = fitDao.getRecentStepLogsFlow(userId, limit)
    
    suspend fun insertStepLog(stepLog: StepLog) = fitDao.insertStepLog(stepLog)

    fun getWorkoutLogsFlow(userId: Int): Flow<List<WorkoutLog>> = fitDao.getWorkoutLogsFlow(userId)
    
    suspend fun insertWorkoutLog(workoutLog: WorkoutLog) = fitDao.insertWorkoutLog(workoutLog)
    
    suspend fun deleteWorkoutLog(id: Int) = fitDao.deleteWorkoutLog(id)

    fun getRecentHeartRateLogsFlow(userId: Int, limit: Int = 100): Flow<List<HeartRateLog>> = fitDao.getRecentHeartRateLogsFlow(userId, limit)
    
    suspend fun insertHeartRateLog(heartRateLog: HeartRateLog) = fitDao.insertHeartRateLog(heartRateLog)

    fun getRecentSleepLogsFlow(userId: Int, limit: Int = 7): Flow<List<SleepLog>> = fitDao.getRecentSleepLogsFlow(userId, limit)
    
    suspend fun insertSleepLog(sleepLog: SleepLog) = fitDao.insertSleepLog(sleepLog)

    fun getNotificationsFlow(userId: Int): Flow<List<NotificationLog>> = fitDao.getNotificationsFlow(userId)
    
    suspend fun insertNotification(notificationLog: NotificationLog) = fitDao.insertNotification(notificationLog)
    
    suspend fun markAllNotificationsAsRead(userId: Int) = fitDao.markAllNotificationsAsRead(userId)
}
