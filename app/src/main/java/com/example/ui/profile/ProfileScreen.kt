package com.example.ui.profile

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ModelTraining
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NotificationSettings
import com.example.data.model.UserProfile
import com.example.data.repository.FirebaseRepository
import com.example.notification.PushNotificationHelper
import com.example.ui.auth.performSignOut
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    currentUserId: String,
    repository: FirebaseRepository,
    snackbarHostState: SnackbarHostState,
    onSignOutComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val userProfile by repository.observeUserProfile(currentUserId).collectAsStateWithLifecycle(initialValue = null)
    val notificationSettings by repository.observeNotificationSettings(currentUserId).collectAsStateWithLifecycle(initialValue = null)

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(PushNotificationHelper.hasNotificationPermission(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasNotificationPermission = granted
            coroutineScope.launch {
                if (granted) {
                    snackbarHostState.showSnackbar("Push notification permission granted!")
                } else {
                    snackbarHostState.showSnackbar("Notification permission denied.")
                }
            }
        }
    )

    // Ensure default profile document is created if empty
    LaunchedEffect(currentUserId) {
        val authUser = Firebase.auth.currentUser
        if (authUser != null && userProfile == null) {
            val initialProfile = UserProfile(
                userId = currentUserId,
                email = authUser.email ?: "researcher@lab.org",
                displayName = authUser.displayName ?: "Genomic Researcher",
                institution = "National Genomics Institute",
                role = "Clinical Geneticist",
                bio = "Specializing in somatic and germline variant classification."
            )
            repository.saveUserProfile(initialProfile)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Notification Permission Alert (if disabled on Android 13+)
        if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Push Notifications",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Grant notification permission to receive task reminders, messages, and variant alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier.testTag("btn_request_notif_permission")
                    ) {
                        Text("Allow")
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // User Personal Details Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_user_profile"),
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
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Avatar",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = userProfile?.displayName ?: "Researcher",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = userProfile?.role ?: "Geneticist",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = userProfile?.email ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("btn_edit_profile")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Institution
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = userProfile?.institution ?: "Genomics Institute",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                userProfile?.phone?.let { phone ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = phone, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                userProfile?.bio?.let { bio ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notification Settings Section
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_notification_settings"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Push Notification Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Customize the alerts and updates sent to your device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                val currentSettings = notificationSettings ?: NotificationSettings(userId = currentUserId)

                // 1. Direct Messages
                NotificationToggleRow(
                    icon = Icons.AutoMirrored.Filled.Message,
                    title = "New Messages",
                    description = "Direct messages and clinical case collaboration comments",
                    checked = currentSettings.newMessages,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            val updated = currentSettings.copy(newMessages = checked)
                            repository.saveNotificationSettings(updated)
                            repository.logActivity(
                                type = "notification_toggle",
                                title = "Updated Notification Preferences",
                                description = "Toggled Direct Messages to ${if (checked) "ON" else "OFF"}."
                            )
                        }
                    },
                    testTag = "switch_new_messages"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 2. Task Reminders
                NotificationToggleRow(
                    icon = Icons.Default.TaskAlt,
                    title = "Task Reminders",
                    description = "Scheduled variant reviews, batch sample processing alerts",
                    checked = currentSettings.taskReminders,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            val updated = currentSettings.copy(taskReminders = checked)
                            repository.saveNotificationSettings(updated)
                            repository.logActivity(
                                type = "notification_toggle",
                                title = "Updated Notification Preferences",
                                description = "Toggled Task Reminders to ${if (checked) "ON" else "OFF"}."
                            )
                        }
                    },
                    testTag = "switch_task_reminders"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 3. Special Offers & Announcements
                NotificationToggleRow(
                    icon = Icons.Default.LocalOffer,
                    title = "Special Offers & Grants",
                    description = "Educational webinars, platform offers, and research grant notices",
                    checked = currentSettings.specialOffers,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            val updated = currentSettings.copy(specialOffers = checked)
                            repository.saveNotificationSettings(updated)
                            repository.logActivity(
                                type = "notification_toggle",
                                title = "Updated Notification Preferences",
                                description = "Toggled Special Offers to ${if (checked) "ON" else "OFF"}."
                            )
                        }
                    },
                    testTag = "switch_special_offers"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 4. Model Training Alerts
                NotificationToggleRow(
                    icon = Icons.Default.ModelTraining,
                    title = "Model Training Alerts",
                    description = "Instant notice when ClinVar retraining finishes",
                    checked = currentSettings.modelTrainingAlerts,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            val updated = currentSettings.copy(modelTrainingAlerts = checked)
                            repository.saveNotificationSettings(updated)
                        }
                    },
                    testTag = "switch_model_training_alerts"
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // 5. Variant Risk Alerts
                NotificationToggleRow(
                    icon = Icons.Default.Science,
                    title = "High-Risk Variant Alerts",
                    description = "Immediate notification when a pathogenic mutation is classified",
                    checked = currentSettings.variantAlerts,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            val updated = currentSettings.copy(variantAlerts = checked)
                            repository.saveNotificationSettings(updated)
                        }
                    },
                    testTag = "switch_variant_alerts"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Test Notification Triggers
                Text(
                    text = "Test Push Notification Channels:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sent = PushNotificationHelper.sendTaskReminder(
                                context = context,
                                settings = currentSettings,
                                title = "Task Reminder: Review ClinVar Samples",
                                body = "Batch #2026-B of 14 BRCA1 candidate mutations is queued for clinical review."
                            )
                            coroutineScope.launch {
                                if (sent) {
                                    snackbarHostState.showSnackbar("Task Reminder posted to notification drawer!")
                                } else {
                                    snackbarHostState.showSnackbar("Notification suppressed by Task Reminder switch or system permission.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_test_task_reminder")
                    ) {
                        Text("Test Task")
                    }

                    OutlinedButton(
                        onClick = {
                            val sent = PushNotificationHelper.sendMessageNotification(
                                context = context,
                                settings = currentSettings,
                                sender = "Dr. Elena Vance (Genomics Lab)",
                                body = "Can you check the VUS classification for TP53 exon 5 mutation?"
                            )
                            coroutineScope.launch {
                                if (sent) {
                                    snackbarHostState.showSnackbar("Direct Message notification posted!")
                                } else {
                                    snackbarHostState.showSnackbar("Notification suppressed by Messages switch.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_test_message_notif")
                    ) {
                        Text("Test Message")
                    }

                    OutlinedButton(
                        onClick = {
                            val sent = PushNotificationHelper.sendSpecialOfferNotification(
                                context = context,
                                settings = currentSettings,
                                title = "Special Grant Offer: 2026 Genomic AI Compute",
                                body = "Apply for sponsored cloud compute grants for your high-throughput variant screening project."
                            )
                            coroutineScope.launch {
                                if (sent) {
                                    snackbarHostState.showSnackbar("Special Offer notification posted!")
                                } else {
                                    snackbarHostState.showSnackbar("Notification suppressed by Special Offers switch.")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("btn_test_offer_notif")
                    ) {
                        Text("Test Offer")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sign Out Button
        Button(
            onClick = {
                performSignOut(
                    context = context,
                    credentialManager = credentialManager,
                    onComplete = onSignOutComplete,
                    scope = coroutineScope
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_sign_out")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sign Out",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(userProfile?.displayName ?: "") }
        var editRole by remember { mutableStateOf(userProfile?.role ?: "") }
        var editInstitution by remember { mutableStateOf(userProfile?.institution ?: "") }
        var editPhone by remember { mutableStateOf(userProfile?.phone ?: "") }
        var editBio by remember { mutableStateOf(userProfile?.bio ?: "") }
        var isSavingProfile by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Personal Details") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_display_name"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = { Text("Professional Role") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_role"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editInstitution,
                        onValueChange = { editInstitution = it },
                        label = { Text("Institution / Organization") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_institution"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_phone"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = { Text("Bio / Research Focus") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_bio"),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSavingProfile = true
                        coroutineScope.launch {
                            val updatedProfile = (userProfile ?: UserProfile(userId = currentUserId)).copy(
                                displayName = editName,
                                role = editRole,
                                institution = editInstitution,
                                phone = editPhone.ifBlank { null },
                                bio = editBio.ifBlank { null }
                            )
                            val res = repository.saveUserProfile(updatedProfile)
                            isSavingProfile = false
                            if (res.isSuccess) {
                                repository.logActivity(
                                    type = "profile_update",
                                    title = "Updated Profile",
                                    description = "Saved personal details and professional credentials."
                                )
                                snackbarHostState.showSnackbar("Profile updated successfully.")
                                showEditProfileDialog = false
                            } else {
                                snackbarHostState.showSnackbar("Failed to update profile.")
                            }
                        }
                    },
                    enabled = !isSavingProfile
                ) {
                    if (isSavingProfile) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NotificationToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
