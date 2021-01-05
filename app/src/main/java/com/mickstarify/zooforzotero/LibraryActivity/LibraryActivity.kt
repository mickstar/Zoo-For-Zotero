package com.mickstarify.zooforzotero.LibraryActivity

import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.core.view.iterator
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.mickstarify.zooforzotero.AttachmentManager.AttachmentManager
import com.mickstarify.zooforzotero.LibraryActivity.ItemView.ItemAttachmentEntry
import com.mickstarify.zooforzotero.LibraryActivity.ItemView.ItemViewFragment
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteInteractionListener
import com.mickstarify.zooforzotero.LibraryActivity.Notes.NoteView
import com.mickstarify.zooforzotero.LibraryActivity.WebDAV.WebDAVSetup
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.SettingsActivity
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Note
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.GroupInfo
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import kotlinx.android.synthetic.main.activity_library.*


class LibraryActivity : AppCompatActivity(), Contract.View,
    NavigationView.OnNavigationItemSelectedListener,
    SwipeRefreshLayout.OnRefreshListener,
    ItemViewFragment.OnItemFragmentInteractionListener,
    ItemAttachmentEntry.OnAttachmentFragmentInteractionListener,
    NoteInteractionListener {

    private val MENU_ID_UNFILED_ITEMS: Int = 1
    private val MENU_ID_TRASH: Int = 2

    private val MENU_ID_COLLECTIONS_OFFSET: Int = 10

    private lateinit var presenter: Contract.Presenter
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var itemView: ItemViewFragment? = null

    // used to "uncheck" the last pressed menu item if we change via code.
    private var currentPressedMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        this.setupActionbar()
        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        navigationView.setNavigationItemSelectedListener(this)

        presenter = LibraryActivityPresenter(this, this)
    }

    lateinit var collectionsMenu: Menu
    lateinit var sharedCollections: Menu

    override fun initUI() {
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

        // add our "Trash" entry
        navigationView.menu.add(R.id.group_other, MENU_ID_TRASH, Menu.NONE, "Trash")
            .setIcon(R.drawable.baseline_delete_24).isCheckable = true

        sharedCollections = navigationView.menu.addSubMenu(
            R.id.group_shared_collections,
            Menu.NONE,
            Menu.NONE,
            "Group Libraries"
        )

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)
        swipeRefresh.setOnRefreshListener(this)
    }

    val collectionKeyByMenuId = SparseArray<String>()

    override fun addNavigationEntry(collection: Collection, parent: String) {
        // we will add a collection to the menu, with the following menu ID.
        val menuId = MENU_ID_COLLECTIONS_OFFSET + collectionKeyByMenuId.size()
        collectionKeyByMenuId.put(menuId, collection.key)

        collectionsMenu.add(R.id.group_collections, menuId, Menu.NONE, collection.name)
            .setIcon(R.drawable.ic_folder_black_24dp).isCheckable = true
    }

    override fun highlightMenuItem(state: LibraryModelState) {
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
    override fun addSharedCollection(groupInfo: GroupInfo) {
        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        sharedCollections.add(
            R.id.group_shared_collections,
            Menu.NONE,
            Menu.NONE,
            groupInfo.name
        )
            .setIcon(R.drawable.ic_folder_black_24dp).isCheckable = true
    }

    override fun clearSidebar() {
        collectionsMenu.clear()
    }

    fun showFilterMenu() {
        LibraryFilterMenuDialog(this, { presenter.redisplayItems() }).show()
    }

    private fun showWebDAVSetup() {
        val intent = Intent(this, WebDAVSetup::class.java)
        startActivity(intent)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_menu -> {
                showFilterMenu()
            }

            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.webdav_setup -> {
                showWebDAVSetup()
            }
            R.id.zotero_save -> {
                val url = "https://www.zotero.org/save"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
            R.id.attachment_manager -> {
                val intent = Intent(this, AttachmentManager::class.java)
                startActivity(intent)
            }
            R.id.force_resync -> {
                presenter.requestForceResync()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    lateinit var searchView: SearchView
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_library_actionbar, menu)

        val menuItem = menu.findItem(R.id.search)
        menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                presenter.closeQuery()
                return true
            }

        })

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    setTitle("Search results for $query")
                    // we will only search on submit for android 7<=
                    if (!presenter.isLiveSearchEnabled()) {
                        presenter.filterEntries(query)
                    }
                    presenter.addFilterState(query)
                    return true
                }

                override fun onQueryTextChange(query: String): Boolean {
                    if (!presenter.isLiveSearchEnabled()) {
                        return false
                    }
                    presenter.filterEntries(query)
                    return true
                }

            })
        }

        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d("zotero", "pressed ${item.groupId} - ${item.itemId}")
        currentPressedMenuItem?.let {
            it.isChecked = false
        }

        currentPressedMenuItem = item
        if (item.itemId == R.id.my_library) {
            presenter.setCollection("all")
        } else if (item.itemId == MENU_ID_UNFILED_ITEMS) {
            presenter.setCollection("unfiled_items")
        } else if (item.itemId == MENU_ID_TRASH) {
            presenter.openTrash()
        } else if (item.groupId == R.id.group_shared_collections) {
            presenter.openGroup(item.title.toString())
        } else if (item.groupId == R.id.group_collections) {
            presenter.setCollection(collectionKeyByMenuId[item.itemId], isSubCollection = false)
        } else {
            Log.e("zotero", "error unhandled menuitem. ${item.title}")
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawerlayout_library)
        drawer.closeDrawer(GravityCompat.START)
        // shouldnt be neccessary but there was a bug where android wouldn't highlight on certain occassions
        item.isChecked = true
        return true
    }

    override fun populateEntries(entries: List<ListEntry>) {
        if (entries.size == 0) {
            this.showEmptyList()
            return
        }
        this.hideLibraryContentDisplay()
        val listView: ListView = findViewById(R.id.library_listview)
        listView.adapter = ZoteroItemListAdapter(this, entries)


        listView.setOnItemLongClickListener { _, _, position, _ ->
            val entry = listView.adapter.getItem(position) as ListEntry
            if (entry.isCollection()) {
                presenter.setCollection(entry.getCollection().name, isSubCollection = true)
            } else {
                presenter.selectItem(entry.getItem(), longPress = true)
            }
            true
        }

        listView.setOnItemClickListener { _, _, position: Int, _ ->
            val entry = listView.adapter.getItem(position) as ListEntry
            if (entry.isCollection()) {
                presenter.setCollection(entry.getCollection().key, isSubCollection = true)
            } else {
                presenter.selectItem(entry.getItem(), false)
            }
        }

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)

        listView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScroll(
                view: AbsListView,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                if (listView.getChildAt(0) != null) {
                    swipeRefreshLayout.isEnabled =
                        listView.firstVisiblePosition == 0 && listView.getChildAt(
                            0
                        ).top == 0
                }
            }

            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {
            }

        })
    }

    override fun onRefresh() {
        Log.d("Zotero", "got request for refresh")
        presenter.requestLibraryRefresh()

    }

    override fun createErrorAlert(title: String, message: String, onClick: () -> Unit) {
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

    override fun showLoadingAnimation(showScreen: Boolean) {
        if (showScreen) {
            this.showLibraryContentDisplay(message = "Downloading your library.")
            this.updateLibraryLoadingProgress(0, message = "")
        } else {
            val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)
            swipeRefresh.isRefreshing = true
        }
    }

    override fun hideLoadingAnimation() {
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)
        swipeRefresh.isRefreshing = false
        if (presenter.isShowingContent()) {
            this.hideLibraryContentDisplay()
        } else {
            this.showLibraryContentDisplay(message = getString(R.string.library_content_empty_library))
            progressBar?.visibility = View.INVISIBLE
        }
    }

    private fun setupActionbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        supportActionBar?.setHomeButtonEnabled(false)
        val drawer = findViewById<DrawerLayout>(R.id.drawerlayout_library)
        val toggle = ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun setTitle(title: String) {
        this.setupActionbar()
        this.supportActionBar?.title = title
    }

    override fun showItemDialog(
        item: Item,
        attachments: List<Item>,
        notes: List<Note>
    ) {
        itemView = ItemViewFragment.newInstance(item, attachments, notes)
        val fm = supportFragmentManager
        itemView?.show(fm, "hello world")

    }

    override fun showNote(note: Note) {
        val noteView = NoteView(this, note, this)
        noteView.show()
    }

    override fun closeItemView() {
        itemView?.dismiss()
        itemView = null
    }

    override fun showBasicSyncAnimation() {
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)
        swipeRefresh.isRefreshing = true
    }

    override fun hideBasicSyncAnimation() {
        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)
        swipeRefresh.isRefreshing = false
    }

    @Suppress("DEPRECATION")
    var progressDialog: ProgressDialog? = null

    @Suppress("DEPRECATION")
    override fun updateAttachmentDownloadProgress(progress: Int, total: Int) {
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
//        else {
//            progressDialog?.isIndeterminate = false
//            progressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
//            progressDialog?.setProgress(progress)
//            if (total == -1) {
//                progressDialog?.setMessage("We are connecting to the zotero servers and requesting the file.")
//            } else {
//                progressDialog?.max = total
//                progressDialog?.setMessage("${progress}KB of ${total}KB")
//            }
//        }
        progressDialog?.show()
    }

    override fun hideAttachmentDownloadProgress() {
        progressDialog?.hide()
        progressDialog = null
    }

    var uploadProgressDialog: ProgressDialog? = null
    override fun showAttachmentUploadProgress(attachment: Item) {
        uploadProgressDialog = ProgressDialog(this)
        uploadProgressDialog?.setTitle("Uploading Attachment")
        uploadProgressDialog?.setMessage(
            "Uploading ${attachment.data["filename"]}. " +
                    "This may take a while, do not close the app."
        )
        uploadProgressDialog?.isIndeterminate = true
        uploadProgressDialog?.show()
    }

    override fun hideAttachmentUploadProgress() {
        uploadProgressDialog?.hide()
        uploadProgressDialog = null
    }

    override fun createYesNoPrompt(
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

    override fun makeToastAlert(message: String) {
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

    var listPosition: Int = 0
    override fun onPostResume() {
        Log.e("zotero", "post-resumed")
        super.onPostResume()
        presenter.onResume()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.e("zotero", "restored!")
    }

    var progressBar: ProgressBar? = null
    override fun updateLibraryLoadingProgress(
        progress: Int,
        total: Int,
        message: String
    ) {
        if (progressBar == null) {
            progressBar = findViewById(R.id.progressBar_library)
        }
        if (progressBar?.visibility == View.INVISIBLE || progressBar?.visibility == View.GONE) {
            progressBar?.visibility = View.VISIBLE
        }
        val textView = findViewById<TextView>(R.id.textView_Library_label)

        progressBar?.progress = progress
        if (total == -1) {
            progressBar?.isIndeterminate = true
            textView.text = getString(R.string.loading_library_text)
        } else {
            progressBar?.max = total
            progressBar?.isIndeterminate = false
            textView.text = if (message == "") {
                "Downloading ${progress} of ${total} entries."
            } else {
                message
            }
        }
        progressBar?.isActivated = true
    }

    fun showEmptyList() {
        showLibraryContentDisplay(message = "This catalog is empty. If you believe this is an error please refresh your library.")
    }

    override fun showLibraryContentDisplay(message: String) {
        val constraintLayout =
            findViewById<ConstraintLayout>(R.id.constraintLayout_library_content)
        constraintLayout.visibility = View.VISIBLE
        val textView = findViewById<TextView>(R.id.textView_Library_label)
        if (message == "") {
            textView.setText(R.string.library_content_empty_library)
        } else {
            textView.text = message
        }
    }

    override fun hideLibraryContentDisplay() {
        val constraintLayout =
            findViewById<ConstraintLayout>(R.id.constraintLayout_library_content)
        constraintLayout.visibility = View.GONE
        progressBar?.visibility = View.GONE
        progressBar = null

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)
        swipeRefresh.isRefreshing = false
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
        if (!searchView.isIconified) {
            searchView.isIconified = true
            presenter.closeQuery()
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
}
