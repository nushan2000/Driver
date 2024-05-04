package com.example.driver

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"
    private val CHANNEL_ID = "notification_channel"
    private val CHANNEL_NAME = "Driver App"
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received: ${remoteMessage.data}")

        remoteMessage.notification?.let {
            Log.d(TAG, "Notification body: ${it.body}")
            generateNotification(it.title ?: "", it.body ?: "")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")
        // You can send the token to your server for further processing, if needed
    }

    override fun onCreate() {
        super.onCreate()
        setupFirestoreListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFirestoreListener()
    }

    private fun setupFirestoreListener() {
        Log.d(TAG, "Setting up Firestore listener")
        listener = firestore.collection("Request")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e(TAG, "Listen failed", error)
                    return@addSnapshotListener
                }

                for (doc in value!!.documentChanges) {
                    if (doc.type == DocumentChange.Type.ADDED) {
                        Log.d(TAG, "New document added: ${doc.document.data}")
                        val title = doc.document.getString("reason") ?: ""
                        val message = doc.document.getString("destination") ?: ""
                        Log.d(TAG, "Notification title: $title, message: $message")
                        generateNotification(title, message)
                    }
                }
            }
    }

    private fun removeFirestoreListener() {
        listener?.remove()
    }

    private fun generateNotification(title: String, message: String) {
        Log.d(TAG, "Generating notification: $title - $message")

        // Check for empty title and message
        if (title.isEmpty() || message.isEmpty()) {
            Log.e(TAG, "Empty title or message. Notification not generated.")
            return
        }

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo3)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Setup notification channel
        createNotificationChannel(notificationManager)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
        Log.d(TAG, "Notification sent")
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
}
