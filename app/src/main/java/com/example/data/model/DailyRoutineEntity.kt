package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_routine")
data class DailyRoutineEntity(
    @PrimaryKey
    val id: Long = 1L,
    val morningAlarmTime: String = "06:30", // HH:mm format
    val morningAlarmEnabled: Boolean = true,
    val morningAlarmLabel: String = "Morning Study Wake-Up Alarm",
    val nightSleepTime: String = "22:45", // HH:mm format
    val nightSleepEnabled: Boolean = true,
    val nightSleepLabel: String = "Night Sleep & Wind-Down Reminder",
    val morningMotivationQuote: String = "Early morning review solidifies memory. Read for 20 mins before your day begins!",
    val nightSleepAdvice: String = "Wind down. Review today's completed notes and sleep 7-8 hours for memory consolidation.",
    val preferredStudyHour: Int = 7
)
