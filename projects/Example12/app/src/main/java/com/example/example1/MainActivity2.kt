import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.example1.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.bson.Document
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Call the function to retrieve data
        fetchData()
    }

    private fun fetchData() {
        GlobalScope.launch(Dispatchers.IO) {
            val client = KMongo.createClient() // Create a client
            val database = client.getDatabase("your_database_name").coroutine // Replace with your database name
            val collection = database.getCollection<Document>("requests") // Replace "requests" with your collection name

            // Perform a find operation
            val result = collection.find().toList()

            // Handle the result, for example, print it
            result.forEach { document ->
                val date = document["date"]
                val reason = document["reason"]
                val destination = document["destination"]
                val applier = document["applier"]
                println("Date: $date, Reason: $reason, Destination: $destination, Applier: $applier")
            }

            client.close() // Close the client when done
        }
    }
}
