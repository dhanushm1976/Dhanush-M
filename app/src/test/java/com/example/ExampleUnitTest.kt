package com.example

import com.example.data.model.DailyCheckInEntity
import com.example.data.model.DailyTaskEntity
import com.example.data.model.StudyMaterialEntity
import com.example.data.model.SubjectEntity
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSubjectEntityCreation() {
        val subject = SubjectEntity(
            id = 1L,
            name = "Organic Chemistry",
            code = "CHEM 201",
            colorHex = "#10B981",
            description = "Reaction mechanisms and synthesis",
            targetDailyMinutes = 45,
            isUpToDate = true,
            lastCheckedDate = "2026-09-24",
            notes = "Exam next Tuesday"
        )
        assertEquals("Organic Chemistry", subject.name)
        assertEquals("CHEM 201", subject.code)
        assertTrue(subject.isUpToDate)
        assertEquals(45, subject.targetDailyMinutes)
    }

    @Test
    fun testDailyTaskEntityDefaultsAndCompletion() {
        val task = DailyTaskEntity(
            id = 10L,
            subjectId = 1L,
            subjectName = "Organic Chemistry",
            title = "Complete Reaction Set 3",
            dueDate = 1700000000000L,
            priority = "HIGH",
            isCompleted = false,
            isAssignment = true
        )
        assertFalse(task.isCompleted)
        assertTrue(task.isAssignment)
        assertEquals("HIGH", task.priority)

        val completed = task.copy(isCompleted = true, completedAt = 1700000050000L)
        assertTrue(completed.isCompleted)
        assertNotNull(completed.completedAt)
    }

    @Test
    fun testStudyMaterialEntityCreation() {
        val material = StudyMaterialEntity(
            id = 5L,
            subjectId = 1L,
            subjectName = "Organic Chemistry",
            title = "Chapter 4 Reaction Notes.pdf",
            filePath = "/storage/emulated/0/Download/chem_ch4.pdf",
            pageCount = 35,
            currentPage = 12,
            notes = "Important: check page 14 resonance structures"
        )
        assertEquals(35, material.pageCount)
        assertEquals(12, material.currentPage)
        assertEquals("Chapter 4 Reaction Notes.pdf", material.title)
    }

    @Test
    fun testDailyCheckInEntityCreation() {
        val checkIn = DailyCheckInEntity(
            dateString = "2026-09-24",
            allSubjectsUpToDate = true,
            completedTasksCount = 5,
            totalTasksCount = 6,
            notes = "All subjects reviewed today"
        )
        assertEquals("2026-09-24", checkIn.dateString)
        assertTrue(checkIn.allSubjectsUpToDate)
        assertEquals(5, checkIn.completedTasksCount)
    }

    @Test
    fun testCloudSyncUiStatePersonalizedStorage() {
        val syncState = com.example.ui.viewmodel.CloudSyncUiState(
            isSyncing = false,
            lastSyncTime = "10:30 AM",
            syncStatusMessage = "Personalized Cloud Vault Synced",
            userEmail = "dhanushmanupushpa@gmail.com",
            userDisplayName = "Dhanush",
            isUserSignedIn = true,
            isAnonymous = false,
            userUid = "user_xyz12345",
            authProvider = "email",
            syncedItemsCount = 18
        )
        assertTrue(syncState.isUserSignedIn)
        assertFalse(syncState.isAnonymous)
        assertEquals("email", syncState.authProvider)
        assertEquals("dhanushmanupushpa@gmail.com", syncState.userEmail)
        assertEquals("user_xyz12345", syncState.userUid)
        assertEquals(18, syncState.syncedItemsCount)
    }

    @Test
    fun testAuthUserStateDefaultAndSigned() {
        val guestState = com.example.data.firebase.AuthUserState()
        assertFalse(guestState.isSignedIn)
        assertEquals("local_device_user", guestState.uid)

        val signedState = com.example.data.firebase.AuthUserState(
            isSignedIn = true,
            isAnonymous = false,
            displayName = "Dhanush",
            email = "dhanushmanupushpa@gmail.com",
            uid = "firebase_uid_987",
            authProvider = "email"
        )
        assertTrue(signedState.isSignedIn)
        assertEquals("firebase_uid_987", signedState.uid)
        assertEquals("email", signedState.authProvider)
    }
}

