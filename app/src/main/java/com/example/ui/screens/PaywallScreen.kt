package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.billing.BillingConfig
import com.example.billing.BillingManager
import com.example.ui.theme.PremiumGrayBorder
import com.example.ui.theme.PremiumGrayDark
import com.example.ui.theme.PremiumGrayMedium

// ==================== PAYWALL ====================
// Full-screen upgrade prompt shown when a free user hits the weekly AI-analyzed
// class cap or taps "Upgrade" from Dashboard/Coach. Presents the two Play
// Billing base plans (monthly / yearly) resolved by BillingManager and lets the
// user launch a purchase flow, restore existing purchases, or dismiss.
@Composable
fun PaywallScreen(
    proPlans: BillingManager.ProPlans,
    isConnected: Boolean,
    onSubscribe: (basePlanId: String) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit
) {
    val plansLoaded = proPlans.monthly != null || proPlans.yearly != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Top bar with close affordance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Text(
                text = "Kinetic Pro",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Unlock unlimited AI-analyzed classes and coach uploads.",
                fontSize = 15.sp,
                color = PremiumGrayMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Benefits list
            PaywallBenefitRow(
                text = "Unlimited AI-analyzed workout classes (free tier caps at " +
                    "${BillingConfig.FREE_WEEKLY_CLASS_LIMIT}/week)"
            )
            Spacer(modifier = Modifier.height(12.dp))
            PaywallBenefitRow(text = "Coach multimodal uploads — photos, video, and files")
            Spacer(modifier = Modifier.height(12.dp))
            PaywallBenefitRow(text = "Full workout history & analytics")

            Spacer(modifier = Modifier.height(28.dp))

            if (!plansLoaded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading plans…", fontSize = 13.sp, color = PremiumGrayMedium)
                    if (!isConnected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reconnecting to Play Store…",
                            fontSize = 12.sp,
                            color = PremiumGrayMedium
                        )
                    }
                }
            } else {
                val showBestValueBadge = proPlans.monthly != null && proPlans.yearly != null

                proPlans.monthly?.let { monthly ->
                    PlanCard(
                        plan = monthly,
                        periodSuffix = "/mo",
                        badgeText = null,
                        onSubscribe = { onSubscribe(BillingConfig.BASE_PLAN_MONTHLY) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                proPlans.yearly?.let { yearly ->
                    PlanCard(
                        plan = yearly,
                        periodSuffix = "/yr",
                        badgeText = if (showBestValueBadge) "Best value" else null,
                        onSubscribe = { onSubscribe(BillingConfig.BASE_PLAN_YEARLY) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = onRestore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Restore purchases", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Subscriptions auto-renew and can be managed or cancelled anytime in your " +
                    "Google Play Store subscription settings. Prices and free trial availability " +
                    "may vary by account and region.",
                fontSize = 11.sp,
                color = PremiumGrayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PaywallBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun PlanCard(
    plan: BillingManager.PlanOffer,
    periodSuffix: String,
    badgeText: String?,
    onSubscribe: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PremiumGrayDark),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PremiumGrayBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${plan.formattedPrice}$periodSuffix",
                    fontSize = 22.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (plan.freeTrialDays != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${plan.freeTrialDays}-day free trial",
                    fontSize = 12.sp,
                    color = PremiumGrayMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSubscribe,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Subscribe", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
