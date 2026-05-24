package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class FitTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Home),
    WORKOUTS("Workouts", Icons.Default.PlayArrow),
    HEALTH("Health Logs", Icons.Default.Favorite),
    WEARABLE("Wearable", Icons.Default.Build),
    PROFILE("Profile & AI", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitAppContent(viewModel: FitViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle runtime notification results gracefully
    }

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                // Minor safe delay to ensure UI has drawn and window focus has stabilized
                kotlinx.coroutines.delay(1000)
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val activeUser by viewModel.activeUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentDaySteps by viewModel.currentDaySteps.collectAsState()
    val recentSteps by viewModel.recentSteps.collectAsState()
    val workouts by viewModel.workouts.collectAsState()
    val recentHeartRates by viewModel.recentHeartRates.collectAsState()
    val recentSleepLogs by viewModel.recentSleepLogs.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val coachRecommendation by viewModel.coachRecommendation.collectAsState()
    val isCoachLoading by viewModel.isCoachLoading.collectAsState()
    val isWearableConnected by viewModel.isWearableConnected.collectAsState()
    val wearableBattery by viewModel.wearableBattery.collectAsState()

    var activeTab by remember { mutableStateOf(FitTab.DASHBOARD) }
    var showProfileSelectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(ActiveVolt)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FitPulse",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showProfileSelectionDialog = true },
                        modifier = Modifier.testTag("profile_selector_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Switch athlete profile",
                            tint = ActiveVolt
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CharcoalBg
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = OnyxSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                FitTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (activeTab == tab) Color.Black else MutedSlate
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == tab) ActiveVolt else MutedSlate
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = ActiveVolt
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase(Locale.ROOT)}")
                    )
                }
            }
        },
        containerColor = CharcoalBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen switching with transitions
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                },
                label = "tab_switch"
            ) { tab ->
                val configuration = LocalConfiguration.current
                val isWideScreen = configuration.screenWidthDp > 600
                val modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isWideScreen) Modifier
                            .widthIn(max = 620.dp)
                            .align(Alignment.TopCenter)
                        else Modifier
                    )

                when (tab) {
                    FitTab.DASHBOARD -> DashboardScreen(
                        modifier = modifier,
                        user = activeUser,
                        todaySteps = currentDaySteps,
                        recentSteps = recentSteps,
                        viewModel = viewModel
                    )
                    FitTab.WORKOUTS -> WorkoutsScreen(
                        modifier = modifier,
                        workouts = workouts,
                        viewModel = viewModel
                    )
                    FitTab.HEALTH -> HealthScreen(
                        modifier = modifier,
                        heartRates = recentHeartRates,
                        sleepLogs = recentSleepLogs,
                        viewModel = viewModel
                    )
                    FitTab.WEARABLE -> WearableScreen(
                        modifier = modifier,
                        isConnected = isWearableConnected,
                        battery = wearableBattery,
                        notifications = notifications,
                        viewModel = viewModel
                    )
                    FitTab.PROFILE -> ProfileScreen(
                        modifier = modifier,
                        user = activeUser,
                        allUsers = allUsers,
                        isCoachLoading = isCoachLoading,
                        coachRecommendation = coachRecommendation,
                        viewModel = viewModel
                    )
                }
            }

            // Profile Switcher Dialog
            if (showProfileSelectionDialog) {
                ProfileSelectionDialog(
                    allUsers = allUsers,
                    activeUser = activeUser,
                    onDismiss = { showProfileSelectionDialog = false },
                    onSelect = { id ->
                        viewModel.switchActiveProfile(id)
                        showProfileSelectionDialog = false
                    }
                )
            }
        }
    }
}

// ==================== DASHBOARD TAB ====================
@Composable
fun DashboardScreen(
    modifier: Modifier,
    user: User?,
    todaySteps: StepLog?,
    recentSteps: List<StepLog>,
    viewModel: FitViewModel
) {
    if (user == null || todaySteps == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ActiveVolt)
        }
        return
    }

    val stepProgress = if (user.dailyStepGoal > 0) todaySteps.steps.toFloat() / user.dailyStepGoal else 0f
    val calProgress = if (user.dailyCalorieGoal > 0) todaySteps.calories.toFloat() / user.dailyCalorieGoal else 0f
    val waterProgress = if (user.dailyWaterGoalMl > 0) todaySteps.waterMl.toFloat() / user.dailyWaterGoalMl else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Welcome Card with Streak/Goal Info
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "HELLO, CHAMP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActiveVolt,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = user.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Ready to conquer your ${user.dailyStepGoal} steps goal?",
                            fontSize = 13.sp,
                            color = MutedSlate
                        )
                    }
                }
            }
        }

        item {
            // Main Concentric Steps Tracker Ring
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = Color(0xFF1E212D),
                                startAngle = -220f,
                                sweepAngle = 260f,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        // Progress arc
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(ActiveVolt, ActiveBlue, ActiveVolt)
                                ),
                                startAngle = -220f,
                                sweepAngle = (stepProgress.coerceIn(0f, 1f) * 260f),
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // Inside stats
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Steps icon",
                                tint = ActiveVolt,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = todaySteps.steps.toString(),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "/ ${user.dailyStepGoal}",
                                fontSize = 12.sp,
                                color = MutedSlate,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom fast-step simulated buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.addSteps(1000) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("quick_steps_1000")
                        ) {
                            Text("+1k Steps", color = ActiveVolt, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { viewModel.addSteps(3000) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("quick_steps_3000")
                        ) {
                            Text("+3k Steps", color = ActiveBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            // Secondary cards: Calorie, active mins, distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CalorieCrimson)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CALORIES", fontSize = 11.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${todaySteps.calories} kcal",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { calProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = CalorieCrimson,
                            trackColor = Color(0xFF22242D)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Goal: ${user.dailyCalorieGoal}",
                            fontSize = 11.sp,
                            color = MutedSlate
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ActiveBlue)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DISTANCE", fontSize = 11.sp, color = MutedSlate, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${"%.2f".format(todaySteps.distanceKm)} km",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${todaySteps.activeMinutes} Active Minutes",
                            fontSize = 12.sp,
                            color = ActiveBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Walk/running sessions today",
                            fontSize = 10.sp,
                            color = MutedSlate
                        )
                    }
                }
            }
        }

        item {
            // Water hydration segment
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HYDRATION TARGET",
                                fontSize = 11.sp,
                                color = MutedSlate,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${todaySteps.waterMl} / ${user.dailyWaterGoalMl} ml",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { viewModel.addWater(250) },
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF222634))
                                .testTag("add_water_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Water Glass", tint = ActiveBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Simple interactive glasses layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val glassCount = 10
                        val waterPerGlass = user.dailyWaterGoalMl / glassCount
                        val glassesFilled = if (waterPerGlass > 0) todaySteps.waterMl / waterPerGlass else 0
                        
                        for (i in 1..glassCount) {
                            val isFilled = i <= glassesFilled
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(35.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isFilled) ActiveBlue else MutedSlate,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        if (isFilled) ActiveBlue.copy(alpha = 0.35f) else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Each cup adds 250ml of hydration log.",
                        fontSize = 11.sp,
                        color = MutedSlate
                    )
                }
            }
        }

        item {
            // Steps Analytics Chart
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "7-DAY ACTIVITY TRENDS",
                        fontSize = 11.sp,
                        color = MutedSlate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (recentSteps.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Awaiting logs from your active schedule...", color = MutedSlate, fontSize = 12.sp)
                        }
                    } else {
                        val sortedLogs = recentSteps.sortedBy { it.date }
                        val goal = user.dailyStepGoal
                        
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        ) {
                            val maxSteps = (sortedLogs.maxOfOrNull { it.steps } ?: goal).coerceAtLeast(1000).toFloat()
                            val spacing = size.width / (sortedLogs.size + 1)
                            
                            // Draw grid lines
                            val gridLines = 3
                            for (i in 1..gridLines) {
                                val y = (size.height / (gridLines + 1)) * i
                                drawLine(
                                    color = Color(0xFF282B33),
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            // Draw bars
                            sortedLogs.forEachIndexed { index, log ->
                                val x = spacing * (index + 0.9f)
                                val barHeight = (log.steps.toFloat() / maxSteps) * (size.height - 35.dp.toPx())
                                val isPassed = log.steps >= goal

                                drawRoundRect(
                                    color = if (isPassed) ActiveVolt else ActiveBlue.copy(alpha = 0.7f),
                                    topLeft = Offset(x - 10.dp.toPx(), size.height - barHeight - 20.dp.toPx()),
                                    size = Size(18.dp.toPx(), barHeight),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )
                            }
                        }

                        // Bottom date markers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            sortedLogs.forEach { log ->
                                val formattedDate = log.date.substringAfterLast("-")
                                Text(
                                    text = formattedDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MutedSlate,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== WORKOUTS TAB ====================
@Composable
fun WorkoutsScreen(
    modifier: Modifier,
    workouts: List<WorkoutLog>,
    viewModel: FitViewModel
) {
    var workoutType by remember { mutableStateOf("Running") }
    var durationText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    
    // GPS Simulation running state
    var isGpsRunning by remember { mutableStateOf(false) }
    var gpsActivityType by remember { mutableStateOf("Running") }
    var gpsSecondsElapsed by remember { mutableStateOf(0) }
    var gpsDistanceSimulated by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    // GPS Timer simulation loop
    LaunchedEffect(isGpsRunning) {
        if (isGpsRunning) {
            gpsSecondsElapsed = 0
            gpsDistanceSimulated = 0f
            while (isGpsRunning) {
                delay(1000)
                gpsSecondsElapsed++
                // Simulate slow progress path coordinates logging
                gpsDistanceSimulated += 0.002f + java.util.Random().nextFloat() * (0.005f - 0.002f)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "WORKOUT DISPATCHER",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        // --- SECTION: Virtual GPS Tracker Simulator ---
        item {
            Card(
                border = BorderStroke(1.5.dp, ActiveBlue.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = "GPS", tint = ActiveBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS TRAIL ACTIVATOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActiveBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isGpsRunning) {
                        Text(
                            text = "Ready to hit the outdoor trail? Trigger a real-time GPS coordinates simulator to map distance logs.",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Running", "Cycling").forEach { type ->
                                FilterChip(
                                    selected = gpsActivityType == type,
                                    onClick = { gpsActivityType = type },
                                    label = { Text(type) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ActiveBlue,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { isGpsRunning = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_gps_button")
                        ) {
                            Text("Start Live GPS Simulation", color = Color.Black, fontWeight = FontWeight.Black)
                        }
                    } else {
                        // GPS Active Simulation UI
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F141F), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "GPS RECORDING IN PROGRESS",
                                fontSize = 11.sp,
                                color = ActiveBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("TIME", fontSize = 10.sp, color = MutedSlate)
                                    val mins = gpsSecondsElapsed / 60
                                    val secs = gpsSecondsElapsed % 60
                                    Text(
                                        "${"%02d".format(mins)}:${"%02d".format(secs)}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DISTANCE", fontSize = 10.sp, color = MutedSlate)
                                    Text(
                                        "${"%.3f".format(gpsDistanceSimulated)} km",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ActiveVolt
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Mock Map Coordinate Drawing
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(Color(0xFF1B2332), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Generate coordinates trail waves
                                    val pathLength = size.width - 40
                                    val progress = (gpsSecondsElapsed % 30) / 30f
                                    drawCircle(
                                        color = ActiveBlue,
                                        radius = 6.dp.toPx(),
                                        center = Offset(20 + pathLength * progress, size.height / 2)
                                    )
                                    // Simulated lines
                                    drawLine(
                                        color = ActiveBlue.copy(alpha = 0.5f),
                                        start = Offset(20f, size.height / 2),
                                        end = Offset(size.width - 20f, size.height / 2),
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }
                                Text(
                                    text = "Simulated GPS Track Latitude / Longitude Syncing...",
                                    fontSize = 11.sp,
                                    color = MutedSlate
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    isGpsRunning = false
                                    val loggedMins = (gpsSecondsElapsed / 60).coerceAtLeast(1)
                                    viewModel.simulateGpsLogging(gpsActivityType, loggedMins)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CalorieCrimson),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stop_gps_button")
                            ) {
                                Text("Stop & Sync Trails", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION: Log Manual Workout ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MANUAL LOG SESSIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActiveVolt,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Workout Quick chips selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf("Strength", "Running", "Yoga", "Cycling", "Walking")
                        Column {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                types.take(3).forEach { t ->
                                    FilterChip(
                                        selected = workoutType == t,
                                        onClick = { workoutType = t },
                                        label = { Text(t) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ActiveVolt,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                types.drop(3).forEach { t ->
                                    FilterChip(
                                        selected = workoutType == t,
                                        onClick = { workoutType = t },
                                        label = { Text(t) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ActiveVolt,
                                            selectedLabelColor = Color.Black
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { durationText = it },
                        label = { Text("Duration (minutes)", color = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActiveVolt,
                            unfocusedBorderColor = Color(0xFF2E313D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_duration_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Session specifications (e.g., bench, warmups)", color = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActiveVolt,
                            unfocusedBorderColor = Color(0xFF2E313D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_notes_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val duration = durationText.toIntOrNull() ?: 0
                            if (duration > 0) {
                                // Simple calorie calculator estimate
                                val burnedFactor = when (workoutType) {
                                    "Running" -> 11
                                    "Cycling" -> 8
                                    "Yoga" -> 4
                                    "Strength" -> 5
                                    else -> 6
                                }
                                val cal = duration * burnedFactor
                                viewModel.addWorkout(workoutType, duration, cal, notesText)
                                durationText = ""
                                notesText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_workout_submit")
                    ) {
                        Text("Save Workout Log", color = ActiveVolt, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SECTION: History list ---
        item {
            Text(
                text = "LOGGED HISTORY (${workouts.size})",
                fontSize = 12.sp,
                color = MutedSlate,
                fontWeight = FontWeight.Bold
            )
        }

        if (workouts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No sports logged yet today. Build your streak now!", color = MutedSlate, fontSize = 13.sp)
                }
            }
        } else {
            items(workouts) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (item.type.startsWith("GPS")) ActiveBlue else ActiveVolt)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.type.uppercase(Locale.ROOT),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.type.startsWith("GPS")) ActiveBlue else ActiveVolt
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.durationMinutes} minutes Session",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (item.notes.isNotEmpty()) {
                                Text(
                                    text = item.notes,
                                    fontSize = 12.sp,
                                    color = Color.LightGray
                                )
                            }
                            val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp))
                            Text(
                                text = dateStr,
                                fontSize = 10.sp,
                                color = MutedSlate
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-${item.caloriesBurned} kcal",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = CalorieCrimson
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            IconButton(
                                onClick = { viewModel.deleteWorkout(item.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MutedSlate,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== HEALTH TAB (Heats & Sleeps) ====================
@Composable
fun HealthScreen(
    modifier: Modifier,
    heartRates: List<HeartRateLog>,
    sleepLogs: List<SleepLog>,
    viewModel: FitViewModel
) {
    var sleepHoursText by remember { mutableStateOf("") }
    var sleepQuality by remember { mutableStateOf(3) }
    var sleepNotesText by remember { mutableStateOf("") }

    // Pulsing Scanner interactive demo values
    var isScanningPulse by remember { mutableStateOf(false) }
    val scannerScale = remember { Animatable(1f) }
    var simulatedPulseRate by remember { mutableStateOf(72) }

    if (isScanningPulse) {
        LaunchedEffect(Unit) {
            scannerScale.animateTo(
                targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
        LaunchedEffect(Unit) {
            while (isScanningPulse) {
                delay(800)
                simulatedPulseRate = (68..85).random()
            }
        }
    } else {
        LaunchedEffect(Unit) {
            scannerScale.snapTo(1f)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "BIOMETRIC MONITORING",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        // --- INTERACTIVE BIOPULSE HEART DETECTOR SENSOR MOCK ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REAL-TIME BIO-PULSE COGNIZANCE",
                        fontSize = 11.sp,
                        color = CalorieCrimson,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF221118))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isScanningPulse = true
                                        tryAwaitRelease()
                                        isScanningPulse = false
                                        // Auto save logged reading on release
                                        viewModel.addPulseReading(simulatedPulseRate)
                                        viewModel.triggerSystemNotification(
                                            "BPM Synced! ❤️",
                                            "Recorded static BPM measurement of $simulatedPulseRate bpm."
                                        )
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsating background ring
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .scale(scannerScale.value)
                                .border(
                                    width = 3.dp,
                                    color = if (isScanningPulse) CalorieCrimson else Color(0xFF4A1A28),
                                    shape = CircleShape
                                )
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart pulse",
                                tint = CalorieCrimson,
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(if (isScanningPulse) scannerScale.value else 1f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isScanningPulse) "$simulatedPulseRate" else "HOLD",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isScanningPulse) "bpm" else "TO SCAN",
                                fontSize = 10.sp,
                                color = MutedSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Firmly press and hold the scanner circle to simulate active smartwatch optical pulse checking.",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- HEART RATES HISTORIES ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HEART RECORDINGS (BPM DATA)",
                        fontSize = 11.sp,
                        color = MutedSlate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (heartRates.isEmpty()) {
                        Text("Hold scanner above to feed initial cardiovascular beats.", color = MutedSlate, fontSize = 12.sp)
                    } else {
                        // Display a quick row list of pulses
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            heartRates.take(8).forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .background(Color(0xFF22242D), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${item.heartRate}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CalorieCrimson
                                    )
                                    Text(
                                        text = "BPM",
                                        fontSize = 10.sp,
                                        color = MutedSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SLEEP REGISTER SCREEN ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SLEEP DIARY RECORDER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActiveBlue,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = sleepHoursText,
                        onValueChange = { sleepHoursText = it },
                        label = { Text("Sleeping duration (hours)", color = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActiveBlue,
                            unfocusedBorderColor = Color(0xFF2E313D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_hours_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("How was your sleeping quality?", fontSize = 12.sp, color = MutedSlate)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (q in 1..5) {
                            val activeVal = q <= sleepQuality
                            IconButton(onClick = { sleepQuality = q }) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "$q Stars",
                                    tint = if (activeVal) ActiveBlue else Color(0xFF2E313D)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = sleepNotesText,
                        onValueChange = { sleepNotesText = it },
                        label = { Text("Sleep Notes (e.g., interrupted dreams, light noise)", color = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActiveBlue,
                            unfocusedBorderColor = Color(0xFF2E313D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_notes_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val hrs = sleepHoursText.toFloatOrNull() ?: 0f
                            if (hrs > 0f) {
                                viewModel.addSleepLog(hrs, sleepQuality, sleepNotesText)
                                sleepHoursText = ""
                                sleepNotesText = ""
                                sleepQuality = 3
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13232C)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("log_sleep_submit")
                    ) {
                        Text("Log Night Entry", color = ActiveBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- SLEEP HISTORIES LIST ---
        item {
            Text(
                text = "SLEEP LOG HISTORIES (${sleepLogs.size})",
                fontSize = 11.sp,
                color = MutedSlate,
                fontWeight = FontWeight.Bold
            )
        }

        if (sleepLogs.isEmpty()) {
            item {
                Text("Log sleeping sessions to populate diary files.", color = MutedSlate, fontSize = 12.sp)
            }
        } else {
            items(sleepLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val durationHrs = (log.endTime - log.startTime) / (3600.0f * 1000)
                            Text(
                                text = "Duration: ${"%.1f".format(durationHrs)} hrs logged",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (log.notes.isNotEmpty()) {
                                Text(text = log.notes, fontSize = 12.sp, color = Color.LightGray)
                            }
                            Text(text = "Date: ${log.date}", fontSize = 11.sp, color = MutedSlate)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Quality",
                                fontSize = 10.sp,
                                color = MutedSlate
                            )
                            Row {
                                for (i in 1..log.quality) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "*",
                                        tint = ActiveBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== WEARABLE TAB ====================
@Composable
fun WearableScreen(
    modifier: Modifier,
    isConnected: Boolean,
    battery: Int,
    notifications: List<NotificationLog>,
    viewModel: FitViewModel
) {
    var customAlertTitle by remember { mutableStateOf("") }
    var customAlertBody by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "WEARABLE PERIPHERAL INTEGRATION",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        // --- HARDWARE PAIR PANEL ---
        item {
            Card(
                border = BorderStroke(1.5.dp, if (isConnected) ActiveVolt else MutedSlate),
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PULSEWEAR RING v4",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (isConnected) "ACTIVE & REAL-TIME STREAMING" else "DISCONNECTED",
                                fontSize = 11.sp,
                                color = if (isConnected) ActiveVolt else MutedSlate,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = isConnected,
                            onCheckedChange = { viewModel.toggleWearableSync() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = ActiveVolt
                            ),
                            modifier = Modifier.testTag("wearable_sync_switch")
                        )
                    }

                    if (isConnected) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🔋 Smartwatch Ring Battery:", fontSize = 13.sp, color = Color.LightGray)
                            Text("$battery%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ActiveVolt)
                        }
                        Text(
                            "Continuous PPG biometric pulse checks & accelerometer logs synchronized flawlessly in the background.",
                            fontSize = 12.sp,
                            color = MutedSlate,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // --- MANUALLY TRIGGER IN-APP NOTIFICATION ALERTS PANEL ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MANUALLY STREAM NOTIFICATIONS",
                        fontSize = 11.sp,
                        color = MutedSlate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customAlertTitle,
                        onValueChange = { customAlertTitle = it },
                        label = { Text("Notification Title", color = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActiveVolt,
                            unfocusedBorderColor = Color(0xFF2E313D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("push_title_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customAlertBody,
                        onValueChange = { customAlertBody = it },
                        label = { Text("Notification Alert Message", color = MutedSlate) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ActiveVolt,
                            unfocusedBorderColor = Color(0xFF2E313D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("push_body_input")
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (customAlertTitle.isNotEmpty() && customAlertBody.isNotEmpty()) {
                                viewModel.triggerSystemNotification(customAlertTitle, customAlertBody)
                                customAlertTitle = ""
                                customAlertBody = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_push_submit")
                    ) {
                        Text("Send Simulation Alarm Alert", color = ActiveVolt, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- NOTIFICATIONS RECORDS HISTORY LIST ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PUSH ALARM HISTORY (${notifications.size})",
                    fontSize = 11.sp,
                    color = MutedSlate,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { viewModel.clearNotifications() }) {
                    Text("Clear Alerts", color = CalorieCrimson, fontSize = 11.sp)
                }
            }
        }

        if (notifications.isEmpty()) {
            item {
                Text("Sync devices or create notification alarms to record items here.", color = MutedSlate, fontSize = 12.sp)
            }
        } else {
            items(notifications) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = log.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(log.timestamp))
                            Text(text = timeStr, fontSize = 10.sp, color = MutedSlate)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = log.body, fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

// ==================== PROFILE & GEMINI COACH TAB ====================
@Composable
fun ProfileScreen(
    modifier: Modifier,
    user: User?,
    allUsers: List<User>,
    isCoachLoading: Boolean,
    coachRecommendation: String,
    viewModel: FitViewModel
) {
    if (user == null) return

    // Account creation inputs code state
    var editName by remember { mutableStateOf("") }
    var editEmail by remember { mutableStateOf("") }
    var editHeight by remember { mutableStateOf("") }
    var editWeight by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editGender by remember { mutableStateOf("Female") }
    var editStepsGoal by remember { mutableStateOf("") }
    var editWaterGoal by remember { mutableStateOf("") }

    var isAddingAthlete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. THE AI ADVISORY SECTION (Gemini) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141F1A)),
                border = BorderStroke(1.5.dp, ActiveVolt.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = "AI Coach", tint = ActiveVolt)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI FIT-PULSE ADVISORY COACH",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActiveVolt,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "FitPulse Coach compiles your unique weight indices, daily steps logs, workout histories, and sleeping cycles to synthesize tailored dietary regimens and next-tier exercising routines.",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (isCoachLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = ActiveVolt)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Coach compiling database metrics & mapping forecasts standard...", fontSize = 11.sp, color = MutedSlate)
                        }
                    } else {
                        if (coachRecommendation.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0C1310), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "COACH HEALTH FORECASTS",
                                    fontSize = 11.sp,
                                    color = ActiveVolt,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = coachRecommendation,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    lineHeight = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = { viewModel.explainHealthProfileWithAIGoach() },
                            colors = ButtonDefaults.buttonColors(containerColor = ActiveVolt),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_coach_button")
                        ) {
                            Text(
                                "Request Personalized Wellness advisory",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // --- 2. ATHLETE ACCOUNT MANAGEMENT ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACTIVE ATHLETE PROFILE",
                        fontSize = 12.sp,
                        color = MutedSlate,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Name: ${user.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Email: ${user.email}", fontSize = 14.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Weight", fontSize = 11.sp, color = MutedSlate)
                            Text("${user.weightKg} kg", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Height", fontSize = 11.sp, color = MutedSlate)
                            Text("${user.heightCm} cm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column {
                            Text("Age / Sex", fontSize = 11.sp, color = MutedSlate)
                            Text("${user.age} yrs / ${user.gender}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // --- 3. CREATE ADDITIONAL ATHLETE PROFILE ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = OnyxSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REGISTER NEW ATHLETE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ActiveVolt,
                            fontFamily = FontFamily.Monospace
                        )
                        IconButton(onClick = { isAddingAthlete = !isAddingAthlete }) {
                            Icon(
                                imageVector = if (isAddingAthlete) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Toggle signup register",
                                tint = ActiveVolt
                            )
                        }
                    }

                    if (isAddingAthlete) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Athlete Name", color = MutedSlate) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveVolt),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("athlete_name_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Athlete Email ID", color = MutedSlate) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveVolt),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("athlete_email_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editHeight,
                                onValueChange = { editHeight = it },
                                label = { Text("Height (cm)", color = MutedSlate) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveVolt),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("athlete_height_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editWeight,
                                onValueChange = { editWeight = it },
                                label = { Text("Weight (kg)", color = MutedSlate) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveVolt),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("athlete_weight_input"),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editAge,
                                onValueChange = { editAge = it },
                                label = { Text("Age (years)", color = MutedSlate) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveVolt),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("athlete_age_input"),
                                singleLine = true
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    listOf("Male", "Female").forEach { g ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { editGender = g }
                                        ) {
                                            RadioButton(
                                                selected = editGender == g,
                                                onClick = { editGender = g },
                                                colors = RadioButtonDefaults.colors(selectedColor = ActiveVolt)
                                            )
                                            Text(g, fontSize = 12.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editStepsGoal,
                                onValueChange = { editStepsGoal = it },
                                label = { Text("Step Goal", color = MutedSlate) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveVolt),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("athlete_steps_goal_input"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editWaterGoal,
                                onValueChange = { editWaterGoal = it },
                                label = { Text("Water (ml)", color = MutedSlate) },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ActiveVolt),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("athlete_water_goal_input"),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val ht = editHeight.toFloatOrNull() ?: 170f
                                val wt = editWeight.toFloatOrNull() ?: 65f
                                val ag = editAge.toIntOrNull() ?: 25
                                val sg = editStepsGoal.toIntOrNull() ?: 10000
                                val wg = editWaterGoal.toIntOrNull() ?: 2000

                                if (editName.isNotEmpty() && editEmail.isNotEmpty()) {
                                    viewModel.registerNewUser(
                                        editName,
                                        editEmail,
                                        ht,
                                        wt,
                                        ag,
                                        editGender,
                                        sg,
                                        sg * 1 / 4,
                                        wg
                                    )
                                    editName = ""
                                    editEmail = ""
                                    editHeight = ""
                                    editWeight = ""
                                    editAge = ""
                                    editStepsGoal = ""
                                    editWaterGoal = ""
                                    isAddingAthlete = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222634)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_athlete_submit")
                        ) {
                            Text("Confirm & Create Profile", color = ActiveVolt, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Dialog to select/switch user profiles
@Composable
fun ProfileSelectionDialog(
    allUsers: List<User>,
    activeUser: User?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Switch Athlete Profile", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allUsers) { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (u.id == activeUser?.id) Color(0xFF1D2821) else Color(0xFF20222B),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (u.id == activeUser?.id) ActiveVolt else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelect(u.id) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = u.name, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = u.email, fontSize = 11.sp, color = MutedSlate)
                        }
                        if (u.id == activeUser?.id) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Active", tint = ActiveVolt)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ActiveVolt)
            }
        },
        containerColor = OnyxSurface
    )
}
