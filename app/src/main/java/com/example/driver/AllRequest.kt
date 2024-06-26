package com.example.driver

import RequestAdapter
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.driver.MainActivity
import com.example.driver.R
import com.example.driver.Request
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException

class AllRequest : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var requestAdapter: RequestAdapter
    private lateinit var requestList: MutableList<Request>
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var loggedInDriverEmail: String
    private lateinit var socket: Socket
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_request)
        App.context = applicationContext

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize socket
        try {
            socket = IO.socket("http://your-backend-url:3000")
            socket.connect()
        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        val backButton = findViewById<Button>(R.id.button8)
        backButton.setOnClickListener {
            val intent = Intent(this@AllRequest, MainActivity::class.java)
            startActivity(intent)
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        loggedInDriverEmail = currentUser?.email ?: ""

        requestList = mutableListOf()

        recyclerView = findViewById(R.id.recyclerView3)
        recyclerView.layoutManager = LinearLayoutManager(this)

        requestAdapter = RequestAdapter(requestList, socket)
        recyclerView.adapter = requestAdapter

        fetchRequestsAndSendLocation()
    }

    private fun fetchRequestsAndSendLocation() {
        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
            return
        }

        // Fetch location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    sendLocationUpdates(latitude, longitude)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting location", e)
            }

        // Fetch requests
        firestore.collection("Vehicle")
            .whereEqualTo("driverEmail", loggedInDriverEmail)
            .get()
            .addOnSuccessListener { vehicleDocuments ->
                if (!vehicleDocuments.isEmpty) {
                    val vehicleDocument = vehicleDocuments.documents[0]
                    val vehicleName = vehicleDocument.getString("vehicleName")
                    if (vehicleName != null) {
                        Log.d(TAG, "Vehicle for logged-in driver $loggedInDriverEmail: $vehicleName")

                        // Fetch requests for the specific vehicleName
                        firestore.collection("Request")
                            .whereEqualTo("vehicle", vehicleName)  // Adjusted to match your data structure
                            .whereEqualTo("approveDeenAr", true)
                            .whereEqualTo("driverStatus", "approved")
                            .get()
                            .addOnSuccessListener { requestDocuments ->
                                requestList.clear()
                                for (doc in requestDocuments) {
                                    try {
                                        val request = doc.toObject(Request::class.java)
                                        request?.let { requestList.add(it) }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error deserializing document", e)
                                    }
                                }
                                requestAdapter.notifyDataSetChanged()
                                Log.d(TAG, "Received ${requestList.size} documents from Firestore")
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Error getting requests: ", exception)
                            }
                    } else {
                        Log.e(TAG, "Vehicle name is null for driver: $loggedInDriverEmail")
                    }
                } else {
                    Log.e(TAG, "No vehicle found for driver: $loggedInDriverEmail")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting vehicle: ", exception)
            }


    }

    private fun sendLocationUpdates(latitude: Double, longitude: Double) {
        val locationData = JSONObject()
        try {
            locationData.put("latitude", latitude)
            locationData.put("longitude", longitude)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        socket.emit("locationUpdate", locationData)
    }

    override fun onDestroy() {
        super.onDestroy()
        socket.disconnect()
    }

    companion object {
        private const val TAG = "AllRequestActivity"
        private const val REQUEST_LOCATION_PERMISSION = 1001
    }
}
