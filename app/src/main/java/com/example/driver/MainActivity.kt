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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {


    companion object {
        private const val NOTIFICATION_ID = 1
    }
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var loggedInDriverEmail: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: FirebaseFirestore // Firestore instance

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val TAG = "MainActivity"
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val driverNameTextView = findViewById<TextView>(R.id.textView)
        val vehicleTextView = findViewById<TextView>(R.id.textView5)
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            signOut()
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        loggedInDriverEmail = currentUser?.email ?: ""

        // Now you can proceed with other initialization code
        firestore.collection("Vehicle")
            .whereEqualTo("driverEmail", loggedInDriverEmail)
            .get()
            .addOnSuccessListener { vehicleDocuments ->
                if (!vehicleDocuments.isEmpty) {
                    val vehicleDocument = vehicleDocuments.documents[0]
                    val vehicleName = vehicleDocument.getString("vehicleName")
                    val Name = vehicleDocument.getString("driverName")
                    Log.d(TAG, "Vehicle for logged-in driver $loggedInDriverEmail: $vehicleName")

                    // Now you can use the vehicle details as needed
                    driverNameTextView.text = Name
                    vehicleTextView.text = vehicleName
                } else {
                    Log.e(TAG, "No vehicle found for driver: $loggedInDriverEmail")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting vehicle: ", exception)
            }



        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        db = FirebaseFirestore.getInstance() // Initialize Firestore


        setupButtons()

        //////////////////////////////////////
        // Initialize Firestore




        // Notification function

        ///////////////////////////////////////

    }
    private fun signOut() {
        auth.signOut()
        clearAuthenticationState()
        // Redirect the user back to the login screen
        startActivity(Intent(this, Login::class.java))
        finish() // Close MainActivity
    }

    private fun clearAuthenticationState() {
        // Clear the authentication state from SharedPreferences
        val sharedPreferences = getSharedPreferences("auth", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("authenticated", false).apply()
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
