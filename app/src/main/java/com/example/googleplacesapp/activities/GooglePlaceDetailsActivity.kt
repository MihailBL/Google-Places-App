package com.example.googleplacesapp.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.googleplacesapp.R
import com.example.googleplacesapp.databinding.ActivityGooglePlaceDetailsBinding
import com.example.googleplacesapp.models.GooglePlaceModel

class GooglePlaceDetailsActivity : AppCompatActivity() {

    // Binding object
    private var binding: ActivityGooglePlaceDetailsBinding? = null
    private var googlePlaceItem: GooglePlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up binding
        this.binding = ActivityGooglePlaceDetailsBinding.inflate(layoutInflater)
        setContentView(this.binding?.root)

        //Set up the toolbar as supportActionBar
        setSupportActionBar(this.binding?.toolbarPlaceDetails)
        if (this.supportActionBar != null){
            this.supportActionBar?.title = "Place Details"
            this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        this.binding?.toolbarPlaceDetails?.setNavigationOnClickListener {
            onBackPressed()
        }

        // Get the placeItem object from the intent
        if (intent.hasExtra(MainActivity.PLACE_DETAILS_EXTRA)){
            this.googlePlaceItem = intent.getParcelableExtra(MainActivity.PLACE_DETAILS_EXTRA)
        }
        if (this.googlePlaceItem != null){
            this.binding?.ivPlaceDetailsImage?.setImageURI(Uri.parse(googlePlaceItem?.image))
            this.binding?.tvPlaceDetailsTitle?.text = this.googlePlaceItem?.title
            this.binding?.tvPlaceDetailsDescription?.text = this.googlePlaceItem?.description
        }

        // User clicks on 'btnViewOnMap' place
        this.binding?.btnViewOnMap?.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            // 'googlePlaceItem' is object of the current place holding every info
            intent.putExtra(MainActivity.PLACE_DETAILS_EXTRA, googlePlaceItem)
            startActivity(intent)
        }
    }
}