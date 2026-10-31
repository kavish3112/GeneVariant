package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.NotificationSettings

object PushNotificationHelper {

    const val CHANNEL_TASKS = "channel_tasks"
    const val CHANNEL_MESSAGES = "channel_messages"
    const val CHANNEL_OFFERS = "channel_offers"
    const val CHANNEL_VARIANTS = "channel_variants"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled variant reviews and ClinVar sample batches."
                enableVibration(true)
            }

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Direct Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for collaboration messages and peer case reviews."
            }

            val offersChannel = NotificationChannel(
                CHANNEL_OFFERS,
                "Special Offers & Announcements",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Genomics symposium invites, grant alerts, and platform special offers."
            }

            val variantsChannel = NotificationChannel(
                CHANNEL_VARIANTS,
                "Variant & Model Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when priority gene models finish retraining or high-risk variants are detected."
                enableVibration(true)
            }

            manager.createNotificationChannels(
                listOf(tasksChannel, messagesChannel, offersChannel, variantsChannel)
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun sendTaskReminder(
        context: Context,
        settings: NotificationSettings?,
        title: String,
        body: String
    ): Boolean {
        if (settings != null && !settings.taskReminders) return false
        return dispatchNotification(
            context = context,
            channelId = CHANNEL_TASKS,
            notificationId = 1001,
            title = title,
            body = body,
            subText = "Task Reminder"
        )
    }

    fun sendMessageNotification(
        context: Context,
        settings: NotificationSettings?,
        sender: String,
        body: String
    ): Boolean {
        if (settings != null && !settings.newMessages) return false
        return dispatchNotification(
            context = context,
            channelId = CHANNEL_MESSAGES,
            notificationId = 1002,
            title = "Message from $sender",
            body = body,
            subText = "Direct Message"
        )
    }

    fun sendSpecialOfferNotification(
        context: Context,
        settings: NotificationSettings?,
        title: String,
        body: String
    ): Boolean {
        if (settings != null && !settings.specialOffers) return false
        return dispatchNotification(
            context = context,
            channelId = CHANNEL_OFFERS,
            notificationId = 1003,
            title = title,
            body = body,
            subText = "Special Offer"
        )
    }

    fun sendVariantOrModelAlert(
        context: Context,
        settings: NotificationSettings?,
        title: String,
        body: String
    ): Boolean {
        if (settings != null && !settings.variantAlerts && !settings.modelTrainingAlerts) return false
        return dispatchNotification(
            context = context,
            channelId = CHANNEL_VARIANTS,
            notificationId = 1004,
            title = title,
            body = body,
            subText = "Genomic Alert"
        )
    }

    private fun dispatchNotification(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        body: String,
        subText: String
    ): Boolean {
        createNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        }
    }
}
