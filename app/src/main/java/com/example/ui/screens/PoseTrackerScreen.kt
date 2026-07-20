package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.key
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Composable
fun PoseTrackerScreen(
    exerciseName: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Honest demo feed: when a flag file is present, cycle bundled squat keyframes
    // through the SAME real analyzeFrame -> Gemini pipeline instead of the live camera.
    // Lets the workout screen be demonstrated where no real camera/person is available
    // (e.g. emulator). When the flag is absent, production behavior is unchanged.
    val demoMode = remember {
        java.io.File(context.getExternalFilesDir(null), "demo_feed").exists()
    }
    val demoFrames = remember { if (demoMode) loadDemoFrames(context) else emptyList() }
    var demoIndex by remember { mutableIntStateOf(0) }

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
    var averageFormScore by remember { mutableStateOf(0.0) }
    var currentCritique by remember { mutableStateOf("Position yourself in front of the camera to begin.") }
    var isFormWarning by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Non-recomposing handoff between the CameraX analyzer thread and the coroutine
    // that talks to Gemini: the analyzer drops the freshest JPEG frame here and the
    // analysis loop consumes it. Decouples capture rate from network latency.
    val latestFrame = remember { AtomicReference<String?>(null) }
    val analyzerActive = remember { AtomicBoolean(false) }
    val lastConvertMs = remember { AtomicLong(0L) }
    var reachedBottom by remember { mutableStateOf(false) }
    var formSamples by remember { mutableIntStateOf(0) }
    // Front/back lens for the small live self-view camera (PiP); flip cycles the two.
    var cameraLens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }

    LaunchedEffect(isWorkoutActive) { analyzerActive.set(isWorkoutActive) }

    // Elapsed-time ticker
    LaunchedEffect(isWorkoutActive) {
        if (isWorkoutActive) {
            viewModel.speak("Starting $exerciseName tracker. Let's do this!")
            while (isWorkoutActive) {
                delay(1000)
                totalSeconds++
            }
        }
    }

    // Live Gemini form-analysis loop: pulls the freshest frame and asks Gemini to
    // grade form and report the movement phase. Reps are counted client-side from
    // bottom->top phase transitions (LLMs are unreliable at counting directly).
    LaunchedEffect(isWorkoutActive) {
        if (!isWorkoutActive) return@LaunchedEffect
        val systemPrompt = "You are an expert fitness form coach analyzing ONE still video frame of a " +
            "user performing $exerciseName. Respond ONLY with compact minified JSON (no markdown, no prose) " +
            "with exactly these keys: person_detected (boolean), phase (one of \"top\",\"bottom\",\"mid\",\"none\"), " +
            "form_score (integer 0-100), critique (string, max 8 words, one actionable coaching cue). " +
            "phase is the user's position in the $exerciseName movement: top=start/standing, " +
            "bottom=deepest/hardest point, mid=in transition, none=no person visible."
        var lastSpokenCritique = ""
        var lastCritiqueSpokenAt = 0L
        while (isWorkoutActive) {
            val frame = latestFrame.getAndSet(null)
            if (frame == null) { delay(200); continue }
            isAnalyzing = true
            val raw = withContext(Dispatchers.IO) {
                RetrofitClient.analyzeFrame(
                    imageBase64 = frame,
                    prompt = "Analyze this $exerciseName frame.",
                    systemPrompt = systemPrompt
                )
            }
            isAnalyzing = false
            val json = raw?.let { parseAnalysis(it) }
            if (json != null) {
                val person = json.optBoolean("person_detected", false)
                val phase = json.optString("phase", "none")
                val score = json.optInt("form_score", 0)
                val critique = json.optString("critique", "").ifBlank { "Analyzing your form..." }
                if (person) {
                    currentCritique = critique
                    isFormWarning = score in 1..79
                    averageFormScore = (averageFormScore * formSamples + score) / (formSamples + 1)
                    formSamples++
                    // Speak the live coaching cue aloud: only when it changes and at most
                    // every ~4s, queued (not flushed) so rep announcements take priority.
                    val now = System.currentTimeMillis()
                    if (critique != lastSpokenCritique && critique != "Analyzing your form..." &&
                        now - lastCritiqueSpokenAt > 4000) {
                        viewModel.speak(critique, android.speech.tts.TextToSpeech.QUEUE_ADD)
                        lastSpokenCritique = critique
                        lastCritiqueSpokenAt = now
                    }
                    // Count a rep on a full descent->stand cycle: once the lifter has
                    // clearly left the top (bottom OR mid) and then returns to top.
                    // (Real footage often reads as "mid" at depth rather than a strict
                    // "bottom", so we key off leaving/returning to the top position.)
                    when (phase) {
                        "bottom", "mid" -> reachedBottom = true
                        "top" -> if (reachedBottom) {
                            reachedBottom = false
                            repCount++
                            viewModel.speak(if (score >= 80) "Rep $repCount. Good form." else "Rep $repCount. Watch your form.")
                        }
                    }
                } else {
                    currentCritique = "Position yourself fully in frame to begin."
                    isFormWarning = false
                }
            }
            delay(250)
        }
    }

    // Demo feed driver: advances the displayed squat keyframe smoothly for the viewer
    // and drops the current frame into latestFrame for the real Gemini analysis loop
    // to consume (throttled to match the live-camera capture cadence).
    LaunchedEffect(isWorkoutActive, demoMode) {
        if (!demoMode || demoFrames.isEmpty() || !isWorkoutActive) return@LaunchedEffect
        var lastFed = 0L
        while (isWorkoutActive) {
            demoIndex = (demoIndex + 1) % demoFrames.size
            val now = System.currentTimeMillis()
            if (now - lastFed >= 700) {
                latestFrame.set(demoFrames[demoIndex].second)
                lastFed = now
            }
            delay(360)
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
        // --- LIVE CAMERA WITH GEMINI FRAME ANALYSIS (or bundled demo feed) ---
        if (demoMode && demoFrames.isNotEmpty()) {
            Image(
                bitmap = demoFrames[demoIndex].first.asImageBitmap(),
                contentDescription = "Demo workout feed",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasCameraPermission) {
            // Live camera now shown in the small self-view PiP (top-right); keep a
            // dark analysis backdrop here so the HUD reads clearly.
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020617)))
        } else {
            // Placeholder animator shown only when the camera permission is denied.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 240.dp)
            ) {
                WorkoutAnimator(
                    exerciseName = exerciseName,
                    modifier = Modifier.fillMaxSize(),
                    isWarningMode = isFormWarning && isWorkoutActive
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

        // --- SMALL LIVE CAMERA SELF-VIEW (PiP) + FRONT/BACK FLIP ---
        if (hasCameraPermission) {
            val pipExecutor = remember { Executors.newSingleThreadExecutor() }
            DisposableEffect(Unit) { onDispose { pipExecutor.shutdown() } }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 118.dp, end = 16.dp)
                    .size(width = 104.dp, height = 150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
            ) {
                key(cameraLens) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            val pv = PreviewView(ctx)
                            val future = ProcessCameraProvider.getInstance(ctx)
                            future.addListener({
                                try {
                                    val provider = future.get()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.surfaceProvider = pv.surfaceProvider
                                    }
                                    val analysis = ImageAnalysis.Builder()
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                        .build()
                                    analysis.setAnalyzer(pipExecutor) { imageProxy ->
                                        try {
                                            val now = System.currentTimeMillis()
                                            // In demo mode analysis comes from the bundled squat
                                            // frames, so the live camera is preview-only here.
                                            if (analyzerActive.get() && !demoMode && now - lastConvertMs.get() >= 700) {
                                                val b64 = imageProxyToBase64(imageProxy, 512)
                                                if (b64 != null) {
                                                    latestFrame.set(b64)
                                                    lastConvertMs.set(now)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("PoseTracker", "PiP frame conversion failed", e)
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }
                                    val selector = CameraSelector.Builder()
                                        .requireLensFacing(cameraLens)
                                        .build()
                                    provider.unbindAll()
                                    provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                                } catch (e: Exception) {
                                    Log.e("PoseTracker", "PiP camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            pv
                        }
                    )
                }
                Text(
                    text = if (demoMode) "CAM" else "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color(0xAAEF4444), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                )
                IconButton(
                    onClick = {
                        cameraLens = if (cameraLens == CameraSelector.LENS_FACING_FRONT)
                            CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(34.dp)
                        .background(Color(0xCC0F172A), CircleShape)
                        .testTag("flip_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch front/back camera",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
                        imageVector = if (isFormWarning) Icons.Default.Info else Icons.Default.Mic,
                        contentDescription = "Critique Icon",
                        tint = if (isFormWarning) Color(0xFFEF4444) else Color(0xFF38BDF8),
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
                        imageVector = if (hasCameraPermission || demoMode) Icons.Default.Videocam else Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = if (hasCameraPermission || demoMode) Color(0xFF10B981) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (demoMode) "Demo Feed • Live AI" else if (hasCameraPermission) "Camera Feed Active" else "Camera Blocked",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAnalyzing) "AI analyzing…" else "Gemini Live Coach",
                        fontSize = 12.sp,
                        color = if (isAnalyzing) Color(0xFF38BDF8) else Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Live analysis",
                        tint = if (isAnalyzing) Color(0xFF38BDF8) else Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
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

// Loads bundled demo squat keyframes from assets/demo_squat as (bitmap for display,
// base64 JPEG for Gemini). Used only when the "demo_feed" flag file is present.
private fun loadDemoFrames(context: android.content.Context): List<Pair<Bitmap, String>> {
    return try {
        val dir = "demo_squat"
        val names = context.assets.list(dir)?.filter { it.endsWith(".jpg") }?.sorted() ?: emptyList()
        names.mapNotNull { name ->
            try {
                val bytes = context.assets.open("$dir/$name").use { it.readBytes() }
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (bmp != null) Pair(bmp, b64) else null
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        Log.e("PoseTracker", "loadDemoFrames failed", e)
        emptyList()
    }
}

// Converts a CameraX frame to a downscaled, upright JPEG encoded as base64 (NO_WRAP)
// suitable for sending to Gemini. Returns null on any failure.
private fun imageProxyToBase64(image: ImageProxy, maxDim: Int): String? {
    return try {
        val bitmap = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        val w = bitmap.width
        val h = bitmap.height
        val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
        val matrix = Matrix()
        if (scale < 1f) matrix.postScale(scale, scale)
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        val out = if (scale < 1f || rotation != 0)
            Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
        else bitmap
        val stream = ByteArrayOutputStream()
        out.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.e("PoseTracker", "imageProxyToBase64 failed", e)
        null
    }
}

// Tolerant JSON extraction from the model's reply (strips markdown fences / prose).
private fun parseAnalysis(raw: String): JSONObject? {
    return try {
        var s = raw.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start >= 0 && end > start) s = s.substring(start, end + 1)
        JSONObject(s)
    } catch (e: Exception) {
        Log.e("PoseTracker", "parseAnalysis failed: $raw", e)
        null
    }
}
