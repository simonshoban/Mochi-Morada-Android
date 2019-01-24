package shoban.simon.mochimorada.location

import shoban.simon.mochimorada.ListLocationsActivity

class LocationFactory private constructor() {
    companion object {
        fun createLocation(documentId: String? = null, name: String, type: String?, date: String?, collectionName: String) : Location {
            var locationType = type
            var dateFirstVisited = date

            locationType = locationType ?: "Unknown"
            dateFirstVisited = dateFirstVisited ?: "Unknown"

            when (collectionName) {
                ListLocationsActivity.NEW_PLACES_DOCUMENT_NAME -> {
                    dateFirstVisited = ""
                }
                ListLocationsActivity.OLD_PLACES_DOCUMENT_NAME -> {

                }
            }

            return Location(documentId, name, locationType, dateFirstVisited)
        }
    }
}