package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FitDao {
    // User Queries
    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveUser(): User?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET isActive = 0")
    suspend fun clearActiveUsers()

    @Transaction
    suspend fun switchUser(userId: Int) {
        clearActiveUsers()
        setActiveUser(userId)
    }

    @Query("UPDATE users SET isActive = 1 WHERE id = :userId")
    suspend fun setActiveUser(userId: Int)

    // StepLog Queries
    @Query("SELECT * FROM step_logs WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getStepLog(userId: Int, date: String): StepLog?

    @Query("SELECT * FROM step_logs WHERE userId = :userId AND date = :date LIMIT 1")
    fun getStepLogFlow(userId: Int, date: String): Flow<StepLog?>

    @Query("SELECT * FROM step_logs WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentStepLogsFlow(userId: Int, limit: Int = 7): Flow<List<StepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepLog(stepLog: StepLog)

    // WorkoutLog Queries
    @Query("SELECT * FROM workout_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getWorkoutLogsFlow(userId: Int): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(workoutLog: WorkoutLog)

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteWorkoutLog(id: Int)

    // HeartRateLog Queries
    @Query("SELECT * FROM heart_rate_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentHeartRateLogsFlow(userId: Int, limit: Int = 100): Flow<List<HeartRateLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeartRateLog(heartRateLog: HeartRateLog)

    // SleepLog Queries
    @Query("SELECT * FROM sleep_logs WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentSleepLogsFlow(userId: Int, limit: Int = 7): Flow<List<SleepLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepLog(sleepLog: SleepLog)

    // Notification Queries
    @Query("SELECT * FROM notification_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsFlow(userId: Int): Flow<List<NotificationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notificationLog: NotificationLog)

    @Query("UPDATE notification_logs SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: Int)
}
