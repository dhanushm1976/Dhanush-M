package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIdentityDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        docType: String,
        title: String,
        docNumber: String,
        issuer: String,
        fileUri: Uri?,
        notes: String
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf("AADHAAR") }
    var title by remember { mutableStateOf("My Aadhaar Card") }
    var docNumber by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("UIDAI - Govt. of India") }
    var notes by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            selectedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected Document.pdf"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_add_identity_document")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add Identity Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Document Type Selector Chips
                Text(
                    text = "Document Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "AADHAAR",
                        onClick = {
                            selectedType = "AADHAAR"
                            if (title.isBlank() || title.startsWith("My ")) title = "My Aadhaar Card"
                            issuer = "UIDAI - Govt. of India"
                        },
                        label = { Text("Aadhaar") },
                        leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == "PAN",
                        onClick = {
                            selectedType = "PAN"
                            if (title.isBlank() || title.startsWith("My ")) title = "My PAN Card"
                            issuer = "Income Tax Department"
                        },
                        label = { Text("PAN") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "ID_CARD",
                        onClick = {
                            selectedType = "ID_CARD"
                            if (title.isBlank() || title.startsWith("My ")) title = "Student College ID"
                            issuer = "College / University"
                        },
                        label = { Text("Student ID") },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == "OTHER",
                        onClick = {
                            selectedType = "OTHER"
                            if (title.isBlank() || title.startsWith("My ")) title = "Official Certificate"
                            issuer = "Issuing Authority"
                        },
                        label = { Text("Other ID") },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Label / Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Document Number Input
                OutlinedTextField(
                    value = docNumber,
                    onValueChange = { docNumber = it },
                    label = {
                        Text(
                            when (selectedType) {
                                "AADHAAR" -> "Aadhaar Number (12 digits) *"
                                "PAN" -> "PAN Number (10 alphanumeric chars) *"
                                "ID_CARD" -> "Student ID / Roll Number *"
                                else -> "Document ID / Number *"
                            }
                        )
                    },
                    placeholder = {
                        Text(
                            when (selectedType) {
                                "AADHAAR" -> "e.g. 5432 9876 1234"
                                "PAN" -> "e.g. ABCDE1234F"
                                "ID_CARD" -> "e.g. STU-2026-8904"
                                else -> "e.g. DL-042011009"
                            }
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Issuer Input
                OutlinedTextField(
                    value = issuer,
                    onValueChange = { issuer = it },
                    label = { Text("Issuing Authority") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // File Attachment (PDF)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedFileName != null) "File Attached" else "Attach PDF / Scan",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = selectedFileName ?: "Optional • Auto-generates certified card if omitted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { pdfPickerLauncher.launch("application/pdf") },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (selectedUri != null) "Change" else "Browse")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Secure Notes (Optional)") },
                    placeholder = { Text("e.g. Linked phone, validity, exam registration") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Confirm / Cancel Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    selectedType,
                                    title.trim(),
                                    docNumber.trim(),
                                    issuer.trim(),
                                    selectedUri,
                                    notes.trim()
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save to Vault")
                    }
                }
            }
        }
    }
}
