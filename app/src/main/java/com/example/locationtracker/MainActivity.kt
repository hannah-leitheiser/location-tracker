package com.example.locationtracker
import android.R.string
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import android.os.*
import android.view.WindowManager
import android.os.PowerManager
import android.text.format.DateUtils
import java.util.*
import android.content.DialogInterface
import android.util.Log
import android.widget.*

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

    private var startTime = 0L
    private var stopTime = 0L
    var lastPresses : LongArray = longArrayOf( 0,0,0,0,0)

    fun wakeLockInit() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MainActivity:MainWakeLock"
        )
    }

    fun wakeLockAquire(){
        while(!wl.isHeld())
            wl.acquire()
    }
    fun wakeLockRelease(){
        if(wl.isHeld())
            wl.release()
    }


    private fun getDateTime(s: Long): String? {
        try {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss ")
            val netDate = Date(s)
            return sdf.format(netDate)
        } catch (e: Exception) {
            return e.toString()
        }
    }
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
            putExtra(Intent.EXTRA_TITLE, android.os.Build.MODEL+"_" + date + "_DATA.txt")

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

        Log.i("LT","test")


        wakeLockInit()
        wakeLockAquire()

        val WMLP = window.attributes
        WMLP.screenBrightness = 0f
        window.attributes = WMLP

        saveFile = LocalJSONFileManager(this, "myfile")


        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {

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



                findViewById<TextView>(R.id.startTime).text = getDateTime(startTime)
                findViewById<TextView>(R.id.stopTime).text = getDateTime(stopTime)
                if( stopTime > startTime)
                    findViewById<TextView>(R.id.duration).text = DateUtils.formatElapsedTime((stopTime - startTime)/1000L)
                else
                    findViewById<TextView>(R.id.duration).text = ""

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


        var button2 = findViewById<Button>(R.id.startTimer)

        button2.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                startTime =  System.currentTimeMillis()
            }
        }
        )

        var button3 = findViewById<Button>(R.id.stopTimer)

        button3.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                stopTime =  System.currentTimeMillis()
                lastPresses = longArrayOf ( lastPresses[1], lastPresses[2], lastPresses[3], lastPresses[4], stopTime)

                if((lastPresses[4] - lastPresses[0] < 1000) && (stopTime > startTime) && ((stopTime - startTime) < (86400L * 1000L)) ) {

                    val contextText = findViewById<ToggleButton>(R.id.toggleButton).text.toString()
                    val show = AlertDialog.Builder(this@MainActivity)
                        .setTitle("Title")
                        .setMessage("Do you really want to save " + findViewById<EditText>(R.id.editLocationLabel).text.toString()+ ":" + contextText +  " for " +
                                DateUtils.formatElapsedTime((stopTime - startTime)/1000L)+"?")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(
                            "Yes",
                            DialogInterface.OnClickListener { dialog, whichButton ->
                                Toast.makeText(
                                    this@MainActivity,
                                    "Saving "+ findViewById<EditText>(R.id.editLocationLabel).text.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                                saveFile.writeData( startTime, "specified location",
                                    arrayOf(
                                    arrayOf(
                                 arrayOf( "context", contextText),
                                 arrayOf( "label", findViewById<EditText>(R.id.editLocationLabel).text.toString()),
                                 arrayOf( "start time", saveFile.convertTimestampToDecimal(startTime)),
                                 arrayOf( "end time", saveFile.convertTimestampToDecimal(stopTime))

                                )
                                ))
                                startTime = 0
                                stopTime = 0
                            })
                        .setNegativeButton("No!", null).show()
                }

            }
        }
        )
    }

    private fun getLocation() {

        Intent(applicationContext, MyCellInfoListener::class.java).also {
            intent -> startService(intent)
        }

        Intent(applicationContext, MyLocationService::class.java).also {
                intent -> startService(intent)
        }

        Intent(applicationContext, MyWifiScanListener::class.java).also {
                intent -> startService(intent)
        }

        Intent(applicationContext, MySensorListener::class.java).also {
                intent -> startService(intent)
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
        wakeLockRelease()
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