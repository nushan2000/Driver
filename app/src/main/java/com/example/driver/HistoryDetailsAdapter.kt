import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.driver.R
import com.example.driver.Request

class HistoryDetailsAdapter(private val requestList: MutableList<Request>) : RecyclerView.Adapter<HistoryDetailsAdapter.ViewHolder>()  {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        val textViewName: TextView = itemView.findViewById(R.id.textViewName)
        val textViewModel: TextView = itemView.findViewById(R.id.textViewModel)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requestList[position]
        holder.textViewTitle.text = request.reason
        holder.textViewName.text = request.applier
        holder.textViewModel.text = request.vehicle

    }

    override fun getItemCount(): Int {
        return requestList.size
    }
}
