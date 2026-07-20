package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AttachmentPickerButton
import com.example.ui.ChatAttachmentPreview
import com.example.ui.ChatMessage
import com.example.ui.MainViewModel
import com.example.ui.components.WorkoutAnimator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onStartWorkout: (String) -> Unit,
    onStartClass: () -> Unit,
    onNavigateToOnboarding: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val profile by viewModel.userProfile.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (isWideScreen) {
                // Adaptive layout: Sidebar navigation rail for Expanded tablet/ChromeOS displays
                NavigationRail(
                    containerColor = Color(0xFF18181B),
                    header = {
                        IconButton(onClick = onNavigateToOnboarding) {
                            Icon(Icons.Default.Settings, contentDescription = "Edit Profile", tint = Color.White)
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Coach AI") },
                        label = { Text("Coach AI", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFF71717A),
                            indicatorColor = Color(0xFF27272A)
                        )
                    )
                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Workouts") },
                        label = { Text("Workouts", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFF71717A),
                            indicatorColor = Color(0xFF27272A)
                        )
                    )
                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.QueryStats, contentDescription = "Analytics") },
                        label = { Text("Analytics", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFF71717A),
                            indicatorColor = Color(0xFF27272A)
                        )
                    )
                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Social") },
                        label = { Text("Leaderboard", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFF71717A),
                            indicatorColor = Color(0xFF27272A)
                        )
                    )
                    // GAP G7: About/Help tab (index 4)
                    NavigationRailItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                        label = { Text("About", fontSize = 10.sp) },
                        colors = NavigationRailItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            unselectedIconColor = Color(0xFF71717A),
                            indicatorColor = Color(0xFF27272A)
                        )
                    )
                }
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                containerColor = Color.Black,
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = Color(0xFF18181B),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Chat, contentDescription = "Coach AI") },
                                label = { Text("Coach AI", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color(0xFF71717A),
                                    unselectedTextColor = Color(0xFF71717A),
                                    indicatorColor = Color(0xFF27272A)
                                ),
                                modifier = Modifier.testTag("tab_coach")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Workouts") },
                                label = { Text("Workouts", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color(0xFF71717A),
                                    unselectedTextColor = Color(0xFF71717A),
                                    indicatorColor = Color(0xFF27272A)
                                ),
                                modifier = Modifier.testTag("tab_workouts")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.QueryStats, contentDescription = "Analytics") },
                                label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color(0xFF71717A),
                                    unselectedTextColor = Color(0xFF71717A),
                                    indicatorColor = Color(0xFF27272A)
                                ),
                                modifier = Modifier.testTag("tab_analytics")
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard") },
                                label = { Text("Leaderboard", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color(0xFF71717A),
                                    unselectedTextColor = Color(0xFF71717A),
                                    indicatorColor = Color(0xFF27272A)
                                ),
                                modifier = Modifier.testTag("tab_leaderboard")
                            )
                            // GAP G7: About/Help tab (index 4)
                            NavigationBarItem(
                                selected = selectedTab == 4,
                                onClick = { selectedTab = 4 },
                                icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                                label = { Text("About", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedIconColor = Color(0xFF71717A),
                                    unselectedTextColor = Color(0xFF71717A),
                                    indicatorColor = Color(0xFF27272A)
                                ),
                                modifier = Modifier.testTag("tab_about")
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        0 -> CoachTab(viewModel, onNavigateToOnboarding)
                        1 -> WorkoutsTab(onStartWorkout, onStartClass)
                        2 -> AnalyticsTab(viewModel)
                        3 -> LeaderboardTab(viewModel)
                        4 -> AboutTab() // GAP G7: About/Help screen
                    }
                }
            }
        }
    }
}

// ==================== 1. COACH AI TAB ====================
@Composable
fun CoachTab(viewModel: MainViewModel, onEditProfile: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val isGeneratingProgram by viewModel.isGeneratingProgram.collectAsState()

    var inputMsg by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll chat to bottom whenever messages list grows
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // AI Program Card at top
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Program", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("YOUR AI FIT PROGRAM", fontSize = 12.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEditProfile,
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Edit Stats", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.signOutUser() },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Sign Out", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isGeneratingProgram) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Coach Iron is crafting schedule...", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }
                } else {
                    val programText = profile?.workoutProgram
                    if (!programText.isNullOrBlank()) {
                        Text(
                            text = programText,
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.generateCustomProgram() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Regenerate Plan", fontSize = 12.sp, color = Color.White)
                        }
                    } else {
                        Text(
                            text = "No customized program yet. Click setup to generate classes on your weekly schedule!",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.generateCustomProgram() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Generate Program", fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Live Chat Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { msg ->
                val isCoach = msg.sender == "coach"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isCoach) Arrangement.Start else Arrangement.End
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCoach) Color(0xFF1E293B) else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isCoach) 2.dp else 16.dp,
                            bottomEnd = if (isCoach) 16.dp else 2.dp
                        ),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // --- G4/G5/G6: render an image thumbnail, playable video, or file chip above the text ---
                            if (msg.attachmentUri != null) {
                                ChatAttachmentPreview(msg, modifier = Modifier.padding(bottom = if (msg.text.isNotBlank()) 8.dp else 0.dp))
                            }
                            if (msg.text.isNotBlank()) {
                                Text(
                                    text = msg.text,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            if (isChatLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Coach is typing...", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggested Prompt chips
        val suggestionChips = listOf("Give me squat tips", "Suggest a 5 min program", "How does form tracker work?")
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestionChips) { chip ->
                Card(
                    modifier = Modifier.clickable { viewModel.sendMessage(chip) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text(
                        chip,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input send box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- G4/G5/G6: attachment button (photo / video / file) ---
            AttachmentPickerButton(
                onImagePicked = { uri -> viewModel.sendImageMessage(uri, inputMsg); inputMsg = "" },
                onVideoPicked = { uri -> viewModel.sendVideoMessage(uri, inputMsg); inputMsg = "" },
                onFilePicked = { uri, name, mimeType -> viewModel.sendFileMessage(uri, name, mimeType, inputMsg); inputMsg = "" }
            )

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = inputMsg,
                onValueChange = { inputMsg = it },
                placeholder = { Text("Ask Coach Iron...", color = Color(0xFF64748B)) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF0F172A),
                    unfocusedContainerColor = Color(0xFF0F172A)
                ),
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(
                onClick = {
                    if (inputMsg.isNotBlank()) {
                        viewModel.sendMessage(inputMsg)
                        inputMsg = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_send_button"),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF2563EB))
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send Message", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==================== 2. WORKOUTS TAB ====================
data class ExerciseItem(
    val name: String,
    val muscleGroup: String,
    val description: String,
    val durationMin: Int
)

@Composable
fun WorkoutsTab(onStartWorkout: (String) -> Unit, onStartClass: () -> Unit) {
    val exercises = listOf(
        ExerciseItem("Squats", "Quads, Glutes & Core", "Classic squat. Keep weight in heels, descend with thighs parallel, maintain a tall back alignment.", 4),
        ExerciseItem("Pushups", "Chest, Shoulders & Triceps", "Maintain strict diagonal plank. Bend elbows to 90 degrees, lowering chest close to ground.", 3),
        ExerciseItem("Jumping Jacks", "Full Body Cardio", "High energy metabolic jack. Stand feet together, jump wide while sweeping hands above head.", 2)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // PRD v2 (Lane B): prominent "Start today's class" CTA — launches the multi-exercise class.
        Button(
            onClick = onStartClass,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("start_class_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start today's class", fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text("CHOOSE A WORKOUT", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        Text("AI Pose-Tracker Active", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Black)

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(exercises) { exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exercise.name.uppercase(), fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Black)
                                Text(exercise.muscleGroup, fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${exercise.durationMin} MIN",
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            exercise.description,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // High fidelity Preview Animation inside the selector card itself! Awesome!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            WorkoutAnimator(
                                exerciseName = exercise.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onStartWorkout(exercise.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_workout_card_${exercise.name}"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start AI Tracking Session", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==================== 3. ANALYTICS TAB ====================
@Composable
fun AnalyticsTab(viewModel: MainViewModel) {
    val profile by viewModel.userProfile.collectAsState()
    val sessions by viewModel.workoutSessions.collectAsState()

    val totalWorkouts = sessions.size
    val totalPoints = profile?.experiencePoints ?: 0
    val activeStreak = profile?.streakDays ?: 0
    val averageAccuracy = if (sessions.isNotEmpty()) sessions.map { it.formScore }.average() else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("PERFORMANCE ANALYTICS", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        Text("Historical Progress", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Black)

        Spacer(modifier = Modifier.height(16.dp))

        // High level stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SESSIONS", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalWorkouts", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("STREAK", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$activeStreak Days", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TOTAL XP", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalPoints", fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom drawn graph 1: Reps Progress Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("REPS GROWTH (LATEST SESSIONS)", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val repList = if (sessions.isNotEmpty()) {
                        sessions.take(6).reversed().map { it.reps.toFloat() }
                    } else {
                        listOf(4f, 8f, 12f, 15f, 20f, 22f) // fallback demo data if no workouts
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val padding = 30f

                        val maxVal = (repList.maxOrNull() ?: 10f) * 1.2f
                        val segmentW = (w - 2 * padding) / (repList.size - 1).coerceAtLeast(1)

                        // Draw Grid & Axes
                        drawLine(Color(0xFF334155), Offset(padding, h - padding), Offset(w - padding, h - padding), 3f)
                        drawLine(Color(0xFF334155), Offset(padding, padding), Offset(padding, h - padding), 3f)

                        // Connect points path
                        val points = repList.mapIndexed { idx, value ->
                            val cx = padding + idx * segmentW
                            val cy = h - padding - ((value / maxVal) * (h - 2 * padding))
                            Offset(cx, cy)
                        }

                        val strokePath = Path().apply {
                            if (points.isNotEmpty()) {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    lineTo(points[i].x, points[i].y)
                                }
                            }
                        }

                        drawPath(strokePath, color = Color(0xFF2563EB), style = Stroke(width = 6f))

                        points.forEach { pt ->
                            drawCircle(Color(0xFF38BDF8), radius = 6f, center = pt)
                            drawCircle(Color.White, radius = 3f, center = pt)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom drawn graph 2: Form Accuracy Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("FORM SAFETY ACCURACY (AVERAGE: ${averageAccuracy.toInt()}%)", fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val accuracyList = if (sessions.isNotEmpty()) {
                        sessions.take(6).reversed().map { it.formScore.toFloat() }
                    } else {
                        listOf(85f, 92f, 78f, 96f, 91f, 95f) // fallback demo data if no workouts
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val padding = 30f
                        val barCount = accuracyList.size
                        val spaceW = (w - 2 * padding) / barCount
                        val barW = spaceW * 0.6f

                        // Draw floor axis
                        drawLine(Color(0xFF334155), Offset(padding, h - padding), Offset(w - padding, h - padding), 3f)

                        accuracyList.forEachIndexed { idx, score ->
                            val bx = padding + idx * spaceW + (spaceW - barW) / 2
                            val barH = ((score / 100f) * (h - 2 * padding))
                            val by = h - padding - barH

                            drawRect(
                                color = if (score >= 90f) Color(0xFF10B981) else Color(0xFFF59E0B),
                                topLeft = Offset(bx, by),
                                size = androidx.compose.ui.geometry.Size(barW, barH)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workout Log list
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("HISTORICAL EXERCISE LOGS", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                if (sessions.isEmpty()) {
                    Text("No sessions recorded yet. Start training to log history!", fontSize = 13.sp, color = Color(0xFF64748B))
                } else {
                    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
                    sessions.take(5).forEach { session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(session.exerciseName, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(sdf.format(Date(session.timestamp)), fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${session.reps} Reps", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Black)
                                Text("Score: ${session.formScore.toInt()}%", fontSize = 11.sp, color = if (session.formScore >= 90.0) Color(0xFF10B981) else Color(0xFFF59E0B))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // PRD v2 (Lane B): class-level history — past "today's class" sessions.
        ClassHistorySection(viewModel)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// PRD v2 (Lane B): shows completed multi-exercise classes from viewModel.workoutClasses.
@Composable
fun ClassHistorySection(viewModel: MainViewModel) {
    val classes by viewModel.workoutClasses.collectAsState()
    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("class_history"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("CLASS HISTORY", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (classes.isEmpty()) {
                Text(
                    "No classes completed yet. Tap \"Start today's class\" to begin!",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            } else {
                classes.take(10).forEach { wc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${wc.exerciseCount} exercises · ${wc.totalReps} reps",
                                fontSize = 14.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(sdf.format(Date(wc.completedAt)), fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${wc.totalPoints} pts", fontSize = 14.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Black)
                            Text(
                                "Form ${wc.avgFormScore.toInt()}%",
                                fontSize = 11.sp,
                                color = if (wc.avgFormScore >= 90.0) Color(0xFF10B981) else Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun LeaderboardTab(viewModel: MainViewModel) {
    val leaderboard by viewModel.leaderboard.collectAsState()
    val badges by viewModel.badges.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Leaderboard Section
        Text("ACTIVE COMMUNITY CHALLENGE", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        Text("Global Leaderboard", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Black)

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                leaderboard.sortedByDescending { it.points }.forEachIndexed { idx, competitor ->
                    val isUser = competitor.isCurrentUser
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isUser) Color(0xFF2563EB).copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${idx + 1}",
                                fontSize = 14.sp,
                                color = if (idx == 0) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(36.dp)
                            )

                            Text(
                                text = competitor.name,
                                fontSize = 14.sp,
                                color = if (isUser) Color(0xFF38BDF8) else Color.White,
                                fontWeight = if (isUser) FontWeight.Black else FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${competitor.points} XP",
                            fontSize = 14.sp,
                            color = if (isUser) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Badges Section
        Text("MOTIVATIONAL MILESTONES", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        Text("Your Earned Badges", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Black)

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            badges.forEach { badge ->
                val isUnlocked = badge.unlockedAt != null
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isUnlocked) Color(0xFF2563EB) else Color(0xFF334155).copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge Icon (glowing star/cup)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (isUnlocked) Color(0xFF2563EB).copy(alpha = 0.2f) else Color(0xFF1E293B),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                contentDescription = badge.title,
                                tint = if (isUnlocked) Color(0xFFF59E0B) else Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                badge.title,
                                fontSize = 15.sp,
                                color = if (isUnlocked) Color.White else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                badge.description,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )

                            if (isUnlocked) {
                                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                Text(
                                    "Unlocked on ${sdf.format(Date(badge.unlockedAt!!))}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
