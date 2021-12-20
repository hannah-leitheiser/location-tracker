package com.example.locationtracker
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import android.os.*
import android.view.WindowManager
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.text.format.DateFormat
import java.util.*
import android.content.DialogInterface





lateinit var saveFile : LocalJSONFileManager
var satellitesUsed = 0
var GPSFixCount = 0
var NETFixCount = 0
var GPSAccuracy =0
var FusedFixCount = 0
var WIFIScanCount = 0
var Networks=0
var CellScanCount = 0
var Towers = 0
var Pressure = 0
var PressureC = 0f
var Acceleration = 0
var Accel : FloatArray = floatArrayOf( 0f,0f,0f)
var bestWifi : String = ""


const val CREATE_FILE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var tvGpsLocation: TextView
    private val locationPermissionCode = 2
    private val TAG = "MainActivity"
    private val LAST_OPENED_URI_KEY =
        "com.example.android.actionopendocument.pref.LAST_OPENED_URI_KEY"

    private lateinit var wl: PowerManager.WakeLock
    var Seconds = 0f


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
            val pattern = "yyyy-MM-dd"
            val simpleDateFormat = SimpleDateFormat(pattern)
            val date: String = simpleDateFormat.format(Date())

            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/text"
            putExtra(Intent.EXTRA_TITLE, date+"_"+android.os.Build.MODEL+"_DATA.txt")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(intent, CREATE_FILE)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        val WMLP = window.attributes
        WMLP.screenBrightness = 0f
        window.attributes = WMLP

        saveFile = LocalJSONFileManager(this, "myfile")


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

                Seconds = Seconds + 0.2f




                findViewById<TextView>(R.id.info).text =
                    "-------------------------------------------\n" +
                            "              Stored Data                       \n\n" +
                            "GPSFixes    : " + GPSFixCount.toString() + "\n" +
                            "NETFixes    : " + NETFixCount.toString() + "\n" +
                            "Wifi Scan   : " + WIFIScanCount.toString() + "\n" +
                            "CellScan    : " + CellScanCount.toString() + "\n" +
                            "Pressure    : " + Pressure.toString() + "\n" +
                            "Accel    : " + Acceleration.toString() + "\n\n" +
                            "-------------------------------------------\n" +
                            "                Current Status                   \n\n" +
                            "Networks    : " + Networks.toString() + "\n" +
                            "Best    : " + bestWifi.toString() + "\n" +
                            "GPS Sats    : " + satellitesUsed.toString() + "\n" +
                            "Cell Towers : " + Towers.toString() + "\n" +
                            "Pressure : " + PressureC.toString() + "\n" +
                            "Accel: %.2f %.2f %.2f".format(Accel[0], Accel[1], Accel[2]) + "\n" +
                            "Seconds: %.1f".format(Seconds)





                mainHandler.postDelayed(this, 200)
            }
        })

        //val success = wifiManager.startScan()




        getLocation()

        var button = findViewById<Button>(R.id.button)

        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

               createFile(MediaStore.Files.getContentUri("external"))



            }

        }
        )

    }

    private fun getLocation() {

        Intent(this, MyLocationListener::class.java).also { intent ->
            startService(intent)
        }
            Intent(this, MyWifiScanListener::class.java).also { intent ->
                startService(intent)
            }

                Intent(this, MyCellInfoListener::class.java).also { intent ->
                    startService(intent)

            }

        Intent(this, MySensorListener::class.java).also { intent ->
            startService(intent)

        }



    }

        override fun onPause()
        {
            super.onPause()
            //Toast.makeText(this, "Location Tracker Paused", Toast.LENGTH_SHORT).show()
        }

        override fun onResume()
        {
            super.onResume()
            Toast.makeText(this, "Location Tracker Resume", Toast.LENGTH_SHORT).show()

        }

    override fun onDestroy(){
        super.onDestroy()
        Toast.makeText(this, "Location Tracker Destroyed", Toast.LENGTH_SHORT).show()

    }

    override fun onStop() {
        super.onStop()
        Toast.makeText(this, "Location Tracker Stop", Toast.LENGTH_SHORT).show()

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



    }