package com.example.ui.prioritize

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.VariantPrediction
import com.example.data.repository.FirebaseRepository
import com.example.ui.theme.StatusBenign
import com.example.ui.theme.StatusBenignContainer
import com.example.ui.theme.StatusPathogenic
import com.example.ui.theme.StatusPathogenicContainer
import com.example.ui.theme.StatusVUS
import com.example.ui.theme.StatusVUSContainer
import kotlinx.coroutines.launch

@Composable
fun SavedPredictionsScreen(
    currentUserId: String,
    repository: FirebaseRepository,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val predictions by repository.observePredictions(currentUserId).collectAsStateWithLifecycle(initialValue = null)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Pathogenic, Benign, VUS
    var exportReportDialogContent by remember { mutableStateOf<String?>(null) }
    var itemToDelete by remember { mutableStateOf<VariantPrediction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Saved Variant Analyses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${predictions?.size ?: 0} records archived in cloud",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (!predictions.isNullOrEmpty()) {
                IconButton(
                    onClick = {
                        val report = buildString {
                            appendLine("=== GENEVARIANT MUTATION REPORT ===")
                            appendLine("Generated for User: $currentUserId")
                            appendLine("Total Variants: ${predictions?.size}")
                            appendLine("-----------------------------------")
                            predictions?.forEach { p ->
                                appendLine("${p.gene} | Chr ${p.chromosome}:${p.position} ${p.refAllele}>${p.altAllele}")
                                appendLine("Classification: ${p.predictedLabel} (${(p.riskScore * 100).toInt()}% risk)")
                                if (!p.notes.isNullOrBlank()) appendLine("Notes: ${p.notes}")
                                appendLine("---")
                            }
                        }
                        exportReportDialogContent = report
                        coroutineScope.launch {
                            repository.logActivity(
                                type = "export",
                                title = "Exported Variant Report",
                                description = "Generated clinical summary for ${predictions?.size} mutations."
                            )
                        }
                    },
                    modifier = Modifier.testTag("btn_export_report")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export Report")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter saved by gene, Chr, or note...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("input_search_saved"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("All", "Pathogenic", "Benign", "VUS").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (predictions == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val filtered = predictions!!.filter { pred ->
                val matchesSearch = pred.gene.contains(searchQuery, ignoreCase = true) ||
                        pred.chromosome.contains(searchQuery, ignoreCase = true) ||
                        pred.variationId?.contains(searchQuery, ignoreCase = true) == true ||
                        pred.notes?.contains(searchQuery, ignoreCase = true) == true

                val matchesFilter = when (selectedFilter) {
                    "Pathogenic" -> pred.predictedLabel.contains("Pathogenic", ignoreCase = true)
                    "Benign" -> pred.predictedLabel.contains("Benign", ignoreCase = true)
                    "VUS" -> pred.predictedLabel.contains("VUS", ignoreCase = true)
                    else -> true
                }
                matchesSearch && matchesFilter
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved variant analyses found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { item ->
                        val (bg, fg) = when (item.predictedLabel) {
                            "Pathogenic", "Likely Pathogenic" -> StatusPathogenicContainer to StatusPathogenic
                            "Benign", "Likely Benign" -> StatusBenignContainer to StatusBenign
                            else -> StatusVUSContainer to StatusVUS
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("card_saved_${item.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.gene,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(bg)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.predictedLabel,
                                                color = fg,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }

                                    IconButton(onClick = { itemToDelete = item }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete prediction",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "GRCh38 Chr ${item.chromosome}:${item.position} (${item.refAllele} > ${item.altAllele}) • ${item.variantType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Risk: ${(item.riskScore * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Conf: ${(item.confidence * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { item.riskScore.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                    color = fg,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                item.notes?.let { note ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Notes: $note",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete confirmation dialog
        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Saved Analysis?") },
                text = { Text("Are you sure you want to remove the analysis for ${item.gene} (Chr ${item.chromosome}:${item.position})?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.deletePrediction(item.id)
                                snackbarHostState.showSnackbar("Analysis deleted.")
                                itemToDelete = null
                            }
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Export report dialog
        exportReportDialogContent?.let { report ->
            AlertDialog(
                onDismissRequest = { exportReportDialogContent = null },
                title = { Text("Clinical Analysis Summary") },
                text = {
                    Column {
                        Text(
                            text = report,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { exportReportDialogContent = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
