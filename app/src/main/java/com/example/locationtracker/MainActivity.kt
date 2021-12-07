package com.example.locationtracker
import android.Manifest
import android.app.Activity
import android.content.*
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

fun formatJSONData( phoneName : String, timeStampms : Long, measurementType : String, propertyArray : Array<Array<Array<String>>>): String {
    var outputJSON = "*".repeat(32) + "\n" +
                     "{ \"source\"            : \"" + phoneName + "\",\n" +
                     "  \"timestamp\"         : \"" + timeStampms.toString() + "\",\n" +
                     "  \"measurement_type\"  : \"" + measurementType + "\",\n"

    outputJSON = outputJSON +
                     "  \"data\"              : [ \n"

    for(ii in 0..propertyArray.size-1) {
        outputJSON = outputJSON + "  {\n"
        for (i in 0..propertyArray[ii].size - 1) {

            outputJSON =
                outputJSON + "     \"" + propertyArray[ii][i][0] + "\" : \"" + propertyArray[ii][i][1] + "\""
            if (i < propertyArray[ii].size - 1)
                outputJSON = outputJSON + ",\n"
            else
                outputJSON = outputJSON + " ] \n"
        }
        outputJSON = outputJSON + "}\n\n"

    }
    outputJSON = outputJSON + "]\n}\n"
    return outputJSON
}




const val CREATE_FILE = 1


class MainActivity : AppCompatActivity(), LocationListener, SensorEventListener {
    private lateinit var locationManager: LocationManager
    private lateinit var tvGpsLocation: TextView
    private val locationPermissionCode = 2
    private val TAG = "MainActivity"
    private val LAST_OPENED_URI_KEY =
        "com.example.android.actionopendocument.pref.LAST_OPENED_URI_KEY"
    private lateinit var sensorManager: SensorManager
    private var mSensor: Sensor? = null
    private lateinit var wifiManager: WifiManager

    //val contentResolver  = applicationContext.contentResolver


    fun alterDocument(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {


                FileOutputStream(it.fileDescriptor).use {
                    val theData = openFileInput("myfile")
                    theData.copyTo(it)

                }

            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    override fun onActivityResult(
        requestCode: Int, resultCode: Int, resultData: Intent?
    ) {
        if (requestCode == CREATE_FILE
            && resultCode == Activity.RESULT_OK
        ) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            resultData?.data?.also { uri ->

                alterDocument(uri)
                // Perform operations on the document using its URI.
                val fileContents = ""
                openFileOutput("myfile", Context.MODE_PRIVATE).use {
                    it.write(fileContents.toByteArray())
                }

            }
        }
    }

    private fun createFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/text"
            putExtra(Intent.EXTRA_TITLE, "data.txt")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(intent, CREATE_FILE)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val context : Context = applicationContext

        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiScanReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                var success = wifiManager.startScan()

                if (success) {
                // scan failure handling
                    scanSuccess()
                }

                mainHandler.postDelayed(this, 2000)
            }
        })

        //val success = wifiManager.startScan()



        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {

            val gravSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_PRESSURE)
            // Use the version 3 gravity sensor.
            mSensor = gravSensors[0]

            // Success! There's a magnetometer.
        } else {
            // Failure! No magnetometer.
        }


        getLocation()

        var button = findViewById<Button>(R.id.button)

        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                //your implementation goes here

                openFileInput("myfile").bufferedReader().useLines {

                    var tt = findViewById<TextView>(R.id.info)
                    var text = ""
                    for (item in it)
                        text = text + item.toString()

                    tt.text = text.toString()

                }

                createFile(MediaStore.Files.getContentUri("external"))












            }

        }
        )

    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)
        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0f, this)

    }

    override fun onLocationChanged(location: Location) {


        val data = arrayOf(
            arrayOf (
                arrayOf( "latitude" ,           location.latitude.toString()),
                arrayOf( "longitude",           location.longitude.toString()),
                arrayOf( "altitude",            location.altitude.toString()),
                arrayOf( "speed",               location.speed.toString()),
                arrayOf( "bearing",             location.bearing.toString()),
                arrayOf( "accuracy, position",  location.accuracy.toString()),
                arrayOf( "accuracy, vertical",  location.verticalAccuracyMeters.toString()),
                arrayOf( "accuracy, speed",     location.speedAccuracyMetersPerSecond.toString()),
                arrayOf( "accuracy, bearing",   location.bearingAccuracyDegrees.toString()),
                //arrayOf( "extras",              location.extras.toString())
                ))

        val loc = location.elapsedRealtimeNanos / 1000000L
        val tsLong = System.currentTimeMillis()
        val bootTime = tsLong - (SystemClock.elapsedRealtimeNanos() / 1000000L)
        val timeStamps = (loc + bootTime)
        val delta =
            (location.elapsedRealtimeNanos / 1000L) - (SystemClock.elapsedRealtimeNanos() / 1000L)

        val fileContents = formatJSONData( android.os.Build.MODEL, timeStamps, "location - " + location.provider.toString(), data)

        val filename = "myfile"

        openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(fileContents.toByteArray())
        }


    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        val lux = event.values[0]
        // Do something with this sensor value.
        val meas = arrayOf (
                    arrayOf (
                        arrayOf( "Value", lux.toString() ) ) )

        val fileContents = formatJSONData( android.os.Build.MODEL, System.currentTimeMillis(), "pressure", meas)
        val filename = "myfile"

        openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(fileContents.toByteArray())
        }

    }

    override fun onResume() {
        super.onResume()
        mSensor?.also { light ->
            sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }


    private fun scanSuccess() {

        val tsLong = System.currentTimeMillis()

        val bootTime = tsLong - (SystemClock.elapsedRealtimeNanos() / 1000000L)
        var earliestTimestamp = -1L
        val results = wifiManager.scanResults
        var dataPayloadMutable : MutableList<Array<Array<String>>> =
             mutableListOf <Array<Array<String>>>( )
        for(i in 0..results.size) {
            val wifiTimestamp = (results[0].timestamp + bootTime)
            if ( (earliestTimestamp == -1L) || (wifiTimestamp < earliestTimestamp) )
                earliestTimestamp = wifiTimestamp
            dataPayloadMutable.add(
                arrayOf(
                    arrayOf("BSSID", results[0].BSSID.toString()),
                    arrayOf("SSID", results[0].SSID.toString()),
                    arrayOf("level", results[0].level.toString()),
                    arrayOf("frequency", results[0].frequency.toString()),
                    arrayOf("timestamp", wifiTimestamp.toString())
                )
            )
        }

        val fileContents = formatJSONData( android.os.Build.MODEL, earliestTimestamp, "wifi scan", dataPayloadMutable.toTypedArray() )

        val filename = "myfile"

        openFileOutput(filename, Context.MODE_APPEND).use {
            it.write(fileContents.toByteArray())
        }



    }

    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        // val results = wifiManager.scanResults
    }

}