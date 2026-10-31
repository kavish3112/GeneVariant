package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ActivityRecord
import com.example.data.model.NotificationSettings
import com.example.data.model.UserProfile
import com.example.data.model.VariantPrediction
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- User Profile ---

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        require(profile.userId == uid) { "Cannot modify another user's profile" }
        val docRef = db.collection("users").document(uid)

        val existingDoc = try {
            docRef.get().await()
        } catch (e: Exception) {
            null
        }

        val existingCreatedAt = if (existingDoc != null && existingDoc.exists()) {
            existingDoc.getTimestamp("createdAt")
        } else {
            null
        }

        val payload = buildMap<String, Any?> {
            put("userId", uid)
            put("email", profile.email)
            put("displayName", profile.displayName)
            if (!profile.phone.isNullOrBlank()) put("phone", profile.phone)
            if (!profile.institution.isNullOrBlank()) put("institution", profile.institution)
            if (!profile.role.isNullOrBlank()) put("role", profile.role)
            if (!profile.bio.isNullOrBlank()) put("bio", profile.bio)
            if (!profile.avatarUrl.isNullOrBlank()) put("avatarUrl", profile.avatarUrl)
            if (profile.createdAt != null) {
                put("createdAt", profile.createdAt)
            } else if (existingCreatedAt != null) {
                put("createdAt", existingCreatedAt)
            } else {
                put("createdAt", FieldValue.serverTimestamp())
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Notification Settings ---

    fun observeNotificationSettings(userId: String): Flow<NotificationSettings> = flow {
        val path = "users/$userId/settings/notifications"
        emitAll(
            db.collection("users").document(userId).collection("settings").document("notifications")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(NotificationSettings::class.java) ?: NotificationSettings(userId = userId)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settings").document("notifications")

        val payload = mapOf(
            "userId" to uid,
            "newMessages" to settings.newMessages,
            "taskReminders" to settings.taskReminders,
            "specialOffers" to settings.specialOffers,
            "modelTrainingAlerts" to settings.modelTrainingAlerts,
            "variantAlerts" to settings.variantAlerts,
            "emailNotifications" to settings.emailNotifications,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Activity Log ---

    fun observeActivities(userId: String): Flow<List<ActivityRecord>> = flow {
        val path = "users/$userId/activities"
        emitAll(
            db.collection("users").document(userId).collection("activities")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ActivityRecord::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun logActivity(
        type: String,
        title: String,
        description: String,
        gene: String? = null,
        variantId: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val activitiesRef = db.collection("users").document(uid).collection("activities")
        val newDocRef = activitiesRef.document()
        val activityId = newDocRef.id

        val payload = buildMap<String, Any?> {
            put("id", activityId)
            put("userId", uid)
            put("type", type)
            put("title", title)
            put("description", description)
            if (!gene.isNullOrBlank()) put("gene", gene)
            if (!variantId.isNullOrBlank()) put("variantId", variantId)
            put("timestamp", FieldValue.serverTimestamp())
        }.filterValues { it != null }

        return try {
            newDocRef.set(payload).await()
            Result.success(activityId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, newDocRef.path)
            Result.failure(e)
        }
    }

    // --- Variant Predictions ---

    fun observePredictions(userId: String): Flow<List<VariantPrediction>> = flow {
        val path = "users/$userId/predictions"
        emitAll(
            db.collection("users").document(userId).collection("predictions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(VariantPrediction::class.java)
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun savePrediction(prediction: VariantPrediction): Result<String> {
        val uid = requireUserId()
        val predCol = db.collection("users").document(uid).collection("predictions")
        val docRef = if (prediction.id.isNotBlank()) predCol.document(prediction.id) else predCol.document()
        val predId = docRef.id

        val payload = buildMap<String, Any?> {
            put("id", predId)
            put("userId", uid)
            put("gene", prediction.gene)
            put("assembly", prediction.assembly)
            put("chromosome", prediction.chromosome)
            put("position", prediction.position)
            put("refAllele", prediction.refAllele)
            put("altAllele", prediction.altAllele)
            put("variantType", prediction.variantType)
            if (!prediction.variationId.isNullOrBlank()) put("variationId", prediction.variationId)
            put("predictedLabel", prediction.predictedLabel)
            put("riskScore", prediction.riskScore)
            put("confidence", prediction.confidence)
            if (!prediction.notes.isNullOrBlank()) put("notes", prediction.notes)
            put("createdAt", FieldValue.serverTimestamp())
        }.filterValues { it != null }

        return try {
            docRef.set(payload).await()
            logActivity(
                type = "prediction",
                title = "Analyzed ${prediction.gene} Variant",
                description = "Chr ${prediction.chromosome}:${prediction.position} ${prediction.refAllele}>${prediction.altAllele} classified as ${prediction.predictedLabel} (${(prediction.riskScore * 100).toInt()}% risk).",
                gene = prediction.gene,
                variantId = prediction.variationId
            )
            Result.success(predId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePrediction(predictionId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("predictions").document(predictionId)
        return try {
            docRef.delete().await()
            logActivity(
                type = "prediction_delete",
                title = "Deleted Saved Variant",
                description = "Removed variant record $predictionId from repository."
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
