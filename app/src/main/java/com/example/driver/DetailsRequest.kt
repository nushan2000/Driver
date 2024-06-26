package com.example.driver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore

class DetailsRequest : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: FirebaseFirestore // Firestore instance

    private val TAG = "DetailsRequest"
    private var previousLocation: Location? = null
    private var totalDistance: Double = 0.0
    private lateinit var requestId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details_request)

        // Retrieve extras from the Intent
        requestId = intent.getStringExtra("REQUEST_ID") ?: ""
        val requestReason = intent.getStringExtra("REQUEST_REASON") ?: ""
        val requestDistance = intent.getStringExtra("REQUEST_DISTANCE") ?: ""

        // Display the request details in TextViews or other views
        val requestIdTextView = findViewById<TextView>(R.id.requestIdTextView)
        requestIdTextView.text = "Request ID: $requestId"

        val requestReasonTextView = findViewById<TextView>(R.id.requestReasonTextView)
        requestReasonTextView.text = "Reason: $requestReason"



        if (requestId.isNotEmpty()) {
            db = FirebaseFirestore.getInstance()

            // Start location updates
            startLocationUpdates()

            val finishButton = findViewById<Button>(R.id.finishButton)
            finishButton.setOnClickListener {
                stopLocationUpdates()
                // Send total distance to Firebase
                sendDistanceToFirebase()

                // Update the driverStatus field to "finish"
                val requestRef = db.collection("Request").document(requestId)
                requestRef.update("driverStatus", "finish")
                    .addOnSuccessListener {
                        // On success, navigate back to the main page
                        val intent = Intent(this@DetailsRequest, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Optional: Close this activity
                    }
                    .addOnFailureListener { e ->
                        // Handle failure
                        Log.e("Firestore", "Error updating document", e)
                        // You can show a toast or a message to the user indicating the failure
                    }
            }
        } else {
            // Handle the case where requestId is empty (optional)
            Log.e("DetailsRequest", "Request ID is empty")
            finish()
        }
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (previousLocation != null) {
                        val distance = calculateDistance(previousLocation!!, location)
                        totalDistance += distance
                        Log.d(TAG, "Distance traveled: $distance meters")
                        Log.d(TAG, "Total distance: $totalDistance meters")
                    }
                    previousLocation = location
                }
            }
        }

        if (checkLocationPermission()) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun calculateDistance(start: Location, end: Location): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            start.latitude,
            start.longitude,
            end.latitude,
            end.longitude,
            results
        )
        return results[0]
    }

    private fun sendDistanceToFirebase() {
        val requestRef = db.collection("Request").document(requestId)

        // Update the total distance field in the request document
        requestRef.update("totalDistance", totalDistance)
            .addOnSuccessListener {
                // Handle success
                Log.d(TAG, "Total distance sent to Firebase: $totalDistance")
                // Optionally, navigate back to the main page or show a success message
                val intent = Intent(this@DetailsRequest, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e(TAG, "Error updating total distance in Firestore", e)
                // Show an error message to the user
            }
    }
}
