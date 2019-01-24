package shoban.simon.mochimorada

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*

import kotlinx.android.synthetic.main.activity_list_locations.list

import shoban.simon.mochimorada.location.Location
import shoban.simon.mochimorada.location.LocationAdapter

class ListLocationsActivity : AppCompatActivity() {
    companion object {
        // Standardized format we save dates to in database
        const val DB_DATE_FORMAT = "yyyy-MM-dd"

        // Root-level collection names in Firestore
        const val NEW_PLACES_DOCUMENT_NAME = "New Places"
        const val OLD_PLACES_DOCUMENT_NAME = "Old Places"

        // Intent request codes
        const val RC_SIGN_IN = 20
    }

    private lateinit var authenticator: Authenticator
    private lateinit var firestoreHelper: FirestoreHelper
    private lateinit var dialogManager: DialogManager

    private lateinit var collectionName: String

    private var searchView: SearchView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_locations)

        collectionName = intent?.extras?.get("Collection") as String

        authenticator = Authenticator(this)
        firestoreHelper = FirestoreHelper(this, authenticator, ::updateUI)
        dialogManager = DialogManager(firestoreHelper, collectionName, this, viewingNewPlaces())

        firestoreHelper.setFirestoreSettings()
        firestoreHelper.loadFirestoreData(collectionName)

        setListViewListener()
        setupToolbar()
    }

    override fun onResume() {
        super.onResume()
        authenticator.refreshCurrentUser(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.list_menu, menu)

        val searchViewItem = menu.findItem(R.id.search_for_location)

        // Get the SearchView and set the searchable configuration
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView = searchViewItem.actionView as SearchView

        searchView!!.queryHint = resources.getString(R.string.search_for_a_location)
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))

        val queryTextListener = object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(query: String): Boolean {
                updateUI(firestoreHelper.queryLocations(query))

                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                updateUI(firestoreHelper.queryLocations(query))
                hideKeyboard()

                return true
            }
        }

        searchView!!.setOnQueryTextListener(queryTextListener)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.add_item_button -> dialogManager.openAddLocationDialog()
            R.id.sort_button -> dialogManager.openSortOptionsDialog(::updateUI)
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setListViewListener() {
        list.onItemClickListener = AdapterView.OnItemClickListener {_, view, _, _ ->
            dialogManager.openEditLocationDialog(firestoreHelper.getLocationById(view.tag as String)!!)
        }

        list.onItemLongClickListener = AdapterView.OnItemLongClickListener {_, view, _, _ ->
            dialogManager.openDeleteLocationDialog(collectionName, firestoreHelper.getLocationById(view.tag as String)!!)

            return@OnItemLongClickListener true
        }
    }

    /**
     * Are we looking at new locations?
     */
    private fun viewingNewPlaces() : Boolean {
        return collectionName == ListLocationsActivity.NEW_PLACES_DOCUMENT_NAME
    }

    private fun updateUI(listOfLocations: MutableList<Location>) {
        val lastViewedPosition = list.firstVisiblePosition

        //get offset of first visible view
        val v = list.getChildAt(0)
        val topOffset = v?.top ?: 0

        list.adapter = LocationAdapter(this, listOfLocations, viewingNewPlaces(), dialogManager::openMarkLocationAsVisitedDialog)

        //Stay in the same place after resetting adapter. Otherwise you'll go back to the top each time.
        list.setSelectionFromTop(lastViewedPosition, topOffset)
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    private fun hideKeyboard() {
        val inputMethodManager = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.hideSoftInputFromWindow(this.currentFocus!!.windowToken, 0)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        when (requestCode) {
            RC_SIGN_IN -> {
                authenticator.authenticate(data)
            }
        }
    }
}
