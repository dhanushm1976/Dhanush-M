package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_checkins")
data class DailyCheckInEntity(
    @PrimaryKey
    val dateString: String, // YYYY-MM-DD
    val allSubjectsUpToDate: Boolean = false,
    val completedTasksCount: Int = 0,
    val totalTasksCount: Int = 0,
    val notes: String = "",
    val checkedTimestamp: Long = System.currentTimeMillis()
)
