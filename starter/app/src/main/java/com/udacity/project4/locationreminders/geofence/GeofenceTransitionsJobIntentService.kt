package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext

private const val TAG = "GeofenceService"

/**
 * Create a notification when the user enters into a reminder's geofence
 */
class GeofenceTransitionsJobIntentService() : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        if (intent.action == SaveReminderFragment.ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)

            if (!geofencingEvent.hasError()) {
                if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    val fenceId = when {
                        // Display a notification with information on the reminder that the
                        // user entered within the geofence of
                        geofencingEvent.triggeringGeofences.isNotEmpty() ->
                            sendNotification(geofencingEvent.triggeringGeofences)
                        else -> {
                            Log.e(TAG, "No Geofence Trigger Found!")
                            return
                        }
                    }
                }
            }
        }
    }

    /**
     * Post a notification with information on the reminder that can open an activity
     * to display more details
     */
    private fun sendNotification(triggeringGeofences: List<Geofence>) {

        // Post a notification for each of the reminders that have been triggered
        triggeringGeofences.forEach {
            val requestId = it.requestId
            // Get the local repository instance from Koin
            val remindersLocalRepository: ReminderDataSource by inject()
            // Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                // Get the reminder with the corresponding request id
                val result = remindersLocalRepository.getReminder(requestId as String)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    // Post a notification to the user with the reminder details
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
            }
        }
    }

}