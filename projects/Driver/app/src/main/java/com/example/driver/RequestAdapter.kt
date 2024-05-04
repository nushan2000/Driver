import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.driver.MainActivity
import com.example.driver.R
import com.example.driver.Request
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import io.socket.client.Socket

class RequestAdapter(
    private val requestList: MutableList<Request>,
    private val socket: Socket
) :
    RecyclerView.Adapter<RequestAdapter.ViewHolder>() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var db: FirebaseFirestore // Firestore instance

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val TAG = "RequestAdapter"

    init {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(App.context)
        db = FirebaseFirestore.getInstance() // Initialize Firestore
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewModel: TextView = itemView.findViewById(R.id.textViewModel)
        val buttonStart: Button = itemView.findViewById(R.id.button7)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_request, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requestList[position]
        holder.textViewTitle.text = request.reason
        holder.textViewName.text = request.applier
        holder.textViewModel.text = request.vehicle

        holder.buttonStart.setOnClickListener {
            Log.d(TAG, "Start button clicked")
            if (checkLocationPermission()) {
                Log.d(TAG, "Location permission granted")
                startLocationUpdates(request.id, request.applier)
            } else {
                Log.d(TAG, "Location permission not granted, requesting...")
                requestLocationPermission()
            }
            val requestId = request.id
            val firestore = FirebaseFirestore.getInstance()
            val requestRef = firestore.collection("Request").document(requestId)
            requestRef.update("driverStatus", "start")
                .addOnSuccessListener {
                    val context = holder.itemView.context
                    val intent = Intent(context, MainActivity::class.java)
                    context.startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating document", e)
                }
        }
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    private fun startLocationUpdates(requestId: String,applier: String) {
        Log.d(TAG, "startLocationUpdates for request ID: $requestId")
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d(TAG, "Received location update")
                for (location in locationResult.locations) {
                    Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")
                    sendLocationToFirestore(requestId, location, applier)
                }
            }
        }

        if (ContextCompat.checkSelfPermission(
                App.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "Requesting location updates")
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun sendLocationToFirestore(requestId: String, location: Location,applier: String) {
        Log.d(TAG, "Sending location to Firestore for request ID: $requestId")
        val locationData = hashMapOf(
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "applier" to applier
        )

        db.collection("LocationTrack").document(requestId)
            .set(locationData)
            .addOnSuccessListener {
                Log.d(TAG, "Location updated successfully in Firestore for request ID: $requestId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating location in Firestore for request ID: $requestId", e)
            }
    }

    private fun checkLocationPermission(): Boolean {
        val context = App.context
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        Log.d(TAG, "Requesting location permission...")
        ActivityCompat.requestPermissions(
            App.context as MainActivity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
}

object App {
    lateinit var context: Context
}
