package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyTaskEntity
import com.example.data.model.SubjectEntity
import com.example.ui.components.AddEditTaskDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SubjectCheckAndReportScreen(
    subjects: List<SubjectEntity>,
    tasks: List<DailyTaskEntity>,
    onToggleSubjectUpToDate: (Long, Boolean) -> Unit,
    onMarkAllUpToDate: (Boolean) -> Unit,
    onCompleteTask: (Long) -> Unit,
    onAddTask: (
        subjectId: Long,
        subjectName: String,
        title: String,
        description: String,
        dueDate: Long,
        priority: String,
        isAssignment: Boolean,
        hasReminder: Boolean,
        estimatedMinutes: Int
    ) -> Unit,
    generateReportText: () -> String
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Daily Subject Check, 1: Pending Assignments Report
    var showQuickAddTaskDialog by remember { mutableStateOf<Long?>(null) }
    var showFullReportModal by remember { mutableStateOf(false) }

    val pendingAssignments = remember(tasks) {
        tasks.filter { it.isAssignment && !it.isCompleted }
    }

    val now = System.currentTimeMillis()
    val overdueAssignments = remember(pendingAssignments) {
        pendingAssignments.filter { it.dueDate < now }
    }
    val dueTodayAssignments = remember(pendingAssignments) {
        pendingAssignments.filter {
            val cal1 = Calendar.getInstance().apply { timeInMillis = it.dueDate }
            val cal2 = Calendar.getInstance()
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
    }
    val upcomingAssignments = remember(pendingAssignments) {
        pendingAssignments.filter { it.dueDate > now && !dueTodayAssignments.contains(it) }
    }

    val allSubjectsUpToDate = subjects.isNotEmpty() && subjects.all { it.isUpToDate }
    val subjectsWithFewTasks = subjects.filter { sub ->
        val count = tasks.count { it.subjectId == sub.id && !it.isCompleted }
        count <= 1
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Dual Tab Header (Daily Subject Check vs Pending Assignments Report)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daily Subject Check", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    modifier = Modifier.testTag("tab_daily_subject_check")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        BadgedBox(
                            badge = {
                                if (pendingAssignments.isNotEmpty()) {
                                    Badge { Text("${pendingAssignments.size}") }
                                }
                            }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assignments Report", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    },
                    modifier = Modifier.testTag("tab_assignments_report")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // --- TAB 0: DAILY SUBJECT UP-TO-DATE CHECK ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        // Hero Status Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (allSubjectsUpToDate)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (allSubjectsUpToDate)
                                                "All Subjects Up to Date! 🎉"
                                            else
                                                "Daily Check: Are All Subjects Up to Date?",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (allSubjectsUpToDate)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (allSubjectsUpToDate)
                                                "You have verified all subjects today. Great study rhythm!"
                                            else
                                                "Review each course below. Subjects with few tasks need attention.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (allSubjectsUpToDate)
                                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            else
                                                MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                        )
                                    }

                                    Icon(
                                        imageVector = if (allSubjectsUpToDate) Icons.Default.CheckCircle else Icons.Default.HelpOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = if (allSubjectsUpToDate)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.tertiary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onMarkAllUpToDate(!allSubjectsUpToDate) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("toggle_all_up_to_date_button")
                                    ) {
                                        Icon(
                                            imageVector = if (allSubjectsUpToDate) Icons.Default.Refresh else Icons.Default.DoneAll,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (allSubjectsUpToDate) "Reset Status" else "Mark All Up-to-Date")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Course Up-to-Date Status (${subjects.count { it.isUpToDate }}/${subjects.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(subjects, key = { it.id }) { subject ->
                        val subTasks = tasks.filter { it.subjectId == subject.id && !it.isCompleted }
                        val isFew = subTasks.size <= 1
                        val subColor = runCatching {
                            Color(android.graphics.Color.parseColor(subject.colorHex))
                        }.getOrDefault(MaterialTheme.colorScheme.primary)

                        ElevatedCard(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("subject_check_card_${subject.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(subColor)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = subject.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${subject.code} • ${subTasks.size} active tasks",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Switch or Checkbox to mark Up to date
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (subject.isUpToDate) "Up-to-Date" else "Needs Review",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (subject.isUpToDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            checked = subject.isUpToDate,
                                            onCheckedChange = { isChecked ->
                                                onToggleSubjectUpToDate(subject.id, isChecked)
                                            },
                                            modifier = Modifier.testTag("switch_uptodate_${subject.id}")
                                        )
                                    }
                                }

                                if (isFew) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Subject has few tasks (${subTasks.size}). Add daily tasks to keep up!",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                            TextButton(
                                                onClick = { showQuickAddTaskDialog = subject.id },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("+ Add Task", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // --- TAB 1: SUMMARY REPORT OF PENDING ASSIGNMENTS ---
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        // Metrics Overview Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Pending
                            ReportMetricCard(
                                title = "Pending",
                                count = pendingAssignments.size.toString(),
                                icon = Icons.Default.Assignment,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            // Overdue
                            ReportMetricCard(
                                title = "Overdue",
                                count = overdueAssignments.size.toString(),
                                icon = Icons.Default.ErrorOutline,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                            // Due Today
                            ReportMetricCard(
                                title = "Due Today",
                                count = dueTodayAssignments.size.toString(),
                                icon = Icons.Default.Today,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            // Upcoming
                            ReportMetricCard(
                                title = "Upcoming",
                                count = upcomingAssignments.size.toString(),
                                icon = Icons.Default.Upcoming,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        // Action row (Copy, Share, View formatted)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val reportText = generateReportText()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("StudySync Report", reportText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Summary report copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copy_report_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Report")
                            }

                            Button(
                                onClick = {
                                    val reportText = generateReportText()
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                        putExtra(Intent.EXTRA_SUBJECT, "StudySync Pending Assignments Summary")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Study Summary Report")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_report_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share Report")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Pending Assignments Breakdown (${pendingAssignments.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (pendingAssignments.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TaskAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "All Caught Up!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "No pending assignments currently due.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(pendingAssignments, key = { it.id }) { assignment ->
                            val isOverdue = assignment.dueDate < now
                            val sdf = remember { SimpleDateFormat("EEE, MMM d, yyyy • h:mm a", Locale.getDefault()) }

                            ElevatedCard(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("pending_assignment_card_${assignment.id}")
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Surface(
                                            color = if (isOverdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = if (isOverdue) "OVERDUE" else assignment.subjectName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOverdue) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        FilledTonalButton(
                                            onClick = { onCompleteTask(assignment.id) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Mark Done", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = assignment.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    if (assignment.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = assignment.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Due: ${sdf.format(Date(assignment.dueDate))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Add Task Dialog for a subject
    if (showQuickAddTaskDialog != null) {
        val targetSubjectId = showQuickAddTaskDialog!!
        AddEditTaskDialog(
            subjects = subjects,
            initialSubjectId = targetSubjectId,
            onDismiss = { showQuickAddTaskDialog = null },
            onConfirm = { subId, subName, title, desc, due, priority, isAssignment, hasReminder, minutes ->
                onAddTask(subId, subName, title, desc, due, priority, isAssignment, hasReminder, minutes)
                showQuickAddTaskDialog = null
            }
        )
    }
}

@Composable
fun ReportMetricCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
