package com.paxboda.customer.listeners

import com.google.android.gms.maps.model.LatLng

interface CompleteListener {
    fun onComplete(value:String){

    }
    fun onComplete(value: Int){

    }
    fun onComplete(value: Long){

    }

    fun onPoints(decodePoly: List<LatLng>, distance: String) {

    }
}