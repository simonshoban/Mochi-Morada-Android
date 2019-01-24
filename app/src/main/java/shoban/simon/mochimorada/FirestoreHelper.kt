package shoban.simon.mochimorada

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.*
import shoban.simon.mochimorada.location.Location
import shoban.simon.mochimorada.location.LocationFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FirestoreHelper(private val context: Context, private val authenticator: Authenticator, private val updateUI: (listOfLocations: MutableList<Location>) -> Unit) {
    companion object {
        // Default location sorting option
        val DEFAULT_SORT_OPTION = SortOptions.NAME_ASC
    }

    private val db = FirebaseFirestore.getInstance()
    private var mapOfLocations: HashMap<String, Location> = HashMap()
    private lateinit var currentLocationList: MutableList<Location>
    private var currentSortOption: SortOptions = DEFAULT_SORT_OPTION

    fun getCurrentSortOption() : SortOptions = currentSortOption

    fun getCurrentLocations() : MutableList<Location> = currentLocationList

    fun sortLocations(locations: MutableList<Location>, sortOption: SortOptions = currentSortOption) : MutableList<Location> {
        currentSortOption = sortOption

        when (sortOption) {
            SortOptions.NAME_ASC -> locations.sortBy { it.name }
            SortOptions.DATE_ASC -> locations.sortBy { it.date }
            SortOptions.TYPE_ASC -> locations.sortBy { it.type }
            SortOptions.NAME_DESC -> locations.sortByDescending { it.name }
            SortOptions.DATE_DESC -> locations.sortByDescending { it.date }
            SortOptions.TYPE_DESC -> locations.sortByDescending { it.type }
        }

        return locations
    }

    fun queryLocations(query: String) : MutableList<Location> {
        val listOfLocations = ArrayList<Location>()

        mapOfLocations.forEach {
            if (it.value.name.contains(query, true)) {
                listOfLocations.add(it.value)
            }
        }

        currentLocationList = listOfLocations

        return sortLocations(listOfLocations)
    }

    fun getLocationById(id: String) : Location? {
        return mapOfLocations[id]
    }

    fun setFirestoreSettings() {
        val settings: FirebaseFirestoreSettings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        db.firestoreSettings = settings
    }

    fun deleteLocation(collectionName: String, locationId: String) {
        if (authenticator.userIsLoggedIn()) {
            db.collection(collectionName).document(locationId).delete()
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete document: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
        } else {
            authenticator.promptForLogin()

            if (authenticator.userIsLoggedIn()) {
                deleteLocation(collectionName, locationId)
            }
        }
    }

    fun markLocationAsVisited(locationId: String) {
        if (authenticator.userIsLoggedIn()) {
            val location = mapOfLocations[locationId]!!

            location.date = LocalDate.now().format(DateTimeFormatter.ofPattern(ListLocationsActivity.DB_DATE_FORMAT))

            db.collection(ListLocationsActivity.NEW_PLACES_DOCUMENT_NAME).document(locationId).delete()
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete document: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                    }.addOnSuccessListener {
                        writeLocationToFirestore(ListLocationsActivity.OLD_PLACES_DOCUMENT_NAME, location)
                    }
        } else {
            authenticator.promptForLogin()

            if (authenticator.userIsLoggedIn()) {
                markLocationAsVisited(locationId)
            }
        }
    }

    fun writeLocationToFirestore(collectionName: String, location: Location) {
        if (authenticator.userIsLoggedIn()) {
            db.collection(collectionName).add(createFirestoreLocation(location))
                    .addOnSuccessListener {
                        Toast.makeText(context, "Success!", Toast.LENGTH_LONG).show()
                    }.addOnFailureListener {
                        Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show()
                    }
        } else {
            authenticator.promptForLogin()

            // Try just one more time to write location to Firebase,
            // we don't want to loop forever if user doesn't want to log in
            if (authenticator.userIsLoggedIn()) {
                writeLocationToFirestore(collectionName, location)
            }
        }
    }

    fun updateLocationInFirestore(collectionName: String, location: Location) {
        if (authenticator.userIsLoggedIn()) {
            db.collection(collectionName).document(location.documentId!!).set(createFirestoreLocation(location), SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(context, "Update successful", Toast.LENGTH_LONG).show()
                        //LOG.info("Success")
                    }
                    .addOnFailureListener{
                        Toast.makeText(context, "Update failed", Toast.LENGTH_LONG).show()
                        //e -> LOG.error("Failure " + e.toString())
                    }
        } else {
            authenticator.promptForLogin()

            if (authenticator.userIsLoggedIn()) {
                updateLocationInFirestore(collectionName, location)
            }
        }
    }

    /**
     * Get data from Cloud Firestore and attach listener to update the UI to reflect and changes to the database
     */
    fun loadFirestoreData(collectionName: String) {
        db.collection(collectionName).addSnapshotListener {snapshots, e ->
            if (e != null) {
                Log.w("firestoreListenEvent", "listen:error", e)
                return@addSnapshotListener
            }

            if (snapshots != null) {
                for (changes in snapshots.documentChanges) {
                    when (changes.type) {
                        DocumentChange.Type.ADDED -> displayLocations(changes.document, collectionName)
                        DocumentChange.Type.MODIFIED -> updateDisplayedLocations(changes.document, collectionName)
                        DocumentChange.Type.REMOVED -> removeLocationFromDisplay(changes.document)
                    }
                }
            }

            currentLocationList = getAllLocations()

            updateUI(sortLocations(currentLocationList))
        }
    }

    private fun getAllLocations() : MutableList<Location> {
        val listOfLocations = ArrayList<Location>()

        mapOfLocations.forEach {
            listOfLocations.add(it.value)
        }

        return listOfLocations
    }

    private fun createFirestoreLocation(location: Location) : MutableMap<String, Any> {
        val data: MutableMap<String, Any> = HashMap()

        data += "Name" to location.name
        data += "Type" to location.type
        data += "Date" to location.date

        return data
    }

    private fun displayLocations(document: QueryDocumentSnapshot, collectionName: String) {
        mapOfLocations[document.id] = LocationFactory.createLocation(
                document.id,
                document.data["Name"] as String,
                document.data["Type"] as String?,
                document.data["Date"] as String?,
                collectionName
        )
    }

    private fun updateDisplayedLocations(document: QueryDocumentSnapshot, collectionName: String) {
        mapOfLocations[document.id] = LocationFactory.createLocation(
                document.id,
                document.data["Name"] as String,
                document.data["Type"] as String?,
                document.data["Date"] as String?,
                collectionName
        )
    }

    private fun removeLocationFromDisplay(document: QueryDocumentSnapshot) {
        mapOfLocations.remove(document.id)
    }
}