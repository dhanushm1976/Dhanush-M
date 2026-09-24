package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailyCheckInEntity
import com.example.data.model.DailyRoutineEntity
import com.example.data.model.DailyTaskEntity
import com.example.data.model.StudyMaterialEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.SubjectQaEntity
import com.example.data.model.SubjectVideoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- SUBJECTS ---
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :subjectId")
    suspend fun deleteSubject(subjectId: Long)

    @Query("UPDATE subjects SET isUpToDate = :isUpToDate, lastCheckedDate = :date WHERE id = :subjectId")
    suspend fun updateSubjectUpToDate(subjectId: Long, isUpToDate: Boolean, date: String)

    @Query("UPDATE subjects SET isUpToDate = :isUpToDate, lastCheckedDate = :date")
    suspend fun updateAllSubjectsUpToDate(isUpToDate: Boolean, date: String)

    // --- TASKS ---
    @Query("SELECT * FROM daily_tasks ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks WHERE subjectId = :subjectId ORDER BY dueDate ASC")
    fun getTasksForSubject(subjectId: Long): Flow<List<DailyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTaskEntity): Long

    @Update
    suspend fun updateTask(task: DailyTaskEntity)

    @Query("DELETE FROM daily_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: Long)

    @Query("DELETE FROM daily_tasks WHERE subjectId = :subjectId")
    suspend fun deleteTasksBySubject(subjectId: Long)

    @Query("UPDATE daily_tasks SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: Long, isCompleted: Boolean, completedAt: Long?)

    // --- STUDY MATERIALS (PDFs) ---
    @Query("SELECT * FROM study_materials ORDER BY lastOpenedTimestamp DESC")
    fun getAllMaterials(): Flow<List<StudyMaterialEntity>>

    @Query("SELECT * FROM study_materials WHERE subjectId = :subjectId ORDER BY title ASC")
    fun getMaterialsForSubject(subjectId: Long): Flow<List<StudyMaterialEntity>>

    @Query("SELECT * FROM study_materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialById(id: Long): StudyMaterialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMaterial(material: StudyMaterialEntity): Long

    @Update
    suspend fun updateMaterial(material: StudyMaterialEntity)

    @Query("DELETE FROM study_materials WHERE id = :id")
    suspend fun deleteMaterial(id: Long)

    @Query("DELETE FROM study_materials WHERE subjectId = :subjectId")
    suspend fun deleteMaterialsBySubject(subjectId: Long)

    @Query("UPDATE study_materials SET currentPage = :page, lastOpenedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateMaterialProgress(id: Long, page: Int, timestamp: Long)

    @Query("UPDATE study_materials SET notes = :notes WHERE id = :id")
    suspend fun updateMaterialNotes(id: Long, notes: String)

    // --- DAILY CHECK-INS ---
    @Query("SELECT * FROM daily_checkins WHERE dateString = :date LIMIT 1")
    fun getCheckInForDate(date: String): Flow<DailyCheckInEntity?>

    @Query("SELECT * FROM daily_checkins ORDER BY dateString DESC")
    fun getAllCheckIns(): Flow<List<DailyCheckInEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCheckIn(checkIn: DailyCheckInEntity)

    // --- CLOUD SYNC BULK OPERATIONS ---
    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    @Query("SELECT COUNT(*) FROM study_materials WHERE documentType != 'STUDY'")
    suspend fun getIdentityDocumentCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<DailyTaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMaterials(materials: List<StudyMaterialEntity>)

    // --- SUBJECT QUESTIONS & ANSWERS (Q&A) ---
    @Query("SELECT * FROM subject_qa ORDER BY id DESC")
    fun getAllQa(): Flow<List<SubjectQaEntity>>

    @Query("SELECT * FROM subject_qa WHERE subjectId = :subjectId ORDER BY id DESC")
    fun getQaForSubject(subjectId: Long): Flow<List<SubjectQaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQa(qa: SubjectQaEntity): Long

    @Update
    suspend fun updateQa(qa: SubjectQaEntity)

    @Query("DELETE FROM subject_qa WHERE id = :id")
    suspend fun deleteQa(id: Long)

    @Query("UPDATE subject_qa SET isMastered = :isMastered WHERE id = :id")
    suspend fun toggleQaMastered(id: Long, isMastered: Boolean)

    @Query("SELECT COUNT(*) FROM subject_qa")
    suspend fun getQaCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQa(list: List<SubjectQaEntity>)

    // --- SUBJECT YOUTUBE VIDEOS & WATCH PROGRESS ---
    @Query("SELECT * FROM subject_videos ORDER BY id DESC")
    fun getAllVideos(): Flow<List<SubjectVideoEntity>>

    @Query("SELECT * FROM subject_videos WHERE subjectId = :subjectId ORDER BY id DESC")
    fun getVideosForSubject(subjectId: Long): Flow<List<SubjectVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: SubjectVideoEntity): Long

    @Update
    suspend fun updateVideo(video: SubjectVideoEntity)

    @Query("DELETE FROM subject_videos WHERE id = :id")
    suspend fun deleteVideo(id: Long)

    @Query("UPDATE subject_videos SET watchedMinutes = :watchedMinutes, isCompleted = :isCompleted, lastWatchedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateVideoProgress(id: Long, watchedMinutes: Int, isCompleted: Boolean, timestamp: Long)

    @Query("SELECT COUNT(*) FROM subject_videos")
    suspend fun getVideoCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllVideos(list: List<SubjectVideoEntity>)

    // --- DAILY ROUTINE & ALARMS ---
    @Query("SELECT * FROM daily_routine WHERE id = 1 LIMIT 1")
    fun getDailyRoutine(): Flow<DailyRoutineEntity?>

    @Query("SELECT * FROM daily_routine WHERE id = 1 LIMIT 1")
    suspend fun getDailyRoutineSync(): DailyRoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRoutine(routine: DailyRoutineEntity)
}
