package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

private const val TAG = "SELECTLOCATIONFRAGMENT"

/** Denotes a request to turn on the location service */
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

/** Denotes a request to access the users location */
private const val REQUEST_LOCATION_PERMISSION = 1

/**
 * Allows the user to choose a POI on a Google Map and return the information to the reminder
 * view model. The logic for retrieving permissions is based off the example code found in the
 * Geo Fences sample app at https://github.com/udacity/android-kotlin-geo-fences.git
 */
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel<SaveReminderViewModel>()
    private lateinit var binding: FragmentSelectLocationBinding

    /** Display why location permissions are required and provide quick access to the
     * app's settings page */
    private lateinit var settingsSnackbar: Snackbar

    /** Denotes if we are currently waiting for the users location to be provided */
    var requestingLocationUpdates = false

    /** The POI the user selected on the map */
    private lateinit var reminderPoi: PointOfInterest
    private lateinit var map: GoogleMap

    /** When the user's location has been determined focus on it on the map */
    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            updateMapUserLocation(locationResult.locations.lastOrNull())
        }
    }

    companion object {
        /** Identifies a request to sign in */
        const val LOCATION_SETTINGS_REQUEST_CODE = 102
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Display why we need location permissions and provide a button to take the
        // uer to the app's settings page
        settingsSnackbar = Snackbar.make(
            binding.selectLocationConstraint,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.settings) {
            // https://stackoverflow.com/questions/7910840/android-startactivityforresult-immediately-triggering-onactivityresult
            startActivityForResult(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            }, LOCATION_SETTINGS_REQUEST_CODE)
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used. This is
        // in part based off the logic found at:
        // https://stackoverflow.com/questions/63931552/how-to-use-google-maps-android-sdk-in-a-fragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Get a location provider client to obtain the users current location. This is used to
        // move the map's position to the users current location. This is based on the logic found
        // in the tutorial at:
        // https://developers.google.com/maps/documentation/android-sdk/current-place-tutorial
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationServiceStatus(false)
        }

        if (requestCode == LOCATION_SETTINGS_REQUEST_CODE) {
            enableMyLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        binding.saveButton.setOnClickListener { onLocationSelected() }

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        return binding.root
    }

    /**
     * Pass back the data about the selected POI so it can be used for creating a new reminder
     */
    private fun onLocationSelected() {
        if (this::reminderPoi.isInitialized) {
            _viewModel.selectReminderLocation(reminderPoi)
        }
        // Navigate back to the reminder screen with the selected POI so the user
        // can continue creating a reminder
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // Set the map type to the type the user selected
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Initialize the map with the supplied map now that it is ready
        map = googleMap

        // Allow the user to select a POI
        setPoiClick(map)

        // Allow the user to select a custom location at any point
        setMapLongClick(map)

        // Use the customized map styling
        setMapStyle(map)

        // Request location permissions to focus the map on the user's current location
        enableMyLocation()
    }

    /**
     * After a POI is selected add a marker and enable the save button
     */
    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            _viewModel.enableSaving.value = true
            map.clear()
            reminderPoi = poi
            map.addMarker(MarkerOptions().position(poi.latLng).title(poi.name)).showInfoWindow()
        }
    }

    /**
     * Select any point on the map on a long press and display a marker at the selected location.
     * This logic is based off of the Wander example app from the Udacity Android Nano Developer
     * Degree program
     */
    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            _viewModel.enableSaving.value = true
            map.clear()
            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
            ).showInfoWindow()

            reminderPoi = PointOfInterest(latLng, getString(R.string.dropped_pin), snippet)
        }
    }

    /** Use the map style to customize the Google Map view */
    private fun setMapStyle(map: GoogleMap) {
        try {
            val success =
                map.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireContext(),
                        R.raw.map_style_file
                    )
                )

            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error:", e)
        }
    }

    /** Request location permissions and if provided focus the map on the users current location */
    private fun enableMyLocation() {
        if (!isPermissionGranted()) {
            requestPermissions(
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        } else {
            // Verify that the location service is turned on
            checkDeviceLocationServiceStatus()
            // Provide the user with the option to focus the map on their location
            map.isMyLocationEnabled = true

            startLocationUpdates()
        }
    }

    /** Get the current position of the user and focus the map on it once it has been determined */
    fun updateMapUserLocation(lastKnownLocation: Location?) {
        val zoomLevel = 15f
        // Display the user's location once it is available
        if (lastKnownLocation != null) {
            requestingLocationUpdates = false
            stopLocationUpdates()
            val lastKnownLongLat =
                lastKnownLocation.let {
                    LatLng(
                        lastKnownLocation.latitude, lastKnownLocation.longitude
                    )
                }
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    lastKnownLongLat,
                    zoomLevel
                )
            )
        } else {
            // If we aren't already watching for updates request location updates
            if (!requestingLocationUpdates) {
                startLocationUpdates()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        settingsSnackbar.dismiss()
        stopLocationUpdates()
    }

    /**
     * Request the user's location so we can focus on it on the map. This is based on the code from
     * the documentation at https://developer.android.com/training/location/change-location-settings
     */
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest().setPriority(PRIORITY_BALANCED_POWER_ACCURACY)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        requestingLocationUpdates = true
    }

    override fun onResume() {
        super.onResume()
        if (requestingLocationUpdates) enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    /**
     * Stop requesting updates on the user's location. This is based off the documentation at
     * https://developer.android.com/training/location/request-updates#updates
     */
    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Dismiss the snackbar until we validate if the required permissions were provided
        if (this::settingsSnackbar.isInitialized) {
            settingsSnackbar.dismiss()
        }

        // Check if location permissions are granted and if so enable the
        // location data layer and change the map's focus to the user's current position.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Now that the required permissions are granted get the user's current location
                // and then focus the map on their location
                enableMyLocation()
            } else {
                // Display a snackbar with information on why we need location permissions and
                // provide quick access to the app's settings page
                settingsSnackbar.show()
            }
        }
    }

    /** Check if location permissions have been granted */
    private fun isPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationServiceStatus(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        // If the snackbar was displaying the option to go to the settings hide it as we verify if
        // permissions have been granted
        if (this::settingsSnackbar.isInitialized) {
            settingsSnackbar.dismiss()
        }

        if (!map.isMyLocationEnabled) {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val settingsClient = LocationServices.getSettingsClient(requireActivity())
            val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

            // If permissions have been denied then notify the user that this permission is required
            // for the app to function properly
            locationSettingsResponseTask.addOnFailureListener { exception ->
                if (exception is ResolvableApiException && resolve) {
                    try {
                        // If an activity has been supplied to resolve this then start it

                        startIntentSenderForResult(
                            exception.resolution.intentSender,
                            REQUEST_TURN_DEVICE_LOCATION_ON,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                    }
                } else {
                    if (resolve) {
                        settingsSnackbar.show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.location_enable_explanation),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            locationSettingsResponseTask.addOnCanceledListener {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.location_enable_explanation),
                    Toast.LENGTH_SHORT
                ).show()
            }

            // If the required permissions have been added then save the reminder and
            // add a corresponding geofence
            locationSettingsResponseTask.addOnCompleteListener {
                if (!it.isSuccessful) {
                    settingsSnackbar.show()
                }
            }
        }
    }
}