package com.gecko.terraspherecore.fragment


import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.gecko.terraspherecore.R
import com.gecko.terraspherecore.activity.contact.ContactDetailActivity
import com.gecko.terraspherecore.activity.contact.ImportContactActivity
import com.gecko.terraspherecore.common.Settings
import com.gecko.terraspherecore.common.adapter.AutoAdapter
import com.gecko.terraspherecore.common.adapter.getItemsSource
import com.gecko.terraspherecore.common.adapter.updateItemsSource
import com.gecko.terraspherecore.common.extension.toActivity
import com.gecko.terraspherecore.data.ContactData
import com.gecko.terraspherecore.data.DbContext
import io.reactivex.disposables.Disposable
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.fragment_contacts.*
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt


class ContactsFragment : ViewPagerFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    private var contactCopy: ArrayList<ContactData> = arrayListOf()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (toolbar == null) {
            return super.onKeyDown(keyCode, event)
        }
        val searchMenu = toolbar.menu.findItem(R.id.menu_search)
        if (searchMenu.isActionViewExpanded) {
            searchMenu.collapseActionView()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.inflateMenu(R.menu.contacts_toolbar)
        val searchMenu = toolbar.menu.findItem(R.id.menu_search)
        val addMenu = toolbar.menu.findItem(R.id.menu_add)
        val searchView = searchMenu.actionView as SearchView
        searchMenu.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                contactCopy.clear()
                recycler_view.getItemsSource<ContactData>()?.let { contactCopy.addAll(it) }
                addMenu.isVisible = false
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                recycler_view.updateItemsSource(contactCopy)
                addMenu.isVisible = true
                return true
            }

        })
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrEmpty()) {
                    recycler_view.updateItemsSource(contactCopy.filter {
                        it.name.contains(newText)
                    })
                } else {
                    recycler_view.updateItemsSource(contactCopy)
                }
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_search -> {
                    true
                }
                R.id.menu_add -> {
                    addContact()
                    true
                }
                else -> false
            }
        }
        recycler_view.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AutoAdapter<ContactData>(android.R.layout.simple_list_item_1).apply {
                bindText(android.R.id.text1) {
                    it.name
                }
                itemClicked.observe(viewLifecycleOwner, Observer {
                    context.toActivity<ContactDetailActivity>(Intent().apply {
                        putExtra("data", it.item)
                    })
                })
                whenEmpty(R.layout.item_contact_empty)
            }
        }
        subscribeContactList()
    }

    override fun onPageSelected() {
        super.onPageSelected()
        if (!Settings.get("has_contact_entry", false)) {
            activity?.let {
                MaterialTapTargetPrompt.Builder(it)
                        .setTarget(R.id.menu_add)
                        .setPrimaryText(getString(R.string.intro_add_contact_title))
                        .setSecondaryText(getString(R.string.intro_add_contact_desc))
                        .setPromptStateChangeListener { _, _ ->
                            Settings.set("has_contact_entry", true)
                        }
                        .show()
            }
        }
    }

    private fun addContact() {
        context.toActivity<ImportContactActivity>()
    }

    private lateinit var contactSubscribe: Disposable

    private fun subscribeContactList() {
        this.contactSubscribe = DbContext.data.select(ContactData::class).where(ContactData::isUserKey eq false).get().observableResult().subscribe {
            recycler_view.updateItemsSource(it.toList())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!contactSubscribe.isDisposed) {
            contactSubscribe.dispose()
        }
    }
}