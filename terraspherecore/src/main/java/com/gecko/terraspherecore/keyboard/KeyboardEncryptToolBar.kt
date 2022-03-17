package com.gecko.terraspherecore.keyboard

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.gecko.terraspherecore.R
import com.gecko.terraspherecore.activity.IndexActivity
import com.gecko.terraspherecore.common.EncodeDecode
import com.gecko.terraspherecore.common.adapter.AutoAdapter
import com.gecko.terraspherecore.common.adapter.getItemsSource
import com.gecko.terraspherecore.common.collection.CollectionChangedEventArg
import com.gecko.terraspherecore.common.extension.toast
import com.gecko.terraspherecore.data.ContactData
import com.gecko.terraspherecore.data.DbContext
import com.gecko.terraspherecore.data.UserKeyData
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.widget_keyboard_encrypt_toolbar.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.tlaster.kotlinpgp.KotlinPGP
import moe.tlaster.kotlinpgp.data.EncryptParameter
import moe.tlaster.kotlinpgp.data.PublicKeyData


interface ToolbarActionsListener {
    fun getCurrentPage(): Int
    fun toPage(index: Int)
    suspend fun requestInterpret()
    suspend fun requestEncrypt(content: String, pubKeys: List<ContactData>): String
}

class KeyboardEncryptToolBar : RelativeLayout, KeyboardExtendView.Listener, Observer<CollectionChangedEventArg> {
    override fun getCurrentItems(): List<ContactData> {
        return encrypt_toolbar_list.getItemsSource() ?: emptyList()
    }

    override fun onItemSelected(data: ContactData) {
        encrypt_toolbar_list.getItemsSource<ContactData>()?.add(data)
    }

    private lateinit var listener: Listener
    private lateinit var toolbarActionsListener: ToolbarActionsListener
    private lateinit var userKeySubscription: Disposable
    private lateinit var contactSubscribe: Disposable

    interface Listener {
        fun getSelection(): String
        fun overrideSelection(text: String)
        fun requireAllText(): String
        fun overrideAllText(text: String)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_keyboard_encrypt_toolbar, this)
        encrypt_toolbar_interpret.setOnClickListener {
            if (toolbarActionsListener.getCurrentPage() != 0) {
                toolbarActionsListener.toPage(0)
            }
            GlobalScope.launch {
                toolbarActionsListener.requestInterpret()
            }
        }
        encrypt_toolbar_encrypt.setOnClickListener {
            if (toolbarActionsListener.getCurrentPage() != 1) {
                toolbarActionsListener.toPage(1)
            } else {
                kotlin.runCatching {
                    val selection = listener.getSelection()
                    val text = if (selection.isEmpty()) {
                        listener.requireAllText()
                    } else {
                        selection
                    }
                    val contacts = encrypt_toolbar_list.getItemsSource<ContactData>()
                    if (contacts != null && contacts.any()) {
                        val pubKeys = contacts.toList()//.map { it.pubKeyContent }

                        clearSelection()
                        GlobalScope.launch {
                            val result = toolbarActionsListener.requestEncrypt(text, pubKeys)
                            if (selection.isEmpty()) {
                                listener.overrideAllText(result)
                            } else {
                                listener.overrideSelection(result)
                            }
                        }
                    } else {
                        context.toast(context.getString(R.string.error_compose_no_receiver))
                    }
//                    val dec = Base64.decode(text, Base64.DEFAULT)?.toString(Charset.defaultCharset())
//                    if (dec != null) {
//                        listener.overrideAllText(dec)
//                    }
                }.onFailure {
                    //TODO:
                }
            }
        }
        encrypt_toolbar_list.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = AutoAdapter<ContactData>(R.layout.item_keyboard_toolbar_contract).apply {
                bindText(android.R.id.text1) {
                    it.name
                }
                items.collectionChanged.observeForever(this@KeyboardEncryptToolBar)
            }
        }
    }

    //Get the contact the user has selected and override the current text the user has selected
    //With the name of the contact. ALso remove any name before the # sign.
    fun userSelectedContact(contactInfo: ContactData){
        var typedText : String = listener.requireAllText()
        //Removing the current typed contact name and replacing with the selected contact
        typedText = typedText.substring(0, typedText.indexOf("#"))
        //Changing the name of the contact to the selected contact
        var result = encryptMessage(contactInfo, typedText)
        val encodeDecode: EncodeDecode = EncodeDecode()
        result = "https://tessersphere.encryption/?get=" + encodeDecode.encodeString(result)
        listener.overrideAllText(result)
    }

    fun encryptMessage(contactInfo: ContactData, message: String) : String{
        val publicDataList : MutableList<ContactData> = mutableListOf()
        publicDataList.add(contactInfo)
        val result = KotlinPGP.encrypt(EncryptParameter(
                message = message,
                publicKey = publicDataList.map {
                    PublicKeyData(
                            key = it.pubKeyContent
                    )
                }
        ))
        return result
    }


    fun setToolbarActionsListener(listener: ToolbarActionsListener) {
        this.toolbarActionsListener = listener
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun sharePubKey() {
        var contactInfo: ContactData? = getUserContactData()

        if (contactInfo != null) {
            listener.overrideAllText(contactInfo.pubKeyContent ?: "")
        } else {
            val intent = Intent(context, IndexActivity::class.java)
            intent.putExtra("isNoKeyCreated", "true")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

        private fun getUserContactData(): ContactData {
            lateinit var contactInfo: ContactData
            userKeySubscription = DbContext.data.select(UserKeyData::class).get().observableResult().subscribe {

                val result = it.toList()
                Log.i("MAIN", result.size.toString())

                if (result.size > 0) {
                    contactInfo = result.get(0).contactData!!
                }
            }
            return contactInfo
        }

    private fun toggle() {
//        val parentView = parent
//        if (parentView is ViewGroup) {
//            TransitionManager.beginDelayedTransition(parentView)
//        }
        visibility = if (visibility == GONE) {
            VISIBLE
        } else {
            GONE
        }
    }

    fun onClose() {
        clearSelection()
    }

    fun clearSelection() {
        encrypt_toolbar_list.getItemsSource<ContactData>()?.clear()
    }

    override fun onChanged(t: CollectionChangedEventArg) {
        val itemCount = encrypt_toolbar_list.getItemsSource<ContactData>()?.size
        if (itemCount != null && itemCount > 0) {
            if (!encrypt_toolbar_list.isVisible) {
                TransitionManager.beginDelayedTransition(action_container)
                encrypt_toolbar_list.isVisible = true
                encrypt_toolbar_interpret.hideText()
                encrypt_toolbar_encrypt.hideText()
            }
        } else {
            if (encrypt_toolbar_list.isVisible) {
                TransitionManager.beginDelayedTransition(action_container)
                encrypt_toolbar_list.isVisible = false
                encrypt_toolbar_interpret.showText()
                encrypt_toolbar_encrypt.showText()
            }
        }
    }
}