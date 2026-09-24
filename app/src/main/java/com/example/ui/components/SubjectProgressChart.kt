package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyTaskEntity
import com.example.data.model.SubjectEntity
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class SubjectProgressData(
    val subjectId: Long,
    val subjectName: String,
    val subjectCode: String,
    val colorHex: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val totalAssignments: Int,
    val completedAssignments: Int,
    val pendingAssignments: Int,
    val completionPercentage: Int,
    val assignmentClearancePercentage: Int
)

@Composable
fun SubjectProgressChartCard(
    subjects: List<SubjectEntity>,
    tasks: List<DailyTaskEntity>,
    modifier: Modifier = Modifier,
    onNavigateToReports: () -> Unit = {}
) {
    var chartMode by remember { mutableIntStateOf(0) } // 0: Grouped Bars, 1: Progress Matrix, 2: Workload Ring
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }

    // Aggregate progress data per subject
    val progressList = remember(subjects, tasks) {
        subjects.map { sub ->
            val subTasks = tasks.filter { it.subjectId == sub.id }
            val completed = subTasks.count { it.isCompleted }
            val assignments = subTasks.filter { it.isAssignment }
            val completedAsg = assignments.count { it.isCompleted }
            val pendingAsg = assignments.size - completedAsg
            val totalT = subTasks.size
            val completionPct = if (totalT > 0) (completed * 100) / totalT else 0
            val clearancePct = if (assignments.isNotEmpty()) (completedAsg * 100) / assignments.size else 100

            SubjectProgressData(
                subjectId = sub.id,
                subjectName = sub.name,
                subjectCode = sub.code.ifBlank { sub.name.take(4).uppercase() },
                colorHex = sub.colorHex,
                totalTasks = totalT,
                completedTasks = completed,
                totalAssignments = assignments.size,
                completedAssignments = completedAsg,
                pendingAssignments = pendingAsg,
                completionPercentage = completionPct,
                assignmentClearancePercentage = clearancePct
            )
        }
    }

    val totalCompletedTasks = remember(tasks) { tasks.count { it.isCompleted } }
    val totalAssignments = remember(tasks) { tasks.count { it.isAssignment } }
    val totalCompletedAssignments = remember(tasks) { tasks.count { it.isAssignment && it.isCompleted } }
    val totalPendingAssignments = totalAssignments - totalCompletedAssignments
    val overallAssignmentRate = if (totalAssignments > 0) (totalCompletedAssignments * 100) / totalAssignments else 100

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("subject_progress_chart_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Title and Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Subject Progress Chart",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Completed Tasks vs Total Assignments",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact Report Shortcut
                IconButton(onClick = onNavigateToReports) {
                    Icon(
                        imageVector = Icons.Default.Launch,
                        contentDescription = "View Full Report",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Chart View Mode Tabs
            TabRow(
                selectedTabIndex = chartMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Tab(
                    selected = chartMode == 0,
                    onClick = { chartMode = 0 },
                    text = { Text("Bar Chart", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.Equalizer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = chartMode == 1,
                    onClick = { chartMode = 1 },
                    text = { Text("Matrix", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = chartMode == 2,
                    onClick = { chartMode = 2 },
                    text = { Text("Donut", style = MaterialTheme.typography.labelMedium) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Summary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "Done Tasks",
                    value = "$totalCompletedTasks / ${tasks.size}",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Assignments",
                    value = "$totalCompletedAssignments / $totalAssignments",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Pending Asg",
                    value = "$totalPendingAssignments",
                    color = if (totalPendingAssignments > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator(color = Color(0xFF10B981), label = "Completed Tasks")
                Spacer(modifier = Modifier.width(16.dp))
                LegendIndicator(color = Color(0xFFF59E0B), label = "Total Assignments")
                Spacer(modifier = Modifier.width(16.dp))
                LegendIndicator(color = Color(0xFF6366F1), label = "Pending")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (progressList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No subjects registered yet. Add subjects to see progress chart.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                when (chartMode) {
                    0 -> {
                        // 1. Grouped Bar Chart
                        GroupedBarChart(
                            data = progressList,
                            selectedId = selectedSubjectId,
                            onSelect = { selectedSubjectId = if (selectedSubjectId == it) null else it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }
                    1 -> {
                        // 2. Progress Matrix (Subject by Subject)
                        SubjectProgressMatrix(
                            data = progressList,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    2 -> {
                        // 3. Workload Donut Distribution
                        SubjectWorkloadDonut(
                            data = progressList,
                            overallRate = overallAssignmentRate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                        )
                    }
                }

                // Selected Subject Insight Banner
                AnimatedVisibility(visible = selectedSubjectId != null) {
                    val sel = progressList.find { it.subjectId == selectedSubjectId }
                    if (sel != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${sel.subjectCode}: ${sel.subjectName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tasks: ${sel.completedTasks}/${sel.totalTasks} done • Assignments: ${sel.completedAssignments}/${sel.totalAssignments} (${sel.pendingAssignments} pending)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { selectedSubjectId = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupedBarChart(
    data: List<SubjectProgressData>,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxCount = remember(data) {
        val highest = data.maxOfOrNull { max(it.completedTasks, it.totalAssignments) } ?: 1
        max(highest + 1, 4)
    }

    val primaryGreen = Color(0xFF10B981)
    val secondaryAmber = Color(0xFFF59E0B)
    val textOutlineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Canvas(
        modifier = modifier
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val slotWidth = size.width / data.size
                    val index = (offset.x / slotWidth).toInt().coerceIn(0, data.size - 1)
                    if (index in data.indices) {
                        onSelect(data[index].subjectId)
                    }
                }
            }
            .testTag("grouped_progress_canvas")
    ) {
        val w = size.width
        val h = size.height
        val bottomMargin = 40f
        val topMargin = 25f
        val chartHeight = h - bottomMargin - topMargin
        val slotWidth = w / data.size

        // Draw horizontal grid lines
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = topMargin + chartHeight * (1f - i.toFloat() / gridSteps)
            val gridVal = (maxCount * i) / gridSteps
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            drawContext.canvas.nativeCanvas.drawText(
                "$gridVal",
                6f,
                y - 4f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 24f
                    isAntiAlias = true
                }
            )
        }

        // Draw Grouped Bars for each Subject
        data.forEachIndexed { index, item ->
            val centerX = index * slotWidth + slotWidth / 2f
            val barWidth = min(slotWidth * 0.28f, 26f)
            val barSpacing = 4f
            val isSelected = selectedId == item.subjectId

            // Left Bar: Completed Tasks
            val taskBarHeight = chartHeight * (item.completedTasks.toFloat() / maxCount.toFloat())
            val taskBarLeft = centerX - barWidth - barSpacing / 2f
            val taskBarTop = topMargin + chartHeight - taskBarHeight

            // Draw selection glow if selected
            if (isSelected) {
                drawRoundRect(
                    color = primaryGreen.copy(alpha = 0.2f),
                    topLeft = Offset(centerX - slotWidth * 0.45f, topMargin),
                    size = Size(slotWidth * 0.9f, chartHeight + bottomMargin),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }

            // Draw Completed Tasks Bar
            drawRoundRect(
                color = if (isSelected) primaryGreen else primaryGreen.copy(alpha = 0.85f),
                topLeft = Offset(taskBarLeft, taskBarTop),
                size = Size(barWidth, max(taskBarHeight, 4f)),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Right Bar: Total Assignments
            val asgBarHeight = chartHeight * (item.totalAssignments.toFloat() / maxCount.toFloat())
            val asgBarLeft = centerX + barSpacing / 2f
            val asgBarTop = topMargin + chartHeight - asgBarHeight

            drawRoundRect(
                color = if (isSelected) secondaryAmber else secondaryAmber.copy(alpha = 0.85f),
                topLeft = Offset(asgBarLeft, asgBarTop),
                size = Size(barWidth, max(asgBarHeight, 4f)),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // Value text above bars
            drawContext.canvas.nativeCanvas.drawText(
                "${item.completedTasks}",
                taskBarLeft + barWidth / 2f - 6f,
                taskBarTop - 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 24f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
            )

            drawContext.canvas.nativeCanvas.drawText(
                "${item.totalAssignments}",
                asgBarLeft + barWidth / 2f - 6f,
                asgBarTop - 6f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = 24f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
            )

            // X-axis label (Subject Code)
            val labelText = item.subjectCode
            drawContext.canvas.nativeCanvas.drawText(
                labelText,
                centerX - (labelText.length * 6f),
                h - 10f,
                android.graphics.Paint().apply {
                    color = if (isSelected) android.graphics.Color.BLUE else android.graphics.Color.BLACK
                    textSize = 28f
                    isFakeBoldText = isSelected
                    isAntiAlias = true
                }
            )
        }
    }
}

@Composable
private fun SubjectProgressMatrix(
    data: List<SubjectProgressData>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.forEach { item ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(item.colorHex))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${item.subjectCode} • ${item.subjectName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Status Badge
                        Surface(
                            color = when {
                                item.pendingAssignments == 0 && item.completedTasks >= item.totalTasks && item.totalTasks > 0 -> Color(0xFF10B981).copy(alpha = 0.15f)
                                item.pendingAssignments > 0 -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = when {
                                    item.pendingAssignments == 0 && item.completedTasks >= item.totalTasks && item.totalTasks > 0 -> "All Up to Date"
                                    item.pendingAssignments > 0 -> "${item.pendingAssignments} Asg Due"
                                    else -> "In Progress"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    item.pendingAssignments == 0 && item.completedTasks >= item.totalTasks && item.totalTasks > 0 -> Color(0xFF10B981)
                                    item.pendingAssignments > 0 -> Color(0xFFEF4444)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress 1: Completed Tasks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tasks Progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.completedTasks}/${item.totalTasks} (${item.completionPercentage}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (item.totalTasks > 0) item.completedTasks.toFloat() / item.totalTasks.toFloat() else 0f
                        },
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress 2: Assignments Clearance
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assignments Load",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.completedAssignments}/${item.totalAssignments} cleared (${item.assignmentClearancePercentage}%)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.pendingAssignments > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (item.totalAssignments > 0) item.completedAssignments.toFloat() / item.totalAssignments.toFloat() else 1f
                        },
                        color = Color(0xFFF59E0B),
                        trackColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectWorkloadDonut(
    data: List<SubjectProgressData>,
    overallRate: Int,
    modifier: Modifier = Modifier
) {
    val totalWeight = remember(data) {
        val sum = data.sumOf { max(it.totalTasks + it.totalAssignments, 1) }
        max(sum, 1)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(170.dp)) {
            var startAngle = -90f
            val strokeWidth = 24.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            data.forEach { item ->
                val weight = max(item.totalTasks + item.totalAssignments, 1).toFloat()
                val sweep = (weight / totalWeight.toFloat()) * 360f

                val subjectColor = try {
                    Color(android.graphics.Color.parseColor(item.colorHex))
                } catch (e: Exception) {
                    Color(0xFF3B82F6)
                }

                drawArc(
                    color = subjectColor,
                    startAngle = startAngle,
                    sweepAngle = sweep - 2f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                startAngle += sweep
            }
        }

        // Center Content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$overallRate%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Asg Cleared",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
