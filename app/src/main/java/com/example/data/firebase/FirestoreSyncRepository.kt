package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.DailyCheckInEntity
import com.example.data.model.DailyTaskEntity
import com.example.data.model.StudyMaterialEntity
import com.example.data.model.SubjectEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SyncResult(
    val success: Boolean,
    val syncedSubjectsCount: Int = 0,
    val syncedTasksCount: Int = 0,
    val syncedMaterialsCount: Int = 0,
    val syncedCheckInsCount: Int = 0,
    val message: String = "",
    val error: String? = null
)

class FirestoreSyncRepository(private val context: Context) {
    private val TAG = "FirestoreSyncRepo"
    private val firestore: FirebaseFirestore? = FirebaseManager.getFirestore(context)

    suspend fun syncAll(
        userId: String,
        localSubjects: List<SubjectEntity>,
        localTasks: List<DailyTaskEntity>,
        localMaterials: List<StudyMaterialEntity>,
        localCheckIns: List<DailyCheckInEntity>
    ): SyncResult = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext SyncResult(
            success = false,
            message = "Firestore is not available (Offline mode enabled)",
            error = "Firestore instance is null"
        )

        try {
            val userDoc = db.collection("users").document(userId)

            // 1. Push Subjects
            val subjectsColl = userDoc.collection("subjects")
            val batch = db.batch()
            for (sub in localSubjects) {
                val docRef = subjectsColl.document(sub.id.toString())
                val data = mapOf(
                    "id" to sub.id,
                    "name" to sub.name,
                    "code" to sub.code,
                    "colorHex" to sub.colorHex,
                    "description" to sub.description,
                    "targetDailyMinutes" to sub.targetDailyMinutes,
                    "isUpToDate" to sub.isUpToDate,
                    "lastCheckedDate" to sub.lastCheckedDate,
                    "notes" to sub.notes,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // 2. Push Tasks
            val tasksColl = userDoc.collection("tasks")
            for (task in localTasks) {
                val docRef = tasksColl.document(task.id.toString())
                val data = mapOf(
                    "id" to task.id,
                    "subjectId" to task.subjectId,
                    "subjectName" to task.subjectName,
                    "title" to task.title,
                    "description" to task.description,
                    "dueDate" to task.dueDate,
                    "priority" to task.priority,
                    "isCompleted" to task.isCompleted,
                    "completedAt" to (task.completedAt ?: 0L),
                    "isAssignment" to task.isAssignment,
                    "hasReminder" to task.hasReminder,
                    "reminderTime" to (task.reminderTime ?: 0L),
                    "estimatedMinutes" to task.estimatedMinutes,
                    "createdAt" to task.createdAt,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // 3. Push Materials (PDF Metadata & study notes)
            val materialsColl = userDoc.collection("materials")
            for (mat in localMaterials) {
                val docRef = materialsColl.document(mat.id.toString())
                val data = mapOf(
                    "id" to mat.id,
                    "subjectId" to mat.subjectId,
                    "subjectName" to mat.subjectName,
                    "title" to mat.title,
                    "filePath" to mat.filePath,
                    "fileSizeFormatted" to mat.fileSizeFormatted,
                    "pageCount" to mat.pageCount,
                    "currentPage" to mat.currentPage,
                    "lastOpenedTimestamp" to mat.lastOpenedTimestamp,
                    "notes" to mat.notes,
                    "isCloudSynced" to true,
                    "cloudUrl" to mat.cloudUrl,
                    "documentType" to mat.documentType,
                    "documentNumber" to mat.documentNumber,
                    "issuer" to mat.issuer,
                    "updatedAt" to System.currentTimeMillis()
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // 4. Push Check-ins
            val checkinsColl = userDoc.collection("dailyCheckIns")
            for (ci in localCheckIns) {
                val docRef = checkinsColl.document(ci.dateString)
                val data = mapOf(
                    "dateString" to ci.dateString,
                    "allSubjectsUpToDate" to ci.allSubjectsUpToDate,
                    "completedTasksCount" to ci.completedTasksCount,
                    "totalTasksCount" to ci.totalTasksCount,
                    "notes" to ci.notes,
                    "checkedTimestamp" to ci.checkedTimestamp
                )
                batch.set(docRef, data, SetOptions.merge())
            }

            // Commit all changes in batch
            batch.commit().await()

            // Update user metadata doc
            userDoc.set(
                mapOf(
                    "lastSyncTimestamp" to System.currentTimeMillis(),
                    "totalSubjects" to localSubjects.size,
                    "totalTasks" to localTasks.size,
                    "totalMaterials" to localMaterials.size,
                    "clientPlatform" to "Android"
                ),
                SetOptions.merge()
            ).await()

            SyncResult(
                success = true,
                syncedSubjectsCount = localSubjects.size,
                syncedTasksCount = localTasks.size,
                syncedMaterialsCount = localMaterials.size,
                syncedCheckInsCount = localCheckIns.size,
                message = "Synchronized ${localSubjects.size} subjects, ${localTasks.size} tasks, and ${localMaterials.size} PDFs with Firestore Cloud"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firestore sync error", e)
            SyncResult(
                success = false,
                message = "Sync failed: ${e.localizedMessage ?: "Network or permission error"}",
                error = e.message
            )
        }
    }

    suspend fun fetchRemoteData(userId: String): RemoteDataSnapshot = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext RemoteDataSnapshot()
        try {
            val userDoc = db.collection("users").document(userId)

            val subjectsSnap = userDoc.collection("subjects").get().await()
            val remoteSubjects = subjectsSnap.documents.mapNotNull { doc ->
                try {
                    SubjectEntity(
                        id = doc.getLong("id") ?: 0L,
                        name = doc.getString("name") ?: "",
                        code = doc.getString("code") ?: "",
                        colorHex = doc.getString("colorHex") ?: "#3B82F6",
                        description = doc.getString("description") ?: "",
                        targetDailyMinutes = doc.getLong("targetDailyMinutes")?.toInt() ?: 45,
                        isUpToDate = doc.getBoolean("isUpToDate") ?: false,
                        lastCheckedDate = doc.getString("lastCheckedDate") ?: "",
                        notes = doc.getString("notes") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val tasksSnap = userDoc.collection("tasks").get().await()
            val remoteTasks = tasksSnap.documents.mapNotNull { doc ->
                try {
                    DailyTaskEntity(
                        id = doc.getLong("id") ?: 0L,
                        subjectId = doc.getLong("subjectId") ?: 0L,
                        subjectName = doc.getString("subjectName") ?: "",
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        dueDate = doc.getLong("dueDate") ?: System.currentTimeMillis(),
                        priority = doc.getString("priority") ?: "MEDIUM",
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        completedAt = doc.getLong("completedAt")?.takeIf { it > 0 },
                        isAssignment = doc.getBoolean("isAssignment") ?: false,
                        hasReminder = doc.getBoolean("hasReminder") ?: false,
                        reminderTime = doc.getLong("reminderTime")?.takeIf { it > 0 },
                        estimatedMinutes = doc.getLong("estimatedMinutes")?.toInt() ?: 30,
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val materialsSnap = userDoc.collection("materials").get().await()
            val remoteMaterials = materialsSnap.documents.mapNotNull { doc ->
                try {
                    StudyMaterialEntity(
                        id = doc.getLong("id") ?: 0L,
                        subjectId = doc.getLong("subjectId") ?: 0L,
                        subjectName = doc.getString("subjectName") ?: "",
                        title = doc.getString("title") ?: "",
                        filePath = doc.getString("filePath") ?: "",
                        fileSizeFormatted = doc.getString("fileSizeFormatted") ?: "1.2 MB",
                        pageCount = doc.getLong("pageCount")?.toInt() ?: 1,
                        currentPage = doc.getLong("currentPage")?.toInt() ?: 1,
                        lastOpenedTimestamp = doc.getLong("lastOpenedTimestamp") ?: System.currentTimeMillis(),
                        notes = doc.getString("notes") ?: "",
                        isCloudSynced = true,
                        cloudUrl = doc.getString("cloudUrl") ?: "",
                        documentType = doc.getString("documentType") ?: "STUDY",
                        documentNumber = doc.getString("documentNumber") ?: "",
                        issuer = doc.getString("issuer") ?: ""
                    )
                } catch (e: Exception) {
                    null
                }
            }

            RemoteDataSnapshot(
                subjects = remoteSubjects,
                tasks = remoteTasks,
                materials = remoteMaterials
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote data", e)
            RemoteDataSnapshot()
        }
    }
}

data class RemoteDataSnapshot(
    val subjects: List<SubjectEntity> = emptyList(),
    val tasks: List<DailyTaskEntity> = emptyList(),
    val materials: List<StudyMaterialEntity> = emptyList()
)
