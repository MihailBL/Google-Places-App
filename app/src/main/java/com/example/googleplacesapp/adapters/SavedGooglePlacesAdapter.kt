package com.example.googleplacesapp.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.googleplacesapp.activities.AddGoogleMapsPlaceActivity
import com.example.googleplacesapp.activities.MainActivity
import com.example.googleplacesapp.database.DatabaseHandler
import com.example.googleplacesapp.databinding.RecyclerviewItemGooglePlaceBinding
import com.example.googleplacesapp.models.GooglePlaceModel
import kotlinx.coroutines.withContext

class SavedGooglePlacesAdapter(private val googlePlacesList: ArrayList<GooglePlaceModel>) :
    RecyclerView.Adapter<SavedGooglePlacesAdapter.ViewHolder>() {

    /** CLICKABLE ITEM STEP 3: SAVE THE REFERENCE OF THE OVERRODE METHOD ON THE GLOBAL LEVEL */
    private var onItemClickListener: OnItemClickListener? = null

    private var mainActivityContext: Context? = null

    // Get access to the views of the recyclerview_item layout
    inner class ViewHolder(binding: RecyclerviewItemGooglePlaceBinding) : RecyclerView.ViewHolder(binding.root){
        val ivPlaceImage = binding.ivPlaceImage
        val tvPlaceTitle = binding.tvPlaceTitle
        val tvPlaceDescription = binding.tvPlaceDescription
    }

    // Return object of the ViewHolder and pass binding object to the recyclerview_item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(RecyclerviewItemGooglePlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currPlace = this.googlePlacesList[position]
        // Make the binding
        holder.ivPlaceImage.setImageURI(Uri.parse(currPlace.image))
        holder.tvPlaceTitle.text = currPlace.title
        holder.tvPlaceDescription.text = currPlace.description
        /** STEP 5: SET ONCLICK LISTENER FOR EACH ITEM, AND WHEN CLICKED CALL THE OVERRODE 'onClick()' METHOD! */
        holder.itemView.setOnClickListener {
            if (this.onItemClickListener != null){
                this.onItemClickListener?.onClick(position, currPlace)
            }
        }
    }

    override fun getItemCount(): Int {
        return this.googlePlacesList.size
    }

    /** MAKING EACH RECYCLERVIEW ITEM TO BE CLICKABLE:
        1. PASSING FUNCTION/LAMBDA-FUNCTION TO PARAMETER IN THE ADAPTER'S PRIMARY CONSTRUCTOR THEN INVOKE IT
                + THIS ALSO WORKS FOR HANDLING LISTENERS FOR BUTTONS INSIDE EACH RECYCLERVIEW ITEM
                + ITS HANDLED IN 'onBindViewHolder' METHOD!!!
        2. CREATING INTERFACE!!! BEST PRACTICE */
    interface OnItemClickListener{    /** CLICKABLE ITEM STEP 1: CREATE FUNCTION THAT WE WANT TO OVERRIDE IN THE ACTIVITY! */
        fun onClick(position: Int, googlePlaceItem: GooglePlaceModel)
    }

    /** CLICKABLE ITEM STEP 2: CREATE FUNCTION TO PASS THE OBJECT REFERENCING THE OVERRODE METHOD IMPLEMENTED IN THE ACTIVITY! */
    fun setOnItemClickListener(onClickListener: OnItemClickListener){
        this.onItemClickListener = onClickListener
    }

    /** FUNCTION FOR HANDLING SWIPE EDIT FUNCTIONALITY */
    fun notifySwipeEditItem(activity: Activity, context: Context, position: Int, requestCode: Int){
        /** GO TO 'AddGoogleMapsPlaceActivity' WHEN USER SWIPES SPECIFIC RECYCLERVIEW ITEM */
        val intent = Intent(context, AddGoogleMapsPlaceActivity::class.java)
        intent.putExtra(MainActivity.PLACE_DETAILS_EXTRA, this.googlePlacesList[position])
        activity.startActivityForResult(intent, requestCode)
        notifyItemChanged(position)
    }

    /** FUNCTION TO DELETE PLACE AT SPECIFIED POSITION */
    fun deletePlaceAt(context: Context, position: Int){
        val databaseHandler = DatabaseHandler(context)
        val isDeleted = databaseHandler.deleteGooglePlace(this.googlePlacesList[position])
        // If 'isDeleted > 0' the place is removed from the database, we need to remove it from the list too
        if (isDeleted > 0){
            this.googlePlacesList.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}