package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.concurrent.TimeUnit

private const val REMINDER_TITLE_KEY = "REMINDER_TITLE"
private const val REMINDER_DESC_KEY = "REMINDER_DESC"
private const val REMINDER_LOCATION_KEY = "REMINDER_LOCATION"
private const val REMINDER_ID_KEY = "REMINDER_ID"
private const val REMINDER_POI_KEY = "REMINDER_POI"
private const val REMINDER_LAT_KEY = "REMINDER_lAT"
private const val REMINDER_LONG_KEY = "REMINDER_lONG"

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val REQUEST_SET_PERMISSIONS = 30
private const val TAG = "SaveReminderFragment"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val GEOFENCE_RADIUS_IN_METERS = 100f

/**
 * Allow the user to create a new reminder based on a selected POI and the provided title
 * and description. The permissions check logic is partially based on the logic uses in
 * https://github.com/udacity/android-kotlin-geo-fences
 */
class SaveReminderFragment() : BaseFragment() {

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment.reminder.action.ACTION_GEOFENCE_EVENT"
    }

    /** Determine if the app is running on a device running Android Q or later */
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private lateinit var settingsSnackbar: Snackbar

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel<SaveReminderViewModel>()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    /** The amount of time before the geofence expires */
    private val geofenceExpirationInMilliseconds: Long = TimeUnit.DAYS.toMillis(30)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Used to add a geofence for the reminder
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the desired location for the reminder
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        // If we are restoring the fragment use the values that the user has already input. This is
        // important because in the scenario where the user denies location permission from the
        // app settings this fragment will be recreated. This logic is partially based on
        // https://stackoverflow.com/questions/5412746/android-fragment-onrestoreinstancestate
        savedInstanceState?.let {
            _viewModel.reminderTitle.value = savedInstanceState.getString(REMINDER_TITLE_KEY, "")
            _viewModel.reminderDescription.value =
                savedInstanceState.getString(REMINDER_DESC_KEY, String())
            _viewModel.reminderSelectedLocationStr.value =
                savedInstanceState.getString(REMINDER_LOCATION_KEY, String())
            _viewModel.selectedPOI.value = savedInstanceState.getParcelable(REMINDER_POI_KEY)
            _viewModel.latitude.value = savedInstanceState.getDouble(REMINDER_LAT_KEY)
            _viewModel.longitude.value = savedInstanceState.getDouble(REMINDER_LONG_KEY)
            _viewModel.reminderID = savedInstanceState.getString(REMINDER_ID_KEY, "")
        }

        // Set up a message that will give the user the option to go to the settings to grant
        // location permissions. Display this message if the user tries to save the reminder but
        // hasn't granted the necessary location permissions
        settingsSnackbar = Snackbar.make(
            binding.saveReminderConstraint,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.settings) {
            startActivityForResult(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
            }, REQUEST_SET_PERMISSIONS)
        }

        // Save the reminder with the information supplied by the user
        binding.saveReminder.setOnClickListener {
            instantiateReminderData()

            // If the data is valid save it and create a geofence for the reminder notification
            if (_viewModel.validateEnteredData(reminderDataItem)) {
                checkPermissionsAndStartGeofencing()
            }
        }
    }

    /** Ensure reminderDataItem has all the information for the reminder */
    private fun instantiateReminderData() {
        val title = _viewModel.reminderTitle.value
        val description = _viewModel.reminderDescription.value
        val location = _viewModel.reminderSelectedLocationStr.value
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        reminderDataItem =
            ReminderDataItem(title, description, location, latitude, longitude)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save the information supplied by the user if the fragment is being destroyed.
        // This is important because in the scenario where the user denies location permission from the
        // app settings this fragment will be recreated.
        outState.putString(REMINDER_TITLE_KEY, _viewModel.reminderTitle.value)
        outState.putString(REMINDER_DESC_KEY, _viewModel.reminderDescription.value)
        outState.putString(REMINDER_LOCATION_KEY, _viewModel.reminderSelectedLocationStr.value)
        outState.putString(REMINDER_ID_KEY, _viewModel.reminderID)
        outState.putParcelable(REMINDER_POI_KEY, _viewModel.selectedPOI.value)

        if (_viewModel.latitude.value != null && _viewModel.longitude.value != null) {
            outState.putDouble(REMINDER_LAT_KEY, _viewModel.latitude.value!!)
            outState.putDouble(REMINDER_LONG_KEY, _viewModel.longitude.value!!)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }

        // If the snackbar was displaying the option to go to the settings hide it as we verify if
        // permissions have been granted
        if (this::settingsSnackbar.isInitialized) {
            settingsSnackbar.dismiss()
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        // If permissions have been denied then notify the user that this permission is required
        // for the app to function properly
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    // If an activity has been supplied to resolve this then start it
                    // This logic is based off of a code review from Udacity and the documentation at
                    // https://developer.android.com/reference/kotlin/androidx/fragment/app/Fragment#startintentsenderforresult
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
            // If the user cancels providing location data provide them with information
            // on why we need it
            Toast.makeText(
                requireContext(),
                getString(R.string.location_enable_explanation),
                Toast.LENGTH_SHORT
            ).show()
        }

        // If the required permissions have been added then save the reminder and
        // add a corresponding geofence
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                // Once the user's location service has been turned on check If permissions
                // have been granted and save the reminder and create a corresponding geofence
                if (foregroundAndBackgroundLocationPermissionApproved()) {
                    if (!this::reminderDataItem.isInitialized) {
                        instantiateReminderData()
                    }
                    _viewModel.validateAndSaveReminder(reminderDataItem)
                    addGeofenceForReminder()
                } else {
                    settingsSnackbar.show()
                }
            }
        }
    }

    /**
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /**
     * If not already granted, requests ACCESS_FINE_LOCATION and
     * (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.)
     */
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                // When running Android Q or later request background location permission as well
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    /**
     * Adds a Geofence for the reminder. This method should be called after the user has granted
     * the location permission.
     */
    private fun addGeofenceForReminder() {
        // Set the location and radius for the geofence
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                _viewModel.latitude.value!!, _viewModel.longitude.value!!,
                GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(geofenceExpirationInMilliseconds)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        // When the user enters the geofence trigger the broadcast receiver that will
        // post a notification with the details of the reminder
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        val reminderGeofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Add the geofence for the reminder
        geofencingClient.addGeofences(geofencingRequest, reminderGeofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.e(context?.getString(R.string.geofence_added), geofence.requestId)
            }
            addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    R.string.geofences_not_added,
                    Toast.LENGTH_SHORT
                ).show()
                if (it.message != null) {
                    Log.w(TAG, it.message!!)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Provide a user friendly UX by closing the settings snackbar when the fragment is destroyed.
        settingsSnackbar.dismiss()
    }

    /**
     * Check the result of requesting location permissions. In all cases, we need to have the
     * location permission. On Android 10+ (Q) we need to have the background permission as well.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult")
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            if (!foregroundAndBackgroundLocationPermissionApproved()) {
                if (this::settingsSnackbar.isInitialized) {
                    // Show the snackbar to inform the user that we need to location permissions before
                    // continuing
                    settingsSnackbar.show()
                }
            } else {
                // If the required permissions have been granted then save the reminder
                // and add a geofence
                checkDeviceLocationSettingsAndStartGeofence(true)
            }
        } else {
            // If the required permissions have been granted then save the reminder
            // and add a geofence
            checkDeviceLocationSettingsAndStartGeofence(true)
        }
    }

    /**
     *  When we get the result from asking the user to turn on device location, we call
     *  checkDeviceLocationSettingsAndStartGeofence again to make sure it's actually on, but
     *  we don't resolve the check to keep the user from seeing an endless loop.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }

        // Now that the user has provided the necessary permissions begin the process of saving
        // the reminder and if necessary ask the user to turn on the location service.
        if (requestCode == REQUEST_SET_PERMISSIONS) {
            checkDeviceLocationSettingsAndStartGeofence(true)
        }
    }

    /**
     * If the required permissions have been granted then save the
     */
    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence(true)
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }
}