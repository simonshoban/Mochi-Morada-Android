package shoban.simon.mochimorada.location

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView

import shoban.simon.mochimorada.R
import shoban.simon.mochimorada.R.id.*

class LocationAdapter(context: Context, list: MutableList<Location>, private val viewingNewPlaces: Boolean, private val markLocationAsVisited: (Location) -> Unit) : ArrayAdapter<Location>(context, R.layout.location_list_item, R.id.list_location_name, list) {
    private val listOfLocations: MutableList<Location> = list

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = super.getView(position, convertView, parent)
        val location = listOfLocations[position]
        val locationName = view.findViewById(list_location_name) as TextView
        val locationType = view.findViewById(list_location_type) as TextView
        val locationFirstVisited = view.findViewById(list_location_first_visited) as TextView
        val locationMarkVisited = view.findViewById(list_mark_as_visited) as ImageView

        locationName.text = location.name
        locationType.text = this.context.resources.getString(R.string.location_new_placeholder, location.type)

        if (viewingNewPlaces) {
            locationFirstVisited.visibility = View.GONE

            locationMarkVisited.isFocusable = false
            locationMarkVisited.isFocusableInTouchMode = false

            locationMarkVisited.setOnClickListener {
                markLocationAsVisited(location)
            }
        } else {
            locationFirstVisited.text = context.resources.getString(R.string.date_visited_placeholder, location.date)
            locationMarkVisited.visibility = View.GONE
        }

        view.tag = location.documentId

        return view
    }
}