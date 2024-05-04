package com.example.driver

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    companion object {
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: FirebaseFirestore // Firestore instance

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = findViewById<View>(android.R.id.content)
        rootLayout.setBackgroundColor(Color.TRANSPARENT) // Set background color to transparent

        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = FirebaseFirestore.getInstance() // Initialize Firestore

        setupFirebaseMessaging()
        setupButtons()

        //////////////////////////////////////
        // Initialize Firestore
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("Request")
        FirebaseMessaging.getInstance().subscribeToTopic("requests")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to requests topic"
                if (!task.isSuccessful) {
                    msg = "Subscribe to requests topic failed"
                }
                Log.d("YourActivity", msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            }

// Set up Firestore listener
        collectionRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            for (doc in snapshot!!.documents) {
                // Process your documents here
                val data = doc.data
                // Check if data meets criteria for notification
                // If yes, create and show a notification
                if (data != null && data["status"] == "pending") {
                    showNotification()
                }
            }
        }

        // Notification function

        ///////////////////////////////////////

    }
    private fun showNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "your_channel_id"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("Notification Title")
            .setContentText("Notification Text")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Create a unique notification channel if required
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Channel Name", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
    private fun setupFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
        })
    }

    private fun setupButtons() {
        val insertButton = findViewById<Button>(R.id.insertButton)
        insertButton.setOnClickListener {
            val intent = Intent(this@MainActivity, History::class.java)
            startActivity(intent)
        }




        val newButton = findViewById<Button>(R.id.newButton)
        newButton.setOnClickListener {
            val intent = Intent(this@MainActivity, AllRequest::class.java)
            startActivity(intent)
        }
    }

    private fun startLocationUpdates() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    sendLocationToFirestore(location)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun sendLocationToFirestore(location: Location) {
        val driverId = "YourDriverIdentifier" // Replace with actual driver ID
        val locationData = hashMapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude
        )

        db.collection("LocationTrack").document(driverId)
            .set(locationData)
            .addOnSuccessListener {
                Log.d(TAG, "Location updated successfully in Firestore")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating location in Firestore", e)
            }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Location permission is required for this feature to work.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::fusedLocationClient.isInitialized && this::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

}
