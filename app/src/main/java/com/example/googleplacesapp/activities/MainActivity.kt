package com.example.googleplacesapp.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.googleplacesapp.adapters.SavedGooglePlacesAdapter
import com.example.googleplacesapp.database.DatabaseHandler
import com.example.googleplacesapp.databinding.ActivityMainBinding
import com.example.googleplacesapp.models.GooglePlaceModel
import com.example.googleplacesapp.utils.SwipeToDeleteCallback
import com.example.googleplacesapp.utils.SwipeToEditCallback

class MainActivity : AppCompatActivity() {

    companion object{
        private const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
        const val PLACE_DETAILS_EXTRA = "place_details_extra"
        private const val UPDATE_PLACE_ACTIVITY_REQUEST_CODE = 2
    }

    // View binding object
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the binding
        this.binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(this.binding?.root)

        // Add Place button
        this.binding?.fabAddGoogleMapsPlace?.setOnClickListener {
            val intent = Intent(this, AddGoogleMapsPlaceActivity::class.java)
            /** INSTEAD OF USING 'startActivity(intent)' WE CAN USE 'startActivityForResult()' -->
                --> TO DYNAMICALLY UPDATE THE RECYCLER VIEW ON THIS ACTIVITY WHEN NEW PLACE IS ADDED!!! */
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)

        }

        getGooglePlacesListFromLocalDB()
    }

    /** GET ALL SAVED GOOGLE PLACES FROM DATABASE-HANDLER(OUR MANUAL METHOD)! */
    private fun getGooglePlacesListFromLocalDB(){
        val databaseHandler = DatabaseHandler(this)
        val getGooglePlaceList: ArrayList<GooglePlaceModel> = databaseHandler.getSavedGooglePlacesList()
        // IF THE SIZE IS > 0, IT HAS PLACES
        if (getGooglePlaceList.size > 0){
            this.binding?.tvNoPlacesFound?.visibility = View.GONE
            this.binding?.rvSavedGooglePlaces?.visibility = View.VISIBLE
            setUpSavedGooglePlacesRecyclerView(getGooglePlaceList)
        }
        else{
            this.binding?.tvNoPlacesFound?.visibility = View.VISIBLE
            this.binding?.rvSavedGooglePlaces?.visibility = View.GONE
        }
    }

    // Set up recyclerview with our adapter
    private fun setUpSavedGooglePlacesRecyclerView(googlePlacesList: ArrayList<GooglePlaceModel>){
        val adapter = SavedGooglePlacesAdapter(googlePlacesList)
        val rvSavedGooglePlaces = this.binding?.rvSavedGooglePlaces
        rvSavedGooglePlaces?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvSavedGooglePlaces?.setHasFixedSize(true)
        rvSavedGooglePlaces?.adapter = adapter

        /** CLICKABLE ITEM STEP 4 */
        /** CALL 'setOnItemClickListener()' METHOD FROM THE ADAPTER AND OVERRIDE 'onClick()' METHOD FROM THE INTERFACE!!! */
        adapter.setOnItemClickListener(object : SavedGooglePlacesAdapter.OnItemClickListener{
            override fun onClick(position: Int, googlePlaceItem: GooglePlaceModel) {
                val intent = Intent(this@MainActivity, GooglePlaceDetailsActivity::class.java)
                intent.putExtra(PLACE_DETAILS_EXTRA, googlePlaceItem)
                startActivity(intent)
            }
        })

        /** Create new edit swipe handler and override 'onSwiped()' METHOD!! */
        val editSwipeHandler = object : SwipeToEditCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // TODO CHANGED THE ADAPTER HERE(NOT CREATED NEW ONE)
                adapter.notifySwipeEditItem(this@MainActivity, this@MainActivity, viewHolder.adapterPosition, UPDATE_PLACE_ACTIVITY_REQUEST_CODE)
            }
        }
        // Use 'ItemTouchHelper()' to pass Object that has reference to overrode 'onSwiped' method in 'SwipeToEditCallback' Abstract Class
        // NOTE: 'SwipeToEditCallback' IS OUR CUSTOM CALLBACK CLASS FROM THIRD-PARTY
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(rvSavedGooglePlaces)

        /** Create new delete swipe handler and override 'onSwiped()' METHOD!! */
        val deleteSwipeHelper = object : SwipeToDeleteCallback(this){
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                adapter.deletePlaceAt(this@MainActivity, viewHolder.adapterPosition)
            }
        }
        // Create object of 'ItemTouchHelper()' with callback object: 'deleteSwipeHelper'
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHelper)
        deleteItemTouchHelper.attachToRecyclerView(rvSavedGooglePlaces)
    }

    /** Handle the result after calling another activity */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            /** IF THE USER SAVES THE PLACE TO THE DATABASE */
            if (requestCode == ADD_PLACE_ACTIVITY_REQUEST_CODE){
                getGooglePlacesListFromLocalDB()
            }
            /** IF THE USER EDITS THE PLACE--'AddGoogleMapsPlaceActivity' IS CALLED VIA OUR ADAPTER */
            else if (requestCode == UPDATE_PLACE_ACTIVITY_REQUEST_CODE){
                getGooglePlacesListFromLocalDB()
            }
        }
    }
}