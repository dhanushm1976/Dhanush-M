package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val title: String,
    val description: String = "",
    val dueDate: Long = System.currentTimeMillis(),
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isAssignment: Boolean = false,
    val hasReminder: Boolean = false,
    val reminderTime: Long? = null,
    val estimatedMinutes: Int = 30,
    val createdAt: Long = System.currentTimeMillis()
)
