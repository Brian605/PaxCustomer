package com.paxboda.customer.utils

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.androidnetworking.interfaces.StringRequestListener
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.paxboda.customer.BuildConfig
import com.paxboda.customer.listeners.CompleteListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

class Commons {
    val format by lazy { SimpleDateFormat("dd/MM/yyyy @ HH:mm", Locale.getDefault()) }
    val dateFormat by lazy { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    fun generateEmail(phone:String):String{
        val numberString="abcdefghijklmnopqrstuvwxyz"
        var buffer=""
        for(number in phone){
            val num=number.digitToInt();
            buffer+=numberString[num]
        }
        return "$buffer@janpaxltd.com"
    }

    fun getDateDifference(date1:String):Boolean{
        val calendar=Calendar.getInstance()
        //val today=dateFormat.format("${calendar.get(Calendar.DATE)}/${calendar.get(Calendar.MONTH)+1}/${calendar.get(Calendar.YEAR)}")
        //val toSubstract=dateFormat.parse(date1)
        return true
    }
    fun generateIconsId(): Int {
        return (System.nanoTime() and 0xffff).toInt()
    }

    /*fun reverseGeoCode(context: Context, latLng: LatLng, listener: CompleteCallback) {
        val ai: ApplicationInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val value = ai.metaData["com.google.android.geo.API_KEY"]
        val apiKey = value.toString()

        val latlng = "${latLng.latitude},${latLng.longitude}"
        val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=${latlng}&key=${apiKey}"

        AndroidNetworking.post(url)
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject?){

                    val results = response?.optJSONArray("results")
                    val addressObject = results?.optJSONObject(0)
                    val location = addressObject?.optString("formatted_address")
                    if (location != null) {
                        listener.onComplete(location)
                    } else {
                        onError(null)
                    }
                }

                override fun onError(anError: ANError?) {
                    anError?.printStackTrace()
                    listener.onError()
                }
            })
    }
*/
    fun isValidName(name: String?): Boolean {
        return name!!.lowercase().matches(Regex("^[\\p{L} .'-]+$"))

    }

    /*private fun addLog(tLogs: TLogs){
        Firebase.firestore.collection(Db.LOGS)
            .add(tLogs)
    }*/


    fun validRegistration(reg:String):Boolean {
        val regex="^[K][M][A-Z]{2}[0-9]{3}[A-Z]$"
        val pattern= Pattern.compile(regex)
        val matcher=pattern.matcher(reg)
        return matcher.matches()
    }

    fun sendSms(numbers: String, s: String) {
        AndroidNetworking.post("https://janpaxapps.uc.r.appspot.com/sms")
            .addBodyParameter("message", s)
            .addBodyParameter("numbers", numbers)
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsString(object : StringRequestListener {
                override fun onError(anError: ANError?) {

                }

                override fun onResponse(response: String?) {

                }
            })
    }

    fun sanitizePhoneNumber(phone: String): String {
        if (phone.length < 11 && phone.startsWith("0")) {
            return phone.replaceFirst("^0".toRegex(), "254")
        }
        return if (phone.length == 13 && phone.startsWith("+")) {
            phone.replaceFirst("^+".toRegex(), "")
        } else phone
    }

   /* fun getTime(pickupLatLng: LatLng, destLatLng: LatLng, context: Context,completeCallback: CompleteCallback) {
        val ai: ApplicationInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        )
        val value = ai.metaData["com.google.android.geo.API_KEY"]
        val apiKey = value.toString()

        val origin="${pickupLatLng.latitude},${pickupLatLng.longitude}"
        val destination="${destLatLng.latitude},${destLatLng.longitude}"
        val mode="driving"
        val output="json"
        val url="https://maps.googleapis.com/maps/api/distancematrix/$output?origins=$origin&destinations=$destination&mode=$mode&key=$apiKey"
        AndroidNetworking.post(url)
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsJSONObject(object :JSONObjectRequestListener{
                override fun onResponse(response: JSONObject) {
                    val rows=response.getJSONArray("rows")
                    val elements=rows.getJSONObject(0).getJSONArray("elements")
                    val duration=elements.getJSONObject(0).getJSONObject("duration")
                        .getString("text")
                    completeCallback.onDuration(duration)

                 }

                override fun onError(anError: ANError?) {

                }
            })
    }

*/
    fun getRoutes(pickupLatLng: LatLng, destLatLng: LatLng,completeCallback: CompleteListener) {
        val apiKey = BuildConfig.GOOGLE_API_KEY

        val origin="${pickupLatLng.latitude},${pickupLatLng.longitude}"
        val destination="${destLatLng.latitude},${destLatLng.longitude}"
        val mode="driving"
        val output="json"
        val url="https://maps.googleapis.com/maps/api/directions/$output?origin=$origin&destination=$destination&sensor=false&mode=$mode&key=$apiKey"
   AndroidNetworking.post(url)
       .setPriority(Priority.IMMEDIATE)
       .build()
       .getAsJSONObject(object : JSONObjectRequestListener {
           override fun onResponse(response: JSONObject) {
             val routes=response.getJSONArray("routes")
val  distArray=routes.getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONObject("distance")
               val distance=distArray.getString("text")
               Log.e("distArray", response.toString())
               Log.e("distance",distance)

               val polyString=routes.getJSONObject(0).getJSONObject("overview_polyline")
                   .getString("points")
               completeCallback.onPoints(decodePoly(polyString),distance)
           }

           override fun onError(anError: ANError?) {

           }
       })
    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }


}

fun View.show(){
    visibility=View.VISIBLE
}

fun View.hide(){
    visibility=View.GONE
}
fun View.remove(){
    visibility=View.INVISIBLE
}

fun Context.toast(message:String){
    Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
}

fun Context.longToast(message:String){
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}

fun showSnackBar(target:View,message:String){
    val snackbar=Snackbar.make(target,message,Snackbar.LENGTH_LONG)
    val layoutParams=CoordinatorLayout.LayoutParams(snackbar.view.layoutParams)
    layoutParams.gravity=Gravity.TOP
    //snackbar.view.setPadding(0,10,0,0)
    snackbar.view.layoutParams=layoutParams
    snackbar.show()

}