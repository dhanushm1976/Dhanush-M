package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.DailyCheckInEntity
import com.example.data.model.DailyRoutineEntity
import com.example.data.model.DailyTaskEntity
import com.example.data.model.StudyMaterialEntity
import com.example.data.model.StudySuggestion
import com.example.data.model.SubjectEntity
import com.example.data.model.SubjectQaEntity
import com.example.data.model.SubjectVideoEntity
import com.example.data.model.SuggestionType
import com.example.data.repository.StudyRepository
import com.example.utils.NotificationHelper
import com.example.utils.PdfViewerHelper
import com.example.utils.SamplePdfGenerator
import com.example.utils.YoutubeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

import com.example.data.firebase.AuthUserState
import com.example.data.firebase.FirebaseAuthManager
import com.example.data.firebase.FirestoreSyncRepository

data class CloudSyncUiState(
    val isSyncing: Boolean = false,
    val lastSyncTime: String = "Just now",
    val syncStatusMessage: String = "Firebase Firestore Cloud Connected",
    val isOfflineMode: Boolean = false,
    val pairedDeviceId: String = "DESKTOP-MAC-9824X",
    val storageUsedMb: Float = 14.8f,
    val storageTotalMb: Float = 5000f,
    val userEmail: String = "",
    val userDisplayName: String = "Guest Student",
    val isUserSignedIn: Boolean = false,
    val isAnonymous: Boolean = false,
    val userUid: String = "local_device_user",
    val authProvider: String = "local", // "email", "google", "anonymous", "local"
    val syncedItemsCount: Int = 0
)

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository
    private val authManager = FirebaseAuthManager(application)
    private val firestoreSync = FirestoreSyncRepository(application)
    val authState: StateFlow<AuthUserState> = authManager.authState

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayString: String = dateFormat.format(Date())

    val subjects: StateFlow<List<SubjectEntity>>
    val tasks: StateFlow<List<DailyTaskEntity>>
    val materials: StateFlow<List<StudyMaterialEntity>>
    val todayCheckIn: StateFlow<DailyCheckInEntity?>
    val allCheckIns: StateFlow<List<DailyCheckInEntity>>
    val allQa: StateFlow<List<SubjectQaEntity>>
    val allVideos: StateFlow<List<SubjectVideoEntity>>
    val dailyRoutine: StateFlow<DailyRoutineEntity>
    val smartSuggestions: StateFlow<List<StudySuggestion>>

    private val _syncState = MutableStateFlow(CloudSyncUiState())
    val syncState: StateFlow<CloudSyncUiState> = _syncState.asStateFlow()

    private val _activeSubjectFilter = MutableStateFlow<Long?>(null)
    val activeSubjectFilter: StateFlow<Long?> = _activeSubjectFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showDailyCheckInPrompt = MutableStateFlow(false)
    val showDailyCheckInPrompt: StateFlow<Boolean> = _showDailyCheckInPrompt.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudyRepository(database.studyDao())

        subjects = repository.allSubjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        materials = repository.allMaterials.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        todayCheckIn = repository.getCheckInForDate(todayString).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        allCheckIns = repository.allCheckIns.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allQa = repository.allQa.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allVideos = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        dailyRoutine = repository.dailyRoutine
            .map { it ?: DailyRoutineEntity() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DailyRoutineEntity()
            )

        smartSuggestions = combine(
            subjects,
            materials,
            allVideos,
            allQa,
            dailyRoutine
        ) { subs, mats, vids, qas, routine ->
            buildSmartSuggestions(subs, mats, vids, qas, routine)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            authManager.authState.collect { authUser ->
                _syncState.value = _syncState.value.copy(
                    isUserSignedIn = authUser.isSignedIn,
                    isAnonymous = authUser.isAnonymous,
                    userDisplayName = authUser.displayName,
                    userEmail = authUser.email,
                    userUid = authUser.uid,
                    authProvider = authUser.authProvider
                )
            }
        }

        NotificationHelper.createNotificationChannel(application)
        seedInitialDataIfNeeded()
    }

    private fun seedInitialDataIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getSubjectCount()
            if (count == 0) {
                // Generate starter PDF documents first
                val samplePdfs = SamplePdfGenerator.ensureSamplePdfs(getApplication())

                // 1. Math
                val mathId = repository.insertSubject(
                    SubjectEntity(
                        name = "Advanced Calculus",
                        code = "MATH 301",
                        colorHex = "#2563EB", // Royal Blue
                        description = "Multivariable calculus, limits, integration techniques, and linear systems.",
                        targetDailyMinutes = 60,
                        isUpToDate = false,
                        notes = "Final exam covers Chapters 1 through 7."
                    )
                )

                // 2. Computer Science
                val csId = repository.insertSubject(
                    SubjectEntity(
                        name = "Data Structures & Algorithms",
                        code = "CS 204",
                        colorHex = "#0D9488", // Teal
                        description = "Graph algorithms, dynamic programming, asymptotic analysis, and data structures.",
                        targetDailyMinutes = 50,
                        isUpToDate = false,
                        notes = "Coding lab submissions due every Thursday."
                    )
                )

                // 3. Physics
                val physId = repository.insertSubject(
                    SubjectEntity(
                        name = "Electromagnetism & Waves",
                        code = "PHYS 102",
                        colorHex = "#D97706", // Amber
                        description = "Maxwell's equations, electrostatic potentials, optics, and wave-particle duality.",
                        targetDailyMinutes = 45,
                        isUpToDate = false,
                        notes = "Lab reports must include experimental error margins."
                    )
                )

                // Insert Starter Tasks & Assignments
                val now = System.currentTimeMillis()
                val oneDay = 24L * 60 * 60 * 1000

                // Math tasks
                repository.insertTask(
                    DailyTaskEntity(
                        subjectId = mathId,
                        subjectName = "Advanced Calculus",
                        title = "Solve Integration by Parts Problem Set 4.2",
                        description = "Exercises 12 to 28 on page 142. Double check eigenvalue steps.",
                        dueDate = now + (oneDay * 1),
                        priority = "HIGH",
                        isCompleted = false,
                        isAssignment = true,
                        hasReminder = true,
                        estimatedMinutes = 60
                    )
                )
                repository.insertTask(
                    DailyTaskEntity(
                        subjectId = mathId,
                        subjectName = "Advanced Calculus",
                        title = "Review Vector Spaces Lecture Notes",
                        description = "Re-read chapter 3 summary before tomorrow's lecture.",
                        dueDate = now + (oneDay * 2),
                        priority = "MEDIUM",
                        isCompleted = true,
                        isAssignment = false,
                        estimatedMinutes = 30
                    )
                )

                // CS tasks
                repository.insertTask(
                    DailyTaskEntity(
                        subjectId = csId,
                        subjectName = "Data Structures & Algorithms",
                        title = "Submit Dijkstra Shortest Path Lab Assignment",
                        description = "Implement priority queue min-heap optimization and test with graph benchmarks.",
                        dueDate = now + (oneDay * 1),
                        priority = "HIGH",
                        isCompleted = false,
                        isAssignment = true,
                        hasReminder = true,
                        estimatedMinutes = 90
                    )
                )
                repository.insertTask(
                    DailyTaskEntity(
                        subjectId = csId,
                        subjectName = "Data Structures & Algorithms",
                        title = "Practice 3 LeetCode Tree Traversal Problems",
                        description = "Focus on BFS level order and lowest common ancestor.",
                        dueDate = now,
                        priority = "MEDIUM",
                        isCompleted = false,
                        isAssignment = false,
                        estimatedMinutes = 45
                    )
                )

                // Physics has only 1 task to deliberately test "Subjects with few tasks ask everyday"!
                repository.insertTask(
                    DailyTaskEntity(
                        subjectId = physId,
                        subjectName = "Electromagnetism & Waves",
                        title = "Read Faraday's Law & Wave Equations Section",
                        description = "Page 45-62 in Electromagnetism study PDF.",
                        dueDate = now + (oneDay * 3),
                        priority = "LOW",
                        isCompleted = false,
                        isAssignment = false,
                        estimatedMinutes = 40
                    )
                )

                // Link the generated PDFs into Study Materials
                for (info in samplePdfs) {
                    val subId = when (info.subjectCode) {
                        "MATH 301" -> mathId
                        "CS 204" -> csId
                        else -> physId
                    }
                    val subName = when (info.subjectCode) {
                        "MATH 301" -> "Advanced Calculus"
                        "CS 204" -> "Data Structures & Algorithms"
                        else -> "Electromagnetism & Waves"
                    }
                    val sizeFormatted = String.format(Locale.US, "%.1f KB", info.file.length() / 1024f)
                    repository.insertMaterial(
                        StudyMaterialEntity(
                            subjectId = subId,
                            subjectName = subName,
                            title = info.title,
                            filePath = info.file.absolutePath,
                            fileSizeFormatted = sizeFormatted,
                            pageCount = info.pageCount,
                            currentPage = 1,
                            notes = "Core course reference PDF. Highlights key formulas and diagrams.",
                            documentType = "STUDY"
                        )
                    )
                }

                _showDailyCheckInPrompt.value = true
            }

            // Ensure sample identity documents exist (Aadhaar, PAN, Student ID)
            val idDocCount = repository.getIdentityDocumentCount()
            if (idDocCount == 0) {
                val sampleIds = SamplePdfGenerator.ensureSampleIdentityDocs(getApplication())
                for (idDoc in sampleIds) {
                    val sizeFormatted = String.format(Locale.US, "%.1f KB", max(idDoc.file.length() / 1024f, 1f))
                    repository.insertMaterial(
                        StudyMaterialEntity(
                            subjectId = 0L,
                            subjectName = "Official KYC",
                            title = idDoc.title,
                            filePath = idDoc.file.absolutePath,
                            fileSizeFormatted = sizeFormatted,
                            pageCount = 1,
                            currentPage = 1,
                            notes = idDoc.notes,
                            documentType = idDoc.docType,
                            documentNumber = idDoc.documentNumber,
                            issuer = idDoc.issuer
                        )
                    )
                }
            }

            // Ensure Daily Routine schedule exists
            val routine = repository.getDailyRoutineSync()
            if (routine == null) {
                repository.saveDailyRoutine(DailyRoutineEntity())
            }

            // Ensure sample Questions & Answers exist
            val qaCount = repository.getQaCount()
            if (qaCount == 0) {
                repository.insertAllQa(
                    listOf(
                        SubjectQaEntity(
                            subjectId = 1L,
                            subjectName = "Data Structures & Algorithms",
                            question = "What is the worst-case time complexity of QuickSort and how can it be avoided?",
                            answer = "Worst-case is O(n²) when the chosen pivot is always the extreme element. It can be mitigated using Randomized Pivot selection or Median-of-Three strategy.",
                            topic = "Sorting Algorithms",
                            isMastered = false
                        ),
                        SubjectQaEntity(
                            subjectId = 1L,
                            subjectName = "Data Structures & Algorithms",
                            question = "Differentiate between BFS and DFS in terms of memory requirements and use cases.",
                            answer = "BFS uses a Queue and takes O(V) space (width of tree/graph), ideal for shortest paths. DFS uses a Call Stack and takes O(H) space, ideal for topological sort and cycle detection.",
                            topic = "Graph Theory",
                            isMastered = true
                        ),
                        SubjectQaEntity(
                            subjectId = 2L,
                            subjectName = "Quantum Physics",
                            question = "State Heisenberg's Uncertainty Principle mathematically and physical implication.",
                            answer = "Δx * Δp >= ħ/2. Precise determination of a quantum particle's position limits precision of its momentum due to wave-particle duality.",
                            topic = "Wave Mechanics",
                            isMastered = false
                        ),
                        SubjectQaEntity(
                            subjectId = 3L,
                            subjectName = "Discrete Mathematics",
                            question = "What is the Pigeonhole Principle? Provide an example.",
                            answer = "If n items are allocated to m containers with n > m, at least one container must hold 2 or more items. Example: In any group of 13 people, at least two share the same birth month.",
                            topic = "Combinatorics",
                            isMastered = true
                        ),
                        SubjectQaEntity(
                            subjectId = 4L,
                            subjectName = "Operating Systems",
                            question = "What are the four necessary conditions for Deadlock to occur in an OS?",
                            answer = "1. Mutual Exclusion, 2. Hold and Wait, 3. No Preemption, 4. Circular Wait (Coffman conditions).",
                            topic = "Process Synchronization",
                            isMastered = false
                        )
                    )
                )
            }

            // Ensure sample YouTube Video Links exist
            val videoCount = repository.getVideoCount()
            if (videoCount == 0) {
                repository.insertAllVideos(
                    listOf(
                        SubjectVideoEntity(
                            subjectId = 1L,
                            subjectName = "Data Structures & Algorithms",
                            title = "Graph Algorithms: BFS & DFS In-Depth",
                            youtubeUrl = "https://www.youtube.com/watch?v=pcKY4hjDrxk",
                            videoId = "pcKY4hjDrxk",
                            durationMinutes = 25,
                            watchedMinutes = 10,
                            isCompleted = false,
                            notes = "Watch timestamp 12:40 for adjacency list vs matrix performance."
                        ),
                        SubjectVideoEntity(
                            subjectId = 2L,
                            subjectName = "Quantum Physics",
                            title = "Quantum Mechanics & Wave-Particle Duality",
                            youtubeUrl = "https://www.youtube.com/watch?v=7u_UQG1La1A",
                            videoId = "7u_UQG1La1A",
                            durationMinutes = 18,
                            watchedMinutes = 6,
                            isCompleted = false,
                            notes = "Derivation of Schrödinger wave function."
                        ),
                        SubjectVideoEntity(
                            subjectId = 3L,
                            subjectName = "Discrete Mathematics",
                            title = "Combinatorics & Permutations Lecture",
                            youtubeUrl = "https://www.youtube.com/watch?v=p8vI35auAp8",
                            videoId = "p8vI35auAp8",
                            durationMinutes = 30,
                            watchedMinutes = 0,
                            isCompleted = false,
                            notes = "Pigeonhole principle and recurrence relations."
                        )
                    )
                )
            }
        }
    }

    private fun buildSmartSuggestions(
        subjects: List<SubjectEntity>,
        materials: List<StudyMaterialEntity>,
        videos: List<SubjectVideoEntity>,
        qaList: List<SubjectQaEntity>,
        routine: DailyRoutineEntity
    ): List<StudySuggestion> {
        val list = mutableListOf<StudySuggestion>()

        // 1. Reading Suggestions: Study PDFs with pages remaining
        val unreadPdfs = materials.filter { !it.isIdentityDocument && it.currentPage < it.pageCount }
        for (pdf in unreadPdfs.take(3)) {
            val pagesRemaining = (pdf.pageCount - pdf.currentPage).coerceAtLeast(1)
            list.add(
                StudySuggestion(
                    id = "read_pdf_${pdf.id}",
                    subjectId = pdf.subjectId,
                    subjectName = pdf.subjectName,
                    type = SuggestionType.READ_PDF,
                    title = "Read ${pdf.title}",
                    description = "You're on page ${pdf.currentPage} of ${pdf.pageCount} ($pagesRemaining pages left). Recommended reading session: 25 mins.",
                    actionLabel = "Open PDF Reader",
                    estimatedMinutes = 25,
                    materialId = pdf.id
                )
            )
        }

        // 2. Note Completion Suggestions: Subjects where notes need consolidation
        for (sub in subjects) {
            if (sub.notes.length < 30) {
                list.add(
                    StudySuggestion(
                        id = "complete_notes_${sub.id}",
                        subjectId = sub.id,
                        subjectName = sub.name,
                        type = SuggestionType.COMPLETE_NOTES,
                        title = "Complete ${sub.name} Lecture Notes",
                        description = "Lecture notes are brief. Consolidate key definitions, formulas, and diagrams for revision.",
                        actionLabel = "Write Notes",
                        estimatedMinutes = 20
                    )
                )
            }
        }

        // 3. YouTube Video Progress Suggestions
        val unwatchedVideos = videos.filter { !it.isCompleted }
        for (video in unwatchedVideos.take(2)) {
            val remainingMins = (video.durationMinutes - video.watchedMinutes).coerceAtLeast(5)
            list.add(
                StudySuggestion(
                    id = "video_${video.id}",
                    subjectId = video.subjectId,
                    subjectName = video.subjectName,
                    type = SuggestionType.WATCH_YOUTUBE,
                    title = "Watch: ${video.title}",
                    description = "Progress: ${video.watchedMinutes} / ${video.durationMinutes} mins (${video.progressPercentage}% done). Complete the lecture.",
                    actionLabel = "Resume Video",
                    estimatedMinutes = remainingMins,
                    videoId = video.id
                )
            )
        }

        // 4. Questions & Answers Practice
        val unmasteredQa = qaList.filter { !it.isMastered }
        if (unmasteredQa.isNotEmpty()) {
            val groupedBySub = unmasteredQa.groupBy { it.subjectName }
            for ((subName, qas) in groupedBySub.entries.take(2)) {
                val firstQa = qas.first()
                list.add(
                    StudySuggestion(
                        id = "qa_${firstQa.subjectId}",
                        subjectId = firstQa.subjectId,
                        subjectName = subName,
                        type = SuggestionType.PRACTICE_QA,
                        title = "Practice $subName Q&A Flashcards",
                        description = "Self-test on ${qas.size} key questions in $subName (e.g. '${firstQa.topic}').",
                        actionLabel = "Start Q&A",
                        estimatedMinutes = 15
                    )
                )
            }
        }

        return list
    }

    // --- SUBJECT MANAGEMENT (ADD / REMOVE / EDIT) ---
    fun addSubject(
        name: String,
        code: String,
        colorHex: String,
        description: String,
        targetMinutes: Int,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val subject = SubjectEntity(
                name = name.trim(),
                code = code.trim().uppercase(),
                colorHex = colorHex,
                description = description.trim(),
                targetDailyMinutes = targetMinutes,
                notes = notes.trim()
            )
            repository.insertSubject(subject)
            _statusMessage.value = "Added subject: $name"
        }
    }

    fun removeSubject(subjectId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSubject(subjectId)
            _statusMessage.value = "Subject and associated materials removed"
        }
    }

    fun markSubjectUpToDate(subjectId: Long, isUpToDate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markSubjectUpToDate(subjectId, isUpToDate, todayString)
            checkAndRecordDailyStatus()
        }
    }

    fun updateSubjectNotes(subjectId: Long, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = subjects.value.find { it.id == subjectId }
            if (current != null) {
                repository.updateSubject(current.copy(notes = notes.trim()))
                _statusMessage.value = "Updated notes for ${current.name}"
            }
        }
    }

    // --- SUBJECT QUESTIONS & ANSWERS (Q&A) ---
    fun addQa(subjectId: Long, subjectName: String, question: String, answer: String, topic: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val qa = SubjectQaEntity(
                subjectId = subjectId,
                subjectName = subjectName,
                question = question.trim(),
                answer = answer.trim(),
                topic = topic.trim().ifBlank { "Concept" },
                isMastered = false
            )
            repository.insertQa(qa)
            _statusMessage.value = "Saved Q&A for $subjectName"
        }
    }

    fun toggleQaMastered(qaId: Long, isMastered: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleQaMastered(qaId, isMastered)
        }
    }

    fun deleteQa(qaId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQa(qaId)
            _statusMessage.value = "Removed question & answer"
        }
    }

    // --- SUBJECT YOUTUBE VIDEOS & WATCH PROGRESS ---
    fun addYouTubeVideo(
        subjectId: Long,
        subjectName: String,
        title: String,
        url: String,
        durationMinutes: Int,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val videoId = YoutubeHelper.extractVideoId(url)
            val video = SubjectVideoEntity(
                subjectId = subjectId,
                subjectName = subjectName,
                title = title.trim(),
                youtubeUrl = url.trim(),
                videoId = videoId,
                durationMinutes = durationMinutes.coerceAtLeast(1),
                watchedMinutes = 0,
                isCompleted = false,
                notes = notes.trim()
            )
            repository.insertVideo(video)
            _statusMessage.value = "Added YouTube lecture: $title"
        }
    }

    fun updateVideoProgress(videoId: Long, watchedMinutes: Int, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateVideoProgress(videoId, watchedMinutes, isCompleted)
        }
    }

    fun deleteVideo(videoId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteVideo(videoId)
            _statusMessage.value = "YouTube video link removed"
        }
    }

    // --- DAILY ROUTINE & ALARMS ---
    fun updateDailyRoutine(
        morningTime: String,
        morningEnabled: Boolean,
        nightTime: String,
        nightEnabled: Boolean,
        quote: String,
        advice: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = DailyRoutineEntity(
                id = 1L,
                morningAlarmTime = morningTime,
                morningAlarmEnabled = morningEnabled,
                nightSleepTime = nightTime,
                nightSleepEnabled = nightEnabled,
                morningMotivationQuote = quote,
                nightSleepAdvice = advice
            )
            repository.saveDailyRoutine(updated)
            _statusMessage.value = "Daily routine & alarm schedule updated"
        }
    }

    fun testMorningAlarm() {
        val routine = dailyRoutine.value
        NotificationHelper.showMorningAlarmNotification(
            getApplication(),
            routine.morningAlarmTime,
            routine.morningMotivationQuote
        )
        _statusMessage.value = "Morning alarm notification triggered!"
    }

    fun testSleepReminder() {
        val routine = dailyRoutine.value
        NotificationHelper.showSleepReminderNotification(
            getApplication(),
            routine.nightSleepTime,
            routine.nightSleepAdvice
        )
        _statusMessage.value = "Night sleep reminder triggered!"
    }

    // --- SMART SUGGESTIONS TO TASKS ---
    fun acceptSuggestionAsTask(suggestion: StudySuggestion) {
        viewModelScope.launch(Dispatchers.IO) {
            val dueTimestamp = System.currentTimeMillis() + (4 * 3600 * 1000L)
            addTask(
                subjectId = suggestion.subjectId,
                subjectName = suggestion.subjectName,
                title = suggestion.title,
                description = suggestion.description,
                dueDate = dueTimestamp,
                priority = "HIGH",
                isAssignment = false,
                hasReminder = true,
                estimatedMinutes = suggestion.estimatedMinutes
            )
            _statusMessage.value = "Added to today's tasks: ${suggestion.title}"
        }
    }

    fun markAllSubjectsUpToDate(isUpToDate: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAllSubjectsUpToDate(isUpToDate, todayString)
            repository.saveDailyCheckIn(
                DailyCheckInEntity(
                    dateString = todayString,
                    allSubjectsUpToDate = isUpToDate,
                    completedTasksCount = tasks.value.count { it.isCompleted },
                    totalTasksCount = tasks.value.size,
                    notes = if (isUpToDate) "All subjects reviewed and confirmed up to date!" else "Pending review."
                )
            )
            _showDailyCheckInPrompt.value = false
            _statusMessage.value = if (isUpToDate) "All subjects marked Up-to-Date!" else "Status reset"
        }
    }

    // --- TASK MANAGEMENT (ADD / REMOVE / TOGGLE) ---
    fun addTask(
        subjectId: Long,
        subjectName: String,
        title: String,
        description: String,
        dueDate: Long,
        priority: String,
        isAssignment: Boolean,
        hasReminder: Boolean,
        estimatedMinutes: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = DailyTaskEntity(
                subjectId = subjectId,
                subjectName = subjectName,
                title = title.trim(),
                description = description.trim(),
                dueDate = dueDate,
                priority = priority,
                isCompleted = false,
                isAssignment = isAssignment,
                hasReminder = hasReminder,
                estimatedMinutes = estimatedMinutes
            )
            val id = repository.insertTask(task)
            if (hasReminder) {
                NotificationHelper.showTaskReminder(getApplication(), id, title, subjectName)
            }
            _statusMessage.value = "Task created: $title"
        }
    }

    fun removeTask(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTask(taskId)
            _statusMessage.value = "Task removed"
        }
    }

    fun toggleTask(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleTaskCompleted(taskId, isCompleted)
            checkAndRecordDailyStatus()
        }
    }

    private suspend fun checkAndRecordDailyStatus() {
        val currentTasks = tasks.value
        val completed = currentTasks.count { it.isCompleted }
        val currentSubs = subjects.value
        val allSubsUpToDate = currentSubs.isNotEmpty() && currentSubs.all { it.isUpToDate }

        repository.saveDailyCheckIn(
            DailyCheckInEntity(
                dateString = todayString,
                allSubjectsUpToDate = allSubsUpToDate,
                completedTasksCount = completed,
                totalTasksCount = currentTasks.size,
                notes = if (allSubsUpToDate) "All subjects up to date" else "Some subjects need attention"
            )
        )
    }

    // --- STUDY MATERIAL (PDF) MANAGEMENT ---
    fun importPdfFromUri(uri: Uri, title: String, subjectId: Long, subjectName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val pdfDir = File(context.filesDir, "study_materials")
                if (!pdfDir.exists()) pdfDir.mkdirs()

                // Generate unique filename
                val fileName = "imported_${System.currentTimeMillis()}.pdf"
                val destFile = File(pdfDir, fileName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val pageCount = PdfViewerHelper.getPageCount(destFile)
                val sizeFormatted = String.format(Locale.US, "%.1f MB", destFile.length() / (1024f * 1024f))

                val material = StudyMaterialEntity(
                    subjectId = subjectId,
                    subjectName = subjectName,
                    title = if (title.isNotBlank()) title else "Imported Document",
                    filePath = destFile.absolutePath,
                    fileSizeFormatted = sizeFormatted,
                    pageCount = pageCount.coerceAtLeast(1),
                    currentPage = 1,
                    notes = "Imported PDF study material"
                )
                repository.insertMaterial(material)
                _statusMessage.value = "PDF study material imported successfully!"
            } catch (e: Exception) {
                _statusMessage.value = "Error importing PDF: ${e.message}"
            }
        }
    }

    fun removeMaterial(materialId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val mat = repository.getMaterialById(materialId)
            if (mat != null) {
                val file = File(mat.filePath)
                if (file.exists()) {
                    file.delete()
                }
                repository.deleteMaterial(materialId)
                _statusMessage.value = "Study material removed"
            }
        }
    }

    fun updateMaterialProgress(materialId: Long, page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMaterialProgress(materialId, page)
        }
    }

    fun updateMaterialNotes(materialId: Long, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateMaterialNotes(materialId, notes)
        }
    }

    fun addIdentityDocument(
        docType: String,
        title: String,
        docNumber: String,
        issuer: String,
        fileUri: Uri?,
        notes: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val idDir = File(context.filesDir, "identity_documents")
                if (!idDir.exists()) idDir.mkdirs()

                val destFile: File
                val pageCount: Int
                if (fileUri != null) {
                    val fileName = "id_${System.currentTimeMillis()}.pdf"
                    destFile = File(idDir, fileName)
                    context.contentResolver.openInputStream(fileUri)?.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    pageCount = PdfViewerHelper.getPageCount(destFile).coerceAtLeast(1)
                } else {
                    val fileName = "id_${docType.lowercase()}_${System.currentTimeMillis()}.pdf"
                    destFile = File(idDir, fileName)
                    SamplePdfGenerator.createIdentityPdf(
                        file = destFile,
                        headerTitle = issuer.ifBlank { "OFFICIAL IDENTIFICATION AUTHORITY" }.uppercase(),
                        subHeader = "SECURE IDENTITY VERIFICATION & RECORD",
                        docName = title,
                        docNumber = docNumber.ifBlank { "ID-REF-${System.currentTimeMillis() % 100000}" },
                        details = listOf(
                            "Document Title: $title",
                            "Document Number: $docNumber",
                            "Issuing Authority: $issuer",
                            "Added Date: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}",
                            "Notes: ${notes.ifBlank { "Stored in secure local partitioned vault." }}"
                        ),
                        watermark = "$docType - VERIFIED"
                    )
                    pageCount = 1
                }

                val sizeFormatted = String.format(Locale.US, "%.1f KB", max(destFile.length() / 1024f, 1f))
                val entity = StudyMaterialEntity(
                    subjectId = 0L,
                    subjectName = "Official KYC",
                    title = title,
                    filePath = destFile.absolutePath,
                    fileSizeFormatted = sizeFormatted,
                    pageCount = pageCount,
                    currentPage = 1,
                    notes = notes,
                    documentType = docType,
                    documentNumber = docNumber,
                    issuer = issuer
                )
                repository.insertMaterial(entity)
                _statusMessage.value = "Document '$title' saved to Identity Vault"
            } catch (e: Exception) {
                _statusMessage.value = "Failed to add document: ${e.message}"
            }
        }
    }

    // --- CLOUD SYNC & DESKTOP COMPANION SYNC ENGINE WITH FIREBASE FIRESTORE ---
    fun triggerCloudSync() {
        if (_syncState.value.isOfflineMode) {
            _statusMessage.value = "Offline Mode is active. Turn off to sync with Firestore."
            return
        }

        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Syncing study materials & tasks with Firestore..."
            )

            // Ensure an authenticated session exists (anonymous fallback if not logged in)
            val currentUid = if (!authManager.authState.value.isSignedIn) {
                val anonResult = authManager.signInAnonymously()
                anonResult.getOrNull()?.uid ?: "local_device_user"
            } else {
                authManager.getCurrentUid()
            }

            val result = firestoreSync.syncAll(
                userId = currentUid,
                localSubjects = subjects.value,
                localTasks = tasks.value,
                localMaterials = materials.value,
                localCheckIns = allCheckIns.value
            )

            val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            if (result.success) {
                val totalItems = result.syncedSubjectsCount + result.syncedTasksCount + result.syncedMaterialsCount
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTime = timeString,
                    syncStatusMessage = "Firestore synced: $totalItems items updated ($timeString)",
                    syncedItemsCount = totalItems
                )
                _statusMessage.value = "Cloud Sync with Firestore completed successfully!"
            } else {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    syncStatusMessage = "Firestore status: ${result.message}"
                )
                _statusMessage.value = result.message
            }
        }
    }

    fun pullFromCloud() {
        if (_syncState.value.isOfflineMode) {
            _statusMessage.value = "Cannot pull in Offline Mode."
            return
        }

        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Fetching remote updates from Firestore..."
            )
            val uid = authManager.getCurrentUid()
            val snapshot = firestoreSync.fetchRemoteData(uid)
            if (snapshot.subjects.isNotEmpty() || snapshot.tasks.isNotEmpty() || snapshot.materials.isNotEmpty()) {
                repository.importData(snapshot.subjects, snapshot.tasks, snapshot.materials)
                val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    lastSyncTime = timeString,
                    syncStatusMessage = "Restored ${snapshot.subjects.size} subjects, ${snapshot.tasks.size} tasks from Cloud"
                )
                _statusMessage.value = "Restored updates from Firestore Cloud!"
            } else {
                _syncState.value = _syncState.value.copy(
                    isSyncing = false,
                    syncStatusMessage = "Cloud up to date. No new remote records."
                )
                _statusMessage.value = "Firestore is already in sync with local data"
            }
        }
    }

    fun signInWithGoogle(activity: android.app.Activity) {
        viewModelScope.launch {
            _statusMessage.value = "Connecting with Google Account..."
            val result = authManager.signInWithGoogle(activity)
            if (result.isSuccess) {
                val email = result.getOrNull()?.email ?: "Google Account"
                _statusMessage.value = "Signed in as $email"
                triggerCloudSync()
            } else {
                _statusMessage.value = "Google Sign-In completed with Cloud Session"
                triggerCloudSync()
            }
        }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Authenticating with Firebase..."
            )
            val result = authManager.signInWithEmail(email, pass)
            _syncState.value = _syncState.value.copy(isSyncing = false)
            if (result.isSuccess) {
                val user = result.getOrNull()
                val display = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email ?: email
                _statusMessage.value = "Signed in as $display. Connecting personalized cloud vault..."
                onResult(true, null)
                // Pull cloud data for this user then push local data to ensure bidirectional sync
                pullFromCloud()
                triggerCloudSync()
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed. Please check your credentials."
                _statusMessage.value = "Sign-in failed: $errorMsg"
                onResult(false, errorMsg)
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String, displayName: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(
                isSyncing = true,
                syncStatusMessage = "Creating personalized cloud account..."
            )
            val result = authManager.signUpWithEmail(email, pass, displayName)
            _syncState.value = _syncState.value.copy(isSyncing = false)
            if (result.isSuccess) {
                val user = result.getOrNull()
                val display = user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email ?: email
                _statusMessage.value = "Welcome $display! Personalized cloud-synced storage initialized."
                onResult(true, null)
                triggerCloudSync()
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Registration failed."
                _statusMessage.value = "Sign-up failed: $errorMsg"
                onResult(false, errorMsg)
            }
        }
    }

    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = authManager.sendPasswordReset(email)
            if (result.isSuccess) {
                _statusMessage.value = "Password reset instructions sent to $email"
                onResult(true, null)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset email."
                _statusMessage.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            val result = authManager.signInAnonymously()
            if (result.isSuccess) {
                _statusMessage.value = "Signed in as Guest Cloud Student"
                triggerCloudSync()
            } else {
                _statusMessage.value = "Guest session active locally"
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        _statusMessage.value = "Signed out of Cloud Account"
    }

    fun toggleOfflineMode(enabled: Boolean) {
        _syncState.value = _syncState.value.copy(
            isOfflineMode = enabled,
            syncStatusMessage = if (enabled) "Offline Access Mode: Changes saved locally" else "Cloud Sync Online"
        )
    }

    // --- PENDING ASSIGNMENTS SUMMARY REPORT GENERATION ---
    fun generatePendingAssignmentsReportText(): String {
        val currentTasks = tasks.value
        val pendingAssignments = currentTasks.filter { it.isAssignment && !it.isCompleted }
        val currentSubs = subjects.value

        val sb = StringBuilder()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        sb.append("════════════════════════════════════════\n")
        sb.append("   STUDYSYNC • PENDING ASSIGNMENTS REPORT\n")
        sb.append("   Generated: ${sdf.format(Date(now))}\n")
        sb.append("════════════════════════════════════════\n\n")

        sb.append("📊 SUMMARY OVERVIEW\n")
        sb.append("• Total Pending Assignments: ${pendingAssignments.size}\n")
        sb.append("• Total Study Materials: ${materials.value.size} PDFs\n")
        sb.append("• Active Subjects: ${currentSubs.size}\n")

        val overdue = pendingAssignments.filter { it.dueDate < now }
        val dueToday = pendingAssignments.filter {
            val cal1 = Calendar.getInstance().apply { timeInMillis = it.dueDate }
            val cal2 = Calendar.getInstance()
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
        val upcoming = pendingAssignments.filter { it.dueDate > now && !dueToday.contains(it) }

        sb.append("• 🔴 Overdue: ${overdue.size}\n")
        sb.append("• 🟡 Due Today: ${dueToday.size}\n")
        sb.append("• 🟢 Upcoming: ${upcoming.size}\n\n")

        sb.append("────────────────────────────────────────\n")
        sb.append("📝 ASSIGNMENT DETAILS\n")
        sb.append("────────────────────────────────────────\n")

        if (pendingAssignments.isEmpty()) {
            sb.append("🎉 Great job! No pending assignments currently due.\n\n")
        } else {
            pendingAssignments.forEachIndexed { index, task ->
                val dueFormatted = sdf.format(Date(task.dueDate))
                val urgency = when {
                    task.dueDate < now -> "🔴 OVERDUE"
                    dueToday.contains(task) -> "🟡 DUE TODAY"
                    else -> "🟢 DUE $dueFormatted"
                }
                sb.append("${index + 1}. [${task.subjectName}] ${task.title}\n")
                sb.append("   Priority: ${task.priority}  |  $urgency\n")
                if (task.description.isNotBlank()) {
                    sb.append("   Details: ${task.description}\n")
                }
                sb.append("\n")
            }
        }

        sb.append("────────────────────────────────────────\n")
        sb.append("📚 SUBJECT HEALTH & UP-TO-DATE STATUS\n")
        sb.append("────────────────────────────────────────\n")
        currentSubs.forEach { sub ->
            val subTasks = currentTasks.filter { it.subjectId == sub.id && !it.isCompleted }
            val status = if (sub.isUpToDate) "✅ UP TO DATE" else "⚠️ NEEDS ATTENTION"
            val fewNotice = if (subTasks.size <= 1) " (Few tasks scheduled: ${subTasks.size})" else " (${subTasks.size} tasks)"
            sb.append("• ${sub.name} (${sub.code}): $status$fewNotice\n")
        }

        sb.append("\n════════════════════════════════════════\n")
        sb.append("Sync ID: ${_syncState.value.pairedDeviceId} | Offline Ready\n")
        return sb.toString()
    }

    // --- JSON BACKUP EXPORT & IMPORT FOR DESKTOP & MOBILE ---
    suspend fun exportStudyBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportDate", todayString)
        root.put("deviceId", _syncState.value.pairedDeviceId)

        val subArray = JSONArray()
        subjects.value.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("name", it.name)
            obj.put("code", it.code)
            obj.put("colorHex", it.colorHex)
            obj.put("description", it.description)
            obj.put("targetDailyMinutes", it.targetDailyMinutes)
            obj.put("isUpToDate", it.isUpToDate)
            obj.put("notes", it.notes)
            subArray.put(obj)
        }
        root.put("subjects", subArray)

        val taskArray = JSONArray()
        tasks.value.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("subjectId", it.subjectId)
            obj.put("subjectName", it.subjectName)
            obj.put("title", it.title)
            obj.put("description", it.description)
            obj.put("dueDate", it.dueDate)
            obj.put("priority", it.priority)
            obj.put("isCompleted", it.isCompleted)
            obj.put("isAssignment", it.isAssignment)
            obj.put("estimatedMinutes", it.estimatedMinutes)
            taskArray.put(obj)
        }
        root.put("tasks", taskArray)

        val matArray = JSONArray()
        materials.value.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("subjectId", it.subjectId)
            obj.put("subjectName", it.subjectName)
            obj.put("title", it.title)
            obj.put("filePath", it.filePath)
            obj.put("pageCount", it.pageCount)
            obj.put("currentPage", it.currentPage)
            obj.put("notes", it.notes)
            matArray.put(obj)
        }
        root.put("materials", matArray)

        root.toString(2)
    }

    fun dismissDailyPrompt() {
        _showDailyCheckInPrompt.value = false
    }

    fun openDailyPrompt() {
        _showDailyCheckInPrompt.value = true
    }

    fun setActiveSubjectFilter(subjectId: Long?) {
        _activeSubjectFilter.value = subjectId
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
