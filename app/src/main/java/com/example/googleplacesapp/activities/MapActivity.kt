package com.example.googleplacesapp.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.googleplacesapp.R
import com.example.googleplacesapp.databinding.ActivityMapBinding
import com.example.googleplacesapp.models.GooglePlaceModel
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapActivity : AppCompatActivity(), OnMapReadyCallback{

    // binding
    private var binding: ActivityMapBinding? = null
    // Store the current passed Place to use its details to show on the map
    private var currentPlace: GooglePlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(this.binding?.root)

        // Set SupportActionBar
        setSupportActionBar(this.binding?.toolbarPlaceMap)
        if (supportActionBar != null){
            this.supportActionBar?.title = "Map Details"
            this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        this.binding?.toolbarPlaceMap?.setNavigationOnClickListener {
            onBackPressed()
        }

        // Get the place from this intent
        if (this.intent?.hasExtra(MainActivity.PLACE_DETAILS_EXTRA) == true){
            this.currentPlace = this.intent.getParcelableExtra(MainActivity.PLACE_DETAILS_EXTRA) as? GooglePlaceModel
        }
        if (this.currentPlace != null){
            this.supportActionBar?.title = this.currentPlace?.title

            /** GET THE FRAGMENT IN THE LAYOUT XML WHERE WE SHOW THE MAP AS 'SupportMapFragment'*/
            val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.fragment_map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)   /** RUN ON THE BACKGROUND, WHICH MAP SHOULD IT BE: 'this' MAP */
        }
    }

    /** WHEN MAP IS READY DISPLAY A MARKER TO SHOW THE PLACE LOCATION */
    override fun onMapReady(googleMap: GoogleMap) {
        val position = this.currentPlace?.let { LatLng(it.latitude, it.longitude) }
        googleMap.addMarker(MarkerOptions().position(position!!).title(this.currentPlace?.location))
        val zoomToCurrPlace = CameraUpdateFactory.newLatLngZoom(position, 10f)
        googleMap.animateCamera(zoomToCurrPlace)
    }
}