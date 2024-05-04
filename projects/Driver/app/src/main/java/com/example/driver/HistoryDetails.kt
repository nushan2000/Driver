package com.example.driver

import HistoryDetailsAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HistoryDetails : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var requestAdapter: HistoryDetailsAdapter
    private lateinit var requestList: MutableList<Request>
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var loggedInDriverEmail: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_details)
        val backButton = findViewById<Button>(R.id.button8)
        backButton.setOnClickListener {
            val intent = Intent(this@HistoryDetails, MainActivity::class.java)
            startActivity(intent)
        }
        val currentUser = FirebaseAuth.getInstance().currentUser
        loggedInDriverEmail = currentUser?.email ?: ""
        requestList = mutableListOf()

        recyclerView = findViewById(R.id.recyclerView3)
        recyclerView.layoutManager = LinearLayoutManager(this)

        requestAdapter = HistoryDetailsAdapter(requestList)

        recyclerView.adapter = requestAdapter


        firestore.collection("Vehicle")
            .whereEqualTo("driverEmail", loggedInDriverEmail)
            .get()
            .addOnSuccessListener { vehicleDocuments ->
                if (!vehicleDocuments.isEmpty) {
                    val vehicleDocument = vehicleDocuments.documents[0]
                    val vehicleName = vehicleDocument.getString("vehicle")
                    Log.d("Firestore", "Vehicle for logged-in driver $loggedInDriverEmail: $vehicleName")
                    // Query Firestore for requests associated with the retrieved vehicle

                    firestore.collection("Request")
            .whereEqualTo("approveDeenAr", true)
            .whereEqualTo("driverStatus", "finish")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("Firestore", "Error getting documents", error)
                    return@addSnapshotListener
                }

                if (value != null) {
                    requestList.clear()
                    for (doc in value.documents) {
                        val request = doc.toObject(Request::class.java)
                        request?.let { requestList.add(it) }
                    }
                    requestAdapter.notifyDataSetChanged()
                    Log.d("Firestore", "Received ${requestList.size} documents from Firestore")

                    // Log the data for debugging purposes
                    for (request in requestList) {
                        Log.d("Firestore", "Request: $request")
                    }
                } else {
                    Log.d("Firestore", "No documents found")
                }
            }} else {
        Log.e("Firestore", "No vehicle found for driver: $loggedInDriverEmail")
    }
}
.addOnFailureListener { exception ->
    Log.e("Firestore", "Error getting vehicle: ", exception)
}

    }
}