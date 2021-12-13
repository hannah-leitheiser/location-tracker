package com.example.locationtracker
import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.GnssStatus
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import android.os.*
import android.telephony.*
import android.os.PowerManager

lateinit var saveFile : LocalJSONFileManager
var satellitesUsed = 0
var GPSFixCount = 0
var NETFixCount = 0
var FusedFixCount = 0


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

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val locListen  = MyLocationListener()
    private lateinit var locationManager: LocationManager
    private lateinit var tvGpsLocation: TextView
    private val locationPermissionCode = 2
    private val TAG = "MainActivity"
    private val LAST_OPENED_URI_KEY =
        "com.example.android.actionopendocument.pref.LAST_OPENED_URI_KEY"
    private lateinit var sensorManager: SensorManager
    private var mSensor: Sensor? = null
    private lateinit var wifiManager: WifiManager
    private lateinit var telephony : TelephonyManager
    //private lateinit var wakeLock: PowerManager.WakeLock
    var GPSFixes = 0
    var NETFixes = 0
    var Pressure = 0
    var CellScan = 0
    var FusedFixes = 0
    var WIFIScan = 0
    var Satellites = 0
    var Networks = 0
    var Towers = 0
    var Seconds = 0


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


        saveFile = LocalJSONFileManager(this, "myfile")

        //val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocationTracker:wakelock")
        //wakeLock.acquire()

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

                /*if( !wakeLock.isHeld() ) {
                    wakeLock.acquire()
                    findViewById<TextView>(R.id.textStatus).text = "Attempting to Aquire Wake Lock"

                }
                if(wakeLock.isHeld()) {
                    findViewById<TextView>(R.id.textStatus).text = "Wake Lock Held"
                }*/

                Seconds = Seconds + 2

                var success = wifiManager.startScan()

                if (success) {
                // scan failure handling
                    scanSuccess()
                }



                    findViewById<TextView>(R.id.info).text =
                                "-------------------------------------------\n" +
                                "              Stored Data                       \n\n" +
                                "GPSFixes    : " + GPSFixCount.toString() + "\n" +
                                "NETFixes    : " + NETFixCount.toString() + "\n" +
                                "Fused Fixes : " + FusedFixCount.toString() + "\n" +
                                "Wifi Scan   : " + WIFIScan.toString() + "\n" +
                                "CellScan    : " + CellScan.toString() + "\n" +
                                "Pressure    : " + Pressure.toString() + "\n\n" +
                                "-------------------------------------------\n" +
                                "                Current Status                   \n\n" +
                                "Networks    : " + Networks.toString() + "\n" +
                                "GPS Sats    : " + satellitesUsed.toString() + "\n" +
                                "Cell Towers : " + Towers.toString() + "\n" +
                                "Seconds     : " + Seconds.toString()





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


                createFile(MediaStore.Files.getContentUri("external"))

                GPSFixes = 0
                NETFixes = 0
                Pressure = 0
                CellScan = 0
                FusedFixes = 0
                WIFIScan = 0











            }

        }
        )

    }

    private fun getLocation() {
        Intent(this, MyLocationListener::class.java).also { intent ->
            startService(intent)
        }

    }


             /*   var dataPayloadMutable : MutableList<Array<Array<String>>> =
                    mutableListOf <Array<Array<String>>>( )

                var cells = telephony.allCellInfo
                val tsLong = System.currentTimeMillis()
                val bootTime = tsLong - (SystemClock.elapsedRealtimeNanos() / 1000000L)
                var earliestTimestamp = System.currentTimeMillis()
                Towers = cells.size
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
                CellScan = CellScan + 1
                openFileOutput(filename, Context.MODE_APPEND).use {
                    it.write(fileContents.toByteArray())
                }



            }
*/
        //} )

    //}






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

        //val fileContents = formatJSONData( android.os.Build.MODEL, System.currentTimeMillis(), "pressure", meas)
        //val filename = "myfile"

        //openFileOutput(filename, Context.MODE_APPEND).use {
        //    it.write(fileContents.toByteArray())
        //}
        Pressure = Pressure + 1

    }

    override fun onResume() {
        super.onResume()
        mSensor?.also { light ->
            sensorManager.registerListener(this, light, 1000000)
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
        Networks = results.size
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

        //val fileContents = formatJSONData( android.os.Build.MODEL, earliestTimestamp, "wifi scan", dataPayloadMutable.toTypedArray() )

        //val filename = "myfile"

        //openFileOutput(filename, Context.MODE_APPEND).use {
        //    it.write(fileContents.toByteArray())
        //}
        WIFIScan = WIFIScan + 1



    }

    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        // val results = wifiManager.scanResults
    }

}