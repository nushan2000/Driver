import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driver.DetailsRequest
import com.example.driver.MainActivity
import com.example.driver.R
import com.example.driver.Request
import com.google.firebase.firestore.FirebaseFirestore

class RequestAdapterHistory(private val requestList: MutableList<Request>) : RecyclerView.Adapter<RequestAdapterHistory.ViewHolder>()  {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewModel: TextView = itemView.findViewById(R.id.textViewModel)
        val buttonDetails: Button = itemView.findViewById(R.id.button7)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requestList[position]
        holder.textViewTitle.text = request.reason
        holder.textViewName.text = request.applier
        holder.textViewModel.text = request.vehicle

        holder.buttonDetails.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DetailsRequest::class.java).apply {
                // Pass the details of the clicked request as extras
                putExtra("REQUEST_ID", request.id)
                putExtra("REQUEST_REASON", request.reason)
                // Add more extras for other request details as needed
            }
            // Start the activity
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return requestList.size
    }
}
