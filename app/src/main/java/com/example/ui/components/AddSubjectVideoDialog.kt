package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.utils.YoutubeHelper

@Composable
fun AddSubjectVideoDialog(
    subjectName: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String, durationMinutes: Int, notes: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("25") }
    var notes by remember { mutableStateOf("") }

    val detectedVideoId = remember(url) {
        YoutubeHelper.extractVideoId(url)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_add_youtube_video")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Add YouTube Lecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (title.isBlank() && detectedVideoId.isNotBlank()) {
                            title = "Lecture Video (YouTube)"
                        }
                    },
                    label = { Text("YouTube URL or Video Link *") },
                    placeholder = { Text("https://www.youtube.com/watch?v=...") },
                    trailingIcon = {
                        if (detectedVideoId.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Valid link",
                                tint = Color(0xFF16A34A)
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (detectedVideoId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "✓ YouTube Video ID detected: $detectedVideoId",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF16A34A)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Video Title / Topic *") },
                    placeholder = { Text("e.g. Graph Algorithms - BFS & DFS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { char -> char.isDigit() } },
                    label = { Text("Duration (in minutes) *") },
                    placeholder = { Text("e.g. 25") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Lecture Notes / Timestamp Goals") },
                    placeholder = { Text("e.g. Focus on recurrence relation derivation at 14:20") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            val duration = durationText.toIntOrNull() ?: 20
                            if (url.isNotBlank() && title.isNotBlank()) {
                                onConfirm(title.trim(), url.trim(), duration, notes.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                        enabled = url.isNotBlank() && title.isNotBlank()
                    ) {
                        Text("Add Video Link")
                    }
                }
            }
        }
    }
}
