package shoban.simon.mochimorada

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        addButtonListeners()
    }

    private fun addButtonListeners() {
        addListenerToNewPlacesButton()
        addListenerToOldPlacesButton()
    }

    private fun addListenerToNewPlacesButton() {
        new_places_button.setOnClickListener {
            startListLocationsActivity(ListLocationsActivity.NEW_PLACES_DOCUMENT_NAME)
        }
    }

    private fun addListenerToOldPlacesButton() {
        old_places_button.setOnClickListener {
            startListLocationsActivity(ListLocationsActivity.OLD_PLACES_DOCUMENT_NAME)
        }
    }

    private fun startListLocationsActivity(collection: String) {
        val intent = Intent(this, ListLocationsActivity::class.java)

        intent.putExtra("Collection", collection)
        startActivity(intent)
    }
}
