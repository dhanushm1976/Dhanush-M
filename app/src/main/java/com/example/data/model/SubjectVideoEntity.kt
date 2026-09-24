package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject_videos")
data class SubjectVideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val title: String,
    val youtubeUrl: String,
    val videoId: String = "",
    val durationMinutes: Int = 20,
    val watchedMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = "",
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercentage: Int
        get() = if (isCompleted) {
            100
        } else if (durationMinutes > 0) {
            ((watchedMinutes.toFloat() / durationMinutes.toFloat()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

    val formattedProgress: String
        get() = "$watchedMinutes / $durationMinutes mins ($progressPercentage%)"
}
