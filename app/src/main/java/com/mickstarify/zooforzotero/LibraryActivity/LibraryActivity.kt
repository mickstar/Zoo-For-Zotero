package com.mickstarify.zooforzotero.LibraryActivity

import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.navigation.NavigationView
import com.mickstarify.zooforzotero.AttachmentManager.AttachmentManager
import com.mickstarify.zooforzotero.LibraryActivity.ItemView.ItemAttachmentEntry
import com.mickstarify.zooforzotero.LibraryActivity.ItemView.ItemViewFragment
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteInteractionListener
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteView
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.SettingsActivity
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item


class LibraryActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,

    ItemViewFragment.OnItemFragmentInteractionListener,
    ItemAttachmentEntry.OnAttachmentFragmentInteractionListener,
    NoteInteractionListener {

    private val MENU_ID_UNFILED_ITEMS: Int = 1
    private val MENU_ID_TRASH: Int = 2
    private val MENU_ID_MY_PUBLICATIONS = 3

    private val MENU_ID_COLLECTIONS_OFFSET: Int = 10

    lateinit var presenter: LibraryActivityPresenter
    private var itemView: ItemViewFragment? = null

    lateinit var navController: NavController
    lateinit var navHostFragment: NavHostFragment

    // used to "uncheck" the last pressed menu item if we change via code.
    private var currentPressedMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        navController = navHostFragment.navController

        this.setupActionbar()
        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        navigationView.setNavigationItemSelectedListener(this)

        presenter = LibraryActivityPresenter(this, this)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.libraryListFragment -> {
                    libraryListScreenShown()
                }
                R.id.libraryLoadingScreen -> {
                    libraryLoadingScreenShown()
                }
                R.id.barcodeScanningScreen -> {
                    barcodeScanningScreenShown()
                }
                else -> {
                    throw(NotImplementedError("Error screen $destination not handled."))
                }
            }

        }
    }

    private fun barcodeScanningScreenShown() {
        supportActionBar?.title = "Barcode Scanner"
        mDrawerToggle.isDrawerIndicatorEnabled = false
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun libraryLoadingScreenShown() {
        supportActionBar?.title = "Loading"
        mDrawerToggle.isDrawerIndicatorEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    private fun libraryListScreenShown() {
        supportActionBar?.title = "Library"
        mDrawerToggle.isDrawerIndicatorEnabled = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    lateinit var collectionsMenu: Menu
    lateinit var sharedCollections: Menu

    fun initUI() {
        setupActionbar()
        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        navigationView.setCheckedItem(R.id.my_library)
        collectionsMenu = navigationView.menu.addSubMenu(
            R.id.group_collections,
            Menu.NONE,
            Menu.NONE,
            "Collections"
        )

        // add our "unfiled items" entry
        navigationView.menu.add(R.id.group_other, MENU_ID_UNFILED_ITEMS, Menu.NONE, "Unfiled Items")
            .setIcon(R.drawable.baseline_description_24).isCheckable = true

        // add our "My Publications" entry
        navigationView.menu.add(
            R.id.group_other,
            MENU_ID_MY_PUBLICATIONS,
            Menu.NONE,
            "My Publications"
        )
            .setIcon(R.drawable.baseline_book_24).isCheckable = true

        // add our "Trash" entry
        navigationView.menu.add(R.id.group_other, MENU_ID_TRASH, Menu.NONE, "Trash")
            .setIcon(R.drawable.baseline_delete_24).isCheckable = true



        sharedCollections = navigationView.menu.addSubMenu(
            R.id.group_shared_collections,
            Menu.NONE,
            Menu.NONE,
            "Group Libraries"
        )

        navController.navigate(R.id.libraryListFragment)
    }

    val collectionKeyByMenuId = SparseArray<String>()

    fun addNavigationEntry(collection: Collection, parent: String) {
        // we will add a collection to the menu, with the following menu ID.
        val menuId = MENU_ID_COLLECTIONS_OFFSET + collectionKeyByMenuId.size()
        collectionKeyByMenuId.put(menuId, collection.key)

        collectionsMenu.add(R.id.group_collections, menuId, Menu.NONE, collection.name)
            .setIcon(R.drawable.ic_folder_black_24dp).isCheckable = true
    }

    fun highlightMenuItem(state: LibraryModelState) {
        /* Given some collection name / group name, highlight that corresponding item.
        * Needed for back button, whereby the app loads a prior state and needs the UI
        * to reflect this change with respect to the item selected.  */

        currentPressedMenuItem?.let {
            it.isChecked = false
        }

        if (state.isUsingGroup()) {
            for (menuItem: MenuItem in sharedCollections) {
                if (menuItem.title.toString() == state.currentGroup.name) {
                    menuItem.isChecked = true
                    currentPressedMenuItem = menuItem
                    break
                }
            }
            return
        }

        val menuItem: MenuItem? = if (state.currentCollection == "all") {
            val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
            navigationView.menu.findItem(R.id.my_library)
        } else if (state.currentCollection == "unfiled_items") {
            val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
            navigationView.menu.findItem(MENU_ID_UNFILED_ITEMS)
        } else if (state.currentCollection == "zooforzotero_my_publications") {
            val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
            navigationView.menu.findItem(MENU_ID_MY_PUBLICATIONS)
        } else if (state.currentCollection == "zooforzotero_Trash") {
            val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
            navigationView.menu.findItem(MENU_ID_TRASH)
        } else {
            // if it reaches here that means it's a collection.
            val index = collectionKeyByMenuId.indexOfValue(state.currentCollection)
            if (index >= 0) {
                val menuId = collectionKeyByMenuId.keyAt(index)
                collectionsMenu.findItem(menuId)
            } else {
                null
            }
        }
        menuItem?.isChecked = true
        menuItem?.let {
            currentPressedMenuItem = it
        }
    }

    /* Shows a shared Collection (group) on the sidebar. */
    fun addSharedCollection(groupInfo: GroupInfo) {
        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        sharedCollections.add(
            R.id.group_shared_collections,
            Menu.NONE,
            Menu.NONE,
            groupInfo.name
        )
            .setIcon(R.drawable.ic_folder_black_24dp).isCheckable = true
    }

    fun clearSidebar() {
        collectionsMenu.clear()
    }

    fun showFilterMenu() {
        LibraryFilterMenuDialog(this, { presenter.redisplayItems() }).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        when (item.itemId) {
            R.id.filter_menu -> {
                showFilterMenu()
            }

            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.attachment_manager -> {
                val intent = Intent(this, AttachmentManager::class.java)
                startActivity(intent)
            }
            R.id.force_resync -> {
                presenter.requestForceResync()
            }
            android.R.id.home -> {
                onBackPressed()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    lateinit var searchView: SearchView
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_library_actionbar, menu)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d("zotero", "pressed ${item.groupId} - ${item.itemId}")
        currentPressedMenuItem?.let {
            it.isChecked = false
        }

        currentPressedMenuItem = item
        if (item.itemId == R.id.my_library) {
            presenter.setCollection("all", fromNavigationDrawer = true)
        } else if (item.itemId == MENU_ID_UNFILED_ITEMS) {
            presenter.setCollection("unfiled_items", fromNavigationDrawer = true)
        } else if (item.itemId == MENU_ID_MY_PUBLICATIONS) {
            presenter.openMyPublications()
        } else if (item.itemId == MENU_ID_TRASH) {
            presenter.openTrash()
        } else if (item.groupId == R.id.group_shared_collections) {
            // this is the id that refers to group libraries
            presenter.openGroup(item.title.toString())
        } else if (item.groupId == R.id.group_collections) {
            // this is the id that refers to user collections. The term "group collections" is misleading.
            presenter.setCollection(collectionKeyByMenuId[item.itemId], fromNavigationDrawer = true)
        } else {
            Log.e("zotero", "error unhandled menuitem. ${item.title}")
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawerlayout_library)
        drawer.closeDrawer(GravityCompat.START)
        // shouldnt be neccessary but there was a bug where android wouldn't highlight on certain occassions
        item.isChecked = true
        return true
    }

    fun createErrorAlert(title: String, message: String, onClick: () -> Unit) {
        val alert = AlertDialog.Builder(this)
        alert.setIcon(R.drawable.ic_error_black_24dp)
        alert.setTitle(title)
        alert.setMessage(message)
        alert.setPositiveButton("Ok") { _, _ -> onClick() }
        try {
            alert.show()
        } catch (exception: WindowManager.BadTokenException) {
            Log.e("zotero", "error cannot show error dialog. ${message}")
        }
    }

    lateinit var mDrawerToggle: ActionBarDrawerToggle
    private fun setupActionbar() {
        val drawer = findViewById<DrawerLayout>(R.id.drawerlayout_library)
        mDrawerToggle = ActionBarDrawerToggle(
            this,
            drawer,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        mDrawerToggle.isDrawerIndicatorEnabled = true
        mDrawerToggle.isDrawerSlideAnimationEnabled = true

        drawer.addDrawerListener(mDrawerToggle)
        mDrawerToggle.syncState()

        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.show()
    }

    override fun onPostCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onPostCreate(savedInstanceState, persistentState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mDrawerToggle.syncState()
    }

    fun setTitle(title: String) {
        supportActionBar?.title = title
    }

    fun showItemDialog(
    ) {
        itemView = ItemViewFragment()
        val fm = supportFragmentManager
        itemView?.show(fm, "ItemDialog")

    }

    fun showNote(note: Note) {
        val noteView = NoteView(this, note, this)
        noteView.show()
    }

    fun closeItemView() {
        itemView?.dismiss()
        itemView = null
    }

    fun showLoadingAlertDialog(message: String) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
        }
        progressDialog?.isIndeterminate = true
        progressDialog?.setMessage(message)
        progressDialog?.show()

    }

    fun hideLoadingAlertDialog() {
        progressDialog?.hide()
        progressDialog = null
    }

    @Suppress("DEPRECATION")
    var progressDialog: ProgressDialog? = null

    @Suppress("DEPRECATION")
    fun updateAttachmentDownloadProgress(progress: Int, total: Int) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog?.setTitle("Downloading File")
            progressDialog?.setButton("Cancel") { dialogInterface, _ ->
                presenter.cancelAttachmentDownload()
            }
        }
        if (true) { // if (total == 0) {
            progressDialog?.isIndeterminate = true
            val progressString = if (progress == 0) {
                ""
            } else {
                if (total > 0) {
                    "$progress/${total}KB"
                } else {
                    "${progress}KB"
                }
            }

            progressDialog?.setMessage("Downloading your Attachment. $progressString")
        }
        progressDialog?.show()
    }

    fun hideAttachmentDownloadProgress() {
        progressDialog?.hide()
        progressDialog = null
    }

    var uploadProgressDialog: ProgressDialog? = null
    fun showAttachmentUploadProgress(attachment: Item) {
        uploadProgressDialog = ProgressDialog(this)
        uploadProgressDialog?.setTitle("Uploading Attachment")
        uploadProgressDialog?.setMessage(
            "Uploading ${attachment.data["filename"]}. " +
                    "This may take a while, do not close the app."
        )
        uploadProgressDialog?.isIndeterminate = true
        uploadProgressDialog?.show()
    }

    fun hideAttachmentUploadProgress() {
        uploadProgressDialog?.hide()
        uploadProgressDialog = null
    }

    fun createYesNoPrompt(
        title: String,
        message: String, yesText: String, noText: String,
        onYesClick: () -> Unit,
        onNoClick: () -> Unit
    ) {
        val alert = AlertDialog.Builder(this)
        alert.setIcon(R.drawable.ic_error_black_24dp)
        alert.setTitle(title)
        alert.setMessage(message)
        alert.setPositiveButton(yesText) { _, _ -> onYesClick() }
        alert.setNegativeButton(noText) { _, _ -> onNoClick() }
        try {
            alert.show()
        } catch (exception: WindowManager.BadTokenException) {
            Log.e("zotero", "error creating window bro")
        }
    }

    /* Is called by the fragment when an attachment is openned by the user. */
    override fun openAttachmentFileListener(item: Item) {
        presenter.openAttachment(item)
    }

    override fun forceUploadAttachmentListener(item: Item) {
        presenter.uploadAttachment(item)
    }

    override fun openLinkedAttachmentListener(item: Item) {
        presenter.openAttachment(item)
    }

    override fun onListFragmentInteraction(item: Item?) {
        Log.d("zotero", "got onListFragmentInteraction from item ${item?.itemKey}")
    }

    fun makeToastAlert(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(packageName, "got intent ${intent.action} $intent")
        if (intent.action == ACTION_FILTER) {
            val query = intent.getStringExtra(EXTRA_QUERY) ?: ""
            Log.d(packageName, "got intent for library filter $query")
            presenter.filterEntries(query)
        }
    }

    override fun onPause() {
        Log.e("zotero", "paused")
        super.onPause()
    }

    override fun onPostResume() {
        Log.e("zotero", "post-resumed")
        super.onPostResume()
        presenter.onResume()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.e("zotero", "restored!")
    }

    override fun onResume() {
        super.onResume()
        Log.d("zotero", "onResume called.")
    }

    override fun onNoteCreate(note: Note) {
        presenter.createNote(note)
    }

    override fun onNoteEdit(note: Note) {
        presenter.modifyNote(note)
    }

    override fun onNoteDelete(note: Note) {
        presenter.deleteNote(note)
    }

    private var doubleBackToExitPressedOnce = false
    override fun onBackPressed() {
        // our list view has a custom back handler
        // if we are on the barcode screen we will just want to return to the previous screen
        if (getCurrentScreen() == AvailableScreens.BARCODE_SCANNING_SCREEN) {
            navController.navigateUp()
        } else {
            presenter.backButtonPressed()
        }

        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }

    companion object {
        const val ACTION_FILTER =
            "com.mickstarify.zooforzotero.intent.action.LIBRARY_FILTER_INTENT"
        const val EXTRA_QUERY = "com.mickstarify.zooforzotero.intent.EXTRA_QUERY_TEXT"
    }

    override fun deleteNote(note: Note) {
        presenter.deleteNote(note)
    }

    override fun editNote(note: Note) {
        presenter.modifyNote(note)
    }

    override fun shareText(shareText: String) {
        /* Used to share zotero items. */

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    override fun onTagOpenListener(tag: String) {
        presenter.onTagOpen(tag)
    }

    fun getCurrentScreen(): AvailableScreens {
        return when (navController.currentDestination?.id) {
            R.id.libraryListFragment -> AvailableScreens.LIBRARY_LISTVIEW_SCREEN
            R.id.libraryLoadingScreen -> AvailableScreens.LIBRARY_LOADING_SCREEN
            R.id.barcodeScanningScreen -> AvailableScreens.BARCODE_SCANNING_SCREEN
            else -> throw (Exception("Unknown current screen ${navController.currentDestination}"))
        }
    }

    fun openZoteroSaveForQuery(query: String) {
        val url = "https://www.zotero.org/save?q=$query"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        startActivity(intent)
    }
}

enum class AvailableScreens {
    LIBRARY_LOADING_SCREEN,
    LIBRARY_LISTVIEW_SCREEN,
    BARCODE_SCANNING_SCREEN
}