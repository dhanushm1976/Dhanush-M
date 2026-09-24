package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.StudyMaterialEntity
import com.example.utils.PdfViewerHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    material: StudyMaterialEntity,
    onBack: () -> Unit,
    onPageChanged: (newPage: Int) -> Unit,
    onSaveNotes: (notes: String) -> Unit
) {
    var currentPageIndex by remember { mutableIntStateOf(material.currentPage - 1) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }
    var notesText by remember { mutableStateOf(material.notes) }
    var showNotesSheet by remember { mutableStateOf(false) }

    val file = remember(material.filePath) { File(material.filePath) }
    val totalPages = material.pageCount.coerceAtLeast(1)

    // Load page bitmap when currentPageIndex changes
    LaunchedEffect(currentPageIndex, material.filePath) {
        isLoadingPage = true
        val bmp = PdfViewerHelper.renderPageBitmap(file, currentPageIndex)
        pageBitmap = bmp
        isLoadingPage = false
        onPageChanged(currentPageIndex + 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "${material.subjectName} • Page ${currentPageIndex + 1} of $totalPages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pdf_viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Documents"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNotesSheet = !showNotesSheet },
                        modifier = Modifier.testTag("pdf_notes_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (notesText.isNotBlank()) {
                                    Badge { Text("✓") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Study Notes"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Page navigation row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (currentPageIndex > 0) {
                                    currentPageIndex -= 1
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.testTag("pdf_prev_page")
                        ) {
                            Icon(Icons.Default.NavigateBefore, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }

                        // Page badge
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "${currentPageIndex + 1} / $totalPages",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (currentPageIndex < totalPages - 1) {
                                    currentPageIndex += 1
                                }
                            },
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.testTag("pdf_next_page")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.NavigateNext, contentDescription = null)
                        }
                    }

                    if (totalPages > 1) {
                        Slider(
                            value = (currentPageIndex + 1).toFloat(),
                            onValueChange = { currentPageIndex = (it.toInt() - 1).coerceIn(0, totalPages - 1) },
                            valueRange = 1f..totalPages.toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth().testTag("pdf_page_slider")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            if (isLoadingPage) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (pageBitmap != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unable to render PDF page. File may be corrupted or unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Quick Notes Bottom Sheet / Dialog
            if (showNotesSheet) {
                AlertDialog(
                    onDismissRequest = { showNotesSheet = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Study Notes & Formulas")
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Keep annotations, bookmarks, and key equations attached to this document:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                placeholder = { Text("e.g. Remember to memorize equation 4 on page 2 before test...") },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pdf_notes_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onSaveNotes(notesText)
                                showNotesSheet = false
                            },
                            modifier = Modifier.testTag("pdf_save_notes_button")
                        ) {
                            Text("Save Notes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotesSheet = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}
