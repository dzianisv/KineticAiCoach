package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig

// ==================== ABOUT TAB (GAP G7 - Help/About) ====================
// Reachable from Dashboard bottom nav (tab index 4). Shows app identity, the
// AI model used for pose/coaching analysis, and a tappable support email.
@Composable
fun AboutTab() {
    val uriHandler = LocalUriHandler.current
    val supportEmail = "support@agentlabs.cc"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("HELP & INFO", fontSize = 12.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        Text("About", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Black)

        Spacer(modifier = Modifier.height(12.dp))

        // App identity card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8))
                    Text(
                        text = "Kinetic AI Coach",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your personal AI-powered fitness coach — real-time form correction, " +
                        "adaptive workout programs, and progress tracking, all on your device.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI model disclosure card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B))
                    Text(
                        text = "AI Model",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "AI model used for analysis: gemini-3.5-flash (Google Vertex AI)",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback / support card — tappable mailto: link
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Feedback & Support",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), shape = RoundedCornerShape(10.dp))
                        .clickable { uriHandler.openUri("mailto:$supportEmail") }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = "Email support", tint = Color(0xFF38BDF8))
                        Text(
                            text = supportEmail,
                            fontSize = 14.sp,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy note
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = Color(0xFF94A3B8))
                Text(
                    text = "Your workout video is processed for pose analysis and is not shared with " +
                        "third parties beyond what's required to power AI coaching. Contact support " +
                        "for privacy questions.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
