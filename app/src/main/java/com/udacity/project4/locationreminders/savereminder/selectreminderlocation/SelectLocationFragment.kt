package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map :GoogleMap

    private lateinit var fusedLocationProviderClient : FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap) {
        map = p0
        map.isMyLocationEnabled=true
        setMapStyle()
        getDeviceLocation()
        setTheMarker()
        onLocationSelected()
    }

    private fun setTheMarker(){
        map.setOnMapLongClickListener { latlng->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latlng.latitude,latlng.longitude
            )

            map.addMarker(MarkerOptions()
                .position(latlng)
                .title(getString(R.string.title_marker))
                .snippet(snippet)
            )
        }
    }

    private fun onLocationSelected() {
        map.setOnInfoWindowClickListener { marker->
            _viewModel.apply {
                selectedPOI.value = null
                reminderSelectedLocationStr.value = marker.snippet
                latitude.value = marker.position.latitude
                longitude.value = marker.position.longitude
            }
            requireActivity().onBackPressed()
        }

        map.setOnPoiClickListener { poi->
            AlertDialog.Builder(requireContext())
                .setTitle("Do you want to use this location")
                .setPositiveButton("YES"){_,_->
                    _viewModel.selectedPOI.value = poi
                    requireActivity().onBackPressed()
                }
                .setNegativeButton("NO"){dialog, _->
                    dialog.dismiss()
                }.create().show()
        }

    }

    private fun setMapStyle(){
        map.setMapStyle(MapStyleOptions
            .loadRawResourceStyle(requireContext(), R.raw.map_style))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId){
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

    private fun getDeviceLocation() {
        try {
            val locationResult = fusedLocationProviderClient.lastLocation
            locationResult.addOnCompleteListener(context as Activity) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude), 15f))
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.d("Error","Exception: ${e.message}")
        }
    }
}
