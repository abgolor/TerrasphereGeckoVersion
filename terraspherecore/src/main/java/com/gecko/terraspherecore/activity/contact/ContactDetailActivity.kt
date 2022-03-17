package com.gecko.terraspherecore.activity.contact

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.gecko.terraspherecore.R
import com.gecko.terraspherecore.activity.BaseActivity
import com.gecko.terraspherecore.activity.message.ComposeActivity
import com.gecko.terraspherecore.common.adapter.AutoAdapter
import com.gecko.terraspherecore.common.adapter.updateItemsSource
import com.gecko.terraspherecore.common.extension.isExpired
import com.gecko.terraspherecore.common.extension.toActivity
import com.gecko.terraspherecore.common.extension.toFormattedHexText
import com.gecko.terraspherecore.common.extension.type
import com.gecko.terraspherecore.data.ContactData
import com.gecko.terraspherecore.data.DbContext
import io.reactivex.disposables.Disposable
import io.requery.kotlin.eq
import kotlinx.android.synthetic.main.activity_contact_detail.*

class ContactDetailActivity : BaseActivity() {

//    private val data: ContactData by lazy {
//        intent.getParcelableExtra<ContactData>("data")
//    }

    private lateinit var subscription: Disposable
    private lateinit var contactData: ContactData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_detail)
        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@ContactDetailActivity)
            adapter = AutoAdapter<Pair<String, String>>(android.R.layout.simple_list_item_2).apply {
                bindText(android.R.id.text1) {
                    it.first
                }
                bindText(android.R.id.text2) {
                    it.second
                }
            }
        }
        contactData = intent.getParcelableExtra<ContactData>("data")
        subscription = DbContext.data.select(ContactData::class).where(ContactData::dataId eq contactData.dataId).get().observableResult().subscribe {
            val result = it.firstOrNull()
            if (result == null) {
                finish()
            } else {
                updateData(result)
            }
        }
        send_message_button.setOnClickListener {
            toActivity<ComposeActivity>(Intent().apply {
                putExtra("mode", ComposeActivity.Mode.ToContact)
                putExtra("contact", contactData)
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item != null && item.itemId == R.id.menu_edit) {
            toActivity<EditContactActivity>(Intent().apply {
                putExtra("data", contactData)
            })
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.contact_detail_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }


    private fun updateData(data: ContactData) {
        contactData = data
        contact_name.text = data.name
        contact_desc.text = data.email
        recycler_view.updateItemsSource(listOf(
                getString(R.string.contact_detail_fingerprint) to (data.keyData.firstOrNull()?.fingerPrint?.toFormattedHexText() ?: ""),
                getString(R.string.contact_detail_validity) to if (data.isExpired) "Invalid" else "Valid",
                getString(R.string.contact_detail_type) to data.type,
                getString(R.string.contact_detail_create_at) to data.keyData.firstOrNull()?.createAt.toString(),
                getString(R.string.contact_detail_email) to data.email
        ))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!subscription.isDisposed) {
            subscription.dispose()
        }
    }


}
