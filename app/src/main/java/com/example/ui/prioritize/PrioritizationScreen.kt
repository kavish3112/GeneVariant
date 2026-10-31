package com.example.ui.prioritize

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Warning
import com.example.data.service.GeminiVariantAnalysisResult
import com.example.data.service.GeminiVariantAnalysisService
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NotificationSettings
import com.example.data.model.VariantPrediction
import com.example.data.repository.FirebaseRepository
import com.example.data.repository.GenePrioritizationEngine
import com.example.data.repository.PrioritizationResult
import com.example.notification.PushNotificationHelper
import com.example.ui.theme.StatusBenign
import com.example.ui.theme.StatusBenignContainer
import com.example.ui.theme.StatusPathogenic
import com.example.ui.theme.StatusPathogenicContainer
import com.example.ui.theme.StatusVUS
import com.example.ui.theme.StatusVUSContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritizationScreen(
    currentUserId: String,
    repository: FirebaseRepository,
    notificationSettings: NotificationSettings?,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedGene by remember { mutableStateOf("BRCA1") }
    var geneDropdownExpanded by remember { mutableStateOf(false) }

    var chromosome by remember { mutableStateOf("17") }
    var positionText by remember { mutableStateOf("43044295") }
    var refAllele by remember { mutableStateOf("C") }
    var altAllele by remember { mutableStateOf("T") }
    var variantType by remember { mutableStateOf("single nucleotide variant") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var variationId by remember { mutableStateOf("VCV000017684") }
    var customNotes by remember { mutableStateOf("") }

    var result by remember { mutableStateOf<PrioritizationResult?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var savedSuccess by remember { mutableStateOf(false) }

    // Gemini AI Analysis state
    var geminiResult by remember { mutableStateOf<GeminiVariantAnalysisResult?>(null) }
    var isAnalyzingGemini by remember { mutableStateOf(false) }
    var isSavingGemini by remember { mutableStateOf(false) }
    var geminiSaveSuccess by remember { mutableStateOf(false) }

    val variantTypes = listOf(
        "single nucleotide variant",
        "Deletion",
        "Duplication",
        "Indel",
        "Insertion"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Hero Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Variant Prioritization Engine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "ClinVar trained random forest classifier & pathogenic risk scorer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Benchmarks
        Text(
            text = "Quick Benchmark Presets:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(GenePrioritizationEngine.BENCHMARK_VARIANTS) { variant ->
                SuggestionChip(
                    onClick = {
                        selectedGene = variant.geneSymbol
                        chromosome = variant.chromosome
                        positionText = variant.position.toString()
                        refAllele = variant.refAllele
                        altAllele = variant.altAllele
                        variantType = variant.variantType
                        variationId = variant.variationId
                        result = null
                        savedSuccess = false
                    },
                    label = { Text("${variant.geneSymbol} (${variant.clinicalSignificance})") },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Biotech,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mutation Parameters (GRCh38)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Gene Selection Dropdown
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
                            .testTag("dropdown_gene")
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
                                    // Auto-adjust default chromosome
                                    val summary = GenePrioritizationEngine.GENE_SUMMARIES.find { it.symbol == gene }
                                    if (summary != null) {
                                        chromosome = summary.chromosome
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Chromosome and Position
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = chromosome,
                        onValueChange = { chromosome = it },
                        label = { Text("Chr") },
                        modifier = Modifier.weight(1f).testTag("input_chromosome"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = positionText,
                        onValueChange = { positionText = it.filter { char -> char.isDigit() } },
                        label = { Text("Position (VCF/Start)") },
                        modifier = Modifier.weight(2f).testTag("input_position"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ref and Alt Alleles
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = refAllele,
                        onValueChange = { refAllele = it.uppercase() },
                        label = { Text("Ref Allele") },
                        modifier = Modifier.weight(1f).testTag("input_ref_allele"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = altAllele,
                        onValueChange = { altAllele = it.uppercase() },
                        label = { Text("Alt Allele") },
                        modifier = Modifier.weight(1f).testTag("input_alt_allele"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            .testTag("dropdown_variant_type")
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

                Spacer(modifier = Modifier.height(10.dp))

                // ClinVar Variation ID
                OutlinedTextField(
                    value = variationId,
                    onValueChange = { variationId = it },
                    label = { Text("Variation ID (Optional)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_variation_id"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini AI Clinical Analysis Button
                Button(
                    onClick = {
                        val pos = positionText.toLongOrNull() ?: 1L
                        isAnalyzingGemini = true
                        geminiSaveSuccess = false
                        coroutineScope.launch {
                            val aiRes = GeminiVariantAnalysisService.analyzeGeneVariant(
                                gene = selectedGene,
                                chromosome = chromosome,
                                position = pos,
                                refAllele = refAllele,
                                altAllele = altAllele,
                                variantType = variantType,
                                clinicalNotes = customNotes.ifBlank { null }
                            )
                            isAnalyzingGemini = false
                            if (aiRes.isSuccess) {
                                geminiResult = aiRes.getOrNull()
                                snackbarHostState.showSnackbar("Gemini AI ACMG clinical evaluation completed.")
                            } else {
                                snackbarHostState.showSnackbar(aiRes.exceptionOrNull()?.localizedMessage ?: "Gemini analysis failed")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_prioritize_gemini"),
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
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze with Gemini AI (ACMG Guidelines)", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fast Local Model Button
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
                        result = pred
                        savedSuccess = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("btn_prioritize_predict")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Prioritize (Local Model)")
                }
            }
        }

        // Gemini AI Result Card
        geminiResult?.let { gRes ->
            Spacer(modifier = Modifier.height(16.dp))
            val (badgeBg, badgeFg) = when (gRes.predictedLabel) {
                "Pathogenic", "Likely Pathogenic" -> StatusPathogenicContainer to StatusPathogenic
                "Benign", "Likely Benign" -> StatusBenignContainer to StatusBenign
                else -> StatusVUSContainer to StatusVUS
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_prioritize_gemini_result"),
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
                                text = "Gemini ACMG Evaluation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = gRes.predictedLabel,
                                color = badgeFg,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${gRes.gene} Chr ${gRes.chromosome}:${gRes.position} (${gRes.refAllele}>${gRes.altAllele})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Standard: ${gRes.clinicalGuidelines}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Risk Probability: ${(gRes.riskScore * 100).toInt()}% • Confidence: ${(gRes.confidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { gRes.riskScore.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = badgeFg,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    if (gRes.acmgCriteria.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ACMG Evidence Codes:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(gRes.acmgCriteria) { code ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(code, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Mechanism: ${gRes.pathomechanism}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = gRes.clinicalSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isSavingGemini = true
                            coroutineScope.launch {
                                val formattedNotes = buildString {
                                    append("[ACMG] ")
                                    if (gRes.acmgCriteria.isNotEmpty()) {
                                        append("Codes: ")
                                        append(gRes.acmgCriteria.take(3).joinToString(","))
                                        append("; ")
                                    }
                                    append("Mech: ")
                                    append(gRes.pathomechanism.take(120))
                                    if (customNotes.isNotBlank()) {
                                        append("; Note: ")
                                        append(customNotes.take(150))
                                    }
                                }.take(490)

                                val predRecord = VariantPrediction(
                                    userId = currentUserId,
                                    gene = gRes.gene,
                                    chromosome = gRes.chromosome,
                                    position = gRes.position,
                                    refAllele = gRes.refAllele,
                                    altAllele = gRes.altAllele,
                                    variantType = gRes.variantType,
                                    variationId = variationId.ifBlank { null },
                                    predictedLabel = gRes.predictedLabel,
                                    riskScore = gRes.riskScore,
                                    confidence = gRes.confidence,
                                    notes = formattedNotes
                                )
                                val saveRes = repository.savePrediction(predRecord)
                                isSavingGemini = false
                                if (saveRes.isSuccess) {
                                    geminiSaveSuccess = true
                                    snackbarHostState.showSnackbar("Gemini variant classification saved to Firestore.")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to save: ${saveRes.exceptionOrNull()?.message}")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("btn_save_prioritize_gemini"),
                        enabled = !isSavingGemini
                    ) {
                        if (isSavingGemini) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else if (geminiSaveSuccess) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Saved to Firestore")
                        } else {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Gemini Assessment to Firestore")
                        }
                    }
                }
            }
        }

        // Result Card
        result?.let { pred ->
            Spacer(modifier = Modifier.height(16.dp))

            val (badgeBg, badgeFg) = when (pred.predictedLabel) {
                "Pathogenic", "Likely Pathogenic" -> StatusPathogenicContainer to StatusPathogenic
                "Benign", "Likely Benign" -> StatusBenignContainer to StatusBenign
                else -> StatusVUSContainer to StatusVUS
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_prediction_result"),
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
                            text = "${pred.gene} Classification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = pred.predictedLabel,
                                color = badgeFg,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Risk Score Progress
                    Text(
                        text = "Pathogenicity Risk Score: ${(pred.riskScore * 100).toInt()}% (Confidence ${(pred.confidence * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { pred.riskScore.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = badgeFg,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Functional Annotation
                    Text(
                        text = "Functional Region:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = pred.domainOrRegion,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Clinical Assessment:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = pred.impactAssessment,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Recommendation:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = pred.recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Clinical Notes field
                    OutlinedTextField(
                        value = customNotes,
                        onValueChange = { customNotes = it },
                        label = { Text("Clinical / Case Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_notes"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons: Save & Trigger Alert
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isSaving = true
                                coroutineScope.launch {
                                    val predictionRecord = VariantPrediction(
                                        userId = currentUserId,
                                        gene = pred.gene,
                                        chromosome = pred.chromosome,
                                        position = pred.position,
                                        refAllele = pred.refAllele,
                                        altAllele = pred.altAllele,
                                        variantType = pred.variantType,
                                        variationId = variationId.ifBlank { null },
                                        predictedLabel = pred.predictedLabel,
                                        riskScore = pred.riskScore,
                                        confidence = pred.confidence,
                                        notes = customNotes.ifBlank { null }
                                    )
                                    val saveRes = repository.savePrediction(predictionRecord)
                                    isSaving = false
                                    if (saveRes.isSuccess) {
                                        savedSuccess = true
                                        snackbarHostState.showSnackbar("Variant saved to your cloud analyses.")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to save: ${saveRes.exceptionOrNull()?.message}")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_prediction"),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else if (savedSuccess) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Saved")
                            } else {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Analysis")
                            }
                        }

                        // Send Push Notification Trigger
                        OutlinedButton(
                            onClick = {
                                val sent = PushNotificationHelper.sendVariantOrModelAlert(
                                    context = context,
                                    settings = notificationSettings,
                                    title = "Variant Alert: ${pred.gene} ${pred.predictedLabel}",
                                    body = "Chr ${pred.chromosome}:${pred.position} ${pred.refAllele}>${pred.altAllele} scored ${(pred.riskScore * 100).toInt()}% risk in ${pred.domainOrRegion}."
                                )
                                coroutineScope.launch {
                                    if (sent) {
                                        snackbarHostState.showSnackbar("Push notification posted to status bar!")
                                    } else {
                                        snackbarHostState.showSnackbar("Notification was suppressed by your preferences or permission.")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("btn_send_variant_alert")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
