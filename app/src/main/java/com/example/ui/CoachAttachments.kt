package com.example.ui

// ==================== G4/G5/G6: Coach tab attachments ====================
// Small, self-contained file (kept separate from DashboardScreen.kt per the
// concurrency notes for this branch) providing:
//  - AttachmentPickerButton: paperclip "+" button + menu to pick an image
//    (G4), video (G5), or file (G6) without requiring storage permissions.
//  - ChatAttachmentPreview: renders the attachment inside a chat bubble
//    (image thumbnail, playable video, or a filename chip).

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.ui.theme.PremiumGrayDark

/**
 * Paperclip/"+" button shown next to the chat input. Tapping it opens a small
 * menu to choose Photo / Video / File; each choice launches the matching,
 * permission-less Activity Result contract and reports the picked Uri back
 * to the caller via [onImagePicked] / [onVideoPicked] / [onFilePicked].
 */
@Composable
fun AttachmentPickerButton(
    onImagePicked: (Uri) -> Unit,
    onVideoPicked: (Uri) -> Unit,
    onFilePicked: (uri: Uri, fileName: String, mimeType: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    // G4: image-only picker (Android Photo Picker where available, no READ_MEDIA permission needed).
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImagePicked(uri)
    }
    // G5: video-only picker, same permission-less contract.
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onVideoPicked(uri)
    }
    // G6: generic document picker (covers application/pdf, text/plain, and other docs).
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val (name, mimeType) = resolveFileMeta(uri, context)
            onFilePicked(uri, name, mimeType)
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { menuExpanded = true },
            modifier = Modifier
                .size(48.dp)
                .testTag("chat_attach_button"),
            colors = IconButtonDefaults.iconButtonColors(containerColor = PremiumGrayDark)
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Photo") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            )
            DropdownMenuItem(
                text = { Text("Video") },
                leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                }
            )
            DropdownMenuItem(
                text = { Text("File (PDF / text)") },
                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    filePicker.launch("*/*")
                }
            )
        }
    }
}

/** Resolves a display filename + mimeType for a picked document Uri (G6). */
private fun resolveFileMeta(uri: Uri, context: android.content.Context): Pair<String, String?> {
    var name = "file"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)?.let { name = it }
        }
    }
    val mimeType = context.contentResolver.getType(uri)
    return name to mimeType
}

/**
 * Renders a message's attachment (if any) above its text inside the chat
 * bubble: an image thumbnail (G4), a playable video (G5), or a filename chip
 * (G6). Returns without drawing anything when the message has no attachment.
 */
@Composable
fun ChatAttachmentPreview(message: ChatMessage, modifier: Modifier = Modifier) {
    val uriString = message.attachmentUri ?: return
    when (message.attachmentType) {
        AttachmentType.IMAGE -> {
            AsyncImage(
                model = Uri.parse(uriString),
                contentDescription = "Attached photo",
                modifier = modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        }
        AttachmentType.VIDEO -> {
            ChatVideoPlayer(uriString = uriString, modifier = modifier.size(width = 220.dp, height = 160.dp))
        }
        AttachmentType.FILE -> {
            Row(
                modifier = modifier
                    .background(PremiumGrayDark, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = "File", tint = Color.White, modifier = Modifier.size(18.dp))
                Text(
                    text = message.attachmentName ?: "Attached file",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
        null -> Unit
    }
}

/**
 * Minimal, dependency-free video playback for chat bubbles: android.widget.VideoView
 * (with a MediaController for play/pause/seek) satisfies G5's "at minimum the user
 * can play back their uploaded video" requirement without adding a media3 dependency.
 */
@Composable
private fun ChatVideoPlayer(uriString: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(Uri.parse(uriString))
                setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                setOnPreparedListener { player -> player.isLooping = false }
            }
        }
    )
}
