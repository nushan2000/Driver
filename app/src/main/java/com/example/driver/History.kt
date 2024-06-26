package com.example.driver

import RequestAdapterHistory
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class History : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var requestAdapter: RequestAdapterHistory
    private lateinit var requestList: MutableList<Request>
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var loggedInDriverEmail: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        val backButton = findViewById<Button>(R.id.button8)
        backButton.setOnClickListener {
            val intent = Intent(this@History, MainActivity::class.java)
            startActivity(intent)
        }


        val currentUser = FirebaseAuth.getInstance().currentUser
        loggedInDriverEmail = currentUser?.email ?: ""

        requestList = mutableListOf()

        recyclerView = findViewById(R.id.recyclerView3)
        recyclerView.layoutManager = LinearLayoutManager(this)

        requestAdapter = RequestAdapterHistory(requestList)

        recyclerView.adapter = requestAdapter


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
                            .whereEqualTo("driverStatus", "start")
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

        val historyDetailbutton = findViewById<Button>(R.id.historyDetailbutton)
        historyDetailbutton.setOnClickListener {


            val intent = Intent(this, HistoryDetails::class.java)
            startActivity(intent)
            finish()
        }
    }

}