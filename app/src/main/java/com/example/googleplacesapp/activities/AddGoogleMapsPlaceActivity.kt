package com.example.googleplacesapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.googleplacesapp.BuildConfig
import com.example.googleplacesapp.R
import com.example.googleplacesapp.database.DatabaseHandler
import com.example.googleplacesapp.databinding.ActivityAddGoogleMapsPlaceBinding
import com.example.googleplacesapp.models.GooglePlaceModel
import com.example.googleplacesapp.utils.GetAddressFromLatLng
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/** INSTEAD OF DOING ON CLICK LISTENER FOR EACH VIEW WE CAN INHERIT FROM 'View.OnClickListener' TO -->
    --> OVERRIDE THE 'onClick()' METHOD, AND HANDLE ALL LISTENERS THERE!!! */
class AddGoogleMapsPlaceActivity : AppCompatActivity(), View.OnClickListener {

    // Companion Object For Static Variables
    companion object{
        /** TO CHECK IF THE RETURNING RESULT FROM ANOTHER ACTIVITY IS FROM CERTAIN CODE*/
        private const val GALLERY_CODE = 1
        private const val CAMERA_CODE = 2
        private const val DOWNLOADED_IMAGES_DIRECTORY_PATH = "/Download/GooglePlacesAPP"
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }

    // View binding object
    private var binding: ActivityAddGoogleMapsPlaceBinding? = null
    // Calendar instance
    private var calendarInstance = Calendar.getInstance()
    // DatePickerDialog.onDateSetListener Instance
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    // Keep image Uri
    private var savedImageUriToInternalStorage: Uri? = null
    // Latitude and Longitude
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    // If this Activity is called from MainActivity via Adapter for SWIPE EDIT ITEM
    private var googlePlaceDetails: GooglePlaceModel? = null

    // The main entry point for interacting with the Fused Location Provider
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Create and Initialize 'locationCallback', USE IT AS CALLBACK IN 'fusedLocationClient.requestLocationUpdates'
    /** 'onLocationResult(locationResult: LocationResult)' METHOD --- WHAT SHOULD WE DO AFTER GETTING THE LOCATION!!! */
    private var locationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val lastLocation: Location = locationResult.lastLocation!!
            latitude = lastLocation.latitude
            longitude = lastLocation.longitude

            /** Pass latitude and longitude to get actual address */
            val addressTask = GetAddressFromLatLng(this@AddGoogleMapsPlaceActivity, latitude, longitude)
            /** Override the interface's functions */
            addressTask.setAddressListener(object : GetAddressFromLatLng.AddressListener{
                override fun onAddressFound(address: String?) {
                    binding?.etLocation?.setText(address)
                }

                override fun onError() {
                    Toast.makeText(this@AddGoogleMapsPlaceActivity, "Something went wrong!", Toast.LENGTH_LONG).show()
                }
            })
            /** Start our Async-Task Class */
            addressTask.getAddress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set up the binding
        this.binding = ActivityAddGoogleMapsPlaceBinding.inflate(layoutInflater)
        setContentView(this.binding?.root)

        // Set up support action bar
        setSupportActionBar(this.binding?.toolbarAddPlace)
        if (this.supportActionBar != null){
            this.supportActionBar?.title = "ADD PLACE"
            this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
        this.binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
            finish()
        }

        // Initialize the Instance of OnDateSetListener(SET THE 'calendarInstance' WHEN USER SELECTS -->
        // --> SPECIFIC DATE AND CLICKS 'OK')
        this.dateSetListener = DatePickerDialog.OnDateSetListener { datePicker, year, month, dayOfMonth ->
            this.calendarInstance.set(Calendar.YEAR, year)
            this.calendarInstance.set(Calendar.MONTH, month)
            this.calendarInstance.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            // Populate 'etDate' with user selected date
            populateEtDateView()
        }
        // Populate 'etDate' with current date, even if the user didn't choose specific date yet
        populateEtDateView()

        // user clicks on et_date
        this.binding?.etDate?.setOnClickListener(this)
        // user adds an image
        this.binding?.btnAddImage?.setOnClickListener(this)
        // user clicks on SAVE button
        this.binding?.btnSavePlace?.setOnClickListener(this)
        // user clicks on et_location
        this.binding?.etLocation?.setOnClickListener(this)
        // user clicks on btn_getCurrentLocation
        this.binding?.btnGetCurrLocation?.setOnClickListener(this)

        // Check if the intent was send from MainActivity(Adapter-SWIPE ITEM)
        if (intent.hasExtra(MainActivity.PLACE_DETAILS_EXTRA)){
            this.googlePlaceDetails = intent.getParcelableExtra(MainActivity.PLACE_DETAILS_EXTRA)
        }
        if (this.googlePlaceDetails != null){
            changeActivityToEditActivity()
        }

        /** Initialize Google Places With The API-KEY To Be Able To Get Locations */
        if (!Places.isInitialized()){
            Places.initialize(this, BuildConfig.API_KEY)
        }

        /** Initialize Fused Location Provider Object */
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onClick(view: View?) {
        when(view?.id){
            // If the user clicked on the et_date
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddGoogleMapsPlaceActivity,
                    dateSetListener,               // INSTEAD OF WRITING WHOLE FUNCTION HERE WE INITIALIZED THE INSTANCE IN 'onCreate' METHOD
                    this.calendarInstance.get(Calendar.YEAR),       // At start show current year(Not user Selected)
                    this.calendarInstance.get(Calendar.MONTH),      // At start show current month(Not user Selected)
                    this.calendarInstance.get(Calendar.DAY_OF_MONTH)).show()   // At start show current dayOfMonth(Not user Selected)
            }

            // If the user clicked on btn_addImage
            R.id.btn_addImage -> {
                showAddImageAlertDialogChooser()
            }

            // If the user clicked on btn_savePlace
            R.id.btn_savePlace -> {
                // Check if the fields are empty
                when{
                    this.binding?.etTitle?.text?.isEmpty() == true -> {
                        Snackbar.make(this, view, "Please fill the Title Field", Snackbar.LENGTH_LONG).show()
                    }
                    this.binding?.etDescription?.text?.isEmpty() == true -> {
                        Snackbar.make(this, view, "Please fill the Description Field", Snackbar.LENGTH_LONG).show()
                    }
                    this.binding?.etLocation?.text?.isEmpty() == true -> {
                        Snackbar.make(this, view, "Please fill the Location Field", Snackbar.LENGTH_LONG).show()
                    }
                    this.binding?.ivAddImageToPlace == null -> {
                        Snackbar.make(this, view, "Please select an image", Snackbar.LENGTH_LONG).show()
                    }
                    else -> {     // If everything is ok save the place in the SQLite database
                        val googlePlaceModel = GooglePlaceModel(
                            if(this.googlePlaceDetails == null) 0 else this.googlePlaceDetails?.id!!,   // For the id
                            this.binding?.etTitle?.text?.toString()!!,
                            this.savedImageUriToInternalStorage?.toString()!!,     // This is Uri.parse(file.absolutePath)
                            this.binding?.etDescription?.text?.toString()!!,
                            this.binding?.etDate?.text?.toString()!!,
                            this.binding?.etLocation?.text?.toString()!!,
                            this.latitude, this.longitude )
                        val databaseHandler = DatabaseHandler(this)
                        if (this.googlePlaceDetails == null) {
                            val addGooglePlaceResult = databaseHandler.addGooglePlace(googlePlaceModel)
                            // If is > 0 then there is no error
                            if (addGooglePlaceResult > 0){
                                Toast.makeText(this, "Place Was Saved!", Toast.LENGTH_LONG).show()
                                setResult(Activity.RESULT_OK)      // Set RESULT_OK so that this activity returns this result to the caller!!!
                                finish()
                            }
                        }
                        else{
                            val updateGooglePlaceResult = databaseHandler.updateGooglePlace(googlePlaceModel)
                            if(updateGooglePlaceResult > 0){
                                Toast.makeText(this, "Place Edited!", Toast.LENGTH_LONG).show()
                                setResult(Activity.RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }

            // if the user clicked on et_location
            R.id.et_location -> {
                try {
                    /** List of fields that has to be passed */
                    val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)
                    /** Start autocomplete intent with unique request code */
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

            // If the user clicked btn_getCurrLocation
            R.id.btn_getCurrLocation -> {
                if (!isLocationEnabled()) {
                    Toast.makeText(this, "Please enable Your Location Services", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withActivity(this)
                        .withPermissions(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                        .withListener(object : MultiplePermissionsListener{
                            @RequiresApi(Build.VERSION_CODES.S)
                            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                               requestNewUserLocation()
                            }

                            override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?,token: PermissionToken?) {
                                showRationaleDialogForPermissions()
                            }

                        }).onSameThread().check()
                }
            }
        }
    }

    /** Populate et_date -- Selected Year/Month/DayOfMonth by the user is stored in 'calendarInstance' */
    private fun populateEtDateView(){
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        this.binding?.etDate?.setText(sdf.format(this.calendarInstance.time).toString())
    }

    // Alert Dialog for btn_addImage
    private fun showAddImageAlertDialogChooser(){
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Select Action")

        val alertDialogItems = arrayOf("Select photo from gallery", "Capture photo from camera")
        // What do we do if any of the is clicked
        alertDialogBuilder.setItems(alertDialogItems){ dialogInterface, whichItem ->
            when(whichItem){
                // 0 -> Select photo from gallery
                0 -> selectPhotoFromGalleryRequest()

                // 1 -> Capture photo from camera
                1 -> takePhotoFromCameraRequest()
            }
        }

        alertDialogBuilder.show()
    }

    // If the user chooses "Select photo from gallery" in the Alert Dialog
    private fun selectPhotoFromGalleryRequest() {
        /** ASK FOR PERMISSIONS WITH DEXTER THIRD-PARTY LIBRARY */
        Dexter.withActivity(this)
            .withPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener{
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report?.areAllPermissionsGranted() == true){
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        /** STARTS AN ACTIVITY BUT WAITS FOR THE RESULT!!! WE MUST CREATE FUNCTION FOR HANDLING THE RESULT!!! */
                        /** 'GALLERY_CODE' RETURNS AS 'requestCode' in 'onActivityResult' method!!! */
                        startActivityForResult(galleryIntent, GALLERY_CODE)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>?, token: PermissionToken?) {
                    showRationaleDialogForPermissions()
                }
            }).onSameThread().check()

    }

    // If the user chooses 'Take photo from camera' in the Alert Dialog
    private fun takePhotoFromCameraRequest(){
        /** ASK FOR PERMISSION WITH DEXTER THIRD-PARTY LIBRARY */
        Dexter.withActivity(this)
            .withPermission(android.Manifest.permission.CAMERA)
            .withListener(object : PermissionListener{
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(cameraIntent, CAMERA_CODE)
                }
                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    Toast.makeText(this@AddGoogleMapsPlaceActivity, "Permission for camera denied!", Toast.LENGTH_LONG).show()
                }
                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                    showRationaleDialogForPermissions()
                }
            }).onSameThread().check()
    }

    /** HANDLING RETURNING RESULTS FROM USER FROM OTHER APPLICATIONS!!! */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY_CODE){
                if (data != null){
                    // TODO NEED TO CHECK IF CAN BE DONE AS 'data.extras.get("data") as Bitmap' SAME AS BELOW!
                    val contentUri = data.data
                    try {
                        val selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, contentUri)
                        // call this method to save the image on internal storage and return its uri/absolute path
                        this.savedImageUriToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                        Log.e("saved image", "Path :: $savedImageUriToInternalStorage")

                        this.binding?.ivAddImageToPlace?.setImageBitmap(selectedImageBitmap)
                    }catch (e: IOException){
                        e.printStackTrace()
                        Toast.makeText(this, "Failed to load image from the gallery!", Toast.LENGTH_LONG).show()
                    }
                }
            }
            else if (requestCode == CAMERA_CODE){
                val thumbNail: Bitmap = data?.extras?.get("data") as Bitmap

                // call this method to save the image on internal storage and return its uri/absolute path
                this.savedImageUriToInternalStorage = saveImageToInternalStorage(thumbNail)

                this.binding?.ivAddImageToPlace?.setImageBitmap(thumbNail)
            }

            // Returns the code from the google Place
            else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE){
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                this.binding?.etLocation?.setText(place.address)
                this.latitude = place.latLng!!.latitude
                this.longitude = place.latLng!!.longitude
            }
        }
        else{
            Toast.makeText(this, "Something went Wrong! Please Try Again", Toast.LENGTH_LONG).show()
        }
    }

    // Show Rationale Permission Dialog when if the user denied 'Select photo from gallery' request
    private fun showRationaleDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("Permissions Denied! They can be enabled under the apps settings.")
            .setPositiveButton("Go To Settings"){ _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)         /** Request to send user to settings */
                    val appUri = Uri.fromParts("package", packageName, null)     /** Get the Uri of the app's package name */
                    intent.data = appUri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){ dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri{
        val file = File(Environment.getExternalStorageDirectory().absolutePath + "/Download" + File.separator +
                "GooglePlacesApp" + UUID.randomUUID() + ".jpg")

        try {
            val fileOutput = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutput)
            fileOutput.flush()
            fileOutput.close()
        }catch (e: IOException){
            e.printStackTrace()
        }

        // TODO IMPORTANT NOTE HERE!!!
        /** !!!!!!!!!!!!!!!!!!!!!!!!!!!    IMPORTANT    !!!!!!!!!!!!!!!!!!!!!!!!!!! */
        /** ABSOLUTE PATH TAKES ONLY 'Environment.getExternalStorageDirectory().absolutePath'!!!
         * TO GET THE WHOLE IMAGE PATH USE: 'file.path' */
        return Uri.parse(file.path)
    }

    // Change the activity for editing
    @SuppressLint("SetTextI18n")
    private fun changeActivityToEditActivity(){
        this.supportActionBar?.title = "Edit Place"
        this.binding?.etTitle?.setText(this.googlePlaceDetails?.title)
        this.binding?.etDescription?.setText(this.googlePlaceDetails?.description)
        this.binding?.etDate?.setText(this.googlePlaceDetails?.date)
        this.binding?.etLocation?.setText(this.googlePlaceDetails?.location)
        this.latitude = this.googlePlaceDetails?.latitude!!
        this.longitude = this.googlePlaceDetails?.longitude!!

        this.savedImageUriToInternalStorage = Uri.parse(this.googlePlaceDetails?.image)
        this.binding?.ivAddImageToPlace?.setImageURI(this.savedImageUriToInternalStorage)

        this.binding?.btnSavePlace?.text = "EDIT"
    }

    /** Method that checks if the location service of the device is enabled */
    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /** Method for requesting the location of the user */
    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestNewUserLocation(){
        val locationRequest = com.google.android.gms.location.LocationRequest()
        locationRequest.priority = LocationRequest.QUALITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.numUpdates = 1

        /** Already checked for permissions when we click 'btn_getCurrentLocation'! */
        this.fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    override fun onDestroy() {
        super.onDestroy()
        this.binding = null
    }
}