package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.SubjectEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskDialog(
    subjects: List<SubjectEntity>,
    initialSubjectId: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        subjectId: Long,
        subjectName: String,
        title: String,
        description: String,
        dueDate: Long,
        priority: String,
        isAssignment: Boolean,
        hasReminder: Boolean,
        estimatedMinutes: Int
    ) -> Unit
) {
    if (subjects.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("No Subjects Available") },
            text = { Text("Please add at least one subject before creating a task.") },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    var selectedSubjectId by remember {
        mutableStateOf(initialSubjectId ?: subjects.first().id)
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var isAssignment by remember { mutableStateOf(false) }
    var hasReminder by remember { mutableStateOf(true) }
    var estimatedMinutes by remember { mutableIntStateOf(45) }

    // Due date (default tomorrow)
    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
        }
    }
    var dueDateMillis by remember { mutableLongStateOf(calendar.timeInMillis) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }

    var expandedSubjectMenu by remember { mutableStateOf(false) }

    val currentSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isAssignment) Icons.Default.Assignment else Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAssignment) "Add New Assignment" else "Add Daily Study Task",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject Selector Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedSubjectMenu,
                    onExpandedChange = { expandedSubjectMenu = !expandedSubjectMenu }
                ) {
                    OutlinedTextField(
                        value = "${currentSubject.name} (${currentSubject.code})",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubjectMenu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("task_subject_selector")
                    )
                    ExposedDropdownMenu(
                        expanded = expandedSubjectMenu,
                        onDismissRequest = { expandedSubjectMenu = false }
                    ) {
                        subjects.forEach { subject ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    runCatching { Color(android.graphics.Color.parseColor(subject.colorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${subject.name} (${subject.code})")
                                    }
                                },
                                onClick = {
                                    selectedSubjectId = subject.id
                                    expandedSubjectMenu = false
                                }
                            )
                        }
                    }
                }

                // Task Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("e.g. Read Chapter 4 or Solve Practice Set") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input")
                )

                // Description / Notes
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes / Details (Optional)") },
                    placeholder = { Text("Equations, page numbers, instructions...") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_desc_input")
                )

                // Assignment Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Is this an Assignment / Project?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Included in the Pending Assignments Summary Report",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isAssignment,
                        onCheckedChange = { isAssignment = it },
                        modifier = Modifier.testTag("task_is_assignment_switch")
                    )
                }

                // Priority Row
                Column {
                    Text(
                        text = "Priority Level",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                            val isSelected = priority == p
                            val pColor = when (p) {
                                "HIGH" -> MaterialTheme.colorScheme.error
                                "MEDIUM" -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { priority = p },
                                label = { Text(p) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = pColor.copy(alpha = 0.2f),
                                    selectedLabelColor = pColor
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Reminder & Estimated time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (hasReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reminder Notification",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it },
                        modifier = Modifier.testTag("task_reminder_switch")
                    )
                }

                // Due Date Display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Due: ${dateFormat.format(Date(dueDateMillis))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(
                        onClick = {
                            // Advance due date by 1 day
                            dueDateMillis += 24L * 60 * 60 * 1000
                        }
                    ) {
                        Text("+1 Day")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            selectedSubjectId,
                            currentSubject.name,
                            title,
                            description,
                            dueDateMillis,
                            priority,
                            isAssignment,
                            hasReminder,
                            estimatedMinutes
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("task_confirm_button")
            ) {
                Text("Add Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("task_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
