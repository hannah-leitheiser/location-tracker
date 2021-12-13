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
import android.location.GnssStatus
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
import android.location.GpsSatellite
import android.R.string.no
import android.telephony.*

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

            if (propertyArray[ii][i][1] != "" && propertyArray[ii][i][1] != "{}" && propertyArray[ii][i][1] != "UNAVALIABLE" && propertyArray[ii][i][1] != "null"
                && propertyArray[ii][i][1] != "NONE/UNKNOWN") {
                outputJSON =
                    outputJSON + "     \"" + propertyArray[ii][i][0] + "\" : \"" + propertyArray[ii][i][1] + "\",\n"
            }
        }
        if( outputJSON.substring( outputJSON.length-2)  == ",\n" )
            outputJSON = outputJSON.substring( 0, outputJSON.length-2) + "\n"
        if( ii < propertyArray.size - 1)
                outputJSON = outputJSON + "  }, \n"
        else
                outputJSON = outputJSON + "  }\n"

    }
    outputJSON = outputJSON + " ]\n}\n"
    return outputJSON
}

fun formatResult(result : Int, specialValues : Array<Int>, specials : Array<String>) : String {
    for(i in 0..specials.size-1) {
        if(result == specialValues[i])
            return specials[i]
    }
    return result.toString()
}

fun formatResultL(result : Long, specialValues : Array<Long>, specials : Array<String>) : String {
    for(i in 0..specials.size-1) {
        if(result == specialValues[i])
            return specials[i]
    }
    return result.toString()
}


fun printIntArray( a : IntArray ) : String {
    var output = ""
    for(i in 0..a.size-1)
    {
        if(i < a.size-1) output = output + a[i].toString() + ", "
        else output = output + a[i].toString()

    }
    return output
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
    private var gnss : GnssStatus? = null
    private lateinit var telephony : TelephonyManager

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


        telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager




        val context : Context = applicationContext

        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val wifiScanReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    //scanSuccess()
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
        locationManager.registerGnssStatusCallback ( object : GnssStatus.Callback() {


            override fun onSatelliteStatusChanged( s : GnssStatus)
            {
                gnss = s
                var dataPayloadMutable : MutableList<Array<Array<String>>> =
                    mutableListOf <Array<Array<String>>>( )

                var cells = telephony.allCellInfo
                val tsLong = System.currentTimeMillis()
                val bootTime = tsLong - (SystemClock.elapsedRealtimeNanos() / 1000000L)
                var earliestTimestamp = System.currentTimeMillis()
                for(tower in cells) {
                    val towerTimestamp = ((tower.timestampMillis / 1L) + bootTime)
                    if (  (towerTimestamp < earliestTimestamp) )
                        earliestTimestamp = towerTimestamp

                    val towerInfo = tower.cellIdentity
                    val signalStrength = tower.cellSignalStrength
                    if(towerInfo is CellIdentityCdma) {
                        val t = towerInfo as CellIdentityCdma
                        val s = signalStrength as CellSignalStrength
                        dataPayloadMutable.add(
                            arrayOf(
                                arrayOf("operator, long", t.operatorAlphaLong.toString()),
                                arrayOf("operator, short", t.operatorAlphaShort.toString()),
                                arrayOf("CDMA base station id", formatResult( t.basestationId, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("CDMA latitude", formatResult( t.latitude, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("CDMA longitude", formatResult( t.longitude, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("CDMA network id", formatResult( t.networkId, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("CDMA system id", formatResult( t.systemId, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("strength, asu", s.asuLevel.toString() ),
                                arrayOf("strength, dbm", s.dbm.toString() ),
                                arrayOf("strength, level", formatResult( s.level, arrayOf(CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                   CellSignalStrength.SIGNAL_STRENGTH_GREAT, CellSignalStrength.SIGNAL_STRENGTH_MODERATE, CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                   CellSignalStrength.SIGNAL_STRENGTH_POOR), arrayOf("GOOD","GREAT","MODERATE","NONE/UNKNOWN","POOR"))),
                                arrayOf("timestamp", towerTimestamp.toString())
                            )
                        )
                    }
                    if(towerInfo is CellIdentityGsm && signalStrength is CellSignalStrengthGsm) {
                        val t = towerInfo as CellIdentityGsm
                        val s = signalStrength as CellSignalStrengthGsm
                        dataPayloadMutable.add(
                            arrayOf(
                                arrayOf("operator, long", t.operatorAlphaLong.toString()),
                                arrayOf("operator, short", t.operatorAlphaShort.toString()),
                                arrayOf("GSM absolute RF channel number", formatResult( t.arfcn, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("GSM base station identity code", formatResult( t.bsic, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("GSM cell identity", formatResult( t.cid, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("GSM location area", formatResult( t.lac, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("GSM mobile country code", t.mccString.toString()),
                                arrayOf("GSM mobile network code", t.mncString.toString()),
                                arrayOf("GSM mobile network operator", t.mobileNetworkOperator.toString()),
                                arrayOf("GSM additional PLMMs", t.additionalPlmns.toString()),
                                arrayOf("strength, asu", s.asuLevel.toString() ),
                                arrayOf("strength, dbm", s.dbm.toString() ),
                                arrayOf("strength, level", formatResult( s.level, arrayOf(CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT, CellSignalStrength.SIGNAL_STRENGTH_MODERATE, CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR), arrayOf("GOOD","GREAT","MODERATE","NONE/UNKNOWN","POOR"))),
                                arrayOf("timing advance", formatResult( s.timingAdvance, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("timestamp", towerTimestamp.toString())
                            )
                        )
                    }
                    if(towerInfo is CellIdentityLte && signalStrength is CellSignalStrengthLte) {
                        val t = towerInfo as CellIdentityLte
                        val s = signalStrength as CellSignalStrengthLte
                        dataPayloadMutable.add(
                            arrayOf(
                                arrayOf("operator, long", t.operatorAlphaLong.toString()),
                                arrayOf("operator, short", t.operatorAlphaShort.toString()),
                                arrayOf("LTE bands", printIntArray(t.bands)),
                                arrayOf("LTE bandwidth, kHz", formatResult( t.bandwidth, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("LTE cell identity", formatResult( t.ci, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("LTE closed subscriber group", t.closedSubscriberGroupInfo.toString()),
                                arrayOf("LTE absolute RF channel number", formatResult( t.earfcn, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("LTE mobile country code", t.mccString.toString()),
                                arrayOf("LTE mobile network code", t.mncString.toString()),
                                arrayOf("LTE mobile network operator", t.mobileNetworkOperator.toString()),
                                arrayOf("LTE physical cell id", formatResult( t.pci, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("LTE tracking area code", formatResult( t.tac, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("LTE additional PLMMs", t.additionalPlmns.toString()),
                                arrayOf("strength, asu", s.asuLevel.toString() ),
                                arrayOf("strength, dbm", s.dbm.toString() ),
                                arrayOf("strength, level", formatResult( s.level, arrayOf(CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT, CellSignalStrength.SIGNAL_STRENGTH_MODERATE, CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR), arrayOf("GOOD","GREAT","MODERATE","NONE/UNKNOWN","POOR"))),
                                arrayOf("timing advance", formatResult( s.timingAdvance, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("timestamp", towerTimestamp.toString())
                            )
                        )
                    }
                    if(towerInfo is CellIdentityNr && signalStrength is CellSignalStrengthNr) {
                        val t = towerInfo as CellIdentityNr
                        val s = signalStrength as CellSignalStrengthNr
                        dataPayloadMutable.add(
                            arrayOf(
                                arrayOf("operator, long", t.operatorAlphaLong.toString()),
                                arrayOf("operator, short", t.operatorAlphaShort.toString()),
                                arrayOf("NR bands", printIntArray(t.bands)),
                                arrayOf("NR mobile country code", t.mccString.toString()),
                                arrayOf("NR mobile network code", t.mncString.toString()),
                                arrayOf("NR cell identity", formatResultL( t.nci, arrayOf(CellInfo.UNAVAILABLE_LONG), arrayOf("UNAVALIABLE"))),
                                arrayOf("NR absolute RF channel number", formatResult( t.nrarfcn, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("NR physical cell id", formatResult( t.pci, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("NR tracking area code", formatResult( t.tac, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("NR additional PLMMs", t.additionalPlmns.toString()),
                                arrayOf("strength, asu", s.asuLevel.toString() ),
                                arrayOf("strength, dbm", s.dbm.toString() ),
                                arrayOf("strength, level", formatResult( s.level, arrayOf(CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT, CellSignalStrength.SIGNAL_STRENGTH_MODERATE, CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR), arrayOf("GOOD","GREAT","MODERATE","NONE/UNKNOWN","POOR"))),
                                arrayOf("timestamp", towerTimestamp.toString())
                            )
                        )
                    }
                    if(towerInfo is CellIdentityTdscdma && signalStrength is CellSignalStrengthTdscdma) {
                        val t = towerInfo as CellIdentityTdscdma
                        val s = signalStrength as CellSignalStrengthTdscdma
                        dataPayloadMutable.add(
                            arrayOf(
                                arrayOf("operator, long", t.operatorAlphaLong.toString()),
                                arrayOf("operator, short", t.operatorAlphaShort.toString()),
                                arrayOf("TD-SCDMA cell id", formatResult( t.cid, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("TD-SCDMA closed subscriber group", t.closedSubscriberGroupInfo.toString()),
                                arrayOf("TD-SCDMA cell parameters id", formatResult( t.cpid, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("TD-SCDMA local area code", formatResult( t.lac, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("TD-SCDMA mobile country code", t.mccString.toString()),
                                arrayOf("TD-SCDMA mobile network code", t.mncString.toString()),
                                arrayOf("TD-SCDMA mobile network operator", t.mobileNetworkOperator.toString()),
                                arrayOf("TD-SCDMA absolute RF channel number", formatResult( t.uarfcn, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("TD-SCDMA cell parameters id", formatResult( t.cpid, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("TD-SCDMA additional PLMMs", t.additionalPlmns.toString()),
                                arrayOf("strength, asu", s.asuLevel.toString() ),
                                arrayOf("strength, dbm", s.dbm.toString() ),
                                arrayOf("strength, level", formatResult( s.level, arrayOf(CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT, CellSignalStrength.SIGNAL_STRENGTH_MODERATE, CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR), arrayOf("GOOD","GREAT","MODERATE","NONE/UNKNOWN","POOR"))),
                                arrayOf("timestamp", towerTimestamp.toString())
                            )
                        )
                    }
                    if(towerInfo is CellIdentityWcdma && signalStrength is CellSignalStrengthWcdma) {
                        val t = towerInfo as CellIdentityWcdma
                        val s = signalStrength as CellSignalStrengthWcdma
                        dataPayloadMutable.add(
                            arrayOf(
                                arrayOf("operator, long", t.operatorAlphaLong.toString()),
                                arrayOf("operator, short", t.operatorAlphaShort.toString()),
                                arrayOf("WCDMA additional PLMMs", t.additionalPlmns.toString()),
                                arrayOf("WCDMA cell id", formatResult( t.cid, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("WCDMA closed subscriber group", t.closedSubscriberGroupInfo.toString()),
                                arrayOf("WCDMA location area code", formatResult( t.lac, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("WCDMA mobile country code", t.mccString.toString()),
                                arrayOf("WCDMA mobile network code", t.mncString.toString()),
                                arrayOf("WCDMA primary scrambling code", formatResult( t.psc, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("WCDMA absolute RF channel number", formatResult( t.uarfcn, arrayOf(CellInfo.UNAVAILABLE), arrayOf("UNAVALIABLE"))),
                                arrayOf("strength, asu", s.asuLevel.toString() ),
                                arrayOf("strength, dbm", s.dbm.toString() ),
                                arrayOf("strength, level", formatResult( s.level, arrayOf(CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT, CellSignalStrength.SIGNAL_STRENGTH_MODERATE, CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR), arrayOf("GOOD","GREAT","MODERATE","NONE/UNKNOWN","POOR"))),
                                arrayOf("timestamp", towerTimestamp.toString())
                            )
                        )
                    }

                }

                val fileContents = formatJSONData( android.os.Build.MODEL, earliestTimestamp, "cell scan", dataPayloadMutable.toTypedArray() )

                val filename = "myfile"

                openFileOutput(filename, Context.MODE_APPEND).use {
                    it.write(fileContents.toByteArray())
                }



            }

        } )

    }

    override fun onLocationChanged(location: Location) {

        var satsTotal = 0
        var satsUsed = 0
        var data = arrayOf( arrayOf( arrayOf( "", "" )))
        if (location.provider == "gps" && gnss != null) {
            for (sat in 0..gnss!!.satelliteCount-1) {
                satsTotal++
                if (gnss!!.usedInFix(sat)) satsUsed++
                //if (sat.getSnr() != NO_DATA) satsInView++;
            }
            data = arrayOf(
                arrayOf(
                    arrayOf("latitude", location.latitude.toString()),
                    arrayOf("longitude", location.longitude.toString()),
                    arrayOf("altitude", location.altitude.toString()),
                    arrayOf("speed", location.speed.toString()),
                    arrayOf("bearing", location.bearing.toString()),
                    arrayOf("accuracy, position", location.accuracy.toString()),
                    arrayOf("accuracy, vertical", location.verticalAccuracyMeters.toString()),
                    arrayOf("accuracy, speed", location.speedAccuracyMetersPerSecond.toString()),
                    arrayOf("accuracy, bearing", location.bearingAccuracyDegrees.toString()),
                    arrayOf("satellites, total", satsTotal.toString()),
                    arrayOf("satellites, used", satsUsed.toString())
                )
            )

        } else {
            data = arrayOf(
                arrayOf(
                    arrayOf("latitude", location.latitude.toString()),
                    arrayOf("longitude", location.longitude.toString()),
                    arrayOf("altitude", location.altitude.toString()),
                    arrayOf("speed", location.speed.toString()),
                    arrayOf("bearing", location.bearing.toString()),
                    arrayOf("accuracy, position", location.accuracy.toString()),
                    arrayOf("accuracy, vertical", location.verticalAccuracyMeters.toString()),
                    arrayOf("accuracy, speed", location.speedAccuracyMetersPerSecond.toString()),
                    arrayOf("accuracy, bearing", location.bearingAccuracyDegrees.toString()),
                )
            )

        }


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
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            /*if (gnss == null) {
            } else {
                for (t in 0..gnss!!.satelliteCount-1) {
                    val text = gnss!!.getCarrierFrequencyHz(t).toString() + "  |  "
                    it.write(text.toByteArray())
                }
            }*/
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
        var earliestTimestamp = System.currentTimeMillis()
        val results = wifiManager.scanResults
        var dataPayloadMutable : MutableList<Array<Array<String>>> =
             mutableListOf <Array<Array<String>>>( )
        for(i in 0..results.size-1) {
            val wifiTimestamp = ((results[0].timestamp / 1000L) + bootTime)
            if (  (wifiTimestamp < earliestTimestamp) )
                earliestTimestamp = wifiTimestamp
            dataPayloadMutable.add(
                arrayOf(
                    arrayOf("BSSID", results[i].BSSID.toString()),
                    arrayOf("SSID", results[i].SSID.toString()),
                    arrayOf("level", results[i].level.toString()),
                    arrayOf("frequency", results[i].frequency.toString()),
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