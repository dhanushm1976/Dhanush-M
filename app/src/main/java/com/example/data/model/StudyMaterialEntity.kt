package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_materials")
data class StudyMaterialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long = 0,
    val subjectName: String = "",
    val title: String,
    val filePath: String,
    val fileSizeFormatted: String = "1.2 MB",
    val pageCount: Int = 1,
    val currentPage: Int = 1,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isCloudSynced: Boolean = true,
    val cloudUrl: String = "",
    val documentType: String = "STUDY", // "STUDY", "AADHAAR", "PAN", "ID_CARD", "OTHER"
    val documentNumber: String = "", // e.g. masked Aadhaar number, PAN, Student ID
    val issuer: String = "" // e.g. "UIDAI", "Income Tax Department", "University"
) {
    val isIdentityDocument: Boolean
        get() = documentType != "STUDY"

    val displayTypeLabel: String
        get() = when (documentType) {
            "AADHAAR" -> "Aadhaar Card"
            "PAN" -> "PAN Card"
            "ID_CARD" -> "Student ID Card"
            "OTHER" -> "Official ID / Certificate"
            else -> "Study Material"
        }
}

