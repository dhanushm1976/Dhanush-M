package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyRoutineEntity
import com.example.data.model.StudyMaterialEntity
import com.example.data.model.StudySuggestion
import com.example.data.model.SuggestionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyActivityScreen(
    routine: DailyRoutineEntity,
    suggestions: List<StudySuggestion>,
    materials: List<StudyMaterialEntity>,
    onUpdateRoutine: (morningTime: String, morningEnabled: Boolean, nightTime: String, nightEnabled: Boolean, quote: String, advice: String) -> Unit,
    onTestMorningAlarm: () -> Unit,
    onTestSleepReminder: () -> Unit,
    onAcceptSuggestionAsTask: (StudySuggestion) -> Unit,
    onOpenPdfSuggestion: (StudyMaterialEntity) -> Unit,
    onNavigateToSubjectHub: (subjectId: Long, sectionTab: Int) -> Unit
) {
    var morningTime by remember(routine.morningAlarmTime) { mutableStateOf(routine.morningAlarmTime) }
    var morningEnabled by remember(routine.morningAlarmEnabled) { mutableStateOf(routine.morningAlarmEnabled) }
    var nightTime by remember(routine.nightSleepTime) { mutableStateOf(routine.nightSleepTime) }
    var nightEnabled by remember(routine.nightSleepEnabled) { mutableStateOf(routine.nightSleepEnabled) }
    var morningQuote by remember(routine.morningMotivationQuote) { mutableStateOf(routine.morningMotivationQuote) }
    var nightAdvice by remember(routine.nightSleepAdvice) { mutableStateOf(routine.nightSleepAdvice) }

    var showEditAlarmDialog by remember { mutableStateOf(false) }

    fun adjustTime(timeStr: String, deltaMinutes: Int): String {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 6
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
        var totalMinutes = (h * 60 + m + deltaMinutes) % (24 * 60)
        if (totalMinutes < 0) totalMinutes += (24 * 60)
        val newH = totalMinutes / 60
        val newM = totalMinutes % 60
        return String.format("%02d:%02d", newH, newM)
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Screen Header
            item {
                Column {
                    Text(
                        text = "Daily Routine & Smart Planner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Morning wake-up alarm, nighttime sleep schedule & AI-driven study suggestions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // MORNING ALARM CARD
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_morning_alarm")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFFEF3C7).copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.WbSunny,
                                            contentDescription = "Morning",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Morning Wake-Up Alarm",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Time: $morningTime AM",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (morningEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Switch(
                                checked = morningEnabled,
                                onCheckedChange = {
                                    morningEnabled = it
                                    onUpdateRoutine(morningTime, it, nightTime, nightEnabled, morningQuote, nightAdvice)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stepper for Morning Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    morningTime = adjustTime(morningTime, -15)
                                    onUpdateRoutine(morningTime, morningEnabled, nightTime, nightEnabled, morningQuote, nightAdvice)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-15m")
                            }

                            FilledTonalButton(
                                onClick = {
                                    morningTime = adjustTime(morningTime, 15)
                                    onUpdateRoutine(morningTime, morningEnabled, nightTime, nightEnabled, morningQuote, nightAdvice)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+15m")
                            }

                            OutlinedButton(
                                onClick = onTestMorningAlarm,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ring Alarm")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF9C3).copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = morningQuote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            }

            // NIGHT SLEEP SCHEDULE CARD
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_night_sleep")
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFEDE9FE).copy(alpha = 0.4f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bedtime,
                                            contentDescription = "Night Sleep",
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Night Sleep & Wind-Down",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Bedtime: $nightTime",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (nightEnabled) Color(0xFF7C3AED) else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Switch(
                                checked = nightEnabled,
                                onCheckedChange = {
                                    nightEnabled = it
                                    onUpdateRoutine(morningTime, morningEnabled, nightTime, it, morningQuote, nightAdvice)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stepper for Night Sleep Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    nightTime = adjustTime(nightTime, -15)
                                    onUpdateRoutine(morningTime, morningEnabled, nightTime, nightEnabled, morningQuote, nightAdvice)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-15m")
                            }

                            FilledTonalButton(
                                onClick = {
                                    nightTime = adjustTime(nightTime, 15)
                                    onUpdateRoutine(morningTime, morningEnabled, nightTime, nightEnabled, morningQuote, nightAdvice)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+15m")
                            }

                            OutlinedButton(
                                onClick = onTestSleepReminder,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.4f)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sleep Alert")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF3E8FF).copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NightlightRound,
                                    contentDescription = null,
                                    tint = Color(0xFF6B21A8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = nightAdvice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF581C87)
                                )
                            }
                        }
                    }
                }
            }

            // SMART WORK SUGGESTIONS ENGINE HEADER
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "💡 Suggested Work For You Today",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Smart suggestions based on unread chapters, pending notes & video progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // SUGGESTIONS LIST
            if (suggestions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All Caught Up!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your subjects and daily reading are on track. Add more chapters or Q&As to continue learning.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(suggestions, key = { it.id }) { suggestion ->
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("suggestion_card_${suggestion.id}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Icon by suggestion type
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = when (suggestion.type) {
                                            SuggestionType.READ_PDF -> MaterialTheme.colorScheme.primaryContainer
                                            SuggestionType.COMPLETE_NOTES -> Color(0xFFFEF3C7)
                                            SuggestionType.PRACTICE_QA -> Color(0xFFDCFCE7)
                                            SuggestionType.WATCH_YOUTUBE -> Color(0xFFFFE4E6)
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = when (suggestion.type) {
                                                    SuggestionType.READ_PDF -> Icons.Default.PictureAsPdf
                                                    SuggestionType.COMPLETE_NOTES -> Icons.Default.EditNote
                                                    SuggestionType.PRACTICE_QA -> Icons.Default.Quiz
                                                    SuggestionType.WATCH_YOUTUBE -> Icons.Default.PlayCircle
                                                    else -> Icons.Default.Lightbulb
                                                },
                                                contentDescription = null,
                                                tint = when (suggestion.type) {
                                                    SuggestionType.READ_PDF -> MaterialTheme.colorScheme.primary
                                                    SuggestionType.COMPLETE_NOTES -> Color(0xFFD97706)
                                                    SuggestionType.PRACTICE_QA -> Color(0xFF16A34A)
                                                    SuggestionType.WATCH_YOUTUBE -> Color(0xFFE11D48)
                                                    else -> MaterialTheme.colorScheme.secondary
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = suggestion.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${suggestion.subjectName} • ~${suggestion.estimatedMinutes} mins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = suggestion.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // "Do Work Now" button
                                Button(
                                    onClick = {
                                        when (suggestion.type) {
                                            SuggestionType.READ_PDF -> {
                                                val mat = materials.find { it.id == suggestion.materialId }
                                                if (mat != null) {
                                                    onOpenPdfSuggestion(mat)
                                                } else {
                                                    onNavigateToSubjectHub(suggestion.subjectId, 0)
                                                }
                                            }
                                            SuggestionType.COMPLETE_NOTES -> {
                                                onNavigateToSubjectHub(suggestion.subjectId, 3)
                                            }
                                            SuggestionType.PRACTICE_QA -> {
                                                onNavigateToSubjectHub(suggestion.subjectId, 1)
                                            }
                                            SuggestionType.WATCH_YOUTUBE -> {
                                                onNavigateToSubjectHub(suggestion.subjectId, 2)
                                            }
                                            else -> {}
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(suggestion.actionLabel)
                                }

                                // "Add to Today's Tasks" button
                                OutlinedButton(
                                    onClick = { onAcceptSuggestionAsTask(suggestion) },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add to Tasks")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
