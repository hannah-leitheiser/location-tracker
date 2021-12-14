package com.example.locationtracker
import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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


const val CREATE_FILE = 1

class MainActivity : AppCompatActivity() {
    private lateinit var tvGpsLocation: TextView
    private val locationPermissionCode = 2
    private val TAG = "MainActivity"
    private val LAST_OPENED_URI_KEY =
        "com.example.android.actionopendocument.pref.LAST_OPENED_URI_KEY"

    //private lateinit var wakeLock: PowerManager.WakeLock
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




                findViewById<TextView>(R.id.info).text =
                    "-------------------------------------------\n" +
                            "              Stored Data                       \n\n" +
                            "GPSFixes    : " + GPSFixCount.toString() + "\n" +
                            "NETFixes    : " + NETFixCount.toString() + "\n" +
                            "Wifi Scan   : " + WIFIScanCount.toString() + "\n" +
                            "CellScan    : " + CellScanCount.toString() + "\n" +
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




        getLocation()

        var button = findViewById<Button>(R.id.button)

        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                //your implementation goes here


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


/*        Intent(this, MySensorListener::class.java).also { intent ->
            startService(intent)

        }
*/

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