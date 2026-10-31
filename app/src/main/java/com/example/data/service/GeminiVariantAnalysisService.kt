package com.example.data.service

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiVariantService"

/**
 * Structured clinical output for variant interpretation following ACMG/AMP clinical guidelines.
 */
data class GeminiVariantAnalysisResult(
    val gene: String,
    val chromosome: String,
    val position: Long,
    val refAllele: String,
    val altAllele: String,
    val variantType: String,
    val predictedLabel: String,
    val riskScore: Double,
    val confidence: Double,
    val clinicalGuidelines: String,
    val acmgCriteria: List<String>,
    val pathomechanism: String,
    val clinicalSummary: String,
    val actionableRecommendations: List<String>
)

object GeminiVariantAnalysisService {

    // Structured JSON schema definition using Schema.obj as required by Firebase AI Logic
    private val variantSchema = Schema.obj(
        properties = mapOf(
            "predictedLabel" to Schema.enumeration(
                values = listOf("Pathogenic", "Likely Pathogenic", "VUS", "Likely Benign", "Benign"),
                description = "Standard ACMG/AMP 5-tier sequence variant classification"
            ),
            "riskScore" to Schema.double(
                description = "Estimated pathogenic risk probability score from 0.0 (benign) to 1.0 (pathogenic)"
            ),
            "confidence" to Schema.double(
                description = "Assessment confidence score between 0.0 and 1.0"
            ),
            "clinicalGuidelines" to Schema.string(
                description = "Clinical guideline standard reference, e.g. ACMG/AMP 2015 Standards"
            ),
            "acmgCriteria" to Schema.array(
                items = Schema.string(),
                description = "List of invoked ACMG/AMP evidence codes (e.g. PVS1, PS1, PM1, PM2, PP3, BA1, BP4)"
            ),
            "pathomechanism" to Schema.string(
                description = "Molecular/biological mechanism of pathogenicity or neutrality"
            ),
            "clinicalSummary" to Schema.string(
                description = "Clinical justification and synthesis of evidence supporting the classification"
            ),
            "actionableRecommendations" to Schema.array(
                items = Schema.string(),
                description = "Actionable clinical management, screening, or follow-up recommendations"
            )
        ),
        optionalProperties = listOf("actionableRecommendations")
    )

    private val genConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = variantSchema
        temperature = 0.2f // Low temperature for deterministic clinical guideline interpretation
    }

    // Lazy initialization to prevent startup crashes prior to FirebaseApp initialization
    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = genConfig
        )
    }

    /**
     * Analyzes a gene variant input and returns a predicted risk classification
     * based on ACMG/AMP 2015 clinical sequence interpretation guidelines.
     */
    suspend fun analyzeGeneVariant(
        gene: String,
        chromosome: String,
        position: Long,
        refAllele: String,
        altAllele: String,
        variantType: String = "single nucleotide variant",
        assembly: String = "GRCh38",
        clinicalNotes: String? = null
    ): Result<GeminiVariantAnalysisResult> {
        val prompt = buildString {
            appendLine("You are an expert board-certified Clinical Molecular Geneticist evaluating genetic variant pathogenicity.")
            appendLine("Apply the American College of Medical Genetics and Genomics and Association for Molecular Pathology (ACMG/AMP 2015) guidelines.")
            appendLine()
            appendLine("Variant Specifications:")
            appendLine("- Gene: $gene")
            appendLine("- Assembly: $assembly")
            appendLine("- Chromosome: $chromosome")
            appendLine("- Genomic Position: $position")
            appendLine("- Reference Allele: $refAllele")
            appendLine("- Alternate Allele: $altAllele")
            appendLine("- Variant Type: $variantType")
            if (!clinicalNotes.isNullOrBlank()) {
                appendLine("- Clinical Context / Patient Notes: $clinicalNotes")
            }
            appendLine()
            appendLine("Evaluate:")
            appendLine("1. ACMG/AMP 2015 evidence criteria (PVS1, PS1-4, PM1-6, PP1-5, BA1, BS1-4, BP1-7).")
            appendLine("2. Known biological domains (e.g. BRCA1 RING/BRCT domains, TP53 DNA-binding domain, PTEN phosphatase domain).")
            appendLine("3. Pathomechanism (loss of function, haploinsufficiency, dominant negative, or synonymous tolerance).")
            appendLine("4. Assign 5-tier classification: Pathogenic, Likely Pathogenic, VUS, Likely Benign, or Benign.")
            appendLine("5. Provide estimated continuous risk score (0.0 to 1.0) and confidence (0.0 to 1.0).")
            appendLine("Return pure structured JSON conforming to the provided schema.")
        }

        return try {
            // Mandated: 30-60 second timeout for model inference
            val response = withTimeout(60_000L) {
                generativeModel.generateContent(prompt)
            }

            val jsonString = response.text
            if (jsonString.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Gemini returned an empty response."))
            }

            // Direct JSON parsing guaranteed by responseSchema
            val json = JSONObject(jsonString)
            val predictedLabel = json.getString("predictedLabel")
            val riskScore = json.getDouble("riskScore")
            val confidence = json.getDouble("confidence")
            val clinicalGuidelines = json.optString("clinicalGuidelines", "ACMG/AMP 2015 Standards")
            val pathomechanism = json.optString("pathomechanism", "Unknown pathomechanism")
            val clinicalSummary = json.optString("clinicalSummary", "Evaluated based on ACMG/AMP criteria.")

            val criteriaArray = json.optJSONArray("acmgCriteria")
            val criteriaList = if (criteriaArray != null) {
                (0 until criteriaArray.length()).map { criteriaArray.getString(it) }
            } else {
                emptyList()
            }

            val recsArray = json.optJSONArray("actionableRecommendations")
            val recsList = if (recsArray != null) {
                (0 until recsArray.length()).map { recsArray.getString(it) }
            } else {
                emptyList()
            }

            val result = GeminiVariantAnalysisResult(
                gene = gene,
                chromosome = chromosome,
                position = position,
                refAllele = refAllele,
                altAllele = altAllele,
                variantType = variantType,
                predictedLabel = predictedLabel,
                riskScore = riskScore,
                confidence = confidence,
                clinicalGuidelines = clinicalGuidelines,
                acmgCriteria = criteriaList,
                pathomechanism = pathomechanism,
                clinicalSummary = clinicalSummary,
                actionableRecommendations = recsList
            )

            Log.d(TAG, "Gemini clinical analysis succeeded for $gene: $predictedLabel ($riskScore)")
            Result.success(result)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini variant analysis timed out after 60s", e)
            Result.failure(Exception("Gemini AI clinical analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            // ALWAYS rethrow CancellationException to preserve cooperative coroutine cancellation
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini variant analysis failed", e)
            Result.failure(e)
        }
    }
}
