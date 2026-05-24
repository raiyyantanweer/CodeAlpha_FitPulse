package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.GeminiClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FitViewModel(
    application: Application,
    private val repository: FitRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val CHANNEL_ID = "fit_channel_alerts"

    // Authentication / Active profile states
    private val _activeUser = MutableStateFlow<User?>(null)
    val activeUser: StateFlow<User?> = _activeUser.asStateFlow()

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    val allUsers: StateFlow<List<User>> = _allUsers.asStateFlow()

    // Logs related to active user
    private val _currentDaySteps = MutableStateFlow<StepLog?>(null)
    val currentDaySteps: StateFlow<StepLog?> = _currentDaySteps.asStateFlow()

    private val _recentSteps = MutableStateFlow<List<StepLog>>(emptyList())
    val recentSteps: StateFlow<List<StepLog>> = _recentSteps.asStateFlow()

    private val _workouts = MutableStateFlow<List<WorkoutLog>>(emptyList())
    val workouts: StateFlow<List<WorkoutLog>> = _workouts.asStateFlow()

    private val _recentHeartRates = MutableStateFlow<List<HeartRateLog>>(emptyList())
    val recentHeartRates: StateFlow<List<HeartRateLog>> = _recentHeartRates.asStateFlow()

    private val _recentSleepLogs = MutableStateFlow<List<SleepLog>>(emptyList())
    val recentSleepLogs: StateFlow<List<SleepLog>> = _recentSleepLogs.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationLog>>(emptyList())
    val notifications: StateFlow<List<NotificationLog>> = _notifications.asStateFlow()

    // AI Coach Advisory state
    private val _coachRecommendation = MutableStateFlow("")
    val coachRecommendation: StateFlow<String> = _coachRecommendation.asStateFlow()

    private val _isCoachLoading = MutableStateFlow(false)
    val isCoachLoading: StateFlow<Boolean> = _isCoachLoading.asStateFlow()

    // Wearable device state simulation
    private val _isWearableConnected = MutableStateFlow(false)
    val isWearableConnected: StateFlow<Boolean> = _isWearableConnected.asStateFlow()

    private val _wearableBattery = MutableStateFlow(100)
    val wearableBattery: StateFlow<Int> = _wearableBattery.asStateFlow()

    init {
        createNotificationChannel()
        observeActiveUser()
        observeAllUsers()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FitPulse Alerts"
            val descriptionText = "Notifications regarding fitness milestones and exercise reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun observeActiveUser() {
        viewModelScope.launch {
            repository.getActiveUserFlow().collect { user ->
                _activeUser.value = user
                if (user != null) {
                    // Start sub-observations for the newly logged user
                    launchUserObservers(user.id)
                } else {
                    // Reset fields if no user is active
                    _currentDaySteps.value = null
                    _recentSteps.value = emptyList()
                    _workouts.value = emptyList()
                    _recentHeartRates.value = emptyList()
                    _recentSleepLogs.value = emptyList()
                    _notifications.value = emptyList()
                    _coachRecommendation.value = ""
                }
            }
        }
    }

    private fun observeAllUsers() {
        viewModelScope.launch {
            repository.getAllUsersFlow().collect { list ->
                _allUsers.value = list
                // If there are no users, insert a default demonstration user profile!
                if (list.isEmpty()) {
                    createDefaultDemoUser()
                }
            }
        }
    }

    private suspend fun createDefaultDemoUser() {
        val defaultUser = User(
            name = "Alex Carter",
            email = "alex.carter@gmail.com",
            heightCm = 178f,
            weightKg = 73.5f,
            age = 28,
            gender = "Male",
            dailyStepGoal = 10000,
            dailyCalorieGoal = 2400,
            dailyWaterGoalMl = 2500,
            isActive = true
        )
        val id = repository.insertUser(defaultUser)
        
        // Add sample analytics history so charts look marvelous on first run!
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        
        for (i in 6 downTo 1) {
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val pastDate = sdf.format(cal.time)
            
            // Log steps
            val stepCount = (6000..12000).random()
            repository.insertStepLog(
                StepLog(
                    userId = id.toInt(),
                    date = pastDate,
                    steps = stepCount,
                    calories = (stepCount * 0.04).toInt(),
                    distanceKm = (stepCount * 0.00075f),
                    activeMinutes = (stepCount / 100) + 15,
                    waterMl = (1500..3000).random()
                )
            )

            // Log sleep
            val startOffset = cal.timeInMillis - (8 * 3600 * 1000) // Sleep 8 hours ago
            repository.insertSleepLog(
                SleepLog(
                    userId = id.toInt(),
                    date = pastDate,
                    startTime = startOffset,
                    endTime = cal.timeInMillis,
                    quality = (3..5).random(),
                    notes = "Restful sleep"
                )
            )

            // Log occasional workouts
            if (i % 2 == 0) {
                repository.insertWorkoutLog(
                    WorkoutLog(
                        userId = id.toInt(),
                        type = if (i == 4) "Cardio" else "Strength",
                        durationMinutes = if (i == 4) 45 else 60,
                        caloriesBurned = if (i == 4) 420 else 320,
                        notes = if (i == 4) "Outdoor run on GPS trail" else "Focus on core & compound lifts",
                        timestamp = startOffset + 12 * 3600 * 1000 // Daytime workout
                    )
                )
            }

            // Log heart rates
            repository.insertHeartRateLog(
                HeartRateLog(
                    userId = id.toInt(),
                    heartRate = (60..130).random(),
                    timestamp = cal.timeInMillis
                )
            )
        }

        // Add initial system notification welcome alert
        repository.insertNotification(
            NotificationLog(
                userId = id.toInt(),
                title = "Welcome to FitPulse!",
                body = "Your local AI companion is ready. Let's hit our goals together!"
            )
        )
    }

    private var observersJob: List<kotlinx.coroutines.Job> = emptyList()

    private fun launchUserObservers(userId: Int) {
        observersJob.forEach { it.cancel() }
        
        val today = getCurrentDateStr()
        
        observersJob = listOf(
            viewModelScope.launch {
                // Keep today's steps up-to-date and generate if empty
                repository.getStepLogFlow(userId, today).collect { stepLog ->
                    if (stepLog == null) {
                        initializeTodayStepLog(userId, today)
                    } else {
                        _currentDaySteps.value = stepLog
                    }
                }
            },
            viewModelScope.launch {
                repository.getRecentStepLogsFlow(userId, 7).collect { logs ->
                    _recentSteps.value = logs
                }
            },
            viewModelScope.launch {
                repository.getWorkoutLogsFlow(userId).collect { logs ->
                    _workouts.value = logs
                }
            },
            viewModelScope.launch {
                repository.getRecentHeartRateLogsFlow(userId, 15).collect { logs ->
                    _recentHeartRates.value = logs
                }
            },
            viewModelScope.launch {
                repository.getRecentSleepLogsFlow(userId, 7).collect { logs ->
                    _recentSleepLogs.value = logs
                }
            },
            viewModelScope.launch {
                repository.getNotificationsFlow(userId).collect { logs ->
                    _notifications.value = logs
                }
            }
        )
    }

    private suspend fun initializeTodayStepLog(userId: Int, date: String) {
        val initialLog = StepLog(
            userId = userId,
            date = date,
            steps = 0,
            calories = 0,
            distanceKm = 0f,
            activeMinutes = 0,
            waterMl = 0
        )
        repository.insertStepLog(initialLog)
    }

    // Helper to get current date formatted
    fun getCurrentDateStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Interactive Actions
    fun addSteps(stepsToAdd: Int) {
        val user = _activeUser.value ?: return
        val currentLog = _currentDaySteps.value ?: return
        viewModelScope.launch {
            val updatedSteps = currentLog.steps + stepsToAdd
            val updatedCalories = currentLog.calories + (stepsToAdd * 0.04).toInt()
            val updatedDistance = currentLog.distanceKm + (stepsToAdd * 0.00075f)
            val updatedActiveMins = currentLog.activeMinutes + (stepsToAdd / 110)

            val newLog = currentLog.copy(
                steps = updatedSteps,
                calories = updatedCalories,
                distanceKm = updatedDistance,
                activeMinutes = updatedActiveMins
            )
            repository.insertStepLog(newLog)

            // Let's log real-time simulated heart rate spike during activity
            if (stepsToAdd > 100) {
                repository.insertHeartRateLog(
                    HeartRateLog(
                        userId = user.id,
                        heartRate = (120..155).random()
                    )
                )
            }

            // Milestone Alerts Check
            if (currentLog.steps < user.dailyStepGoal && updatedSteps >= user.dailyStepGoal) {
                triggerSystemNotification(
                    "Goal Achieved! 🏆",
                    "Spectacular! You hit your daily goal of ${user.dailyStepGoal} steps today."
                )
            }
        }
    }

    fun addWater(ml: Int) {
        val currentLog = _currentDaySteps.value ?: return
        viewModelScope.launch {
            val newWater = currentLog.waterMl + ml
            repository.insertStepLog(currentLog.copy(waterMl = newWater))
        }
    }

    fun addWorkout(type: String, durationMins: Int, caloriesBurned: Int, notes: String) {
        val user = _activeUser.value ?: return
        viewModelScope.launch {
            val workout = WorkoutLog(
                userId = user.id,
                type = type,
                durationMinutes = durationMins,
                caloriesBurned = caloriesBurned,
                notes = notes
            )
            repository.insertWorkoutLog(workout)

            // Auto log simulated elevated cardiovascular heartbeat
            repository.insertHeartRateLog(
                HeartRateLog(
                    userId = user.id,
                    heartRate = (130..170).random()
                )
            )

            // Send push and in-app notice
            triggerSystemNotification(
                "Workout Logged! 🔥",
                "Superb $type session! Logged $durationMins minutes and burned $caloriesBurned kcal."
            )
        }
    }

    fun deleteWorkout(workoutId: Int) {
        viewModelScope.launch {
            repository.deleteWorkoutLog(workoutId)
        }
    }

    fun addPulseReading(bpm: Int) {
        val user = _activeUser.value ?: return
        viewModelScope.launch {
            repository.insertHeartRateLog(
                HeartRateLog(
                    userId = user.id,
                    heartRate = bpm
                )
            )
        }
    }

    fun addSleepLog(hours: Float, quality: Int, notes: String) {
        val user = _activeUser.value ?: return
        val sdf = getCurrentDateStr()
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val start = now - (hours * 3600 * 1000).toLong()
            val log = SleepLog(
                userId = user.id,
                date = sdf,
                startTime = start,
                endTime = now,
                quality = quality,
                notes = notes
            )
            repository.insertSleepLog(log)
            
            triggerSystemNotification(
                "Night Logged 🌙",
                "Logged sleeping duration of $hours hours. Rated quality: $quality/5."
            )
        }
    }

    fun registerNewUser(
        name: String,
        email: String,
        height: Float,
        weight: Float,
        age: Int,
        gender: String,
        stepGoal: Int,
        calorieGoal: Int,
        waterGoalMl: Int
    ) {
        viewModelScope.launch {
            repository.clearActiveUsers()
            val newUser = User(
                name = name,
                email = email,
                heightCm = height,
                weightKg = weight,
                age = age,
                gender = gender,
                dailyStepGoal = stepGoal,
                dailyCalorieGoal = calorieGoal,
                dailyWaterGoalMl = waterGoalMl,
                isActive = true
            )
            repository.insertUser(newUser)
        }
    }

    fun switchActiveProfile(userId: Int) {
        viewModelScope.launch {
            repository.switchUser(userId)
            triggerSystemNotification(
                "Switched Profile 🔄",
                "Active wellness records loaded dynamically."
            )
        }
    }

    fun clearNotifications() {
        val user = _activeUser.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsAsRead(user.id)
        }
    }

    // Wearable device Simulator Sync
    fun toggleWearableSync() {
        val user = _activeUser.value ?: return
        if (_isWearableConnected.value) {
            _isWearableConnected.value = false
            return
        }

        viewModelScope.launch {
            _isWearableConnected.value = true
            _wearableBattery.value = (35..98).random()
            
            // Sync simulated steps, heartbeat, and sleep
            val simulatedSteps = (1500..3200).random()
            addSteps(simulatedSteps)

            val currentDayBpm = (70..88).random()
            addPulseReading(currentDayBpm)

            triggerSystemNotification(
                "PulseWear Paired! ⏱️",
                "Paired with smartwatch tracker. Synced $simulatedSteps steps and real-time heart rate of $currentDayBpm bpm."
            )
        }
    }

    // GPS Workout route simulation
    fun simulateGpsLogging(activityType: String, durationMins: Int) {
        val user = _activeUser.value ?: return
        viewModelScope.launch {
            val distance = (3f + (durationMins * 0.15f))
            val cal = (durationMins * 8.5f).toInt()
            
            val workout = WorkoutLog(
                userId = user.id,
                type = "GPS $activityType",
                durationMinutes = durationMins,
                caloriesBurned = cal,
                notes = "Auto-tracked GPS Trail: ${"%.2f".format(distance)} km, avg pace ${"%.2f".format(durationMins / distance)} min/km.",
                timestamp = System.currentTimeMillis()
            )
            repository.insertWorkoutLog(workout)

            // Elevate heart rate with activity peak
            repository.insertHeartRateLog(
                HeartRateLog(
                    userId = user.id,
                    heartRate = (145..168).random()
                )
            )

            triggerSystemNotification(
                "GPS Workout Synced 📍",
                "GPS recorded distance of ${"%.2f".format(distance)} km during your $activityType session!"
            )
        }
    }

    // Ask Gemini AI advisor coach
    fun explainHealthProfileWithAIGoach() {
        val user = _activeUser.value ?: return
        val currentSteps = _currentDaySteps.value
        val historySteps = _recentSteps.value
        val historyWorkouts = _workouts.value
        val historySleep = _recentSleepLogs.value

        viewModelScope.launch {
            _isCoachLoading.value = true
            _coachRecommendation.value = ""

            val profileInfo = "Age: ${user.age}, Gender: ${user.gender}, Height: ${user.heightCm}cm, Weight: ${user.weightKg}kg. Daily Target steps: ${user.dailyStepGoal}, calorie burned goal: ${user.dailyCalorieGoal}, water daily target: ${user.dailyWaterGoalMl}ml."
            
            val statsSummary = currentSteps?.let {
                "Today so far: Steps: ${it.steps}, Calories burned: ${it.calories}kcal, Water drank: ${it.waterMl}ml, Active minutes: ${it.activeMinutes}min."
            } ?: "Today has no logged data yet."

            val workoutsSummary = if (historyWorkouts.isEmpty()) {
                "No workouts logged yet."
            } else {
                historyWorkouts.take(4).joinToString(", ") {
                    "${it.type} (${it.durationMinutes} mins, ${it.caloriesBurned} kcal) noting: '${it.notes}'"
                }
            }

            val sleepSummary = if (historySleep.isEmpty()) {
                "No sleep history logged yet."
            } else {
                historySleep.joinToString(", ") {
                    "Rating: ${it.quality}/5 (Notes: ${it.notes})"
                }
            }

            val response = GeminiClient.getRecommendations(
                userName = user.name,
                profileInfo = profileInfo,
                statsSummary = statsSummary,
                recentWorkouts = workoutsSummary,
                recentSleep = sleepSummary
            )

            _coachRecommendation.value = response
            _isCoachLoading.value = false
        }
    }

    // Notification Helper Triggers
    fun triggerSystemNotification(title: String, body: String) {
        val user = _activeUser.value ?: return
        viewModelScope.launch {
            // Save inside database
            repository.insertNotification(
                NotificationLog(
                    userId = user.id,
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis()
                )
            )

            // Trigger platform notification as well!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                
                if (!hasPermission) {
                    Log.w("FitViewModel", "POST_NOTIFICATIONS permission not granted. In-app only.")
                    return@launch
                }
            }

            try {
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

                withContext(Dispatchers.Main) {
                    val notificationManager = NotificationManagerCompat.from(context)
                    // Check required permission for Android 13+ inside platform client
                    notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
                }
            } catch (e: SecurityException) {
                Log.e("FitViewModel", "SecurityException posting notification", e)
            }
        }
    }
}

class FitViewModelFactory(
    private val application: Application,
    private val repository: FitRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
