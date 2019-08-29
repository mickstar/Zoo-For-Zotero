package com.mickstarify.zooforzotero.LibraryActivity

import android.app.ProgressDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment.ItemAttachmentEntry
import com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment.ItemViewFragment
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.SettingsActivity
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import kotlinx.android.synthetic.main.activity_library.*
import java.io.File


class LibraryActivity : AppCompatActivity(), Contract.View,
    NavigationView.OnNavigationItemSelectedListener,
    SwipeRefreshLayout.OnRefreshListener,
    ItemViewFragment.OnListFragmentInteractionListener,
    ItemAttachmentEntry.OnAttachmentFragmentInteractionListener {


    private lateinit var presenter: Contract.Presenter
    private lateinit var firebaseAnalytics: FirebaseAnalytics

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

    override fun initUI() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        collectionsMenu = navigationView.menu.addSubMenu(
            R.id.group_collections,
            Menu.NONE,
            Menu.NONE,
            "Collections"
        )

        val swipeRefresh = findViewById<SwipeRefreshLayout>(R.id.library_swipe_refresh)
        swipeRefresh.setOnRefreshListener(this)
    }


    override fun addNavigationEntry(collection: Collection, parent: String) {
        collectionsMenu.add(R.id.group_collections, Menu.NONE, Menu.NONE, collection.getName())
            .setIcon(R.drawable.ic_folder_black_24dp)
            .setCheckable(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
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
                    return false
                }

                override fun onQueryTextChange(query: String): Boolean {
                    presenter.filterEntries(query)
                    return true
                }

            })
        }

        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.my_library) {
            presenter.setCollection("all")
        } else {
            presenter.setCollection("${item.title}")
        }
        val drawer = findViewById<DrawerLayout>(R.id.drawerlayout_library)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun populateEntries(entries: List<ListEntry>) {
        val listView: ListView = findViewById<ListView>(R.id.library_listview)
        listView.adapter = ZoteroItemListAdapter(this, entries)

        listView.setOnItemClickListener { adapter, view, position: Int, lpos ->
            val entry = listView.adapter.getItem(position) as ListEntry
            if (entry.isCollection()) {
                presenter.setCollection(entry.getCollection().getName())
            } else {
                presenter.selectItem(entry.getItem())
            }
        }
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
        alert.show()
    }

    override fun showLoadingAnimation(showScreen: Boolean) {
        if (showScreen) {
            this.showLibraryContentDisplay(message = "Downloading your library.")
            this.updateLibraryLoadingProgress(0)
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
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun setTitle(title: String) {
        this.setupActionbar()
        this.supportActionBar?.title = title
    }

    override fun showItemDialog(item: Item, attachments: List<Item>) {
        val myBottomSheet = ItemViewFragment.newInstance(item, attachments)
        val fm = supportFragmentManager
        myBottomSheet.show(fm, "hello world")
    }

    override fun openPDF(attachment: File) {
        var intent = Intent(Intent.ACTION_VIEW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", attachment)
            Log.d("zotero", "${uri.query}")
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = uri
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        } else {
            intent.setDataAndType(Uri.fromFile(attachment), "application/pdf")
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent = Intent.createChooser(intent, "Open File")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

    }

    @Suppress("DEPRECATION")
    var progressDialog: ProgressDialog? = null

    @Suppress("DEPRECATION")
    override fun updateAttachmentDownloadProgress(progress: Int, total: Int) {
        if (progressDialog == null) {
            progressDialog = ProgressDialog(this)
            progressDialog?.setTitle("Downloading File")
        }
        if (total == 0) {
            progressDialog?.isIndeterminate = true
        } else {
            progressDialog?.isIndeterminate = false
            progressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog?.progress = progress
            progressDialog?.max = total
            if (total == -1) {
                progressDialog?.setMessage("${progress}KB")
            } else {
                progressDialog?.setMessage("${progress}KB of ${total}KB")
            }
        }
        progressDialog?.show()
    }

    override fun hideAttachmentDownloadProgress() {
        progressDialog?.hide()
    }

    /* Is called by the fragment when an attachment is openned by the user. */
    override fun openAttachmentFileListener(item: Item) {
        presenter.openAttachment(item)
    }

    override fun onListFragmentInteraction(item: Item?) {
        Log.d("zotero", "got onListFragmentInteraction from item ${item?.ItemKey}")
    }

    override fun makeToastAlert(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(packageName, "got intent ${intent.action}")
        if (intent.action == ACTION_FILTER) {
            val query = intent.getStringExtra(EXTRA_QUERY) ?: ""
            Log.d(packageName, "got intent for library filter $query")
            presenter.filterEntries(query)
        }
    }

    override fun onBackPressed() {
        if (!searchView.isIconified) {
            searchView.isIconified = true
            presenter.closeQuery()
        } else {
            super.onBackPressed()
        }
    }

    var progressBar: ProgressBar? = null
    override fun updateLibraryLoadingProgress(progress: Int, total: Int) {
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
            textView.text = "Starting to download your library."
        } else {
            progressBar?.max = total
            progressBar?.isIndeterminate = false
            textView.text = "Downloading ${progress} of ${total} entries."
        }
        progressBar?.isActivated = true
    }

    override fun showLibraryContentDisplay(message: String) {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.constraintLayout_library_content)
        constraintLayout.visibility = View.VISIBLE
        val textView = findViewById<TextView>(R.id.textView_Library_label)
        if (message == "") {
            textView.setText(R.string.library_content_empty_library)
        } else {
            textView.setText(message)
        }
    }

    override fun hideLibraryContentDisplay() {
        val constraintLayout = findViewById<ConstraintLayout>(R.id.constraintLayout_library_content)
        constraintLayout.visibility = View.GONE
    }

    companion object {
        const val ACTION_FILTER = "com.mickstarify.zooforzotero.intent.action.LIBRARY_FILTER_INTENT"
        const val EXTRA_QUERY = "com.mickstarify.zooforzotero.intent.EXTRA_QUERY_TEXT"
    }
}
