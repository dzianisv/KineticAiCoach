package com.example.vision

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * DRAWING-ONLY helper around ML Kit's on-device pose detector. This file never
 * infers reps, sets, or exercise phase — it only locates the 33 ML Kit pose
 * landmarks in a frame and paints a RED skeleton over them. Rep/set counting is
 * Gemini's job, done from a montage of these annotated frames (see
 * PoseTrackerScreen's montage loop), per PRD-v2.
 */
object PoseSkeleton {
    private const val TAG = "PoseSkeleton"

    // Pure red, per PRD ("red bones/skeleton overlay").
    private const val BONE_COLOR = 0xFFFF3B30.toInt()

    /** Bone connections drawn between standard ML Kit pose landmarks. */
    private val CONNECTIONS = listOf(
        // Arms
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
        PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
        // Shoulders / torso / hips
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
        PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
        // Legs
        PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
        PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
        PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
        PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
    )

    /** Joints where a small red dot is drawn on top of the bone lines. */
    private val JOINTS = listOf(
        PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER,
        PoseLandmark.LEFT_ELBOW, PoseLandmark.RIGHT_ELBOW,
        PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST,
        PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP,
        PoseLandmark.LEFT_KNEE, PoseLandmark.RIGHT_KNEE,
        PoseLandmark.LEFT_ANKLE, PoseLandmark.RIGHT_ANKLE
    )

    /**
     * Creates a streaming pose detector (base/fast model) tuned for a live camera
     * feed: call [annotate] with the SAME instance for every frame from one thread,
     * and [PoseDetector.close] it when the analyzer is torn down.
     */
    fun createStreamDetector(): PoseDetector =
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                .build()
        )

    /**
     * Creates a single-image pose detector, suited for annotating a fixed set of
     * independent still images (e.g. the bundled demo frames) rather than a stream.
     */
    fun createSingleImageDetector(): PoseDetector =
        PoseDetection.getClient(
            PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build()
        )

    /**
     * Runs pose detection on an upright [bitmap] (rotationDegrees=0) and returns a
     * NEW bitmap with a red skeleton drawn over any detected person. This call
     * blocks the calling thread until ML Kit finishes (via [Tasks.await]) — only
     * ever invoke it from a background executor/dispatcher, never the main thread.
     * Returns the original [bitmap] unchanged when no person is detected or
     * detection fails, so callers can always safely display/forward the result.
     */
    fun annotate(detector: PoseDetector, bitmap: Bitmap): Bitmap {
        val pose: Pose = try {
            Tasks.await(detector.process(InputImage.fromBitmap(bitmap, 0)))
        } catch (e: Exception) {
            Log.w(TAG, "Pose detection failed; returning frame unannotated", e)
            return bitmap
        }
        if (pose.allPoseLandmarks.isEmpty()) return bitmap

        val out = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val boneStroke = maxOf(4f, out.width / 90f)
        val bonePaint = Paint().apply {
            color = BONE_COLOR
            strokeWidth = boneStroke
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val jointPaint = Paint().apply {
            color = BONE_COLOR
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val jointRadius = boneStroke * 0.9f

        for ((startType, endType) in CONNECTIONS) {
            val start = pose.getPoseLandmark(startType)
            val end = pose.getPoseLandmark(endType)
            if (start != null && end != null) {
                canvas.drawLine(start.position.x, start.position.y, end.position.x, end.position.y, bonePaint)
            }
        }
        for (type in JOINTS) {
            pose.getPoseLandmark(type)?.let { landmark ->
                canvas.drawCircle(landmark.position.x, landmark.position.y, jointRadius, jointPaint)
            }
        }
        return out
    }
}
