package com.paxboda.customer.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DistanceMatrixApi
import com.google.maps.GeoApiContext
import com.google.maps.GeocodingApi
import com.google.maps.model.TravelMode
import com.paxboda.customer.BuildConfig
import com.paxboda.customer.R
import com.paxboda.customer.listeners.CompleteListener
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt

object MapUtils {
    lateinit var geoApiContext: GeoApiContext

    fun setUpGeoApiContext() {
        geoApiContext =GeoApiContext.Builder()
            .apiKey(BuildConfig.GOOGLE_API_KEY)
            .build()
    }
    fun fetchDistance(listener: CompleteListener, from: LatLng, to: LatLng){
        val origins= arrayOf("${from.latitude},${from.longitude}")
        val destinations= arrayOf("${to.latitude},${to.longitude}")
        val apiRequest= DistanceMatrixApi.getDistanceMatrix(geoApiContext,origins,destinations)
        apiRequest.mode(TravelMode.DRIVING)
        val matrix= apiRequest.await()
        var distanceString= matrix.rows.first().elements.first().distance.toString()
        distanceString= distanceString.replace("km","")
        distanceString=distanceString.replace(",","")
        val distance=distanceString.toFloat().roundToInt()
        listener.onComplete(distance)


    }

    fun fetchTime(listener: CompleteListener, from: LatLng, to: LatLng){
        val origins= arrayOf("${from.latitude},${from.longitude}")
        val destinations= arrayOf("${to.latitude},${to.longitude}")
        val apiRequest=DistanceMatrixApi.getDistanceMatrix(geoApiContext,origins,destinations)
        apiRequest.mode(TravelMode.DRIVING)
        val matrix= apiRequest.await()
        val time=matrix.rows.first().elements.first().duration.inSeconds
        listener.onComplete(time)
    }

    fun reverseGeocode(listener: CompleteListener,from: LatLng){
     val result= GeocodingApi.reverseGeocode(geoApiContext,com.google.maps.model.LatLng(from.latitude,from.longitude)).await()
        listener.onComplete(result[0].formattedAddress)

    }

    fun getOriginMarkerBitmap(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.pickup)
    }
    fun getDestinationMarkerBitmap(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.destination)
    }
    fun getMyMarkerBitmap(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.my_location)
    }

    fun getCarBitmap(context: Context): Bitmap {
        return BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
       // return Bitmap.createScaledBitmap(bitmap, 50, 100, false)
    }

    fun polylineAnimator(): ValueAnimator {
        val valueAnimator = ValueAnimator.ofInt(0, 100)
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.duration = 4000
        return valueAnimator
    }

    fun getRotation(start: LatLng, end: LatLng): Float {
        val latDifference: Double = abs(start.latitude - end.latitude)
        val lngDifference: Double = abs(start.longitude - end.longitude)
        var rotation = -1F
        when {
            start.latitude < end.latitude && start.longitude < end.longitude -> {
                rotation = Math.toDegrees(atan(lngDifference / latDifference)).toFloat()
            }
            start.latitude >= end.latitude && start.longitude < end.longitude -> {
                rotation = (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 90).toFloat()
            }
            start.latitude >= end.latitude && start.longitude >= end.longitude -> {
                rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 180).toFloat()
            }
            start.latitude < end.latitude && start.longitude >= end.longitude -> {
                rotation =
                    (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 270).toFloat()
            }
        }
        return rotation
    }

    fun carAnimator(): ValueAnimator {
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 3000
        valueAnimator.interpolator = LinearInterpolator()
        return valueAnimator
    }
}