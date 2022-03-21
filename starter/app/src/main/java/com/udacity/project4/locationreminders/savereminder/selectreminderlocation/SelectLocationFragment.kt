package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

private const val TAG = "SELECTLOCATIONFRAGMENT"

/**
 * Allows the user to choose a POI on a Google Map and return the information to the reminder
 * view model. The logic for retrieving permissions is based off the example code found in the
 * Geo Fences sample app at https://github.com/udacity/android-kotlin-geo-fences.git
 */
class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    /** The POI the user selected on the map */
    private lateinit var reminderPoi: PointOfInterest
    private lateinit var map: GoogleMap

    /** Denotes a request to access the users location */
    private val REQUEST_LOCATION_PERMISSION = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            // Provide the user with the option to focus the map on their location
            map.isMyLocationEnabled = true
            val zoomLevel = 15f

            // Get the current position of the user and focus the map on it once it
            // has been determined
            fusedLocationProviderClient.lastLocation.addOnCompleteListener { locationTask ->
                val lastKnownLocation = locationTask.result
                val lastKnownLongLat =
                    lastKnownLocation?.let {
                        LatLng(
                            lastKnownLocation.latitude, lastKnownLocation.longitude
                        )
                    }
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLongLat, zoomLevel))
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if location permissions are granted and if so enable the
        // location data layer and change the map's focus to the user's current position.
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
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

}
