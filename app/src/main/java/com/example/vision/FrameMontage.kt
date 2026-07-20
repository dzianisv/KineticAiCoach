package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.camera.core.ImageProxy
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Shared camera-frame -> Gemini-montage plumbing used by BOTH [com.example.ui.screens.PoseTrackerScreen]
 * (single exercise) and [com.example.ui.screens.TodaysClassScreen] (multi-exercise class), per PRD-v2:
 * the class flow must run the SAME real vision pipeline as the single-exercise screen, not a fake
 * simulation. Extracted here so neither screen duplicates the montage/ring-buffer logic.
 */

// Cap on the number of bones-annotated frames buffered between Gemini calls.
const val FRAME_RING_CAPACITY = 12
const val MONTAGE_COLS = 2
const val MONTAGE_ROWS = 3

// Loads bundled demo squat keyframes from assets/demo_squat as (bitmap for display,
// base64 JPEG for Gemini). Used only when the "demo_feed" flag file is present.
fun loadDemoFrames(context: Context): List<Pair<Bitmap, String>> {
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
        Log.e("FrameMontage", "loadDemoFrames failed", e)
        emptyList()
    }
}

// Converts a CameraX frame to a downscaled, upright Bitmap (rotation baked in) ready
// for ML Kit pose detection and/or JPEG encoding. Returns null on any failure.
fun imageProxyToUprightBitmap(image: ImageProxy, maxDim: Int): Bitmap? {
    return try {
        val bitmap = image.toBitmap()
        val rotation = image.imageInfo.rotationDegrees
        val w = bitmap.width
        val h = bitmap.height
        val scale = maxDim.toFloat() / maxOf(w, h).toFloat()
        val matrix = Matrix()
        if (scale < 1f) matrix.postScale(scale, scale)
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        if (scale < 1f || rotation != 0)
            Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true)
        else bitmap
    } catch (e: Exception) {
        Log.e("FrameMontage", "imageProxyToUprightBitmap failed", e)
        null
    }
}

// Encodes a bitmap as a base64 JPEG (NO_WRAP) suitable for the Gemini proxy.
fun bitmapToBase64(bitmap: Bitmap, quality: Int): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

// Appends a timestamped, bones-annotated frame to the ring buffer, dropping the
// OLDEST frame once over capacity so a slow consumer never blocks the producer.
fun pushFrame(ring: ConcurrentLinkedDeque<Pair<Long, Bitmap>>, timestampMs: Long, bitmap: Bitmap) {
    ring.addLast(timestampMs to bitmap)
    while (ring.size > FRAME_RING_CAPACITY) ring.pollFirst()
}

// Atomically takes every frame currently buffered, in chronological order, and
// clears the buffer — so the NEXT drain only contains frames captured since this
// call. This is what keeps Gemini's montage windows contiguous (no skipped reps)
// even though each round trip takes a few seconds.
fun drainFrames(ring: ConcurrentLinkedDeque<Pair<Long, Bitmap>>): List<Bitmap> {
    val drained = ArrayList<Pair<Long, Bitmap>>()
    while (true) {
        val item = ring.pollFirst() ?: break
        drained.add(item)
    }
    return drained.sortedBy { it.first }.map { it.second }
}

// Evenly samples up to [maxCount] items from [items], preserving order. Used to
// pick which buffered frames go into the fixed-size montage grid.
fun <T> sampleEvenly(items: List<T>, maxCount: Int): List<T> {
    if (items.size <= maxCount) return items
    val step = items.size.toDouble() / maxCount
    return (0 until maxCount).map { items[(it * step).toInt().coerceAtMost(items.size - 1)] }
}

// Composes up to MONTAGE_COLS x MONTAGE_ROWS frames into ONE grid image, laid out
// chronologically top-left -> bottom-right, so Gemini can read motion across the
// whole window from a single JPEG (keeping the proxy's single-image contract).
// Frames beyond the input count are padded by repeating the last available frame.
fun buildMontage(frames: List<Bitmap>): Bitmap {
    val cellW = 400
    val cellH = 300
    val montage = Bitmap.createBitmap(cellW * MONTAGE_COLS, cellH * MONTAGE_ROWS, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(montage)
    canvas.drawColor(android.graphics.Color.BLACK)
    if (frames.isEmpty()) return montage
    val cellCount = MONTAGE_COLS * MONTAGE_ROWS
    for (index in 0 until cellCount) {
        val bmp = frames[minOf(index, frames.size - 1)]
        val col = index % MONTAGE_COLS
        val row = index / MONTAGE_COLS
        val destRect = android.graphics.Rect(col * cellW, row * cellH, (col + 1) * cellW, (row + 1) * cellH)
        val srcRect = android.graphics.Rect(0, 0, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, srcRect, destRect, null)
    }
    return montage
}

// Tolerant JSON extraction from the model's reply (strips markdown fences / prose).
fun parseAnalysis(raw: String): JSONObject? {
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
        Log.e("FrameMontage", "parseAnalysis failed: $raw", e)
        null
    }
}
