package com.example.data.repository

import com.example.data.db.StudyDao
import com.example.data.model.DailyCheckInEntity
import com.example.data.model.DailyRoutineEntity
import com.example.data.model.DailyTaskEntity
import com.example.data.model.StudyMaterialEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.SubjectQaEntity
import com.example.data.model.SubjectVideoEntity
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val dao: StudyDao) {

    // Subjects
    val allSubjects: Flow<List<SubjectEntity>> = dao.getAllSubjects()

    suspend fun insertSubject(subject: SubjectEntity): Long = dao.insertSubject(subject)

    suspend fun updateSubject(subject: SubjectEntity) = dao.updateSubject(subject)

    suspend fun deleteSubject(subjectId: Long) {
        dao.deleteTasksBySubject(subjectId)
        dao.deleteMaterialsBySubject(subjectId)
        dao.deleteSubject(subjectId)
    }

    suspend fun markSubjectUpToDate(subjectId: Long, isUpToDate: Boolean, date: String) {
        dao.updateSubjectUpToDate(subjectId, isUpToDate, date)
    }

    suspend fun markAllSubjectsUpToDate(isUpToDate: Boolean, date: String) {
        dao.updateAllSubjectsUpToDate(isUpToDate, date)
    }

    // Tasks
    val allTasks: Flow<List<DailyTaskEntity>> = dao.getAllTasks()

    fun getTasksForSubject(subjectId: Long): Flow<List<DailyTaskEntity>> =
        dao.getTasksForSubject(subjectId)

    suspend fun insertTask(task: DailyTaskEntity): Long = dao.insertTask(task)

    suspend fun updateTask(task: DailyTaskEntity) = dao.updateTask(task)

    suspend fun deleteTask(taskId: Long) = dao.deleteTask(taskId)

    suspend fun toggleTaskCompleted(taskId: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        dao.setTaskCompleted(taskId, isCompleted, completedAt)
    }

    // Study Materials (PDFs)
    val allMaterials: Flow<List<StudyMaterialEntity>> = dao.getAllMaterials()

    fun getMaterialsForSubject(subjectId: Long): Flow<List<StudyMaterialEntity>> =
        dao.getMaterialsForSubject(subjectId)

    suspend fun getMaterialById(id: Long): StudyMaterialEntity? = dao.getMaterialById(id)

    suspend fun insertMaterial(material: StudyMaterialEntity): Long = dao.insertMaterial(material)

    suspend fun updateMaterial(material: StudyMaterialEntity) = dao.updateMaterial(material)

    suspend fun deleteMaterial(id: Long) = dao.deleteMaterial(id)

    suspend fun updateMaterialProgress(id: Long, page: Int) {
        dao.updateMaterialProgress(id, page, System.currentTimeMillis())
    }

    suspend fun updateMaterialNotes(id: Long, notes: String) {
        dao.updateMaterialNotes(id, notes)
    }

    // Daily Check-ins
    fun getCheckInForDate(date: String): Flow<DailyCheckInEntity?> = dao.getCheckInForDate(date)

    val allCheckIns: Flow<List<DailyCheckInEntity>> = dao.getAllCheckIns()

    suspend fun saveDailyCheckIn(checkIn: DailyCheckInEntity) = dao.insertOrUpdateCheckIn(checkIn)

    suspend fun getSubjectCount(): Int = dao.getSubjectCount()

    suspend fun getIdentityDocumentCount(): Int = dao.getIdentityDocumentCount()

    // Q&A
    val allQa: Flow<List<SubjectQaEntity>> = dao.getAllQa()
    fun getQaForSubject(subjectId: Long): Flow<List<SubjectQaEntity>> = dao.getQaForSubject(subjectId)
    suspend fun insertQa(qa: SubjectQaEntity): Long = dao.insertQa(qa)
    suspend fun updateQa(qa: SubjectQaEntity) = dao.updateQa(qa)
    suspend fun deleteQa(id: Long) = dao.deleteQa(id)
    suspend fun toggleQaMastered(id: Long, isMastered: Boolean) = dao.toggleQaMastered(id, isMastered)
    suspend fun getQaCount(): Int = dao.getQaCount()
    suspend fun insertAllQa(list: List<SubjectQaEntity>) = dao.insertAllQa(list)

    // YouTube Videos & Progress
    val allVideos: Flow<List<SubjectVideoEntity>> = dao.getAllVideos()
    fun getVideosForSubject(subjectId: Long): Flow<List<SubjectVideoEntity>> = dao.getVideosForSubject(subjectId)
    suspend fun insertVideo(video: SubjectVideoEntity): Long = dao.insertVideo(video)
    suspend fun updateVideo(video: SubjectVideoEntity) = dao.updateVideo(video)
    suspend fun deleteVideo(id: Long) = dao.deleteVideo(id)
    suspend fun updateVideoProgress(id: Long, watchedMinutes: Int, isCompleted: Boolean) =
        dao.updateVideoProgress(id, watchedMinutes, isCompleted, System.currentTimeMillis())
    suspend fun getVideoCount(): Int = dao.getVideoCount()
    suspend fun insertAllVideos(list: List<SubjectVideoEntity>) = dao.insertAllVideos(list)

    // Daily Routine & Alarms
    val dailyRoutine: Flow<DailyRoutineEntity?> = dao.getDailyRoutine()
    suspend fun getDailyRoutineSync(): DailyRoutineEntity? = dao.getDailyRoutineSync()
    suspend fun saveDailyRoutine(routine: DailyRoutineEntity) = dao.insertOrUpdateRoutine(routine)

    // Backup & Cloud Sync Restore
    suspend fun importData(
        subjects: List<SubjectEntity>,
        tasks: List<DailyTaskEntity>,
        materials: List<StudyMaterialEntity>
    ) {
        if (subjects.isNotEmpty()) dao.insertAllSubjects(subjects)
        if (tasks.isNotEmpty()) dao.insertAllTasks(tasks)
        if (materials.isNotEmpty()) dao.insertAllMaterials(materials)
    }
}
