package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WorkoutAnimator(
    exerciseName: String,
    modifier: Modifier = Modifier,
    isWarningMode: Boolean = false
) {
    // Animate a float from 0f to 1f to represent the cycle of the exercise
    val infiniteTransition = rememberInfiniteTransition(label = "workout_animation")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing)
        ),
        label = "progress"
    )

    // Sleek dark tech background
    Box(
        modifier = modifier
            .background(Color(0xFF0F172A), shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2

            // Custom joint colors (glowing cyan / neon green vs alert warning red)
            val boneColor = if (isWarningMode) Color(0xFFEF4444) else Color(0xFF10B981)
            val jointColor = Color(0xFF38BDF8)
            val strokeWidth = 8f

            // Map animationProgress (0..1) to a smooth sin wave (0..1..0) for a natural up/down motion
            val t = (sin(animationProgress * 2 * Math.PI - Math.PI / 2) + 1.0) / 2.0 // ranges from 0.0 to 1.0

            when (exerciseName.lowercase()) {
                "squats" -> {
                    // SQUAT ANIMATION
                    // Standing state (t=0) vs deepest squat state (t=1)
                    val headY = centerY - 120f + (t * 60f).toFloat()
                    val neckY = headY + 30f
                    val shoulderY = neckY
                    val hipY = centerY + 30f + (t * 90f).toFloat()
                    val kneeY = centerY + 110f + (t * 50f).toFloat()
                    val ankleY = centerY + 180f

                    val shoulderWidth = 45f
                    val hipWidth = 35f
                    val ankleWidth = 50f
                    val kneeWidth = 35f + (t * 40f).toFloat() // Knees push out slightly in squat

                    // Core coordinates
                    val head = Offset(centerX, headY)
                    val neck = Offset(centerX, neckY)
                    val shoulderL = Offset(centerX - shoulderWidth, shoulderY)
                    val shoulderR = Offset(centerX + shoulderWidth, shoulderY)
                    val spineBase = Offset(centerX, hipY)
                    val hipL = Offset(centerX - hipWidth, hipY)
                    val hipR = Offset(centerX + hipWidth, hipY)

                    // Knees & Feet
                    val kneeL = Offset(centerX - kneeWidth, kneeY)
                    val kneeR = Offset(centerX + kneeWidth, kneeY)
                    val footL = Offset(centerX - ankleWidth, ankleY)
                    val footR = Offset(centerX + ankleWidth, ankleY)

                    // Arms (folded forward for balance in squat)
                    val elbowL = Offset(centerX - shoulderWidth - 20f, shoulderY + 40f - (t * 20f).toFloat())
                    val elbowR = Offset(centerX + shoulderWidth + 20f, shoulderY + 40f - (t * 20f).toFloat())
                    val handL = Offset(centerX - 30f, shoulderY + 20f - (t * 30f).toFloat())
                    val handR = Offset(centerX + 30f, shoulderY + 20f - (t * 30f).toFloat())

                    // Draw Grid lines background for high-tech holographic look
                    for (i in 1..4) {
                        val gridY = (height / 5) * i
                        drawLine(Color(0xFF334155).copy(alpha = 0.3f), Offset(0f, gridY), Offset(width, gridY), 2f)
                    }

                    // Draw Head
                    drawCircle(jointColor, radius = 25f, center = head, style = Stroke(width = 4f))
                    drawCircle(boneColor.copy(alpha = 0.2f), radius = 23f, center = head)

                    // Connect Spine
                    drawLine(boneColor, neck, spineBase, strokeWidth, StrokeCap.Round)

                    // Connect Shoulders
                    drawLine(boneColor, shoulderL, shoulderR, strokeWidth, StrokeCap.Round)

                    // Connect Shoulders to Hip Joints
                    drawLine(boneColor, shoulderL, hipL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, shoulderR, hipR, strokeWidth, StrokeCap.Round)

                    // Draw Arms
                    drawLine(boneColor, shoulderL, elbowL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, elbowL, handL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, shoulderR, elbowR, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, elbowR, handR, strokeWidth, StrokeCap.Round)

                    // Connect Hips to Knees to Feet (Legs)
                    drawLine(boneColor, hipL, kneeL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, kneeL, footL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, hipR, kneeR, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, kneeR, footR, strokeWidth, StrokeCap.Round)

                    // Draw glowing joints
                    val joints = listOf(neck, shoulderL, shoulderR, hipL, hipR, kneeL, kneeR, footL, footR, elbowL, elbowR, handL, handR)
                    joints.forEach { drawCircle(jointColor, radius = 6f, center = it) }
                }
                "pushups" -> {
                    // PUSHUP ANIMATION (Side Profile view)
                    // Up state (t=0) vs down state (t=1)
                    val pivotX = centerX - 120f // feet stay locked
                    val pivotY = centerY + 100f

                    // Tilt angle changes
                    val angle = 15f - (t * 22f).toFloat() // tilts down to almost flat

                    // Calculate joints relative to feet (pivot)
                    val hipLen = 140f
                    val shoulderLen = 240f
                    val headLen = 290f

                    val cosA = kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                    val sinA = kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()

                    // Joint offsets
                    val hip = Offset(pivotX + hipLen * cosA, pivotY - hipLen * sinA)
                    val shoulder = Offset(pivotX + shoulderLen * cosA, pivotY - shoulderLen * sinA)
                    val head = Offset(pivotX + headLen * cosA, pivotY - headLen * sinA)

                    // Elbow / Hand (Hand is fixed on ground at shoulder projection)
                    val handX = pivotX + shoulderLen * kotlin.math.cos(Math.toRadians(15.0)).toFloat()
                    val handY = centerY + 110f
                    val hand = Offset(handX, handY)

                    // Elbow bends out
                    val elbowX = (shoulder.x + hand.x) / 2f + (t * 30f).toFloat()
                    val elbowY = (shoulder.y + hand.y) / 2f + (t * 25f).toFloat()
                    val elbow = Offset(elbowX, elbowY)

                    // Knees
                    val knee = Offset(pivotX + (hipLen / 2f) * cosA, pivotY - (hipLen / 2f) * sinA)

                    // Grid lines
                    for (i in 1..4) {
                        val gridY = (height / 5) * i
                        drawLine(Color(0xFF334155).copy(alpha = 0.3f), Offset(0f, gridY), Offset(width, gridY), 2f)
                    }

                    // Draw Floor
                    drawLine(Color(0xFF475569), Offset(0f, handY + 10f), Offset(width, handY + 10f), 4f)

                    // Head
                    drawCircle(jointColor, radius = 23f, center = head, style = Stroke(width = 4f))
                    drawCircle(boneColor.copy(alpha = 0.2f), radius = 21f, center = head)

                    // Spine / Body line
                    drawLine(boneColor, Offset(pivotX, pivotY), hip, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, hip, shoulder, strokeWidth, StrokeCap.Round)

                    // Arms (Shoulder -> Elbow -> Hand)
                    drawLine(boneColor, shoulder, elbow, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, elbow, hand, strokeWidth, StrokeCap.Round)

                    // Connect Head to shoulder
                    drawLine(boneColor, shoulder, head, strokeWidth, StrokeCap.Round)

                    // Glowing joints
                    val joints = listOf(Offset(pivotX, pivotY), knee, hip, shoulder, elbow, hand)
                    joints.forEach { drawCircle(jointColor, radius = 6f, center = it) }
                }
                else -> {
                    // JUMPING JACKS (Front Profile view)
                    // Arms down & legs together (t=0) vs arms up & legs wide (t=1)
                    val head = Offset(centerX, centerY - 120f)
                    val neck = Offset(centerX, centerY - 95f)
                    val hip = Offset(centerX, centerY + 30f)

                    val shoulderL = Offset(centerX - 40f, centerY - 95f)
                    val shoulderR = Offset(centerX + 40f, centerY - 95f)

                    // Arms swing from -60 deg (down) to +110 deg (clapping overhead)
                    val armAngle = -65f + (t * 180f).toFloat()
                    val armRadL = Math.toRadians((180f + armAngle).toDouble())
                    val armRadR = Math.toRadians((-armAngle).toDouble())

                    val elbowL = Offset(shoulderL.x + 50f * kotlin.math.cos(armRadL).toFloat(), shoulderL.y + 50f * kotlin.math.sin(armRadL).toFloat())
                    val elbowR = Offset(shoulderR.x + 50f * kotlin.math.cos(armRadR).toFloat(), shoulderR.y + 50f * kotlin.math.sin(armRadR).toFloat())

                    val handL = Offset(elbowL.x + 45f * kotlin.math.cos(armRadL).toFloat(), elbowL.y + 45f * kotlin.math.sin(armRadL).toFloat())
                    val handR = Offset(elbowR.x + 45f * kotlin.math.cos(armRadR).toFloat(), elbowR.y + 45f * kotlin.math.sin(armRadR).toFloat())

                    // Legs open wide (ankleX moves outward by 50f)
                    val ankleSpread = 20f + (t * 55f).toFloat()
                    val kneeSpread = 15f + (t * 30f).toFloat()

                    val hipL = Offset(centerX - 20f, centerY + 30f)
                    val hipR = Offset(centerX + 20f, centerY + 30f)

                    val kneeL = Offset(centerX - kneeSpread, centerY + 105f)
                    val kneeR = Offset(centerX + kneeSpread, centerY + 105f)

                    val footL = Offset(centerX - ankleSpread, centerY + 175f)
                    val footR = Offset(centerX + ankleSpread, centerY + 175f)

                    // Grid lines
                    for (i in 1..4) {
                        val gridY = (height / 5) * i
                        drawLine(Color(0xFF334155).copy(alpha = 0.3f), Offset(0f, gridY), Offset(width, gridY), 2f)
                    }

                    // Head
                    drawCircle(jointColor, radius = 24f, center = head, style = Stroke(width = 4f))
                    drawCircle(boneColor.copy(alpha = 0.2f), radius = 22f, center = head)

                    // Spine
                    drawLine(boneColor, neck, hip, strokeWidth, StrokeCap.Round)

                    // Shoulders
                    drawLine(boneColor, shoulderL, shoulderR, strokeWidth, StrokeCap.Round)

                    // Arms
                    drawLine(boneColor, shoulderL, elbowL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, elbowL, handL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, shoulderR, elbowR, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, elbowR, handR, strokeWidth, StrokeCap.Round)

                    // Legs
                    drawLine(boneColor, hipL, kneeL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, kneeL, footL, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, hipR, kneeR, strokeWidth, StrokeCap.Round)
                    drawLine(boneColor, kneeR, footR, strokeWidth, StrokeCap.Round)

                    // Joints
                    val joints = listOf(neck, shoulderL, shoulderR, hipL, hipR, kneeL, kneeR, footL, footR, elbowL, elbowR, handL, handR)
                    joints.forEach { drawCircle(jointColor, radius = 6f, center = it) }
                }
            }
        }
    }
}
