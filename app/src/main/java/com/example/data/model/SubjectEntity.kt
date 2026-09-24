package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val colorHex: String = "#3B82F6",
    val description: String = "",
    val targetDailyMinutes: Int = 45,
    val isUpToDate: Boolean = false,
    val lastCheckedDate: String = "",
    val notes: String = ""
)
