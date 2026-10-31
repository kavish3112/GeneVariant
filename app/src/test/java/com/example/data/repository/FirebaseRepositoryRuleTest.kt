package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.NotificationSettings
import com.example.data.model.UserProfile
import com.example.data.model.VariantPrediction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveUserProfile_and_observe_succeedsForAuthenticatedUser() = runBlocking {
        val aliceUid = signInTestUser("alice_rule@example.com")
        val repository = FirebaseRepository(firestore)

        val profile = UserProfile(
            userId = aliceUid,
            email = "alice_rule@example.com",
            displayName = "Dr. Alice Genomicist",
            institution = "Cancer Genomics Lab",
            role = "Bioinformatician",
            bio = "Prioritizing ClinVar pathogenic mutations."
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfile(profile)
        }
        saveResult.exceptionOrNull()?.printStackTrace()
        assertTrue("Save failed with: ${saveResult.exceptionOrNull()?.message}", saveResult.isSuccess)

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeUserProfile(aliceUid).first()
        }
        assertNotNull(observed)
        assertEquals("Dr. Alice Genomicist", observed?.displayName)
    }

    @Test
    fun saveNotificationSettings_and_observe_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice_settings@example.com")
        val repository = FirebaseRepository(firestore)

        val settings = NotificationSettings(
            userId = aliceUid,
            newMessages = true,
            taskReminders = true,
            specialOffers = false,
            modelTrainingAlerts = true,
            variantAlerts = true
        )

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveNotificationSettings(settings)
        }
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeNotificationSettings(aliceUid).first()
        }
        assertTrue(observed.taskReminders)
        assertEquals(false, observed.specialOffers)
    }

    @Test
    fun logActivity_createsActivityRecord() = runBlocking {
        val aliceUid = signInTestUser("alice_activity@example.com")
        val repository = FirebaseRepository(firestore)

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.logActivity(
                type = "prediction",
                title = "BRCA1 Analysis",
                description = "Classified mutation as Pathogenic",
                gene = "BRCA1"
            )
        }
        assertTrue(logResult.isSuccess)

        val activities = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observeActivities(aliceUid).first()
        }
        assertTrue(activities.isNotEmpty())
        assertEquals("BRCA1 Analysis", activities[0].title)
    }

    @Test
    fun savePrediction_savesAndCanBeDeleted() = runBlocking {
        val aliceUid = signInTestUser("alice_pred@example.com")
        val repository = FirebaseRepository(firestore)

        val prediction = VariantPrediction(
            userId = aliceUid,
            gene = "TP53",
            chromosome = "17",
            position = 7673802,
            refAllele = "C",
            altAllele = "T",
            variantType = "single nucleotide variant",
            predictedLabel = "Pathogenic",
            riskScore = 0.98,
            confidence = 0.95
        )

        val predId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePrediction(prediction).getOrThrow()
        }
        assertTrue(predId.isNotEmpty())

        val list = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.observePredictions(aliceUid).first()
        }
        assertTrue(list.any { it.id == predId })

        val deleteResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deletePrediction(predId)
        }
        assertTrue(deleteResult.isSuccess)
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 8000L
    }
}
