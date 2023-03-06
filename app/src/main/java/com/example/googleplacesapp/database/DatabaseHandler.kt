package com.example.googleplacesapp.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import com.example.googleplacesapp.models.GooglePlaceModel
import kotlinx.coroutines.Job


/** DATABASE HANDLER FOR SQLITE, MUST TO INHERIT FROM 'SQLiteOpenHelper()'
 * INSTEAD OF USING ROOM DATABASE WHERE WE MUST CREATE ENTITY-DAO-DATABASE  -->
   --> WITH 'SQLiteOpenHelper()' WE NEED TO CREATE JUST MODEL-DATABASE HANDLER*/
class DatabaseHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object{
        private const val DATABASE_NAME = "GooglePlacesDatabase"
        private const val DATABASE_VERSION = 1
        private const val DATABASE_TABLE_NAME = "GooglePlacesTable"

        // Primary Key and Columns for the database table
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_IMAGE = "image"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_DATE = "date"
        private const val KEY_LOCATION = "location"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        /** CREATING TABLES WITH FIELDS(COLUMNS)! --> THIS IS SQLITE COMMANDS FOR CREATING TABLES! */
        val createGooglePlaceTable = ("CREATE TABLE " + DATABASE_TABLE_NAME + " ("
                + KEY_ID + " INTEGER PRIMARY KEY, "
                + KEY_TITLE + " TEXT, "
                + KEY_IMAGE + " TEXT,"
                + KEY_DESCRIPTION + " TEXT, "
                + KEY_DATE + " TEXT,"
                + KEY_LOCATION + " TEXT, "
                + KEY_LATITUDE + " TEXT, "
                + KEY_LONGITUDE + " TEXT)")

        db?.execSQL(createGooglePlaceTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS $DATABASE_TABLE_NAME")
        onCreate(db)
    }

    /** Function to insert Google Place AS ROW in the SQLite database(IN THE ALREADY CREATED TABLE)!!! */
    fun addGooglePlace(googlePlace: GooglePlaceModel): Long{
        val database = this.writableDatabase      /** Object to access the SQLite database for writing/reading */

        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE, googlePlace.title)
        contentValues.put(KEY_IMAGE, googlePlace.image)
        contentValues.put(KEY_DESCRIPTION, googlePlace.description)
        contentValues.put(KEY_DATE, googlePlace.date)
        contentValues.put(KEY_LOCATION, googlePlace.location)
        contentValues.put(KEY_LATITUDE, googlePlace.latitude)
        contentValues.put(KEY_LONGITUDE, googlePlace.longitude)

        // Inserting Row
        /** @param nullColumnHack: is String containing nullColumnHack */
        val result = database.insert(DATABASE_TABLE_NAME, null, contentValues)

        database.close()
        return result
    }

    /** Function to update Google Place */
    fun updateGooglePlace(googlePlace: GooglePlaceModel): Int{
        val database = this.writableDatabase      /** Object to access the SQLite database for writing/reading */

        val contentValues = ContentValues()
        contentValues.put(KEY_TITLE, googlePlace.title)
        contentValues.put(KEY_IMAGE, googlePlace.image)
        contentValues.put(KEY_DESCRIPTION, googlePlace.description)
        contentValues.put(KEY_DATE, googlePlace.date)
        contentValues.put(KEY_LOCATION, googlePlace.location)
        contentValues.put(KEY_LATITUDE, googlePlace.latitude)
        contentValues.put(KEY_LONGITUDE, googlePlace.longitude)

        val result = database.update(DATABASE_TABLE_NAME, contentValues, KEY_ID + "=" + googlePlace.id, null)

        database.close()
        return result
    }

    /** Function to delete Place */
    fun deleteGooglePlace(placeToDelete: GooglePlaceModel): Int{
        val database = this.writableDatabase
        val result = database.delete(DATABASE_TABLE_NAME, KEY_ID + "=" + placeToDelete.id, null)
        database.close()
        return result
    }


    /** Function to get Google Places from SQLite database(FROM THE FILLED TABLE)!!! */
    @SuppressLint("Range")
    fun getSavedGooglePlacesList(): ArrayList<GooglePlaceModel>{
        val googlePlaceList = ArrayList<GooglePlaceModel>()
        val selectQuery = "SELECT * FROM $DATABASE_TABLE_NAME"
        val database = this.readableDatabase                    // Get Instance of the SQLite database just for reading

        try {
            /** CURSOR INTERFACE PROVIDES RANDOM READ-WRITE ACCESS TO THE RESULT SET RETURNED BY A DATABASE QUERY. */
            val cursor: Cursor = database.rawQuery(selectQuery, null)
            if (cursor.moveToFirst()){      // Move the cursor to the first row and start reading the data in the table
                do{
                    // For every single entry(entity) we create google place
                    val currPlace = GooglePlaceModel(
                        cursor.getInt(cursor.getColumnIndex(KEY_ID)),
                        cursor.getString(cursor.getColumnIndex(KEY_TITLE)),
                        cursor.getString(cursor.getColumnIndex(KEY_IMAGE)),
                        cursor.getString(cursor.getColumnIndex(KEY_DESCRIPTION)),
                        cursor.getString(cursor.getColumnIndex(KEY_DATE)),
                        cursor.getString(cursor.getColumnIndex(KEY_LOCATION)),
                        cursor.getDouble(cursor.getColumnIndex(KEY_LATITUDE)),
                        cursor.getDouble(cursor.getColumnIndex(KEY_LONGITUDE))
                    )
                    googlePlaceList.add(currPlace)
                }while (cursor.moveToNext())
            }
            cursor.close()                    // Must close the cursor so our program don't run to errors
        }catch (e: SQLiteException){
            database.execSQL(selectQuery)
            return ArrayList()
        }

        return googlePlaceList
    }
}