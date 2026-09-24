package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyMaterialEntity

@Composable
fun IdentityDocumentCard(
    doc: StudyMaterialEntity,
    onViewDocument: (StudyMaterialEntity) -> Unit,
    onDeleteDocument: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isNumberVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Card Theme Colors based on Document Type
    val (cardGradient, badgeColor, badgeTextColor, docTypeIcon) = when (doc.documentType) {
        "AADHAAR" -> QuadColor(
            gradient = listOf(Color(0xFFFFF7ED), Color(0xFFFEF3C7)),
            badge = Color(0xFFF97316),
            badgeText = Color(0xFF9A3412),
            icon = Icons.Default.Fingerprint
        )
        "PAN" -> QuadColor(
            gradient = listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE)),
            badge = Color(0xFF2563EB),
            badgeText = Color(0xFF1E40AF),
            icon = Icons.Default.Badge
        )
        "ID_CARD" -> QuadColor(
            gradient = listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF)),
            badge = Color(0xFF7C3AED),
            badgeText = Color(0xFF5B21B6),
            icon = Icons.Default.School
        )
        else -> QuadColor(
            gradient = listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
            badge = Color(0xFF475569),
            badgeText = Color(0xFF334155),
            icon = Icons.Default.Description
        )
    }

    val displayDocNumber = remember(doc.documentNumber, isNumberVisible, doc.documentType) {
        if (doc.documentNumber.isBlank()) "NO NUMBER SPECIFIED"
        else if (isNumberVisible) doc.documentNumber
        else when (doc.documentType) {
            "AADHAAR" -> {
                val clean = doc.documentNumber.replace("-", "").replace(" ", "")
                if (clean.length >= 4) "•••• •••• ${clean.takeLast(4)}" else "•••• •••• ••••"
            }
            "PAN" -> {
                if (doc.documentNumber.length >= 4) "${doc.documentNumber.take(2)}••••••${doc.documentNumber.takeLast(2)}"
                else "••••••••••"
            }
            else -> {
                if (doc.documentNumber.length > 4) "••••${doc.documentNumber.takeLast(4)}"
                else "••••••••"
            }
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("identity_card_${doc.id}")
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(cardGradient))
                .padding(16.dp)
        ) {
            // Header Row: Icon, Title & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(42.dp),
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = 0.18f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = docTypeIcon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = doc.issuer.ifBlank { doc.displayTypeLabel },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Type Badge
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (doc.documentType) {
                            "AADHAAR" -> "AADHAAR"
                            "PAN" -> "PAN CARD"
                            "ID_CARD" -> "COLLEGE ID"
                            else -> "OFFICIAL ID"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Document Number Strip with Eye Toggle & Copy Button
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DOCUMENT NUMBER",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = displayDocNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isNumberVisible = !isNumberVisible },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isNumberVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isNumberVisible) "Mask number" else "Reveal number",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (doc.documentNumber.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(doc.title, doc.documentNumber))
                                    Toast.makeText(context, "${doc.displayTypeLabel} number copied!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy number",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (doc.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = doc.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row: View Document (PDF), Share, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onViewDocument(doc) },
                    modifier = Modifier.weight(1f).testTag("button_view_id_${doc.id}"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Document")
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("button_delete_id_${doc.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Document?") },
            text = { Text("Are you sure you want to remove '${doc.title}' from your Identity Documents vault?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteDocument(doc.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class QuadColor(
    val gradient: List<Color>,
    val badge: Color,
    val badgeText: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
