package com.example.locationtracker
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var locationManager: LocationManager
    private lateinit var tvGpsLocation: TextView
    private val locationPermissionCode = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

            getLocation()

        var button = findViewById<Button>( R.id.button)

        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                //your implementation goes here

                openFileInput("myfile").bufferedReader().useLines {

                        var tt = findViewById<TextView>(R.id.info)
                        var text=""
                        for(item in it)
                            text=text+item.toString()

                      tt.text = text.toString()

                    }

                val fileContents = ""
                openFileOutput("myfile", Context.MODE_PRIVATE).use {
                    it.write(fileContents.toByteArray())
                }


                }

            }
        )

    }
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0f, this)

    }
    override fun onLocationChanged(location: Location) {

        val loc = location.elapsedRealtimeNanos / 1000000L
        val tsLong = System.currentTimeMillis()
        val bootTime = tsLong - (SystemClock.elapsedRealtimeNanos() / 1000000L)
        val timeStamps = (loc + bootTime)
        val delta = (location.elapsedRealtimeNanos / 1000L) - (SystemClock.elapsedRealtimeNanos() / 1000L)

        val fileContents = "       " +(timeStamps/1000L).toString() + "\n"
        val filename = "myfile"



        val ts = tsLong.toString()
        openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(fileContents.toByteArray())
        }


    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
