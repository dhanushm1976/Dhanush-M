package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject_qa")
data class SubjectQaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val question: String,
    val answer: String,
    val topic: String = "Core Concepts",
    val isMastered: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
