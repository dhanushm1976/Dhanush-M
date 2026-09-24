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
import com.example.ui.components.AddIdentityDocumentDialog
import com.example.ui.components.IdentityDocumentCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    materials: List<StudyMaterialEntity>,
    subjects: List<SubjectEntity>,
    onSelectMaterial: (StudyMaterialEntity) -> Unit,
    onDeleteMaterial: (Long) -> Unit,
    onImportPdf: (uri: Uri, title: String, subjectId: Long, subjectName: String) -> Unit,
    onAddIdentityDocument: (
        docType: String,
        title: String,
        docNumber: String,
        issuer: String,
        fileUri: Uri?,
        notes: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    initialTab: Int = 0
) {
    // 0: Academic & Study PDFs, 1: Identity Documents (Aadhaar, PAN, College ID)
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }

    // Study PDFs state
    var selectedSubjectId by remember { mutableStateOf<Long?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pickedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var importTitle by remember { mutableStateOf("") }
    var importSubjectId by remember {
        mutableStateOf(subjects.firstOrNull()?.id ?: 0L)
    }

    // Identity Docs state
    var selectedIdentityFilter by remember { mutableStateOf<String?>(null) } // null = All, "AADHAAR", "PAN", "ID_CARD"
    var showAddIdentityDialog by remember { mutableStateOf(false) }

    // Partition documents
    val studyMaterials = remember(materials) { materials.filter { !it.isIdentityDocument } }
    val identityDocuments = remember(materials) { materials.filter { it.isIdentityDocument } }

    // Filtered lists
    val filteredStudyMaterials = remember(studyMaterials, searchQuery, selectedSubjectId) {
        studyMaterials.filter { mat ->
            val matchesSearch = mat.title.contains(searchQuery, ignoreCase = true) ||
                    mat.subjectName.contains(searchQuery, ignoreCase = true)
            val matchesSubject = selectedSubjectId == null || mat.subjectId == selectedSubjectId
            matchesSearch && matchesSubject
        }
    }

    val filteredIdentityDocs = remember(identityDocuments, searchQuery, selectedIdentityFilter) {
        identityDocuments.filter { doc ->
            val matchesSearch = doc.title.contains(searchQuery, ignoreCase = true) ||
                    doc.issuer.contains(searchQuery, ignoreCase = true) ||
                    doc.documentNumber.contains(searchQuery, ignoreCase = true)
            val matchesFilter = selectedIdentityFilter == null || doc.documentType == selectedIdentityFilter
            matchesSearch && matchesFilter
        }
    }

    // PDF picker launcher for academic notes
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            pickedPdfUri = uri
            importTitle = "Study Material ${studyMaterials.size + 1}"
            if (subjects.isNotEmpty() && importSubjectId == 0L) {
                importSubjectId = subjects.first().id
            }
            showImportDialog = true
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    text = { Text("Import PDF") },
                    modifier = Modifier.testTag("import_pdf_fab")
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showAddIdentityDialog = true },
                    icon = { Icon(Icons.Default.AddModerator, contentDescription = null) },
                    text = { Text("Add ID Document") },
                    modifier = Modifier.testTag("add_identity_doc_fab")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Primary Navigation Tabs: Separating Academic Study PDFs vs Identity & Official Documents
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("documents_segmented_tabs")
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        searchQuery = ""
                    },
                    text = {
                        Text(
                            text = "Study Materials (${studyMaterials.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        searchQuery = ""
                    },
                    text = {
                        Text(
                            text = "Identity Vault (${identityDocuments.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Universal Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (selectedTab == 0) "Search study notes, slides & textbooks..."
                        else "Search Aadhaar, PAN, College ID, numbers..."
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("document_search_bar")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Content based on Active Tab
            if (selectedTab == 0) {
                // --- TAB 0: STUDY MATERIALS (PDFs) ---
                // Subject Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubjectId == null,
                            onClick = { selectedSubjectId = null },
                            label = { Text("All Subjects (${studyMaterials.size})") },
                            modifier = Modifier.testTag("filter_all_subjects")
                        )
                    }
                    items(subjects, key = { it.id }) { subject ->
                        val count = studyMaterials.count { it.subjectId == subject.id }
                        FilterChip(
                            selected = selectedSubjectId == subject.id,
                            onClick = {
                                selectedSubjectId = if (selectedSubjectId == subject.id) null else subject.id
                            },
                            label = { Text("${subject.code} ($count)") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Header summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Course Reference & Lecture Vault",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline Cached",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredStudyMaterials.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Study Material PDFs Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Import lecture slides, formula sheets, or textbooks to read offline and sync with desktop.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.testTag("empty_state_import_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import PDF Material")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredStudyMaterials, key = { it.id }) { material ->
                            PdfMaterialCard(
                                material = material,
                                onOpen = { onSelectMaterial(material) },
                                onDelete = { onDeleteMaterial(material.id) }
                            )
                        }
                    }
                }
            } else {
                // --- TAB 1: IDENTITY & KYC DOCUMENTS (AADHAAR, PAN, STUDENT ID) ---
                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedIdentityFilter == null,
                            onClick = { selectedIdentityFilter = null },
                            label = { Text("All IDs (${identityDocuments.size})") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedIdentityFilter == "AADHAAR",
                            onClick = {
                                selectedIdentityFilter = if (selectedIdentityFilter == "AADHAAR") null else "AADHAAR"
                            },
                            leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Aadhaar Card") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedIdentityFilter == "PAN",
                            onClick = {
                                selectedIdentityFilter = if (selectedIdentityFilter == "PAN") null else "PAN"
                            },
                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("PAN Card") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedIdentityFilter == "ID_CARD",
                            onClick = {
                                selectedIdentityFilter = if (selectedIdentityFilter == "ID_CARD") null else "ID_CARD"
                            },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = { Text("Student College ID") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Vault Security Notice Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Partitioned Identity & KYC Vault",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Personal documents like Aadhaar, PAN, and Campus IDs are kept strictly separate from study notes with masked number protection.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredIdentityDocs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Identity Documents Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Store your Aadhaar card, PAN card, Student ID card, or exam admit slips safely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddIdentityDialog = true },
                                modifier = Modifier.testTag("empty_state_add_id_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Identity Document")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredIdentityDocs, key = { it.id }) { doc ->
                            IdentityDocumentCard(
                                doc = doc,
                                onViewDocument = { onSelectMaterial(it) },
                                onDeleteDocument = { onDeleteMaterial(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Import Dialog for Academic PDFs
    if (showImportDialog && pickedPdfUri != null) {
        val pickedUri = pickedPdfUri!!
        val activeSubject = subjects.find { it.id == importSubjectId } ?: subjects.firstOrNull()

        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pickedPdfUri = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import PDF Study Material")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = importTitle,
                        onValueChange = { importTitle = it },
                        label = { Text("Document Title *") },
                        placeholder = { Text("e.g. Chapter 4 Lecture Notes") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_pdf_title_input")
                    )

                    // Subject picker
                    Text(
                        text = "Assign to Subject",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        subjects.forEach { subject ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { importSubjectId = subject.id }
                                    .background(
                                        if (importSubjectId == subject.id)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = importSubjectId == subject.id,
                                    onClick = { importSubjectId = subject.id }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${subject.name} (${subject.code})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val subName = activeSubject?.name ?: "General"
                        onImportPdf(pickedUri, importTitle, importSubjectId, subName)
                        showImportDialog = false
                        pickedPdfUri = null
                    },
                    enabled = importTitle.isNotBlank() && activeSubject != null,
                    modifier = Modifier.testTag("confirm_import_pdf_button")
                ) {
                    Text("Save to Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pickedPdfUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Identity Document Dialog
    if (showAddIdentityDialog) {
        AddIdentityDocumentDialog(
            onDismiss = { showAddIdentityDialog = false },
            onConfirm = { docType, title, docNumber, issuer, fileUri, notes ->
                onAddIdentityDocument(docType, title, docNumber, issuer, fileUri, notes)
                showAddIdentityDialog = false
            }
        )
    }
}

@Composable
fun PdfMaterialCard(
    material: StudyMaterialEntity,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val progressFraction = (material.currentPage.toFloat() / material.pageCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("pdf_card_${material.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${material.subjectName} • ${material.fileSizeFormatted} • ${material.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open PDF Viewer") },
                            onClick = {
                                showMenu = false
                                onOpen()
                            },
                            leadingIcon = { Icon(Icons.Default.Launch, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Document", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reading Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Page ${material.currentPage} of ${material.pageCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progressFraction * 100).toInt()}% Read",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
