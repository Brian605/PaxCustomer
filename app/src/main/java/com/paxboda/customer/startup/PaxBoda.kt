package com.paxboda.customer.startup

import android.app.Application
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.paxboda.customer.R
import com.paxboda.customer.constants.Db
import com.paxboda.customer.models.Token
import com.paxboda.customer.models.User
import com.paxboda.customer.utils.MapUtils

class PaxBoda:Application() {
    private lateinit var currentUser: User

    override fun onCreate() {
        super.onCreate()
        MapUtils.setUpGeoApiContext()
        setUpRemoteConfig()
        getCurrentUser()
    }
    private fun setUpRemoteConfig() {
        val config = Firebase.remoteConfig
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        config.setConfigSettingsAsync(settings)
        config.setDefaultsAsync(R.xml.remote_config_defaults)
        config.fetchAndActivate()
    }

    private fun getCurrentUser() {
        if (Firebase.auth.currentUser != null) {
            Firebase.firestore.collection(Db.USERS)
                .document(Firebase.auth.currentUser!!.uid)
                .get()
                .addOnSuccessListener {
                    if (it.exists()) {
                        currentUser = it.toObject(User::class.java)!!
                        setUpMessaging()
                    }
                }
        }
    }

    private fun setUpMessaging() {

            Firebase.messaging.token.addOnSuccessListener {
                val token= Token(currentUser.icons_id,it)
                Firebase.firestore.collection(Db.TOKENS)
                    .document(currentUser.icons_id.toString())
                    .set(token)
                    .addOnSuccessListener {

                    }
            }
        }


}