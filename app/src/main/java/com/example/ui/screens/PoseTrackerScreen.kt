package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.components.WorkoutAnimator
import kotlinx.coroutines.delay

@Composable
fun PoseTrackerScreen(
    exerciseName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permissions State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Workout Tracker states
    var isWorkoutActive by remember { mutableStateOf(false) }
    var repCount by remember { mutableIntStateOf(0) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var averageFormScore by remember { mutableStateOf(95.0) }
    var currentCritique by remember { mutableStateOf("Position yourself in front of the camera to begin.") }
    var isSimulationMode by remember { mutableStateOf(true) } // Simulation Mode ON by default for emulator

    // Timing & Simulation Loops
    LaunchedEffect(isWorkoutActive) {
        if (isWorkoutActive) {
            viewModel.speak("Starting $exerciseName tracker. Let's do this!")
            while (isWorkoutActive) {
                delay(1000)
                totalSeconds++

                // In Simulation mode, increment reps and trigger TTS feedback periodically
                if (isSimulationMode) {
                    if (totalSeconds % 6 == 0) {
                        repCount++
                        val performanceScore = if (repCount % 3 == 0) 84.0 else 96.0
                        averageFormScore = (averageFormScore * repCount + performanceScore) / (repCount + 1)

                        if (repCount % 3 == 0) {
                            currentCritique = "Warning: Keep your back straight! Lower hips slowly."
                            viewModel.speak("Form warning. Keep your back straight, align your posture.")
                        } else {
                            currentCritique = "Perfect $exerciseName technique! Rep $repCount counted."
                            viewModel.speak("Excellent! Rep $repCount. Beautiful form.")
                        }
                    }
                }
            }
        }
    }

    // Animation scanning lines
    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- BASE CAMERA OR SIMULATION ANIMATOR ---
        if (hasCameraPermission && !isSimulationMode) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            Log.e("PoseTracker", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // High fidelity Workout Animator when Camera isn't active/allowed or simulation mode is toggled on
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 240.dp)
            ) {
                WorkoutAnimator(
                    exerciseName = exerciseName,
                    modifier = Modifier.fillMaxSize(),
                    isWarningMode = currentCritique.contains("Warning") && isWorkoutActive
                )
            }
        }

        // --- CYBERPUNK HUD OVERLAY GRAPHICS ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Scanner crosshairs
            val strokeColor = Color(0xFF38BDF8).copy(alpha = 0.4f)
            val cornerLength = 40f
            val padding = 60f

            // Top-left corner
            drawLine(strokeColor, Offset(padding, padding), Offset(padding + cornerLength, padding), 4f)
            drawLine(strokeColor, Offset(padding, padding), Offset(padding, padding + cornerLength), 4f)

            // Top-right corner
            drawLine(strokeColor, Offset(width - padding, padding), Offset(width - padding - cornerLength, padding), 4f)
            drawLine(strokeColor, Offset(width - padding, padding), Offset(width - padding, padding + cornerLength), 4f)

            // Bottom-left corner
            drawLine(strokeColor, Offset(padding, height - padding - 240.dp.toPx()), Offset(padding + cornerLength, height - padding - 240.dp.toPx()), 4f)
            drawLine(strokeColor, Offset(padding, height - padding - 240.dp.toPx()), Offset(padding, height - padding - 240.dp.toPx() - cornerLength), 4f)

            // Bottom-right corner
            drawLine(strokeColor, Offset(width - padding, height - padding - 240.dp.toPx()), Offset(width - padding - cornerLength, height - padding - 240.dp.toPx()), 4f)
            drawLine(strokeColor, Offset(width - padding, height - padding - 240.dp.toPx()), Offset(width - padding, height - padding - 240.dp.toPx() - cornerLength), 4f)

            // Laser Scanning Line
            if (isWorkoutActive) {
                val scanY = padding + (height - 2 * padding - 240.dp.toPx()) * laserY
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF00F5FF).copy(alpha = 0.8f), Color.Transparent)
                    ),
                    start = Offset(padding, scanY),
                    end = Offset(width - padding, scanY),
                    strokeWidth = 6f
                )
            }
        }

        // --- BACK BUTTON ---
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .testTag("back_button")
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Go Back", tint = Color.White)
        }

        // --- TOP STATS PANEL ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 72.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.85f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("WORKOUT TIME", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60),
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("REPS", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text(
                        text = "$repCount",
                        fontSize = 24.sp,
                        color = Color(0xFF22C55E),
                        fontWeight = FontWeight.Black
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("FORM SCORE", fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format("%.0f%%", averageFormScore),
                        fontSize = 18.sp,
                        color = if (averageFormScore >= 90.0) Color(0xFF10B981) else Color(0xFFF59E0B),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // --- BOTTOM ACTION PANEL ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF020617).copy(alpha = 0.95f), Color(0xFF020617))
                    )
                )
                .padding(24.dp)
        ) {
            // Mode selectors and current feedback message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentCritique.contains("Warning")) Icons.Default.Info else Icons.Default.Mic,
                        contentDescription = "Critique Icon",
                        tint = if (currentCritique.contains("Warning")) Color(0xFFEF4444) else Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = currentCritique,
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Simulation mode toggle + Camera Status info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasCameraPermission) Icons.Default.Videocam else Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = if (hasCameraPermission) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasCameraPermission) "Camera Feed Active" else "Camera Blocked",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Simulate Reps", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isSimulationMode,
                        onCheckedChange = { isSimulationMode = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF064E3B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Start/Stop Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isWorkoutActive) {
                    Button(
                        onClick = { isWorkoutActive = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("start_workout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Workout Session", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = {
                            isWorkoutActive = false
                            // Save session statistics in DB!
                            viewModel.completeWorkout(
                                exerciseName = exerciseName,
                                durationSeconds = totalSeconds,
                                reps = repCount,
                                formScore = averageFormScore,
                                feedback = if (repCount > 0) "Great session! Averaged ${averageFormScore.toInt()}% form safety." else "Workout completed."
                            )
                            viewModel.speak("Session finished! Beautiful job. Your statistics have been synced offline.")
                            onBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("finish_workout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finish & Save Progress", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// BorderStroke helper
@Composable
fun BorderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)
