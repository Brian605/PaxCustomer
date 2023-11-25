package com.paxboda.customer.models

import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.*

data class User(
    var user_id: String = "",
    @ServerTimestamp
    var createdAt: Date = Date(),
    var user_name: String = "",
    var user_phone: String = "",
    var station: String = "",
    var agent_id:Int=0,
    var lastLatitude: Double = 0.0,
    var lastLongitude: Double = 0.0,
    var lastLocation: String = "",
    var user_roles: List<String> = listOf(),
    var user_agents: List<Int> = listOf(),
    var user_referrals: List<Int> = listOf(),
    var user_metadata: Map<String,String> = mapOf(),
    var user_photo: String = "",
    var host_location: String = "",
    var user_id_number: String = "",
    var icons_id: Int = 0,
    var licence_photo: String = "",
    var id_photo_f: String = "",
    var id_photo_b: String = "",
    var number_plate: String = "",
    var status: String = "",
    var profile: String = "Incomplete",
    var emailAddress: String = "",
    var host_id: Int = 0,
    var promoter_id:Int=0,
    var user_county: String=""
) : Serializable