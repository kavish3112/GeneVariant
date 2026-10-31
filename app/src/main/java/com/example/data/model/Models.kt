package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val phone: String? = null,
    val institution: String? = "Genomics Laboratory",
    val role: String? = "Geneticist",
    val bio: String? = "Investigating genetic variants and clinical significance.",
    val avatarUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationSettings(
    val userId: String = "",
    val newMessages: Boolean = true,
    val taskReminders: Boolean = true,
    val specialOffers: Boolean = false,
    val modelTrainingAlerts: Boolean = true,
    val variantAlerts: Boolean = true,
    val emailNotifications: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ActivityRecord(
    val id: String = "",
    val userId: String = "",
    val type: String = "prediction", // prediction, model_training, search, profile_update, notification_toggle, export
    val title: String = "",
    val description: String = "",
    val gene: String? = null,
    val variantId: String? = null,
    @ServerTimestamp val timestamp: Timestamp? = null
)

@IgnoreExtraProperties
data class VariantPrediction(
    val id: String = "",
    val userId: String = "",
    val gene: String = "",
    val assembly: String = "GRCh38",
    val chromosome: String = "",
    val position: Long = 0L,
    val refAllele: String = "",
    val altAllele: String = "",
    val variantType: String = "single nucleotide variant",
    val variationId: String? = null,
    val predictedLabel: String = "Benign",
    val riskScore: Double = 0.0,
    val confidence: Double = 0.0,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class GeneSummary(
    val symbol: String,
    val fullName: String,
    val chromosome: String,
    val rocAuc: Double,
    val accuracy: Double,
    val totalVariants: Int,
    val pathogenicCount: Int,
    val benignCount: Int,
    val status: String = "Trained & Ready",
    val description: String
)

data class ClinVarVariant(
    val variationId: String,
    val geneSymbol: String,
    val assembly: String = "GRCh38",
    val chromosome: String,
    val position: Long,
    val refAllele: String,
    val altAllele: String,
    val variantType: String,
    val clinicalSignificance: String,
    val name: String,
    val riskScore: Double
)
