package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.DocumentEntity
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentInfoDialog(
    document: DocumentEntity,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val lastOpenedDate = dateFormat.format(Date(document.lastOpenedTimestamp))

    val sizeFormatted = when {
        document.fileSizeBytes > 1024 * 1024 -> String.format(Locale.US, "%.1f MB", document.fileSizeBytes / (1024f * 1024f))
        document.fileSizeBytes > 1024 -> String.format(Locale.US, "%.1f KB", document.fileSizeBytes / 1024f)
        document.fileSizeBytes > 0 -> "${document.fileSizeBytes} Bytes"
        else -> "Sample Resource"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Document Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                InfoRow(label = "Document Name", value = document.title)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Slate400.copy(alpha = 0.2f))

                InfoRow(label = "Format", value = document.fileType)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Slate400.copy(alpha = 0.2f))

                val unit = if (document.fileType == "PPTX" || document.fileType == "PPT") "Slides" else "Pages"
                InfoRow(label = "Total Length", value = "${document.pageCount} $unit")
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Slate400.copy(alpha = 0.2f))

                InfoRow(label = "Last Read", value = "$unit ${document.lastPageRead} (${document.progressPercentage}%)")
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Slate400.copy(alpha = 0.2f))

                InfoRow(label = "File Size", value = sizeFormatted)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Slate400.copy(alpha = 0.2f))

                InfoRow(label = "Last Opened", value = lastOpenedDate)

                if (document.summary.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Slate400.copy(alpha = 0.2f))
                    InfoRow(label = "Summary", value = document.summary)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Slate500,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
