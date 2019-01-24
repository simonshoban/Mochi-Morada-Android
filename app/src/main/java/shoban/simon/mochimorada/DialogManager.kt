package shoban.simon.mochimorada

import android.app.Activity
import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.Editable
import android.view.View
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner

import kotlinx.android.synthetic.main.form_location_new.view.*
import kotlinx.android.synthetic.main.form_location_old.view.*
import kotlinx.android.synthetic.main.form_sort_options.view.*

import shoban.simon.mochimorada.location.Location
import shoban.simon.mochimorada.location.LocationFactory

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DialogManager(private val firestoreHelper: FirestoreHelper, private val collectionName: String, private val context: Context, private val viewingNewPlaces: Boolean) {
    fun openSortOptionsDialog(updateUI: (listOfLocations: MutableList<Location>) -> Unit) {
        val dialogBuilder = AlertDialog.Builder(context)
        val view: View = (context as Activity).layoutInflater.inflate(R.layout.form_sort_options, null, false)

        dialogBuilder.setView(view)
        dialogBuilder.setTitle("Sort By")
        dialogBuilder.setCancelable(true)

        setCurrentSortOption(view, firestoreHelper.getCurrentSortOption())

        dialogBuilder.setPositiveButton("Sort") { dialog, _ ->
            val selectSortOption = view.findViewById<RadioButton>(view.sort_options.checkedRadioButtonId).tag as String

            updateUI(firestoreHelper.sortLocations(firestoreHelper.getCurrentLocations(), SortOptions.valueOf(selectSortOption)))
            dialog.cancel()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        dialogBuilder.create().show()
    }

    fun openAddLocationDialog() {
        val dialogBuilder = AlertDialog.Builder(context)
        val view: View = (context as Activity).layoutInflater.inflate(getAppropriateDialogLayout(), null, false)

        if (!viewingNewPlaces) {
            setDefaultDateWhenLayoutIsDrawn(view)
        }

        dialogBuilder.setView(view)
        dialogBuilder.setTitle("Add New Location?")
        dialogBuilder.setCancelable(true)

        dialogBuilder.setPositiveButton("Add Location") { dialog, _ ->
            if (viewingNewPlaces) {
                firestoreHelper.writeLocationToFirestore(collectionName, LocationFactory.createLocation(
                        null,
                        (view.location_name_new as EditText).text.toString(),
                        (view.location_type_new as Spinner).selectedItem.toString(),
                        null,
                        collectionName
                ))
            } else {
                firestoreHelper.writeLocationToFirestore(collectionName, LocationFactory.createLocation(
                        null,
                        (view.location_name_old as EditText).text.toString(),
                        (view.location_type_old as Spinner).selectedItem.toString(),
                        (view.location_date_visited as EditText).text.toString(),
                        collectionName
                ))
            }

            dialog.cancel()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        dialogBuilder.create().show()
    }

    fun openEditLocationDialog(location: Location) {
        val dialogBuilder = AlertDialog.Builder(context)
        val view: View = (context as Activity).layoutInflater.inflate(getAppropriateDialogLayout(), null, false)

        setFormWithLocationData(view, location)

        dialogBuilder.setView(view)
        dialogBuilder.setTitle("Edit Location")
        dialogBuilder.setCancelable(true)

        dialogBuilder.setPositiveButton("Save") { dialog, _ ->
            if (viewingNewPlaces) {
                location.name = (view.location_name_new as EditText).text.toString()
                location.type = (view.location_type_new as Spinner).selectedItem.toString()
            } else {
                location.name = (view.location_name_old as EditText).text.toString()
                location.type = (view.location_type_old as Spinner).selectedItem.toString()
                location.date = (view.location_date_visited as EditText).text.toString()
            }

            firestoreHelper.updateLocationInFirestore(collectionName, location)

            dialog.cancel()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        dialogBuilder.create().show()
    }

    fun openMarkLocationAsVisitedDialog(location: Location) {
        val dialogBuilder = AlertDialog.Builder(context)

        dialogBuilder.setTitle("Mark Location As Visited")
        dialogBuilder.setMessage("Mark ${location.name} as visited?")
        dialogBuilder.setCancelable(true)

        dialogBuilder.setPositiveButton("Yes") { dialog, _ ->
            firestoreHelper.markLocationAsVisited(location.documentId!!)
            dialog.cancel()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        dialogBuilder.create().show()
    }

    fun openDeleteLocationDialog(collectionName: String, location: Location) {
        val dialogBuilder = AlertDialog.Builder(context)

        dialogBuilder.setTitle("Delete this location?")
        dialogBuilder.setMessage("Delete ${location.name}?")
        dialogBuilder.setCancelable(true)

        dialogBuilder.setPositiveButton("Yes") { dialog, _ ->
            firestoreHelper.deleteLocation(collectionName, location.documentId!!)
            dialog.cancel()
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        dialogBuilder.create().show()
    }

    private fun setCurrentSortOption(view: View, currentSortOption: SortOptions) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val sortOption = when (currentSortOption) {
                    SortOptions.NAME_ASC -> view.sort_name_asc
                    SortOptions.NAME_DESC -> view.sort_name_desc
                    SortOptions.TYPE_ASC -> view.sort_type_asc
                    SortOptions.TYPE_DESC -> view.sort_type_desc
                    SortOptions.DATE_ASC -> view.sort_date_asc
                    SortOptions.DATE_DESC -> view.sort_date_desc
                }

                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                sortOption.isChecked = true
            }
        })
    }

    private fun setFormWithLocationData(view: View, location: Location) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val locationName = location.name.toEditable()
                val locationTypeId = context.resources.getStringArray(R.array.location_type).indexOf(location.type)
                val locationDateFirstVisited = location.date

                if (viewingNewPlaces) {
                    view.location_name_new.text = locationName
                    view.location_type_new.setSelection(locationTypeId)
                } else {
                    view.location_name_old.text = locationName
                    view.location_type_old.setSelection(locationTypeId)
                    view.location_date_visited.text = locationDateFirstVisited.toEditable()
                }

                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    /**
     * If user is adding a visited location, set the default visited date to today. Do this
     * once the layout has been drawn so that the date text field can be selected.
     */
    private fun setDefaultDateWhenLayoutIsDrawn(view: View) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val dateField = view.location_date_visited
                val currentDateInIso = LocalDate.now().format(DateTimeFormatter.ofPattern(ListLocationsActivity.DB_DATE_FORMAT))

                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                dateField.text = currentDateInIso.toEditable()
            }
        })
    }

    /**
     * There are two separate layouts for adding a new location and adding an old location
     */
    private fun getAppropriateDialogLayout() : Int {
        return when (collectionName) {
            ListLocationsActivity.NEW_PLACES_DOCUMENT_NAME -> R.layout.form_location_new
            ListLocationsActivity.OLD_PLACES_DOCUMENT_NAME -> R.layout.form_location_old
            else -> throw IllegalArgumentException("Not yet implemented")
        }
    }

    private fun String.toEditable(): Editable = Editable.Factory.getInstance().newEditable(this)
}