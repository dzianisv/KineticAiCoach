package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.MainViewModel
import com.example.ui.components.WorkoutAnimator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PRD v2 (Lane B): orchestrates today's multi-exercise class.
 * Reuses the SAME simulated rep-count workout as PoseTrackerScreen for each exercise.
 * "Set done / Next exercise" records the current sim result and advances; after the last
 * exercise the class is persisted and [onClassFinished] navigates to the results table.
 */
@Composable
fun TodaysClassScreen(
    viewModel: MainViewModel,
    onClassFinished: (Int) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exercises by viewModel.todaysClass.collectAsState()
    val currentIndex by viewModel.currentClassIndex.collectAsState()
    val currentExercise = exercises.getOrNull(currentIndex)
    val exerciseName = currentExercise?.name ?: "Workout"

    // The class always runs in simulation mode (see isSimulationMode below), so
    // the camera preview is never shown and no CAMERA permission is needed.
    // Requesting it here previously popped a system permission dialog over the
    // class, which could swallow taps on the finish button.
    val hasCameraPermission = false

    // Per-exercise simulated workout state
    var repCount by remember { mutableIntStateOf(0) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var averageFormScore by remember { mutableStateOf(95.0) }
    var currentCritique by remember { mutableStateOf("Get ready! Tap start to begin this exercise.") }
    val isSimulationMode = true // Class always uses the simulated workout

    // Reset the simulation whenever we move to a new exercise
    LaunchedEffect(currentIndex) {
        repCount = 0
        totalSeconds = 0
        averageFormScore = 95.0
        currentCritique = "Get ready for $exerciseName! Tap start to begin."
    }

    // Simulation loop — mirrors PoseTrackerScreen's canned rep counting / critiques
    LaunchedEffect(currentIndex) {
        viewModel.speak("Next up: $exerciseName. Let's go!")
        delay(500)
        while (true) {
            delay(1000)
            totalSeconds++
            if (isSimulationMode && totalSeconds % 6 == 0) {
                repCount++
                val performanceScore = if (repCount % 3 == 0) 84.0 else 96.0
                averageFormScore = (averageFormScore * repCount + performanceScore) / (repCount + 1)
                currentCritique = if (repCount % 3 == 0) {
                    "Warning: Keep your back straight! Lower hips slowly."
                } else {
                    "Perfect $exerciseName technique! Rep $repCount counted."
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanning")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(animation = tween(2000), repeatMode = RepeatMode.Reverse),
        label = "laser"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Camera preview or animated avatar (simulation mode -> avatar)
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
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview
                            )
                        } catch (e: Exception) {
                            Log.e("TodaysClass", "Camera binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 260.dp)
            ) {
                WorkoutAnimator(
                    exerciseName = exerciseName,
                    modifier = Modifier.fillMaxSize(),
                    isWarningMode = currentCritique.contains("Warning")
                )
            }
        }

        // Scanning laser HUD
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanY = 60f + (size.height - 120f - 260.dp.toPx()) * laserY
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF00F5FF).copy(alpha = 0.8f), Color.Transparent)
                ),
                start = Offset(60f, scanY),
                end = Offset(size.width - 60f, scanY),
                strokeWidth = 6f
            )
        }

        // Exit button
        IconButton(
            onClick = onExit,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.6f)),
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .testTag("class_exit_button")
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Exit class", tint = Color.White)
        }

        // Top stats + progress panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 72.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Exercise ${currentIndex + 1} of ${exercises.size}",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = exerciseName,
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.testTag("class_exercise_name")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TIME", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60),
                            fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REPS", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text("$repCount", fontSize = 20.sp, color = Color(0xFF22C55E), fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("FORM", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%.0f%%", averageFormScore),
                            fontSize = 16.sp,
                            color = if (averageFormScore >= 90.0) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Bottom action panel
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (currentCritique.contains("Warning")) Icons.Default.Info else Icons.Default.Mic,
                        contentDescription = "Critique",
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

            val isLast = currentIndex >= exercises.size - 1
            Button(
                onClick = {
                    // Record the current simulated result using the same points formula as the repository
                    val formInt = averageFormScore.toInt()
                    val points = (repCount * (averageFormScore / 100.0) * 15).toInt() + 10
                    viewModel.recordExerciseResult(
                        name = exerciseName,
                        reps = repCount,
                        formScore = formInt,
                        points = points
                    )
                    val hasMore = viewModel.advanceClass()
                    if (!hasMore) {
                        // Navigate immediately; persistence runs in the background.
                        viewModel.speak("Class complete! Amazing work.")
                        onClassFinished(viewModel.finishTodaysClass())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("next_exercise"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLast) Color(0xFF10B981) else Color(0xFF2563EB)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isLast) Icons.Default.PlayArrow else Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLast) "Set done — Finish class" else "Set done — Next exercise",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
