package com.mickstarify.zooforzotero.LibraryActivity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment.ItemViewFragment
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Collection
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import kotlinx.android.synthetic.main.activity_library.*

class LibraryActivity : AppCompatActivity(), Contract.View, NavigationView.OnNavigationItemSelectedListener,
    SwipeRefreshLayout.OnRefreshListener, ItemViewFragment.OnListFragmentInteractionListener {
    override fun onListFragmentInteraction(item: Item?) {
    }


    private lateinit var presenter: Contract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)

        setSupportActionBar(toolbar)

        val drawer = findViewById<DrawerLayout>(R.id.drawerlayout_library)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        navigationView.setNavigationItemSelectedListener(this)

        presenter = LibraryActivityPresenter(this, this)
    }

    lateinit var collectionsMenu: Menu

    override fun initUI() {
        val navigationView = findViewById<NavigationView>(R.id.nav_view_library)
        collectionsMenu = navigationView.menu.addSubMenu(R.id.group_collections, Menu.NONE, Menu.NONE, "Collections")
    }

    override fun addNavigationEntry(collection: Collection, parent: String) {
        collectionsMenu.add(R.id.group_collections, Menu.NONE, Menu.NONE, collection.getName())
            .setIcon(R.drawable.ic_folder_black_24dp)
            .setCheckable(true)
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

    override fun populateItems(items: List<Item>) {
        val listView : ListView = findViewById<ListView>(R.id.library_listview)
        listView.adapter = ZoteroItemListAdapter(this, items)

        listView.setOnItemClickListener { adapter, view, position: Int, lpos ->
            val item = listView.adapter.getItem(position) as Item
            presenter.selectItem(item)
        }
    }

    override fun onRefresh() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createErrorAlert(title: String, message: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun showLoadingAnimation() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun hideLoadingAnimation() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setTitle(title: String) {
        this.title = title
    }

    override fun setSidebarEntries(entries: List<String>) {
        TODO("later bro")
    }

    override fun showItemDialog(item: Item){
        val myBottomSheet = ItemViewFragment.newInstance(item)
        val fm = supportFragmentManager
        myBottomSheet.show(fm, "hello world")
    }
}
