package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.theme.PremiumGrayDark
import com.example.ui.theme.PremiumGrayMedium
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: (name: String, email: String) -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Aesthetic Ambient Dark/Monochrome Top Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PremiumGrayDark,
                            Color.Black
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Brand Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glow Silhouette App Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Coach Brand Logo",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "A I   C O A C H",
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Professional AI Biometrics & Real-time Pose Correction",
                    fontSize = 13.sp,
                    color = PremiumGrayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Lower Section holding Authentication Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("login_progress_bar")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Securing authentications...",
                        color = PremiumGrayMedium,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Sign in to securely sync your body stats, weekly programs, and leaderboard points.",
                        fontSize = 12.sp,
                        color = PremiumGrayMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    )

                    // 1. Google Sign-In Button
                    Button(
                        onClick = {
                            authError = null
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setServerClientId(context.getString(R.string.default_web_client_id))
                                        .setFilterByAuthorizedAccounts(false)
                                        .build()
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()
                                    val result = credentialManager.getCredential(context, request)
                                    val credential = result.credential
                                    if (credential is CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                        val idToken = googleIdTokenCredential.idToken
                                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                                        val authResult = withContext(Dispatchers.IO) {
                                            Tasks.await(FirebaseAuth.getInstance().signInWithCredential(firebaseCredential))
                                        }
                                        val user = authResult.user
                                        isLoading = false
                                        val displayName = user?.displayName
                                            ?: user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() }
                                            ?: "Athlete"
                                        onLoginSuccess(displayName, user?.email ?: "")
                                    } else {
                                        isLoading = false
                                        authError = "Unsupported credential type received."
                                    }
                                } catch (e: GetCredentialException) {
                                    isLoading = false
                                    authError = "Google sign-in was cancelled or failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                                } catch (e: Exception) {
                                    isLoading = false
                                    authError = "Sign-in failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .testTag("google_signin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Minimal stylized monochrome Google "G" representation
                            Text(
                                "G ",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Sign in with Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Guest Sign-In Button (Firebase Anonymous Auth)
                    OutlinedButton(
                        onClick = {
                            authError = null
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val authResult = withContext(Dispatchers.IO) {
                                        Tasks.await(FirebaseAuth.getInstance().signInAnonymously())
                                    }
                                    isLoading = false
                                    onLoginSuccess("Guest", "")
                                } catch (e: Exception) {
                                    isLoading = false
                                    authError = "Guest sign-in failed: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .testTag("guest_signin_button"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = PremiumGrayMedium
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Continue as Guest",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    authError?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = error,
                            color = Color.White,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
