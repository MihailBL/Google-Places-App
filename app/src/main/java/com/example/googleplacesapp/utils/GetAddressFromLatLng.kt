package com.example.googleplacesapp.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import java.util.*

class GetAddressFromLatLng(context: Context, private val latitude: Double, private var longitude: Double) : AsyncTask<Void, String, String>() {
    /** GeoCoder gets readable address from latitude and longitude */
    private var geoCoder: Geocoder = Geocoder(context, Locale.getDefault())
    private lateinit var addressListener : AddressListener

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg p0: Void?): String {
        try {
            val addressList: List<Address>? = geoCoder.getFromLocation(this.latitude, this.longitude, 1)

            if (addressList != null && addressList.isNotEmpty()){
                val address = addressList[0]
                val sb = StringBuilder()
                for (i in 0..address.maxAddressLineIndex){
                    sb.append(address.getAddressLine(i)).append(" ")
                }
                sb.deleteCharAt(sb.length-1)
                return sb.toString()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return ""
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("super.onPostExecute(result)", "android.os.AsyncTask")
    )
    override fun onPostExecute(resultString: String?) {
        if(resultString == null){
            addressListener.onError()
        }
        else{
            addressListener.onAddressFound(resultString)
        }
        super.onPostExecute(resultString)
    }

    /** 'execute()' METHOD MUST BE CALLED IN ORDER TO EXECUTE ASYNC-TASK CLASSES!! */
    fun getAddress(){
        execute()
    }

    /** Get the reference of overrode interface in the 'AddGoogleMapsPlaceActivity' */
    fun setAddressListener(addressListener: AddressListener){
        this.addressListener = addressListener
    }


    /** Override this interface's functions in 'AddGoogleMapsPlaceActivity' */
    interface AddressListener{
        //  What happens if we found the address
        fun onAddressFound(address: String?)
        // What shall happen if we get error
        fun onError()
    }
}