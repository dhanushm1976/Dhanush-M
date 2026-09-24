package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.StudyMaterialEntity
import com.example.ui.components.AddEditSubjectDialog
import com.example.ui.components.DailySubjectCheckInDialog
import com.example.ui.screens.*
import com.example.ui.theme.StudySyncTheme
import com.example.ui.viewmodel.StudyViewModel

enum class NavigationItem(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SUBJECTS("Subjects", Icons.Default.MenuBook),
    ROUTINE("Routine", Icons.Default.Alarm),
    DOCUMENTS("Documents", Icons.Default.FolderSpecial),
    TASKS("Tasks", Icons.Default.TaskAlt),
    REPORTS("Reports", Icons.Default.Assessment),
    SYNC("Cloud", Icons.Default.CloudSync)
}

class MainActivity : ComponentActivity() {

    private val viewModel: StudyViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudySyncTheme {
                val subjects by viewModel.subjects.collectAsStateWithLifecycle()
                val tasks by viewModel.tasks.collectAsStateWithLifecycle()
                val materials by viewModel.materials.collectAsStateWithLifecycle()
                val syncState by viewModel.syncState.collectAsStateWithLifecycle()
                val showDailyPrompt by viewModel.showDailyCheckInPrompt.collectAsStateWithLifecycle()
                val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

                val qaList by viewModel.allQa.collectAsStateWithLifecycle()
                val videoList by viewModel.allVideos.collectAsStateWithLifecycle()
                val routine by viewModel.dailyRoutine.collectAsStateWithLifecycle()
                val suggestions by viewModel.smartSuggestions.collectAsStateWithLifecycle()

                var currentNavigation by remember { mutableStateOf(NavigationItem.DASHBOARD) }
                var documentsInitialTab by remember { mutableIntStateOf(0) }
                var selectedSubjectHubId by remember { mutableStateOf<Long?>(null) }
                var showAddSubjectDialog by remember { mutableStateOf(false) }
                var selectedMaterialForReading by remember { mutableStateOf<StudyMaterialEntity?>(null) }
                val snackbarHostState = remember { SnackbarHostState() }

                // Show status messages in Snackbar
                LaunchedEffect(statusMessage) {
                    statusMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearStatusMessage()
                    }
                }

                // If reader is open, show reader screen
                if (selectedMaterialForReading != null) {
                    val mat = selectedMaterialForReading!!
                    PdfViewerScreen(
                        material = mat,
                        onBack = { selectedMaterialForReading = null },
                        onPageChanged = { newPage ->
                            viewModel.updateMaterialProgress(mat.id, newPage)
                        },
                        onSaveNotes = { notes ->
                            viewModel.updateMaterialNotes(mat.id, notes)
                        }
                    )
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isDesktopOrTablet = maxWidth >= 720.dp

                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AutoStories,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "StudySync",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${currentNavigation.label} • ${if (syncState.isOfflineMode) "Offline Ready" else "Cloud Synced"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        // Daily Subject Check Button with alert badge
                                        val subjectsWithFew = subjects.count { sub ->
                                            tasks.count { it.subjectId == sub.id && !it.isCompleted } <= 1
                                        }
                                        IconButton(
                                            onClick = { viewModel.openDailyPrompt() },
                                            modifier = Modifier.testTag("action_daily_checkin")
                                        ) {
                                            BadgedBox(
                                                badge = {
                                                    if (subjectsWithFew > 0 || !subjects.all { it.isUpToDate }) {
                                                        Badge { Text("!") }
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Checklist,
                                                    contentDescription = "Daily Subject Status Check"
                                                )
                                            }
                                        }

                                        // Cloud sync quick trigger
                                        IconButton(
                                            onClick = { viewModel.triggerCloudSync() },
                                            modifier = Modifier.testTag("action_quick_sync")
                                        ) {
                                            Icon(
                                                imageVector = if (syncState.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                                                contentDescription = "Sync",
                                                tint = if (syncState.isSyncing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            },
                            bottomBar = {
                                if (!isDesktopOrTablet) {
                                    NavigationBar(
                                        modifier = Modifier.testTag("mobile_bottom_bar")
                                    ) {
                                        NavigationItem.values().forEach { item ->
                                            val isSelected = currentNavigation == item
                                            val badgeCount = when (item) {
                                                NavigationItem.SUBJECTS -> subjects.size
                                                NavigationItem.ROUTINE -> suggestions.size
                                                NavigationItem.DOCUMENTS -> materials.size
                                                NavigationItem.TASKS -> tasks.count { !it.isCompleted }
                                                NavigationItem.REPORTS -> tasks.count { it.isAssignment && !it.isCompleted }
                                                else -> 0
                                            }

                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = { currentNavigation = item },
                                                icon = {
                                                    BadgedBox(
                                                        badge = {
                                                            if (badgeCount > 0 && item != NavigationItem.DASHBOARD && item != NavigationItem.SYNC) {
                                                                Badge { Text("$badgeCount") }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(item.icon, contentDescription = item.label)
                                                    }
                                                },
                                                label = { Text(item.label, maxLines = 1) },
                                                modifier = Modifier.testTag("nav_item_${item.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                // Desktop / Tablet Navigation Rail
                                if (isDesktopOrTablet) {
                                    NavigationRail(
                                        modifier = Modifier.testTag("desktop_nav_rail")
                                    ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        NavigationItem.values().forEach { item ->
                                            val isSelected = currentNavigation == item
                                            val badgeCount = when (item) {
                                                NavigationItem.SUBJECTS -> subjects.size
                                                NavigationItem.ROUTINE -> suggestions.size
                                                NavigationItem.DOCUMENTS -> materials.size
                                                NavigationItem.TASKS -> tasks.count { !it.isCompleted }
                                                NavigationItem.REPORTS -> tasks.count { it.isAssignment && !it.isCompleted }
                                                else -> 0
                                            }

                                            NavigationRailItem(
                                                selected = isSelected,
                                                onClick = { currentNavigation = item },
                                                icon = {
                                                    BadgedBox(
                                                        badge = {
                                                            if (badgeCount > 0 && item != NavigationItem.DASHBOARD && item != NavigationItem.SYNC) {
                                                                Badge { Text("$badgeCount") }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(item.icon, contentDescription = item.label)
                                                    }
                                                },
                                                label = { Text(item.label) },
                                                modifier = Modifier.testTag("rail_item_${item.name.lowercase()}")
                                            )
                                        }
                                    }
                                    VerticalDivider()
                                }

                                // Main Content Pane
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    when (currentNavigation) {
                                        NavigationItem.DASHBOARD -> DashboardScreen(
                                            subjects = subjects,
                                            tasks = tasks,
                                            materials = materials,
                                            syncState = syncState,
                                            onNavigateToDocuments = { tab ->
                                                documentsInitialTab = tab
                                                currentNavigation = NavigationItem.DOCUMENTS
                                            },
                                            onNavigateToTasks = { currentNavigation = NavigationItem.TASKS },
                                            onNavigateToReports = { currentNavigation = NavigationItem.REPORTS },
                                            onNavigateToSync = { currentNavigation = NavigationItem.SYNC },
                                            onNavigateToRoutine = { currentNavigation = NavigationItem.ROUTINE },
                                            onNavigateToSubjectHub = { subId ->
                                                selectedSubjectHubId = subId
                                                currentNavigation = NavigationItem.SUBJECTS
                                            },
                                            onSelectMaterial = { selectedMaterialForReading = it },
                                            onToggleTask = { id, done -> viewModel.toggleTask(id, done) },
                                            onOpenDailyCheckIn = { viewModel.openDailyPrompt() }
                                        )

                                        NavigationItem.SUBJECTS -> SubjectHubScreen(
                                            subjects = subjects,
                                            materials = materials,
                                            qaList = qaList,
                                            videos = videoList,
                                            selectedSubjectId = selectedSubjectHubId,
                                            onSelectSubject = { selectedSubjectHubId = it },
                                            onOpenPdf = { selectedMaterialForReading = it },
                                            onDeleteMaterial = { viewModel.removeMaterial(it) },
                                            onImportPdf = { uri, title, subId, subName ->
                                                viewModel.importPdfFromUri(uri, title, subId, subName)
                                            },
                                            onAddQa = { subId, subName, q, a, topic ->
                                                viewModel.addQa(subId, subName, q, a, topic)
                                            },
                                            onToggleQaMastered = { qaId, mastered ->
                                                viewModel.toggleQaMastered(qaId, mastered)
                                            },
                                            onDeleteQa = { viewModel.deleteQa(it) },
                                            onAddVideo = { subId, subName, title, url, duration, notes ->
                                                viewModel.addYouTubeVideo(subId, subName, title, url, duration, notes)
                                            },
                                            onUpdateVideoProgress = { vId, watched, completed ->
                                                viewModel.updateVideoProgress(vId, watched, completed)
                                            },
                                            onDeleteVideo = { viewModel.deleteVideo(it) },
                                            onUpdateSubjectNotes = { subId: Long, notes: String ->
                                                viewModel.updateSubjectNotes(subId, notes)
                                            },
                                            onAddNewSubject = { showAddSubjectDialog = true }
                                        )

                                        NavigationItem.ROUTINE -> DailyActivityScreen(
                                            routine = routine,
                                            suggestions = suggestions,
                                            materials = materials,
                                            onUpdateRoutine = { mTime, mEn, nTime, nEn, quote, advice ->
                                                viewModel.updateDailyRoutine(mTime, mEn, nTime, nEn, quote, advice)
                                            },
                                            onTestMorningAlarm = { viewModel.testMorningAlarm() },
                                            onTestSleepReminder = { viewModel.testSleepReminder() },
                                            onAcceptSuggestionAsTask = { viewModel.acceptSuggestionAsTask(it) },
                                            onOpenPdfSuggestion = { selectedMaterialForReading = it },
                                            onNavigateToSubjectHub = { subId, _ ->
                                                selectedSubjectHubId = subId
                                                currentNavigation = NavigationItem.SUBJECTS
                                            }
                                        )

                                        NavigationItem.DOCUMENTS -> DocumentsScreen(
                                            materials = materials,
                                            subjects = subjects,
                                            initialTab = documentsInitialTab,
                                            onSelectMaterial = { selectedMaterialForReading = it },
                                            onDeleteMaterial = { viewModel.removeMaterial(it) },
                                            onImportPdf = { uri, title, subId, subName ->
                                                viewModel.importPdfFromUri(uri, title, subId, subName)
                                            },
                                            onAddIdentityDocument = { docType, title, docNumber, issuer, fileUri, notes ->
                                                viewModel.addIdentityDocument(docType, title, docNumber, issuer, fileUri, notes)
                                            }
                                        )

                                        NavigationItem.TASKS -> TasksScreen(
                                            tasks = tasks,
                                            subjects = subjects,
                                            onAddTask = { subId, subName, title, desc, due, priority, isAssignment, hasReminder, minutes ->
                                                viewModel.addTask(subId, subName, title, desc, due, priority, isAssignment, hasReminder, minutes)
                                            },
                                            onDeleteTask = { viewModel.removeTask(it) },
                                            onToggleTask = { id, done -> viewModel.toggleTask(id, done) },
                                            onAddSubject = { name, code, color, desc, targetMinutes, notes ->
                                                viewModel.addSubject(name, code, color, desc, targetMinutes, notes)
                                            },
                                            onDeleteSubject = { viewModel.removeSubject(it) },
                                            onOpenDailyCheckIn = { viewModel.openDailyPrompt() }
                                        )

                                        NavigationItem.REPORTS -> SubjectCheckAndReportScreen(
                                            subjects = subjects,
                                            tasks = tasks,
                                            onToggleSubjectUpToDate = { id, upToDate ->
                                                viewModel.markSubjectUpToDate(id, upToDate)
                                            },
                                            onMarkAllUpToDate = { upToDate ->
                                                viewModel.markAllSubjectsUpToDate(upToDate)
                                            },
                                            onCompleteTask = { id -> viewModel.toggleTask(id, true) },
                                            onAddTask = { subId, subName, title, desc, due, priority, isAssignment, hasReminder, minutes ->
                                                viewModel.addTask(subId, subName, title, desc, due, priority, isAssignment, hasReminder, minutes)
                                            },
                                            generateReportText = {
                                                viewModel.generatePendingAssignmentsReportText()
                                            }
                                        )

                                        NavigationItem.SYNC -> CloudSyncScreen(
                                            syncState = syncState,
                                            onTriggerSync = { viewModel.triggerCloudSync() },
                                            onPullFromCloud = { viewModel.pullFromCloud() },
                                            onSignInEmail = { email, pass, onResult -> viewModel.signInWithEmail(email, pass, onResult) },
                                            onSignUpEmail = { email, pass, name, onResult -> viewModel.signUpWithEmail(email, pass, name, onResult) },
                                            onResetPassword = { email, onResult -> viewModel.sendPasswordReset(email, onResult) },
                                            onSignInGoogle = { viewModel.signInWithGoogle(this@MainActivity) },
                                            onSignInGuest = { viewModel.signInAnonymously() },
                                            onSignOut = { viewModel.signOut() },
                                            onToggleOfflineMode = { viewModel.toggleOfflineMode(it) },
                                            onExportBackupJson = { viewModel.exportStudyBackupJson() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Subject Check-in Dialog ("ask daily all subjects uptodate")
                if (showDailyPrompt) {
                    DailySubjectCheckInDialog(
                        subjects = subjects,
                        tasks = tasks,
                        onDismiss = { viewModel.dismissDailyPrompt() },
                        onToggleSubjectUpToDate = { id, isUpToDate ->
                            viewModel.markSubjectUpToDate(id, isUpToDate)
                        },
                        onMarkAllUpToDate = {
                            viewModel.markAllSubjectsUpToDate(true)
                        },
                        onQuickAddTaskForSubject = { subjectId ->
                            viewModel.dismissDailyPrompt()
                            currentNavigation = NavigationItem.TASKS
                        }
                    )
                }

                // Add Subject Modal Dialog
                if (showAddSubjectDialog) {
                    AddEditSubjectDialog(
                        onDismiss = { showAddSubjectDialog = false },
                        onConfirm = { name, code, colorHex, desc, targetMinutes, notes ->
                            viewModel.addSubject(name, code, colorHex, desc, targetMinutes, notes)
                            showAddSubjectDialog = false
                        }
                    )
                }
            }
        }
    }
}
