package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.S)
class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient

    private val pendingIntent :PendingIntent by lazy {
        val intent = Intent(requireContext() , GeofenceBroadcastReceiver::class.java)
        intent.action = GEOFENCE_ACTION
        PendingIntent.getBroadcast(requireContext() ,
            0 , intent, PendingIntent.FLAG_MUTABLE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        _viewModel.selectedPOI.observe(viewLifecycleOwner) { poi ->
            if (poi != null) {
                _viewModel.apply {
                    reminderSelectedLocationStr.value = poi.name
                    longitude.value = poi.latLng.longitude
                    latitude.value = poi.latLng.latitude
                }
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
                val title = _viewModel.reminderTitle.value
                val description = _viewModel.reminderDescription.value
                val location = _viewModel.reminderSelectedLocationStr.value
                val latitude = _viewModel.latitude.value
                val longitude = _viewModel.longitude.value

                val reminderData = ReminderDataItem(
                    title, description, location, latitude, longitude
                )

                if (_viewModel.validateEnteredData(reminderData)) {
                    createNewGeofence(reminderData)
                }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createNewGeofence (data :ReminderDataItem){
        val geofence = Geofence.Builder()
            .setRequestId(data.id)
            .setCircularRegion(data.latitude!!, data.longitude!!
                , GEOFENCE_RADIUS_IN_METERS )
            .setExpirationDuration(TimeUnit.DAYS.toMillis(1))
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val requestGeofence = GeofencingRequest.Builder()
            .addGeofence(geofence)
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .build()

        geofencingClient.addGeofences(requestGeofence , pendingIntent).run {
            addOnSuccessListener {
                _viewModel.saveReminder(data)
            }
            addOnFailureListener {
                Toast.makeText(requireContext() ,
                    "can't add the reminder",Toast.LENGTH_SHORT).show()
                Log.e("Geofence", "error ${it.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}

const val GEOFENCE_RADIUS_IN_METERS = 100f
const val GEOFENCE_ACTION = ".locationreminders.geofence.action.ACTION_GEOFENCE_EVENT"