package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyMaterialEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.SubjectQaEntity
import com.example.data.model.SubjectVideoEntity
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectHubScreen(
    subjects: List<SubjectEntity>,
    materials: List<StudyMaterialEntity>,
    qaList: List<SubjectQaEntity>,
    videos: List<SubjectVideoEntity>,
    selectedSubjectId: Long?,
    onSelectSubject: (Long) -> Unit,
    onOpenPdf: (StudyMaterialEntity) -> Unit,
    onDeleteMaterial: (Long) -> Unit,
    onImportPdf: (uri: Uri, title: String, subjectId: Long, subjectName: String) -> Unit,
    onAddQa: (subjectId: Long, subjectName: String, question: String, answer: String, topic: String) -> Unit,
    onToggleQaMastered: (qaId: Long, isMastered: Boolean) -> Unit,
    onDeleteQa: (qaId: Long) -> Unit,
    onAddVideo: (subjectId: Long, subjectName: String, title: String, url: String, durationMinutes: Int, notes: String) -> Unit,
    onUpdateVideoProgress: (videoId: Long, watchedMinutes: Int, isCompleted: Boolean) -> Unit,
    onDeleteVideo: (videoId: Long) -> Unit,
    onUpdateSubjectNotes: (subjectId: Long, notes: String) -> Unit,
    onAddNewSubject: () -> Unit
) {
    val activeSubject = remember(subjects, selectedSubjectId) {
        subjects.find { it.id == selectedSubjectId } ?: subjects.firstOrNull()
    }

    // 0: Study Materials (PDFs), 1: Questions & Answers (Q&A), 2: YouTube Video Lectures, 3: Subject Notes & Overview
    var selectedSectionTab by remember { mutableIntStateOf(0) }

    // Dialogs state
    var showAddQaDialog by remember { mutableStateOf(false) }
    var showAddVideoDialog by remember { mutableStateOf(false) }
    var showImportPdfDialog by remember { mutableStateOf(false) }
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var newPdfTitle by remember { mutableStateOf("") }

    // Notes editing
    var subjectNotesText by remember(activeSubject?.id) {
        mutableStateOf(activeSubject?.notes ?: "")
    }

    // Filter scoped items for active subject
    val subjectMaterials = remember(materials, activeSubject?.id) {
        materials.filter { it.subjectId == activeSubject?.id && !it.isIdentityDocument }
    }
    val subjectQa = remember(qaList, activeSubject?.id) {
        qaList.filter { it.subjectId == activeSubject?.id }
    }
    val subjectVideos = remember(videos, activeSubject?.id) {
        videos.filter { it.subjectId == activeSubject?.id }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && activeSubject != null) {
            pickedPdfUri = uri
            newPdfTitle = "${activeSubject.name} Lecture Notes"
            showImportPdfDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            if (activeSubject != null) {
                when (selectedSectionTab) {
                    0 -> ExtendedFloatingActionButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                        text = { Text("Add PDF Material") },
                        modifier = Modifier.testTag("fab_add_subject_pdf")
                    )
                    1 -> ExtendedFloatingActionButton(
                        onClick = { showAddQaDialog = true },
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) },
                        text = { Text("Add Q&A") },
                        modifier = Modifier.testTag("fab_add_subject_qa")
                    )
                    2 -> ExtendedFloatingActionButton(
                        onClick = { showAddVideoDialog = true },
                        icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                        text = { Text("Add YouTube Link") },
                        modifier = Modifier.testTag("fab_add_subject_video")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Subject Knowledge Hub",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PDF materials, practice Q&A & direct YouTube lectures",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onAddNewSubject,
                    modifier = Modifier.testTag("button_add_new_subject")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add Subject",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Subject Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(subjects, key = { it.id }) { subject ->
                    val isSelected = subject.id == activeSubject?.id
                    val subColor = try {
                        Color(android.graphics.Color.parseColor(subject.colorHex))
                    } catch (e: Exception) {
                        MaterialTheme.colorScheme.primary
                    }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSubject(subject.id) },
                        label = {
                            Text(
                                text = subject.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Surface(
                                shape = CircleShape,
                                color = subColor,
                                modifier = Modifier.size(12.dp)
                            ) {}
                        },
                        modifier = Modifier.testTag("chip_subject_${subject.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeSubject == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No subjects found. Tap '+' to create your first subject!")
                }
            } else {
                // Active Subject Detail Card
                val themeColor = try {
                    Color(android.graphics.Color.parseColor(activeSubject.colorHex))
                } catch (e: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = themeColor.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = themeColor.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = activeSubject.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = themeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = activeSubject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (activeSubject.isUpToDate) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = if (activeSubject.isUpToDate) "Up to Date" else "Needs Check-in",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeSubject.isUpToDate) Color(0xFF15803D) else Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (activeSubject.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeSubject.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target daily minutes badge & counts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🎯 Goal: ${activeSubject.targetDailyMinutes}m daily",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "📄 ${subjectMaterials.size} PDFs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "❓ ${subjectQa.size} Q&As",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "▶️ ${subjectVideos.size} Videos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subject Section Tabs: PDFs, Q&A, Videos, Notes
                SecondaryTabRow(
                    selectedTabIndex = selectedSectionTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedSectionTab == 0,
                        onClick = { selectedSectionTab = 0 },
                        text = { Text("Study PDFs (${subjectMaterials.size})") },
                        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedSectionTab == 1,
                        onClick = { selectedSectionTab = 1 },
                        text = { Text("Q&A (${subjectQa.size})") },
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedSectionTab == 2,
                        onClick = { selectedSectionTab = 2 },
                        text = { Text("YouTube (${subjectVideos.size})") },
                        icon = { Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedSectionTab == 3,
                        onClick = { selectedSectionTab = 3 },
                        text = { Text("Notes") },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Tab Content
                when (selectedSectionTab) {
                    // TAB 0: STUDY MATERIALS (PDFs)
                    0 -> {
                        if (subjectMaterials.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No PDFs linked to ${activeSubject.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Import lecture slides or notes to read offline with progress tracking.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Import Study PDF")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(subjectMaterials, key = { it.id }) { material ->
                                    PdfMaterialCard(
                                        material = material,
                                        onOpen = { onOpenPdf(material) },
                                        onDelete = { onDeleteMaterial(material.id) }
                                    )
                                }
                            }
                        }
                    }

                    // TAB 1: QUESTIONS & ANSWERS (Q&A)
                    1 -> {
                        val masteredCount = subjectQa.count { it.isMastered }
                        if (subjectQa.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Quiz,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No Q&As added for ${activeSubject.name}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add questions and answers to prepare for exams and test your recall.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = { showAddQaDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Question & Answer")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Practice Progress: $masteredCount of ${subjectQa.size} Mastered",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            LinearProgressIndicator(
                                                progress = { if (subjectQa.isNotEmpty()) masteredCount.toFloat() / subjectQa.size.toFloat() else 0f },
                                                modifier = Modifier
                                                    .width(100.dp)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }

                                items(subjectQa, key = { it.id }) { qa ->
                                    SubjectQaCard(
                                        qa = qa,
                                        onToggleMastered = { onToggleQaMastered(qa.id, it) },
                                        onDelete = { onDeleteQa(qa.id) }
                                    )
                                }
                            }
                        }
                    }

                    // TAB 2: YOUTUBE VIDEO LECTURES & PROGRESS
                    2 -> {
                        if (subjectVideos.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFFF0000).copy(alpha = 0.6f),
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No YouTube Videos Added",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Add direct YouTube video links to watch lectures and track progress.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { showAddVideoDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add YouTube Video Link")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                items(subjectVideos, key = { it.id }) { video ->
                                    SubjectVideoCard(
                                        video = video,
                                        onUpdateProgress = { watched, done ->
                                            onUpdateVideoProgress(video.id, watched, done)
                                        },
                                        onDelete = { onDeleteVideo(video.id) }
                                    )
                                }
                            }
                        }
                    }

                    // TAB 3: SUBJECT NOTES
                    3 -> {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Comprehensive Subject Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Consolidate lecture insights, formulas, and references for ${activeSubject.name}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = subjectNotesText,
                                onValueChange = { subjectNotesText = it },
                                placeholder = {
                                    Text("Enter your structured subject notes, exam tips, and key chapter concepts...")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                minLines = 8
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    onUpdateSubjectNotes(activeSubject.id, subjectNotesText)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Subject Notes")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Q&A Dialog
    if (showAddQaDialog && activeSubject != null) {
        AddSubjectQaDialog(
            subjectName = activeSubject.name,
            onDismiss = { showAddQaDialog = false },
            onConfirm = { question, answer, topic ->
                onAddQa(activeSubject.id, activeSubject.name, question, answer, topic)
                showAddQaDialog = false
            }
        )
    }

    // Add YouTube Video Dialog
    if (showAddVideoDialog && activeSubject != null) {
        AddSubjectVideoDialog(
            subjectName = activeSubject.name,
            onDismiss = { showAddVideoDialog = false },
            onConfirm = { title, url, duration, notes ->
                onAddVideo(activeSubject.id, activeSubject.name, title, url, duration, notes)
                showAddVideoDialog = false
            }
        )
    }

    // Import PDF Dialog
    if (showImportPdfDialog && pickedPdfUri != null && activeSubject != null) {
        AlertDialog(
            onDismissRequest = {
                showImportPdfDialog = false
                pickedPdfUri = null
            },
            title = { Text("Import PDF into ${activeSubject.name}") },
            text = {
                OutlinedTextField(
                    value = newPdfTitle,
                    onValueChange = { newPdfTitle = it },
                    label = { Text("Document Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pickedPdfUri!!
                        onImportPdf(uri, newPdfTitle, activeSubject.id, activeSubject.name)
                        showImportPdfDialog = false
                        pickedPdfUri = null
                    },
                    enabled = newPdfTitle.isNotBlank()
                ) {
                    Text("Import PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportPdfDialog = false
                    pickedPdfUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
