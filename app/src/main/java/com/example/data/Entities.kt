package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val heightCm: Float,
    val weightKg: Float,
    val age: Int,
    val gender: String,
    val dailyStepGoal: Int = 10000,
    val dailyCalorieGoal: Int = 2500,
    val dailyWaterGoalMl: Int = 2500,
    val isActive: Boolean = false
)

@Entity(tableName = "step_logs")
data class StepLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val date: String, // Format: YYYY-MM-DD
    val steps: Int,
    val calories: Int,
    val distanceKm: Float,
    val activeMinutes: Int,
    val waterMl: Int = 0
)

@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val type: String, // Running, Cycling, Strength, Swimming, Walking, Yoga
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "heart_rate_logs")
data class HeartRateLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val heartRate: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sleep_logs")
data class SleepLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val date: String, // Format: YYYY-MM-DD
    val startTime: Long, // Epoch millis
    val endTime: Long, // Epoch millis
    val quality: Int, // 1 (Poor) to 5 (Excellent)
    val notes: String = ""
)

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
