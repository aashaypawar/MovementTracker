package com.example.movementtracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.widget.Button
import android.widget.Chronometer
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    // Values and Variables for Coordinates
    private val pid = 42
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    var clat = 0.0
    var clong = 0.0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Timer Settings
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnPause = findViewById<Button>(R.id.btnPause)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val timer = findViewById<Chronometer>(R.id.Timer)
        var magic = 0L
        var isRunning: Boolean
        btnPause.isEnabled = false
        btnReset.isEnabled = false

        //Coordinates Settings
        val tvDistance = findViewById<TextView>(R.id.tvDistance)
        var distance = 0.0
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Button Start
        btnStart.setOnClickListener {
            timer.base = SystemClock.elapsedRealtime() + magic
            timer.start()
            btnStart.text = "Resume"
            btnStart.isEnabled = false
            btnPause.isEnabled = true
            btnReset.isEnabled = true
            isRunning = true
            Toast.makeText(applicationContext,"Stared",Toast.LENGTH_SHORT).show()

            var llat = clat
            var llong = clong
            Thread(Runnable{
                while(isRunning)
                {
                    repeat(10)
                    {
                        getLastLocation()
                    }
                    Thread.sleep(1000)
                    repeat(10)
                    {
                        getLastLocation()
                    }
                    Thread.sleep(1000)

                    val newlat = clat
                    val newlong = clong
                    val oldlat = llat
                    val oldlong = llong
                    llat = newlat
                    llong = newlong
                    var dist = 0.0

                    if(oldlat!=0.0)
                        dist = haversineformula(newlat,newlong,oldlat,oldlong)

                    distance += dist

                    runOnUiThread {
                        tvDistance.text = distance.toString() + "Km"
                    }
                }
            }).start()
        }

        //Button Pause
        btnPause.setOnClickListener {
            tvDistance.text = distance.toString() + "Km"
            timer.stop()
            magic = timer.base - SystemClock.elapsedRealtime()
            btnStart.isEnabled = true
            btnPause.isEnabled = false
            isRunning = false
            Toast.makeText(applicationContext,"Paused",Toast.LENGTH_SHORT).show()
        }

        //Button Reset
        btnReset.setOnClickListener {
            timer.stop()
            timer.base =  SystemClock.elapsedRealtime()
            btnStart.text = "Start"
            btnStart.isEnabled = true
            btnPause.isEnabled = false
            btnReset.isEnabled = false
            magic = 0L
            distance = 0.0
            isRunning = false
            tvDistance.text = "0.0Km"
        }
    }

    //Haversine Formula to find distance between two coordinates
    private fun haversineformula(lat1:Double, lon1:Double, lat2:Double, lon2:Double): Double {
        val r = 6371
        val dLat = deg2rad(lat2-lat1)
        val dLon = deg2rad(lon2-lon1)
        val a =
            sin(dLat/2) * sin(dLat/2) +
                    cos(deg2rad(lat1)) * cos(deg2rad(lat2)) *
                    sin(dLon/2) * sin(dLon/2)

        val c = 2 * atan2(sqrt(a), sqrt(1-a))
        return r * c
    }

    //Function to convert degree to radians for above formula
    private fun deg2rad(deg: Double): Double {
        return deg * (Math.PI/180)
    }


    //Last Location
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        clat = location.latitude
                        clong = location.longitude
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    //Request Location
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation
            clat = mLastLocation.latitude
            clong = mLastLocation.longitude
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            pid
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == pid) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }
}