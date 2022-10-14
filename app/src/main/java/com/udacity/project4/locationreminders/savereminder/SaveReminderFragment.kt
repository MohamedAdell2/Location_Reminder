package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

@RequiresApi(Build.VERSION_CODES.S)
class SaveReminderFragment : BaseFragment() {

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
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

            _viewModel.reminderData = ReminderDataItem(
                title, description, location, latitude, longitude
            )

            if (_viewModel.validateEnteredData()) {
                checkPermissionsAndStartGeofence()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createNewGeofence (){
        val data = _viewModel.reminderData
        val geofence = Geofence.Builder()
            .setRequestId(data.id)
            .setCircularRegion(data.latitude!!, data.longitude!!
                , GEOFENCE_RADIUS_IN_METERS )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocation(false)
        }
    }

    @SuppressLint("InlinedApi")
    private fun isPermissionGranted(): Boolean {
        val foregroundLocation = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val backgroundLocationPermeation = if (runningQOrLater){
            ContextCompat.checkSelfPermission(requireContext()
                , Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }else{true}
        return foregroundLocation && backgroundLocationPermeation
    }

    private fun checkPermissionsAndStartGeofence() {
        if (isPermissionGranted()){
            checkDeviceLocation()
        }else{
            requestPermissions()
        }
    }

    private fun checkDeviceLocation(resolve:Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    startIntentSenderForResult(exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d("error", "Error getting location settings resolution: "
                            + sendEx.message)
                }
            } else {
                Snackbar.make(
                    requireView(),
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocation()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful){
                createNewGeofence()
            }
        }
    }

    private fun requestPermissions(){
        requestPermissions(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ), REQUEST_FOREGROUND_LOCATION_CODE)
        if (runningQOrLater) {
            AlertDialog.Builder(requireContext())
                .setTitle("Background location permission")
                .setMessage("Allow location permission to get location updates in background")
                .setPositiveButton("Allow") { _, _ ->
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_BACKGROUND_Location_CODE
                    )
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_FOREGROUND_LOCATION_CODE){
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Snackbar.make(requireView() ,
                    getString(R.string.snackbar_message), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.snackbar_action)){requestPermissions()}
                    .show()
            }
        }else if(requestCode == REQUEST_BACKGROUND_Location_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(requireContext()
                    , "Permission granted", Toast.LENGTH_SHORT).show()
                checkDeviceLocation()
            } else {
                Snackbar.make(requireView() ,
                    getString(R.string.snackbar_message), Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.snackbar_action)){requestPermissions()}
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
//constant for request permission
const val REQUEST_FOREGROUND_LOCATION_CODE = 11
const val REQUEST_BACKGROUND_Location_CODE = 15
const val REQUEST_TURN_DEVICE_LOCATION_ON = 88
//constant for geofence
const val GEOFENCE_RADIUS_IN_METERS = 100f
const val GEOFENCE_ACTION = ".locationreminders.geofence.action.ACTION_GEOFENCE_EVENT"