package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.network.RetrofitClient
import com.example.ui.MainViewModel
import com.example.ui.components.WorkoutAnimator
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PremiumGrayBorder
import com.example.ui.theme.PremiumGrayDark
import com.example.ui.theme.PremiumGrayMedium
import com.example.vision.MONTAGE_COLS
import com.example.vision.MONTAGE_ROWS
import com.example.vision.PoseSkeleton
import com.example.vision.bitmapToBase64
import com.example.vision.buildMontage
import com.example.vision.drainFrames
import com.example.vision.imageProxyToUprightBitmap
import com.example.vision.loadDemoFrames
import com.example.vision.parseAnalysis
import com.example.vision.pushFrame
import com.example.vision.sampleEvenly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * PRD v2 (Lane B): orchestrates today's multi-exercise class using the SAME real vision
 * pipeline as [PoseTrackerScreen] for EVERY exercise in the plan — front camera via CameraX,
 * ML Kit pose bones drawn in red, montage frames sent to Gemini via the Firebase proxy
 * ([RetrofitClient.analyzeFrame], never a direct API key), and TTS coaching cues. Reps/sets
 * are counted by Gemini reading the montage, exactly as in the single-exercise screen. Once
 * an exercise's target sets are reached (or the user taps "Next exercise"), the result is
 * recorded and the class advances; after the last exercise the class is persisted and
 * [onClassFinished] navigates to the results table.
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
    val targetSets = (currentExercise?.targetSets ?: 3).coerceAtLeast(1)
    val targetReps = (currentExercise?.targetReps ?: 12).coerceAtLeast(1)
    val isLast = currentIndex >= exercises.size - 1

    // Keep the latest navigation callback available to the long-lived Gemini analysis
    // coroutine below (which is NOT re-launched on every recomposition), so it always
    // navigates using the current lambda instance rather than one captured at launch.
    val latestOnClassFinished by rememberUpdatedState(onClassFinished)

    // Honest demo feed: when a flag file is present, cycle bundled squat keyframes
    // through the SAME real analyzeFrame -> Gemini pipeline instead of the live camera.
    // Lets the class flow be demonstrated where no real camera/person is available
    // (e.g. emulator). When the flag is absent, production behavior is unchanged: the
    // live front camera always drives the class.
    val demoMode = remember {
        java.io.File(context.getExternalFilesDir(null), "demo_feed").exists()
    }
    val demoFrames = remember { if (demoMode) loadDemoFrames(context) else emptyList() }
    var demoIndex by remember { mutableIntStateOf(0) }
    var demoAnnotatedFrames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    // Camera permission requested once for the whole class (not per-exercise).
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
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

    // Whole-class active flag: camera + Gemini analysis run continuously across every
    // exercise once the user taps "Start Class"; only the per-exercise counters below reset.
    var isClassActive by remember { mutableStateOf(false) }

    // Per-exercise workout state — reset whenever the class advances to a new exercise.
    var repCount by remember { mutableIntStateOf(0) }
    var setCount by remember { mutableIntStateOf(0) }
    var totalSeconds by remember { mutableIntStateOf(0) }
    var averageFormScore by remember { mutableStateOf(0.0) }
    var currentCritique by remember { mutableStateOf("Position yourself in front of the camera to begin.") }
    var isFormWarning by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var formSamples by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentIndex) {
        repCount = 0
        setCount = 0
        totalSeconds = 0
        averageFormScore = 0.0
        formSamples = 0
        isFormWarning = false
        currentCritique = "Get ready for $exerciseName! ${targetSets} sets of $targetReps reps."
    }

    // Session-wide camera/ML-Kit plumbing, shared across every exercise in the class —
    // mirrors PoseTrackerScreen exactly so the class uses the SAME real pipeline.
    val frameRing = remember { ConcurrentLinkedDeque<Pair<Long, Bitmap>>() }
    val analyzerActive = remember { AtomicBoolean(false) }
    val lastConvertMs = remember { AtomicLong(0L) }
    var cameraLens by remember { mutableIntStateOf(CameraSelector.LENS_FACING_FRONT) }
    // ML Kit draws bones only — never counts reps. One streaming detector, reused
    // across every live camera frame for the whole class, closed when the class ends.
    val streamPoseDetector = remember { PoseSkeleton.createStreamDetector() }
    DisposableEffect(Unit) { onDispose { streamPoseDetector.close() } }
    // Latest bones-annotated live frame, shown in the PiP self-view so the user
    // sees the red skeleton ML Kit drew on themselves in real time.
    var pipDisplayBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(isClassActive) { analyzerActive.set(isClassActive) }

    // Pre-annotate the bundled demo frames once (off the main thread) with the same
    // red skeleton the live camera path draws, so the demo feed looks and montages
    // identically to a real session.
    LaunchedEffect(demoMode) {
        if (demoMode && demoFrames.isNotEmpty()) {
            demoAnnotatedFrames = withContext(Dispatchers.Default) {
                val detector = PoseSkeleton.createSingleImageDetector()
                try {
                    demoFrames.map { (bitmap, _) -> PoseSkeleton.annotate(detector, bitmap) }
                } finally {
                    detector.close()
                }
            }
        }
    }

    // Demo feed driver: advances the displayed squat keyframe smoothly for the
    // viewer and pushes the (pre-annotated, bones-drawn) frame into the SAME
    // frameRing the montage loop consumes, throttled to match the live-camera
    // capture cadence — runs for the whole class, independent of which exercise is active.
    LaunchedEffect(isClassActive, demoMode) {
        if (!demoMode || demoFrames.isEmpty() || !isClassActive) return@LaunchedEffect
        var lastFed = 0L
        while (isClassActive) {
            demoIndex = (demoIndex + 1) % demoFrames.size
            val now = System.currentTimeMillis()
            if (now - lastFed >= 700) {
                val annotated = demoAnnotatedFrames.getOrNull(demoIndex) ?: demoFrames[demoIndex].first
                pushFrame(frameRing, now, annotated)
                lastFed = now
            }
            delay(360)
        }
    }

    // Elapsed-time ticker for the CURRENT exercise.
    LaunchedEffect(isClassActive, currentIndex) {
        if (isClassActive) {
            viewModel.speak("Next up: $exerciseName. Let's go!")
            while (isClassActive) {
                delay(1000)
                totalSeconds++
            }
        }
    }

    // Montage Gemini analysis loop for the CURRENT exercise: ML Kit already drew the red
    // skeleton on every buffered frame (drawing only — it never counts reps). This loop
    // composes a 2x3 grid of the frames captured since the LAST call into ONE JPEG (keeping
    // the proxy's single-image contract) and asks Gemini to read the MOTION across that
    // window: how many complete reps finished, and whether a set boundary (a clear
    // rest/pause) occurred. Restarts fresh (new prompt, cleared ring) every time the class
    // advances to a new exercise. Once the exercise's target sets are reached, the result is
    // recorded and the class auto-advances — same as tapping "Next exercise" manually.
    LaunchedEffect(isClassActive, currentIndex) {
        if (!isClassActive) return@LaunchedEffect
        val exercise = currentExercise ?: return@LaunchedEffect
        val name = exercise.name
        val sets = targetSets
        val reps = targetReps
        frameRing.clear()
        val systemPrompt = "You are an expert fitness form coach analyzing a MONTAGE image for a user " +
            "performing $name as part of a guided multi-exercise class (target: $sets sets of $reps reps). " +
            "The montage is a 2-column by 3-row grid of up to 6 frames, each already annotated with a RED " +
            "pose-skeleton overlay, ordered chronologically top-left to bottom-right and spanning roughly " +
            "the last 2 seconds. Read the SEQUENCE across the grid (not any single cell) to judge motion: " +
            "one rep is one full movement cycle of $name (e.g. descent and return to the start position). " +
            "Respond ONLY with compact minified JSON (no markdown, no prose) with exactly these keys: " +
            "person_detected (boolean, true if a person is visible in the frames), reps_in_window " +
            "(integer 0-3, COMPLETE reps of $name that finished during this window), set_complete " +
            "(boolean, true only if the user has clearly paused or is resting, marking the end of a set), " +
            "form_score (integer 0-100, form quality across the window), critique (string, max 8 words, " +
            "one actionable coaching cue)."
        var lastSpokenCritique = ""
        var lastCritiqueSpokenAt = 0L
        var wasSetComplete = false
        while (isClassActive) {
            val batch = drainFrames(frameRing)
            if (batch.isEmpty()) { delay(300); continue }
            val montage = buildMontage(sampleEvenly(batch, MONTAGE_COLS * MONTAGE_ROWS))
            val imageBase64 = bitmapToBase64(montage, 70)
            isAnalyzing = true
            val raw = withContext(Dispatchers.IO) {
                RetrofitClient.analyzeFrame(
                    imageBase64 = imageBase64,
                    prompt = "Analyze this $name montage window and report reps/set/form.",
                    systemPrompt = systemPrompt
                )
            }
            isAnalyzing = false
            val json = raw?.let { parseAnalysis(it) }
            if (json != null) {
                val person = json.optBoolean("person_detected", false)
                val repsInWindow = json.optInt("reps_in_window", 0).coerceIn(0, 3)
                val setComplete = json.optBoolean("set_complete", false)
                val score = json.optInt("form_score", 0)
                val critique = json.optString("critique", "").ifBlank { "Analyzing your form..." }
                if (person) {
                    currentCritique = critique
                    isFormWarning = score in 1..79
                    averageFormScore = (averageFormScore * formSamples + score) / (formSamples + 1)
                    formSamples++
                    // Gemini counts reps directly from the montage; a window can
                    // legitimately contain 0-3 reps.
                    if (repsInWindow > 0) {
                        repCount += repsInWindow
                        viewModel.speak(if (score >= 80) "Rep $repCount. Good form." else "Rep $repCount. Watch your form.")
                    }
                    // Debounce the set boundary on the false->true edge only, so one
                    // rest period increments setCount exactly once no matter how many
                    // montage windows the rest spans.
                    if (setComplete && !wasSetComplete) {
                        setCount++
                        viewModel.speak("Set $setCount of $sets complete.", android.speech.tts.TextToSpeech.QUEUE_ADD)
                    }
                    wasSetComplete = setComplete
                    val now = System.currentTimeMillis()
                    if (critique != lastSpokenCritique && critique != "Analyzing your form..." &&
                        now - lastCritiqueSpokenAt > 4000) {
                        viewModel.speak(critique, android.speech.tts.TextToSpeech.QUEUE_ADD)
                        lastSpokenCritique = critique
                        lastCritiqueSpokenAt = now
                    }
                    // Target sets reached: record the result and let the coach prompt
                    // the next exercise (PRD-v2 point 5), exactly like the manual
                    // "Next exercise" button below.
                    if (setCount >= sets) {
                        val formInt = averageFormScore.toInt()
                        val points = (repCount * (averageFormScore / 100.0) * 15).toInt() + 10
                        viewModel.recordExerciseResult(
                            name = name,
                            reps = repCount,
                            sets = setCount,
                            formScore = formInt,
                            points = points
                        )
                        val hasMore = viewModel.advanceClass()
                        if (!hasMore) {
                            viewModel.speak("Class complete! Amazing work.")
                            latestOnClassFinished(viewModel.finishTodaysClass())
                        } else {
                            viewModel.speak("Great work on $name! Moving to the next exercise.")
                        }
                        break
                    }
                } else {
                    currentCritique = "Position yourself fully in frame to begin."
                    isFormWarning = false
                    wasSetComplete = false
                }
            }
            delay(250)
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
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- LIVE CAMERA WITH GEMINI FRAME ANALYSIS (or bundled demo feed) ---
        if (demoMode && demoFrames.isNotEmpty()) {
            val demoFrame = demoAnnotatedFrames.getOrNull(demoIndex) ?: demoFrames[demoIndex].first
            Image(
                bitmap = demoFrame.asImageBitmap(),
                contentDescription = "Demo workout feed",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (hasCameraPermission) {
            // Live camera is shown in the small self-view PiP (top-right); keep a dark
            // analysis backdrop here so the HUD reads clearly.
            Box(modifier = Modifier.fillMaxSize().background(ObsidianBlack))
        } else {
            // Placeholder animator shown only when the camera permission is denied.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 260.dp)
            ) {
                Text(
                    text = "HOW TO: ${exerciseName.uppercase()}",
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("class_animation_label")
                )
                WorkoutAnimator(
                    exerciseName = exerciseName,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("class_workout_animator"),
                    isWarningMode = isFormWarning && isClassActive
                )
            }
        }

        // --- CYBERPUNK HUD OVERLAY GRAPHICS ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scanY = 60f + (size.height - 120f - 260.dp.toPx()) * laserY
            if (isClassActive) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent)
                    ),
                    start = Offset(60f, scanY),
                    end = Offset(size.width - 60f, scanY),
                    strokeWidth = 6f
                )
            }
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

        // --- SMALL LIVE CAMERA SELF-VIEW (PiP) + FRONT/BACK FLIP ---
        if (hasCameraPermission && !demoMode) {
            val pipExecutor = remember { Executors.newSingleThreadExecutor() }
            DisposableEffect(Unit) { onDispose { pipExecutor.shutdown() } }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 118.dp, end = 16.dp)
                    .size(width = 104.dp, height = 150.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(2.dp, Color.White, RoundedCornerShape(14.dp))
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
                                            if (analyzerActive.get() && now - lastConvertMs.get() >= 700) {
                                                val bitmap = imageProxyToUprightBitmap(imageProxy, 480)
                                                if (bitmap != null) {
                                                    // ML Kit DRAWS the red skeleton only — it never
                                                    // counts reps. The annotated frame is shown live
                                                    // in this PiP AND buffered for Gemini's montage.
                                                    val annotated = PoseSkeleton.annotate(streamPoseDetector, bitmap)
                                                    pipDisplayBitmap = annotated
                                                    pushFrame(frameRing, now, annotated)
                                                    lastConvertMs.set(now)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("TodaysClass", "PiP frame conversion failed", e)
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
                                    Log.e("TodaysClass", "PiP camera binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            pv
                        }
                    )
                }
                // Overlay the latest bones-annotated frame on top of the raw preview so
                // the user visibly sees the red skeleton ML Kit drew on themselves.
                pipDisplayBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Live pose skeleton",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Text(
                    text = "LIVE",
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
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
                        .background(PremiumGrayDark.copy(alpha = 0.8f), CircleShape)
                        .testTag("flip_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch front/back camera",
                        tint = Color.White,
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
            colors = CardDefaults.cardColors(containerColor = PremiumGrayDark.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Exercise ${currentIndex + 1} of ${exercises.size}",
                    fontSize = 11.sp,
                    color = Color.White,
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
                        Text("TIME", fontSize = 9.sp, color = PremiumGrayMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60),
                            fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Black
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("REPS", fontSize = 9.sp, color = PremiumGrayMedium, fontWeight = FontWeight.Bold)
                        Text("$repCount", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SETS", fontSize = 9.sp, color = PremiumGrayMedium, fontWeight = FontWeight.Bold)
                        Text("$setCount/$targetSets", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Black)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("FORM", fontSize = 9.sp, color = PremiumGrayMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = String.format("%.0f%%", averageFormScore),
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Black
                        )
                    }
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
                        colors = listOf(Color.Transparent, ObsidianBlack.copy(alpha = 0.95f), ObsidianBlack)
                    )
                )
                .padding(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PremiumGrayDark.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PremiumGrayBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isFormWarning) Icons.Default.Info else Icons.Default.Mic,
                        contentDescription = "Critique",
                        // Red is the PRD-mandated form-critique color, tied to the pose warning state.
                        tint = if (isFormWarning) Color(0xFFEF4444) else Color.White,
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

            Spacer(modifier = Modifier.height(12.dp))

            // Camera Status + Gemini live-analysis status row.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasCameraPermission || demoMode) Icons.Default.Videocam else Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = if (hasCameraPermission || demoMode) Color.White else PremiumGrayMedium,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (demoMode) "Demo Feed • Live AI" else if (hasCameraPermission) "Camera Feed Active" else "Camera Blocked",
                        fontSize = 12.sp,
                        color = PremiumGrayMedium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAnalyzing) "AI analyzing…" else "Gemini Live Coach",
                        fontSize = 12.sp,
                        color = if (isAnalyzing) Color.White else PremiumGrayMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Live analysis",
                        tint = if (isAnalyzing) Color.White else PremiumGrayMedium,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isClassActive) {
                Button(
                    onClick = { isClassActive = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_class_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Class", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            } else {
                Button(
                    onClick = {
                        // Manual override: lock in the current exercise's real,
                        // Gemini-measured reps/sets/form and advance now, in case the
                        // user wants to move on before the set_complete rest is detected.
                        val formInt = averageFormScore.toInt()
                        val setsCompleted = setCount.coerceAtLeast(if (repCount > 0) 1 else 0)
                        val points = (repCount * (averageFormScore / 100.0) * 15).toInt() + 10
                        viewModel.recordExerciseResult(
                            name = exerciseName,
                            reps = repCount,
                            sets = setsCompleted,
                            formScore = formInt,
                            points = points
                        )
                        val hasMore = viewModel.advanceClass()
                        if (!hasMore) {
                            viewModel.speak("Class complete! Amazing work.")
                            onClassFinished(viewModel.finishTodaysClass())
                        } else {
                            viewModel.speak("Nice work! Next up: ${viewModel.currentClassExerciseName}.")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("next_exercise"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isLast) Icons.Default.PlayArrow else Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLast) "Set done — Finish class" else "Set done — Next exercise",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
