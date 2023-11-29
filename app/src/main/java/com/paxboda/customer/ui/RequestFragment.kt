package com.paxboda.customer.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.StringRequestListener
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.jianastrero.capiche.iNeed
import com.paxboda.customer.BuildConfig
import com.paxboda.customer.R
import com.paxboda.customer.adapters.AutoCompleteAdapter
import com.paxboda.customer.auth.AuthActivity
import com.paxboda.customer.constants.Constants
import com.paxboda.customer.constants.Db
import com.paxboda.customer.databinding.FragmentRequestBinding
import com.paxboda.customer.listeners.CompleteListener
import com.paxboda.customer.models.Journey
import com.paxboda.customer.models.LocationObj
import com.paxboda.customer.models.MyLocation
import com.paxboda.customer.models.Reason
import com.paxboda.customer.models.User
import com.paxboda.customer.service.MyLocationService
import com.paxboda.customer.utils.Commons
import com.paxboda.customer.utils.MapUtils
import com.paxboda.customer.utils.hide
import com.paxboda.customer.utils.show
import com.paxboda.customer.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.round
import kotlin.math.roundToInt


class RequestFragment : Fragment(), OnMapReadyCallback, OnMapsSdkInitializedCallback,
    LifecycleOwner {
    private var tripDistance: String? = null
    private lateinit var googleMap: GoogleMap
    private lateinit var defaultLocation: LatLng
    private var grayPolyline: Polyline? = null
    private var bluePolyline: Polyline? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var movingCabMarker: Marker? = null
    private var previousLatLng: LatLng? = null
    private var currentLatLng: LatLng? = null
    private var pickupLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private lateinit var mapView: MapView
    private lateinit var binding: FragmentRequestBinding
    private lateinit var currentUser: User
    private var flag = 0
    private var myLocationMarker: Marker? = null
    private var nearbyPhones: MutableList<String> = mutableListOf()
    private var nearbyMarkers: ArrayList<Marker> = arrayListOf()
    private lateinit var bounds: LatLngBounds.Builder
    private lateinit var pickupAdapter: AutoCompleteAdapter
    private lateinit var destinationAdapter: AutoCompleteAdapter
    private lateinit var placesClient: PlacesClient
    private var trip = Journey()
    private var fromAddress = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRequestBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        bounds = LatLngBounds.builder()
        if (!Places.isInitialized()) {
            Places.initialize(requireActivity().applicationContext, BuildConfig.GOOGLE_API_KEY)
        }

        placesClient = Places.createClient(requireActivity())

        binding.pickupValue.threshold = 1
        binding.destinationValue.threshold = 1
        pickupAdapter = AutoCompleteAdapter(requireContext(), placesClient)
        destinationAdapter = AutoCompleteAdapter(requireContext(), placesClient)
        setUpMap(savedInstanceState)
        binding.summaryLayout.hide()
        binding.postingLayout.hide()
        binding.post.setOnClickListener {
            if (nearbyPhones.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No riders nearby")
                    .setMessage("There are no riders nearby to handle your request.Kindly try again later")
                    .setPositiveButton("Okay") { dialog, _ ->
                        dialog?.dismiss()
                    }.create()
                    .show()
                return@setOnClickListener
            }
            binding.summaryLayout.hide()
            binding.postingLayout.show()
            val ref = Firebase.firestore.collection(Db.TRIPS)
                .document(trip.journeyId.toString())
            ref.set(trip)
                .addOnSuccessListener {
                    binding.waitingMessage.text = "Posted!! Calling riders..."
                    binding.cancel.show()
                    sendNotification()
                    listenForChanges(ref)
                }
        }
        binding.cancel.setOnClickListener {
        val reasons=resources.getStringArray(R.array.cancellation_reasons)
            MaterialAlertDialogBuilder(requireContext())
                .setBackground(ContextCompat.getDrawable(requireContext(),R.drawable.bg_white_rounded_10))
                .setSingleChoiceItems(reasons,0){
                    dialog,which->
                    dialog?.dismiss()
                    binding.progress.show()
                    val reason=Reason(currentUser.user_id,trip.journeyId.toString(),reasons[which])
                    Firebase.firestore.collection(Db.TRIPS)
                        .document(trip.journeyId.toString())
                        .update("status",Constants.CANCELLED)
                        .addOnSuccessListener {
                             if (isAdded){
                                 binding.progress.hide()
                                 requireActivity().toast("Request cancelled")
                                 requireActivity().recreate()
                            }
                        }
                    Firebase.firestore.collection(Db.REASONS)
                        .add(reason)
                        .addOnSuccessListener {
                            binding.progress.hide()
                        }
                }
                .setTitle("Please provide a reason for cancelling the request")
                .create()
                .show()
        }
        return binding.root
    }

    private fun listenForChanges(it: DocumentReference?) {
        it?.addSnapshotListener(requireActivity()) { value, error ->
            if (error != null || value == null || !value.exists()) {
                return@addSnapshotListener
            }
            val currentTrip = value.toObject(Journey::class.java)
            if (currentTrip!!.status == Constants.ACCEPTED) {
                binding.waitingMessage.text = "Request accepted!!. The rider is on his way"
            } else
                if (currentTrip.status == Constants.RUNNING) {
                    binding.waitingMessage.text = "Trip started"
                    binding.cancel.hide()
                } else
                    if (currentTrip.status == Constants.ENDED) {
                        requireActivity().toast("Trip completed. Thank you for choosing pax boda")
                        binding.postingLayout.hide()
                    }
            else
            if (currentTrip.status==Constants.CANCELLED){
                if (isAdded){
                    requireActivity().recreate()
                }
            }
        }
    }

    private fun sendNotification() {
        AndroidNetworking.get("https://paxgeofire-dot-janpaxapps.uc.r.appspot.com/notify/${trip.fromName}/${pickupLatLng!!.latitude}/${pickupLatLng!!.longitude}/${trip.journeyId}")
            .setPriority(Priority.IMMEDIATE)
            .build()
            .getAsString(object : StringRequestListener {
                override fun onResponse(response: String?) {

                }

                override fun onError(anError: ANError?) {

                }

            })
    }

    private fun setUpMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.onResume()
        // needed to get the map to display immediately
        try {
            MapsInitializer.initialize(
                requireActivity().applicationContext,
                MapsInitializer.Renderer.LATEST,
                this
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mapView.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap) {
        this.googleMap = p0
        defaultLocation = LatLng(0.0236, 37.9062)
        showDefaultLocationOnMap(defaultLocation)
        getCurrentUser()
    }

    private fun getCurrentUser() {
        Firebase.firestore.collection(Db.USERS)
            .document(Firebase.auth.currentUser!!.uid)
            .get()
            .addOnSuccessListener {
                if (it.exists()) {
                    currentUser = it.toObject(User::class.java)!!
                    getLastTrip()

                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setBackground(
                            ContextCompat.getDrawable(
                                requireContext(),
                                R.drawable.bg_white_rounded_10
                            )
                        )
                        .setTitle("Invalid User")
                        .setMessage("Please Log Out and Log In again")
                        .setPositiveButton("Ok") { dialog, _ ->
                            dialog?.dismiss()
                            Firebase.auth.signOut()
                            startActivity(Intent(requireContext(), AuthActivity::class.java))
                        }.create()
                        .show()
                }
            }
    }

    private fun getLastTrip() {
        Firebase.firestore.collection(Db.TRIPS)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(1)
            .get(Source.SERVER)
            .addOnSuccessListener {
                if (it.isEmpty) {
                    getCurrentLocation()
                } else {
                    val trip = it.first().toObject(Journey::class.java)
                    if (trip.status == Constants.PENDING || trip.status == Constants.ACCEPTED || trip.status == Constants.RUNNING) {
                        setUpTrip(trip,it.first().reference)
                    } else {
                        getCurrentLocation()
                    }
                }
            }
    }

    private fun setUpTrip(thisTrip:Journey,reference: DocumentReference) {
 trip=thisTrip
        binding.pickupValue.setText(trip.fromName)
        binding.destinationValue.setText(trip.toName)
        pickupLatLng=LatLng(trip.fromLatLng.latitude,trip.fromLatLng.longitude)
        destinationLatLng=LatLng(trip.toLatLng.latitude,trip.toLatLng.longitude)
        originMarker=addOriginMarkerAndGet(pickupLatLng!!)
        destinationMarker=addDestinationMarkerAndGet(destinationLatLng!!)
        drawRoute()
        if (trip.status==Constants.PENDING){
            binding.cancel.show()
            binding.waitingMessage.text="Posted.Calling Riders..."
            sendNotification()
        }else
        if (trip.status==Constants.RUNNING){
            binding.cancel.hide()
            binding.waitingMessage.text="Trip in progress..."
            listenForRider(trip.journeyId)

        }else
            if (trip.status==Constants.ACCEPTED){
                binding.cancel.show()
                binding.waitingMessage.text="Rider is on the way..."
                listenForRider(trip.journeyId)

            }
        else
            if(trip.status==Constants.ENDED){
                if (isAdded){
                    requireActivity().toast("You have reached your destination")
                    requireActivity().recreate()
                }
            }
        binding.postingLayout.show()
        listenForChanges(reference)
    }

    private fun listenForRider(journeyId:Int) {

    }

    private fun getCurrentLocation() {
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        requireActivity().iNeed(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
            onGranted = {
                checkGpsEnabled()
            },
            onDenied = {
                MaterialAlertDialogBuilder(requireContext())
                    .setBackground(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.bg_white_rounded_10
                        )
                    )
                    .setTitle("Please allow location permissions")
                    .setMessage("We need your location permission in order to get you the nearest rider.")
                    .setPositiveButton("Allow in settings") { dialog, _ ->
                        dialog?.dismiss()
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", requireContext().packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }.setNegativeButton("Cancel") { dialog, _ ->
                        dialog?.dismiss()
                    }.create()
                    .show()

            })
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (flag == 1) {
                flag = 0
                checkGpsEnabled()
            }
        }

    private fun checkGpsEnabled() {
        val manager: LocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !manager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            ) && !manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) && !manager.isProviderEnabled(
                LocationManager.FUSED_PROVIDER
            )
        ) {
            flag = 1
            resultLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            Toast.makeText(requireContext(), "Please turn on GPS", Toast.LENGTH_LONG).show()
        } else {
            startLocationService()
        }

    }

    private fun startLocationService() {
        val intent = Intent(requireContext(), MyLocationService::class.java)
        requireActivity().startService(intent)
    }

    private fun getNearbyRiders() {
        val myLocation = Location("A")
        myLocation.longitude = defaultLocation.longitude
        myLocation.latitude = defaultLocation.latitude
        val riderLocation = Location("B")
        lifecycleScope.launch(Dispatchers.IO) {
            val ridersRef = Firebase.database.getReference("Riders")
            val geoFire = GeoFire(ridersRef)
            val geoQuery = geoFire.queryAtLocation(
                GeoLocation(defaultLocation.latitude, defaultLocation.longitude),
                1.0
            )

            geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
                override fun onKeyEntered(key: String?, location: GeoLocation?) {
                    if (nearbyMarkers.size >= 5) {
                        geoQuery.removeAllListeners()
                        return
                    }

                    if (key != null && location != null) {
                        riderLocation.latitude = location.latitude
                        riderLocation.longitude = location.longitude
                        checkAndAddMarker(key, riderLocation)
                    }
                }

                override fun onKeyExited(key: String?) {

                }

                override fun onKeyMoved(key: String?, location: GeoLocation?) {

                }


                override fun onGeoQueryError(error: DatabaseError?) {

                }

                override fun onGeoQueryReady() {
                    geoQuery.removeAllListeners()

                }
            })
        }
    }

    private fun checkAndAddMarker(key: String, riderLocation: Location) {
        Firebase.firestore.collection("Users")
            .document(key)
            .get()
            .addOnSuccessListener {
                if (!it.exists()) {
                    return@addOnSuccessListener
                }
                val rider = it.toObject(User::class.java)!!
                if (rider.status == "Active") {
                    addMarker(key, riderLocation)
                    nearbyPhones.add(rider.user_phone)
                }
            }
    }

    private fun addMarker(key: String, riderLocation: Location) {
        requireActivity().runOnUiThread {
            val rotation = MapUtils.getRotation(
                pickupLatLng!!,
                LatLng(riderLocation.latitude, riderLocation.longitude)
            )
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(riderLocation.latitude, riderLocation.longitude))
                    .rotation(rotation)
                    .draggable(false)
                    .visible(true)
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitmap(requireContext()))
                    ).title(key)


            )
            if (marker != null) {
                nearbyMarkers.add(marker)
            }
            bounds = LatLngBounds.builder()
            bounds.include(defaultLocation)
            nearbyMarkers.forEach {
                bounds.include(it.position)
            }

            val height = resources.displayMetrics.heightPixels.times(0.8).roundToInt()
            val width = resources.displayMetrics.widthPixels
            val padding = width.times(0.30).roundToInt()
            val update = CameraUpdateFactory.newLatLngBounds(bounds.build(), width, height, padding)
            googleMap.animateCamera(update)
        }


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun myLocationReady(myLocation: MyLocation) {
        defaultLocation = LatLng(myLocation.latitude, myLocation.longitude)
        shoMyLocation(defaultLocation)
        getNearbyRiders()
        setUpPickupAndDestinationAdapters()
        pickupLatLng = defaultLocation
        originMarker = addOriginMarkerAndGet(defaultLocation)
        MapUtils.reverseGeocode(object : CompleteListener {
            override fun onComplete(value: String) {
                trip.fromName = value
                binding.pickupValue.setText(value)
                fromAddress = value
                Log.e("Reversed", value)
            }
        }, pickupLatLng!!)

    }

    private fun setUpPickupAndDestinationAdapters() {
        binding.pickupValue.setOnItemClickListener { _, _, position, _ ->
            try {
                binding.progress.show()
                val item = pickupAdapter.getItem(position)
                val placeID: String = item.placeId

//                To specify which data types to return, pass an array of Place.Fields in your FetchPlaceRequest
//                Use only those fields which are required.
                val placeFields = listOf(
                    Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG
                )
                val request: FetchPlaceRequest? = FetchPlaceRequest.builder(placeID, placeFields)
                    .build()
                if (request != null) {
                    placesClient.fetchPlace(request).addOnSuccessListener { task ->
                        binding.progress.hide()

                        pickupLatLng =
                            LatLng(task.place.latLng!!.latitude, task.place.latLng!!.longitude)
                        trip.fromName = task.place.name!!
                        fromAddress = task.place.address!!
                        originMarker = addOriginMarkerAndGet(pickupLatLng!!)
                        showApproximateSummary()
                        drawRoute()
                    }
                        .addOnFailureListener { e ->
                            binding.progress.hide()
                            e.printStackTrace()
                            requireActivity().toast("Failed to retrieve location")
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.destinationValue.setOnItemClickListener { _, _, position, _ ->
            try {
                binding.progress.show()
                val item = destinationAdapter.getItem(position)
                val placeID: String = item.placeId

//                To specify which data types to return, pass an array of Place.Fields in your FetchPlaceRequest
//                Use only those fields which are required.
                val placeFields = listOf(
                    Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG
                )
                val request: FetchPlaceRequest? = FetchPlaceRequest.builder(placeID, placeFields)
                    .build()

                request?.let {
                    placesClient.fetchPlace(it).addOnSuccessListener { task ->
                        binding.progress.hide()
                        destinationLatLng =
                            LatLng(task.place.latLng!!.latitude, task.place.latLng!!.longitude)
                        trip.toName = task.place.name!!
                        showApproximateSummary()
                        drawRoute()

                    }
                        .addOnFailureListener { e ->
                            binding.progress.hide()
                            e.printStackTrace()
                            requireActivity().toast("Failed to retrieve location")
                        }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.pickupValue.setAdapter(pickupAdapter)
        binding.destinationValue.setAdapter(destinationAdapter)


    }

    private fun drawRoute() {
        if (originMarker == null || destinationMarker == null) {
            return
        }
        Commons().getRoutes(pickupLatLng!!, destinationLatLng!!, object : CompleteListener {
            override fun onPoints(decodePoly: List<LatLng>, distance: String) {
                tripDistance = distance
                showApproximateSummary()
                grayPolyline?.remove()
                val poly = PolylineOptions()
                poly.color(
                    ContextCompat.getColor(
                        requireContext(),
                        com.google.android.libraries.places.R.color.quantum_grey500
                    )
                )
                poly.addAll(decodePoly)
                grayPolyline = googleMap.addPolyline(poly)
                animateToBounds()
            }
        })
    }

    private fun showApproximateSummary() {
        if (pickupLatLng == null || destinationLatLng == null) {
            return
        }
        val distance = getDistance()
        var cost: Int
        val nai = resources.getStringArray(R.array.nairobi_areas)
        if (fromAddress.contains("Nairobi", true) || nai.any {
                it.contains(fromAddress, true) || fromAddress.contains(it, true)
            }) {
            cost = roundToNearest10(getNairobiPricing(distance))
        } else {
            val amount = Firebase.remoteConfig.getLong("DEFAULT_AMOUNT").toString().toInt()
            val percentage = Firebase.remoteConfig.getLong("DEFAULT_PERCENTAGE").toString().toInt()
            val minimumDistance = Firebase.remoteConfig.getDouble("MINIMUM_DISTANCE")
            if (getCurrentTime() in 6..19) {
                if (distance <= minimumDistance) {
                    cost = amount
                } else {
                    cost =
                        (((distance.minus(minimumDistance)).times(percentage)).toInt()).plus(amount)
                    cost = (round((cost + 5) / 10.0) * 10).roundToInt()
                }
            } else {
                val nightamount = Firebase.remoteConfig.getLong("NIGHT_AMOUNT").toString().toInt()

                if (distance <= minimumDistance) {
                    cost = nightamount
                } else {
                    cost = (((distance.minus(minimumDistance)).times(percentage)).toInt()).plus(
                        nightamount
                    )
                    cost = (round((cost + 5) / 10.0) * 10).roundToInt()
                }
            }
        }
        trip.cost = cost
        binding.costView.text = "Ksh.$cost"
        trip.icons_id = currentUser.icons_id
        trip.fromLatLng = LocationObj(pickupLatLng!!.latitude, pickupLatLng!!.longitude)
        trip.toLatLng = LocationObj(destinationLatLng!!.latitude, destinationLatLng!!.longitude)
        trip.userName = currentUser.user_name
        trip.userPhone = currentUser.user_phone
        trip.journeyId = (System.nanoTime() and 0xfffff).toInt()
        trip.startTime = System.currentTimeMillis()
        binding.summaryLayout.show()

    }

    private fun getCurrentTime(): Int {
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone("Africa/Nairobi")
        return calendar.get(Calendar.HOUR_OF_DAY)
    }

    private fun getDistance(): Float {
        if (tripDistance != null) {
            if (tripDistance!!.contains("km", true)) {

                return tripDistance!!.replace("km", "", true)
                    .trim().toFloat()
            } else
                if (tripDistance!!.contains("m", true)) {

                    return tripDistance!!.replace("m", "", true)
                        .trim().toFloat()
                }
        }
        val fromLocation = Location("LocationA")
        fromLocation.latitude = pickupLatLng!!.latitude
        fromLocation.longitude = pickupLatLng!!.longitude

        val toLocation = Location("LocationB")
        toLocation.latitude = destinationLatLng!!.latitude
        toLocation.longitude = destinationLatLng!!.longitude

        val distance = fromLocation.distanceTo(toLocation)

        val format = DecimalFormat("#.##")
        format.roundingMode = RoundingMode.CEILING

        return format.format(distance.div(1000)).toFloat()

    }

    private fun roundToNearest10(int: Int): Int {
        if (int.toString().endsWith("0")) {
            return int
        }
        return (round((int - 5) / 10.0) * 10).roundToInt()
    }

    private fun getNairobiPricing(distance: Float): Int {
        val below05 = Firebase.remoteConfig.getLong("NAIROBI_BELOW_05").toString().toInt()
        val baseToNine = Firebase.remoteConfig.getLong("DEFAULT_AMOUNT_NAIROBI").toString().toInt()
        if (distance <= 0.5) {
            return below05
        } else
            if (distance > 0.5 && distance <= 1) {
                return ((distance.minus(0.5)).times(90)).plus(60).roundToInt()
            } else
                if (distance > 1 && distance < 2) {
                    val base = distance.minus(1).times(18)
                    return base.plus(baseToNine).roundToInt()
                } else
                    if (distance > 1 && distance < 3) {
                        val base = distance.minus(1).times(38)
                        return base.plus(baseToNine).roundToInt()
                    } else
                        if (distance > 1 && distance <= 10) {
                            val base = distance.minus(1).times(18)
                            return base.plus(baseToNine).roundToInt()
                        } else {
                            val base = 400
                            val price = distance.minus(10).times(10)
                            return base.plus(price).roundToInt()
                        }

    }

    private fun animateToBounds() {
        bounds = LatLngBounds.builder()
        bounds.include(pickupLatLng!!)
        bounds.include(destinationLatLng!!)
        val height = resources.displayMetrics.heightPixels.times(0.8).roundToInt()
        val width = resources.displayMetrics.widthPixels
        val padding = width.times(0.30).roundToInt()
        val update = CameraUpdateFactory.newLatLngBounds(bounds.build(), width, height, padding)
        googleMap.animateCamera(update)
    }


    private fun moveCamera(latLng: LatLng) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng) {
        val cameraPosition = CameraPosition.Builder().target(latLng).zoom(15.5f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun showDefaultLocationOnMap(latLng: LatLng) {
        moveCamera(latLng)
        animateCamera(latLng)
    }

    private fun shoMyLocation(latLng: LatLng) {
        myLocationMarker = googleMap.addMarker(
            MarkerOptions().icon(
                BitmapDescriptorFactory.fromBitmap(
                    MapUtils.getMyMarkerBitmap(
                        requireContext()
                    )
                )
            )
                .position(latLng)
                .title("Your Position")
        )
        animateCamera(latLng)
    }

    private fun showPath(latLngList: ArrayList<LatLng>) {
        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        // this is used to set the bound of the Map
        val bounds = builder.build()
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2))

        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(5f)
        polylineOptions.addAll(latLngList)
        grayPolyline = googleMap.addPolyline(polylineOptions)

        val blackPolylineOptions = PolylineOptions()
        blackPolylineOptions.color(Color.BLUE)
        blackPolylineOptions.width(5f)
        bluePolyline = googleMap.addPolyline(blackPolylineOptions)

        originMarker = addOriginMarkerAndGet(latLngList[0])
        originMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker = addDestinationMarkerAndGet(latLngList[latLngList.size - 1])
        destinationMarker?.setAnchor(0.5f, 0.5f)

        val polylineAnimator = MapUtils.polylineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (grayPolyline?.points!!.size) * (percentValue / 100.0f).toInt()
            bluePolyline?.points = grayPolyline?.points!!.subList(0, index)
        }
        polylineAnimator.start()

    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker? {
        val bitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitmap(requireContext()))
        return googleMap.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }


    private fun updateCarLocation(latLng: LatLng) {
        if (movingCabMarker == null) {
            movingCabMarker = addCarMarkerAndGet(latLng)
        }
        if (previousLatLng == null) {
            currentLatLng = latLng
            previousLatLng = currentLatLng
            movingCabMarker?.position = currentLatLng as LatLng
            movingCabMarker?.setAnchor(0.5f, 0.5f)
            animateCamera(currentLatLng!!)
        } else {
            previousLatLng = currentLatLng
            currentLatLng = latLng
            val valueAnimator = MapUtils.carAnimator()
            valueAnimator.addUpdateListener { va ->
                if (currentLatLng != null && previousLatLng != null) {
                    val multiplier = va.animatedFraction
                    val nextLocation = LatLng(
                        multiplier * currentLatLng!!.latitude + (1 - multiplier) * previousLatLng!!.latitude,
                        multiplier * currentLatLng!!.longitude + (1 - multiplier) * previousLatLng!!.longitude
                    )
                    movingCabMarker?.position = nextLocation
                    movingCabMarker?.setAnchor(0.5f, 0.5f)
                    animateCamera(nextLocation)
                }
                valueAnimator.start()
            }
        }
    }

    private fun addOriginMarkerAndGet(latLng: LatLng): Marker? {
        originMarker?.remove()
        val bitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(MapUtils.getOriginMarkerBitmap(requireContext()))
        return googleMap.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    private fun addDestinationMarkerAndGet(latLng: LatLng): Marker? {
        destinationMarker?.remove()
        val bitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(
                MapUtils.getDestinationMarkerBitmap(
                    requireContext()
                )
            )
        return googleMap.addMarker(
            MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor)
        )
    }

    override fun onMapsSdkInitialized(p0: MapsInitializer.Renderer) {

    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (this::currentUser.isInitialized){
            getLastTrip()
        }else{
            getCurrentUser()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}

