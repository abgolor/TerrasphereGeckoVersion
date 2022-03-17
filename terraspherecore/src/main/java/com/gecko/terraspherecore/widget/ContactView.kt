package com.gecko.terraspherecore.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.gridlayout.widget.GridLayout
import com.gecko.terraspherecore.R
import com.gecko.terraspherecore.common.extension.toFormattedHexText
import com.gecko.terraspherecore.common.prettyTime
import com.gecko.terraspherecore.data.ContactData
import kotlinx.android.synthetic.main.widget_contact.view.*

class ContactView: GridLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)
    init {
        LayoutInflater.from(context).inflate(R.layout.widget_contact, this)
    }

    var contact: ContactData? = null
    set(value) {
        field = value
        updateUI(value)
    }

    private fun updateUI(value: ContactData?) {
        widget_contact_name.text = value?.name
        widget_contact_email.text = value?.email
        widget_contact_fingerprint.text = value?.keyId?.toFormattedHexText()
        widget_contact_created_at.text = prettyTime.format(value?.keyData?.firstOrNull()?.createAt)
    }
}