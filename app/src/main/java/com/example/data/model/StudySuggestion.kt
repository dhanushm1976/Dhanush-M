package com.example.data.model

data class StudySuggestion(
    val id: String,
    val subjectId: Long,
    val subjectName: String,
    val type: SuggestionType,
    val title: String,
    val description: String,
    val actionLabel: String,
    val estimatedMinutes: Int = 20,
    val materialId: Long? = null,
    val videoId: Long? = null,
    val isCompleted: Boolean = false
)

enum class SuggestionType {
    READ_PDF,
    COMPLETE_NOTES,
    PRACTICE_QA,
    WATCH_YOUTUBE,
    WAKEUP_ALARM,
    BEDTIME_WINDDOWN
}
