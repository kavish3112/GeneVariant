package com.example.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.data.service.GeminiVariantAnalysisResult
import com.example.data.service.GeminiVariantAnalysisService
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
import com.example.data.repository.GenePrioritizationEngine
import com.example.data.repository.PrioritizationResult
import com.example.ui.theme.StatusBenign
import com.example.ui.theme.StatusBenignContainer
import com.example.ui.theme.StatusPathogenic
import com.example.ui.theme.StatusPathogenicContainer
import com.example.ui.theme.StatusVUS
import com.example.ui.theme.StatusVUSContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    currentUserId: String,
    repository: FirebaseRepository,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val savedVariants by repository.observePredictions(currentUserId).collectAsStateWithLifecycle(initialValue = null)

    // Input form state
    var isInputExpanded by remember { mutableStateOf(true) }
    var selectedGene by remember { mutableStateOf("BRCA1") }
    var geneDropdownExpanded by remember { mutableStateOf(false) }
    var chromosome by remember { mutableStateOf("17") }
    var positionText by remember { mutableStateOf("43044295") }
    var refAllele by remember { mutableStateOf("C") }
    var altAllele by remember { mutableStateOf("T") }
    var variantType by remember { mutableStateOf("single nucleotide variant") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var variationId by remember { mutableStateOf("VCV000017684") }
    var clinicalNotes by remember { mutableStateOf("") }

    var latestResult by remember { mutableStateOf<PrioritizationResult?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveSucceeded by remember { mutableStateOf(false) }

    // Gemini AI Analysis state
    var geminiResult by remember { mutableStateOf<GeminiVariantAnalysisResult?>(null) }
    var isAnalyzingGemini by remember { mutableStateOf(false) }
    var geminiError by remember { mutableStateOf<String?>(null) }
    var isSavingGemini by remember { mutableStateOf(false) }
    var geminiSaveSucceeded by remember { mutableStateOf(false) }

    // List filtering state
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var variantToDelete by remember { mutableStateOf<VariantPrediction?>(null) }

    val variantTypes = listOf(
        "single nucleotide variant",
        "Deletion",
        "Duplication",
        "Indel",
        "Insertion"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. Dashboard Overview Stats ---
        item {
            val totalSaved = savedVariants?.size ?: 0
            val pathogenicCount = savedVariants?.count { it.predictedLabel.contains("Pathogenic", ignoreCase = true) } ?: 0
            val benignCount = savedVariants?.count { it.predictedLabel.contains("Benign", ignoreCase = true) } ?: 0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Saved Variants",
                    value = totalSaved.toString(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    onColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pathogenic",
                    value = pathogenicCount.toString(),
                    color = StatusPathogenicContainer,
                    onColor = StatusPathogenic
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Benign",
                    value = benignCount.toString(),
                    color = StatusBenignContainer,
                    onColor = StatusBenign
                )
            }
        }

        // --- 2. Gene Variant Input Section ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_variant_input"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Biotech,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Input Gene Variant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { isInputExpanded = !isInputExpanded }) {
                            Icon(
                                imageVector = if (isInputExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isInputExpanded) "Collapse input" else "Expand input"
                            )
                        }
                    }

                    AnimatedVisibility(visible = isInputExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Benchmark Quick Presets
                            Text(
                                text = "Benchmark Presets:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(GenePrioritizationEngine.BENCHMARK_VARIANTS.take(4)) { variant ->
                                    SuggestionChip(
                                        onClick = {
                                            selectedGene = variant.geneSymbol
                                            chromosome = variant.chromosome
                                            positionText = variant.position.toString()
                                            refAllele = variant.refAllele
                                            altAllele = variant.altAllele
                                            variantType = variant.variantType
                                            variationId = variant.variationId
                                            latestResult = null
                                            saveSucceeded = false
                                        },
                                        label = { Text("${variant.geneSymbol} (${variant.clinicalSignificance})") }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Gene Selection
                            ExposedDropdownMenuBox(
                                expanded = geneDropdownExpanded,
                                onExpandedChange = { geneDropdownExpanded = !geneDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedGene,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Gene Symbol") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = geneDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("dashboard_dropdown_gene")
                                )
                                ExposedDropdownMenu(
                                    expanded = geneDropdownExpanded,
                                    onDismissRequest = { geneDropdownExpanded = false }
                                ) {
                                    GenePrioritizationEngine.SUPPORTED_GENES.forEach { gene ->
                                        DropdownMenuItem(
                                            text = { Text(gene) },
                                            onClick = {
                                                selectedGene = gene
                                                geneDropdownExpanded = false
                                                val summary = GenePrioritizationEngine.GENE_SUMMARIES.find { it.symbol == gene }
                                                if (summary != null) chromosome = summary.chromosome
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Chromosome and Position
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = chromosome,
                                    onValueChange = { chromosome = it },
                                    label = { Text("Chr") },
                                    modifier = Modifier.weight(1f).testTag("dashboard_input_chr"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = positionText,
                                    onValueChange = { positionText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Position (VCF/Start)") },
                                    modifier = Modifier.weight(2f).testTag("dashboard_input_pos"),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Reference and Alternate Alleles
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = refAllele,
                                    onValueChange = { refAllele = it.uppercase() },
                                    label = { Text("Ref") },
                                    modifier = Modifier.weight(1f).testTag("dashboard_input_ref"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = altAllele,
                                    onValueChange = { altAllele = it.uppercase() },
                                    label = { Text("Alt") },
                                    modifier = Modifier.weight(1f).testTag("dashboard_input_alt"),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Variant Type Dropdown
                            ExposedDropdownMenuBox(
                                expanded = typeDropdownExpanded,
                                onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = variantType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Variant Type") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .testTag("dashboard_dropdown_type")
                                )
                                ExposedDropdownMenu(
                                    expanded = typeDropdownExpanded,
                                    onDismissRequest = { typeDropdownExpanded = false }
                                ) {
                                    variantTypes.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type) },
                                            onClick = {
                                                variantType = type
                                                typeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = clinicalNotes,
                                onValueChange = { clinicalNotes = it },
                                label = { Text("Clinical Notes / Patient ID (Optional)") },
                                modifier = Modifier.fillMaxWidth().testTag("dashboard_input_notes"),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Gemini AI Clinical Evaluation Button (ACMG Guidelines)
                            Button(
                                onClick = {
                                    val pos = positionText.toLongOrNull() ?: 1L
                                    isAnalyzingGemini = true
                                    geminiError = null
                                    geminiSaveSucceeded = false
                                    coroutineScope.launch {
                                        val aiRes = GeminiVariantAnalysisService.analyzeGeneVariant(
                                            gene = selectedGene,
                                            chromosome = chromosome,
                                            position = pos,
                                            refAllele = refAllele,
                                            altAllele = altAllele,
                                            variantType = variantType,
                                            clinicalNotes = clinicalNotes.ifBlank { null }
                                        )
                                        isAnalyzingGemini = false
                                        if (aiRes.isSuccess) {
                                            geminiResult = aiRes.getOrNull()
                                            snackbarHostState.showSnackbar("Gemini AI clinical guideline analysis completed.")
                                        } else {
                                            val err = aiRes.exceptionOrNull()?.localizedMessage ?: "Gemini analysis failed"
                                            geminiError = err
                                            snackbarHostState.showSnackbar(err)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("dashboard_btn_gemini_analyze"),
                                enabled = !isAnalyzingGemini
                            ) {
                                if (isAnalyzingGemini) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyzing with Gemini AI (ACMG)...")
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze with Gemini AI (ACMG Guidelines)", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Local ML Fast Prioritization Button
                            OutlinedButton(
                                onClick = {
                                    val pos = positionText.toLongOrNull() ?: 1L
                                    val pred = GenePrioritizationEngine.predictVariant(
                                        gene = selectedGene,
                                        chromosome = chromosome,
                                        position = pos,
                                        ref = refAllele,
                                        alt = altAllele,
                                        variantType = variantType
                                    )
                                    latestResult = pred
                                    saveSucceeded = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("dashboard_btn_evaluate")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Quick Prioritize (Local Model)")
                            }
                        }
                    }
                }
            }
        }

        // --- 2b. Gemini AI Loading & Results Section ---
        if (isAnalyzingGemini) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_gemini_loading"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                "Gemini AI Evaluating Variant...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Applying ACMG/AMP clinical guidelines, functional domains, and population evidence...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        geminiResult?.let { res ->
            item {
                val (badgeBg, badgeFg) = when (res.predictedLabel) {
                    "Pathogenic", "Likely Pathogenic" -> StatusPathogenicContainer to StatusPathogenic
                    "Benign", "Likely Benign" -> StatusBenignContainer to StatusBenign
                    else -> StatusVUSContainer to StatusVUS
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_gemini_result"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gemini Clinical Classification",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = res.predictedLabel,
                                    color = badgeFg,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${res.gene} Chr ${res.chromosome}:${res.position} (${res.refAllele}>${res.altAllele})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Standard: ${res.clinicalGuidelines}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pathogenicity Risk Score
                        Text(
                            text = "Pathogenicity Risk Score: ${(res.riskScore * 100).toInt()}% • Confidence: ${(res.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { res.riskScore.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = badgeFg,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        // ACMG Evidence Criteria Chips
                        if (res.acmgCriteria.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "ACMG/AMP Evidence Criteria:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(res.acmgCriteria) { criterion ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(criterion, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        // Pathomechanism
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Pathomechanism: ${res.pathomechanism}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Clinical Summary
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = res.clinicalSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Recommendations
                        if (res.actionableRecommendations.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Actionable Recommendations:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            res.actionableRecommendations.forEach { rec ->
                                Text(
                                    text = "• $rec",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Save Gemini Analysis to Firestore Button
                        Button(
                            onClick = {
                                isSavingGemini = true
                                coroutineScope.launch {
                                    val formattedNotes = buildString {
                                        append("[ACMG] ")
                                        if (res.acmgCriteria.isNotEmpty()) {
                                            append("Codes: ")
                                            append(res.acmgCriteria.take(3).joinToString(","))
                                            append("; ")
                                        }
                                        append("Mech: ")
                                        append(res.pathomechanism.take(120))
                                        if (clinicalNotes.isNotBlank()) {
                                            append("; Note: ")
                                            append(clinicalNotes.take(150))
                                        }
                                    }.take(490)

                                    val predictionRecord = VariantPrediction(
                                        userId = currentUserId,
                                        gene = res.gene,
                                        chromosome = res.chromosome,
                                        position = res.position,
                                        refAllele = res.refAllele,
                                        altAllele = res.altAllele,
                                        variantType = res.variantType,
                                        variationId = variationId.ifBlank { null },
                                        predictedLabel = res.predictedLabel,
                                        riskScore = res.riskScore,
                                        confidence = res.confidence,
                                        notes = formattedNotes
                                    )
                                    val saveRes = repository.savePrediction(predictionRecord)
                                    isSavingGemini = false
                                    if (saveRes.isSuccess) {
                                        geminiSaveSucceeded = true
                                        snackbarHostState.showSnackbar("Gemini variant classification saved to Firestore.")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to save: ${saveRes.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("dashboard_btn_save_gemini"),
                            enabled = !isSavingGemini
                        ) {
                            if (isSavingGemini) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else if (geminiSaveSucceeded) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saved in Cloud Database")
                            } else {
                                Icon(Icons.Default.Bookmark, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Gemini Analysis to Firestore")
                            }
                        }
                    }
                }
            }
        }

        // --- 3. Instant Result Card & Save Action ---
        latestResult?.let { res ->
            item {
                val (badgeBg, badgeFg) = when (res.predictedLabel) {
                    "Pathogenic", "Likely Pathogenic" -> StatusPathogenicContainer to StatusPathogenic
                    "Benign", "Likely Benign" -> StatusBenignContainer to StatusBenign
                    else -> StatusVUSContainer to StatusVUS
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_latest_result"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${res.gene} Chr ${res.chromosome}:${res.position} (${res.refAllele}>${res.altAllele})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = res.predictedLabel,
                                    color = badgeFg,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pathogenicity Risk Score
                        Text(
                            text = "Pathogenicity Risk Score: ${(res.riskScore * 100).toInt()}% • Confidence: ${(res.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { res.riskScore.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = badgeFg,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Functional Domain: ${res.domainOrRegion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = res.impactAssessment,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                isSaving = true
                                coroutineScope.launch {
                                    val predictionRecord = VariantPrediction(
                                        userId = currentUserId,
                                        gene = res.gene,
                                        chromosome = res.chromosome,
                                        position = res.position,
                                        refAllele = res.refAllele,
                                        altAllele = res.altAllele,
                                        variantType = res.variantType,
                                        variationId = variationId.ifBlank { null },
                                        predictedLabel = res.predictedLabel,
                                        riskScore = res.riskScore,
                                        confidence = res.confidence,
                                        notes = clinicalNotes.ifBlank { null }
                                    )
                                    val saveRes = repository.savePrediction(predictionRecord)
                                    isSaving = false
                                    if (saveRes.isSuccess) {
                                        saveSucceeded = true
                                        snackbarHostState.showSnackbar("Variant saved to Firestore database.")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to save: ${saveRes.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("dashboard_btn_save_variant"),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else if (saveSucceeded) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saved in Cloud Database")
                            } else {
                                Icon(Icons.Default.Bookmark, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Variant to Firestore")
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Saved Variants Header & Filter Section ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Variants (Firestore)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${savedVariants?.size ?: 0} recorded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter saved by gene, Chr, or notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("dashboard_search_saved"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Pathogenic", "Benign", "VUS").forEach { chip ->
                    FilterChip(
                        selected = selectedFilter == chip,
                        onClick = { selectedFilter = chip },
                        label = { Text(chip) }
                    )
                }
            }
        }

        // --- 5. Saved Variants List (Fetched from Firestore) ---
        if (savedVariants == null) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            val filteredList = savedVariants!!.filter { v ->
                val matchesQuery = v.gene.contains(searchQuery, ignoreCase = true) ||
                        v.chromosome.contains(searchQuery, ignoreCase = true) ||
                        v.notes?.contains(searchQuery, ignoreCase = true) == true
                val matchesFilter = when (selectedFilter) {
                    "Pathogenic" -> v.predictedLabel.contains("Pathogenic", ignoreCase = true)
                    "Benign" -> v.predictedLabel.contains("Benign", ignoreCase = true)
                    "VUS" -> v.predictedLabel.contains("VUS", ignoreCase = true)
                    else -> true
                }
                matchesQuery && matchesFilter
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No saved variants match your filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Use the input form above to classify and save gene variants.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { variant ->
                    val (badgeBg, badgeFg) = when (variant.predictedLabel) {
                        "Pathogenic", "Likely Pathogenic" -> StatusPathogenicContainer to StatusPathogenic
                        "Benign", "Likely Benign" -> StatusBenignContainer to StatusBenign
                        else -> StatusVUSContainer to StatusVUS
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_variant_card_${variant.id}"),
                        shape = RoundedCornerShape(12.dp),
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
                                        text = variant.gene,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = variant.predictedLabel,
                                            color = badgeFg,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { variantToDelete = variant },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete variant",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "GRCh38 Chr ${variant.chromosome}:${variant.position} (${variant.refAllele} > ${variant.altAllele}) • ${variant.variantType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Risk: ${(variant.riskScore * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Conf: ${(variant.confidence * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { variant.riskScore.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = badgeFg,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            variant.notes?.let { note ->
                                Spacer(modifier = Modifier.height(6.dp))
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
    variantToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { variantToDelete = null },
            title = { Text("Delete Saved Variant?") },
            text = { Text("Are you sure you want to remove the record for ${target.gene} (Chr ${target.chromosome}:${target.position}) from Firestore?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.deletePrediction(target.id)
                            snackbarHostState.showSnackbar("Variant removed from Firestore.")
                            variantToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { variantToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    onColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = onColor.copy(alpha = 0.9f)
            )
        }
    }
}
