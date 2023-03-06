package com.example.googleplacesapp.models

import android.os.Parcel
import android.os.Parcelable

/** MAKING A CLASS SERIALIZABLE MEANS THAT THIS CLASS CAN BE PASSED FROM ONE ACTIVITY/FRAGMENT TO ANOTHER -->
    --> USING 'intent.putExtra(string, Serializable)'*/

/** MAKING A CLASS PARCELABLE HAS THE SAME FUNCTION AS SERIALIZABLE BUT MORE EFFICIENT AND FASTER!!! */
data class GooglePlaceModel(val id: Int,
                            val title: String?,
                            val image: String?,
                            val description: String?,
                            val date: String?,
                            val location: String?,
                            val latitude: Double,
                            val longitude: Double) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readDouble(),
        parcel.readDouble()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(title)
        parcel.writeString(image)
        parcel.writeString(description)
        parcel.writeString(date)
        parcel.writeString(location)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GooglePlaceModel> {
        override fun createFromParcel(parcel: Parcel): GooglePlaceModel {
            return GooglePlaceModel(parcel)
        }

        override fun newArray(size: Int): Array<GooglePlaceModel?> {
            return arrayOfNulls(size)
        }
    }
}
