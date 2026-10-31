package com.example

import com.example.data.service.GeminiVariantAnalysisResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GeminiVariantAnalysisModelTest {

    @Test
    fun testGeminiClinicalJsonParsing() {
        val sampleJson = """
            {
              "predictedLabel": "Pathogenic",
              "riskScore": 0.94,
              "confidence": 0.92,
              "clinicalGuidelines": "ACMG/AMP 2015 Standards",
              "acmgCriteria": [
                "PVS1 (Null variant in LOF gene)",
                "PM1 (Critical functional domain)",
                "PP3 (Damaging in silico predictors)"
              ],
              "pathomechanism": "Premature truncation causing loss of DNA double-strand break repair.",
              "clinicalSummary": "The BRCA1 c.5266dupC variant results in a frameshift leading to premature termination and severe predisposition to hereditary breast and ovarian cancer.",
              "actionableRecommendations": [
                "Refer for certified genetic counseling",
                "High-risk breast surveillance via annual breast MRI and mammography",
                "Cascade testing of adult first-degree relatives"
              ]
            }
        """.trimIndent()

        val json = JSONObject(sampleJson)
        val predictedLabel = json.getString("predictedLabel")
        val riskScore = json.getDouble("riskScore")
        val confidence = json.getDouble("confidence")
        val guidelines = json.getString("clinicalGuidelines")
        val pathomechanism = json.getString("pathomechanism")
        val summary = json.getString("clinicalSummary")

        val criteriaArray = json.getJSONArray("acmgCriteria")
        val criteria = (0 until criteriaArray.length()).map { criteriaArray.getString(it) }

        val recsArray = json.getJSONArray("actionableRecommendations")
        val recs = (0 until recsArray.length()).map { recsArray.getString(it) }

        val result = GeminiVariantAnalysisResult(
            gene = "BRCA1",
            chromosome = "17",
            position = 43044295L,
            refAllele = "C",
            altAllele = "insC",
            variantType = "Insertion",
            predictedLabel = predictedLabel,
            riskScore = riskScore,
            confidence = confidence,
            clinicalGuidelines = guidelines,
            acmgCriteria = criteria,
            pathomechanism = pathomechanism,
            clinicalSummary = summary,
            actionableRecommendations = recs
        )

        assertEquals("Pathogenic", result.predictedLabel)
        assertEquals(0.94, result.riskScore, 0.001)
        assertEquals(3, result.acmgCriteria.size)
        assertTrue(result.acmgCriteria.any { it.contains("PVS1") })
        assertEquals(3, result.actionableRecommendations.size)
        assertNotNull(result.pathomechanism)
    }
}
