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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PremiumGrayDark
import com.example.ui.theme.PremiumGrayMedium
import kotlinx.coroutines.delay

/**
 * PRD v2 (Lane B): results TABLE shown after the last exercise in today's class.
 * Reads the in-memory [MainViewModel.classResults] for immediacy (still populated
 * right after the class finishes) and shows per-exercise reps + form + points plus a total row.
 */
@Composable
fun ClassResultsScreen(
    viewModel: MainViewModel,
    classId: Int,
    onDone: () -> Unit
) {
    val results by viewModel.classResults.collectAsState()

    // Guard against an in-flight double-tap from the previous "Finish class"
    // button (same screen position): ignore taps on Done for a short window so
    // finishing the class can't accidentally skip past the results table.
    var doneEnabled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(800)
        doneEnabled = true
    }

    val totalReps = results.sumOf { it.reps }
    val totalSets = results.sumOf { it.sets }
    val totalPoints = results.sumOf { it.points }
    val avgForm = if (results.isNotEmpty()) results.map { it.formScore }.average().toInt() else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "  Class Complete!",
                    fontSize = 26.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
            Text(
                "Class #$classId — great work across ${results.size} exercises.",
                fontSize = 13.sp,
                color = PremiumGrayMedium
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = PremiumGrayDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("class_results_table")
                        .padding(16.dp)
                ) {
                    // Header row
                    ResultRow(
                        name = "EXERCISE",
                        reps = "REPS",
                        sets = "SETS",
                        form = "FORM",
                        points = "PTS",
                        isHeader = true
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(results) { r ->
                            ResultRow(
                                name = r.name,
                                reps = "${r.reps}",
                                sets = "${r.sets}",
                                form = "${r.formScore}%",
                                points = "${r.points}"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // Total row
                    ResultRow(
                        name = "TOTAL",
                        reps = "$totalReps",
                        sets = "$totalSets",
                        form = "$avgForm%",
                        points = "$totalPoints",
                        isTotal = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { if (doneEnabled) onDone() },
                enabled = doneEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("results_done"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
private fun ResultRow(
    name: String,
    reps: String,
    sets: String,
    form: String,
    points: String,
    isHeader: Boolean = false,
    isTotal: Boolean = false
) {
    val color = when {
        isHeader -> PremiumGrayMedium
        isTotal -> Color.White
        else -> Color.White
    }
    val weight = if (isHeader || isTotal) FontWeight.Black else FontWeight.Medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 13.sp, color = color, fontWeight = weight, modifier = Modifier.weight(2f))
        Text(reps, fontSize = 13.sp, color = color, fontWeight = weight, modifier = Modifier.weight(1f))
        Text(sets, fontSize = 13.sp, color = color, fontWeight = weight, modifier = Modifier.weight(1f))
        Text(form, fontSize = 13.sp, color = color, fontWeight = weight, modifier = Modifier.weight(1f))
        Text(points, fontSize = 13.sp, color = color, fontWeight = weight, modifier = Modifier.weight(1f))
    }
}
