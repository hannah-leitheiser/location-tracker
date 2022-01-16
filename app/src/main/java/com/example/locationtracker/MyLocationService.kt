package com.example.locationtracker
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.PowerManager.WakeLock
import android.widget.Toast
import android.location.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.util.Consumer
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import java.util.concurrent.Executor

internal class DirectExecutor : Executor {
    override fun execute(r: Runnable) {
        r.run()
    }
}

var gnss : GnssStatus? = null

class MyLocationService : Service()  {

    private class MyLocationListener : LocationListener {

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
                satellitesUsed = satsUsed
                data = arrayOf(
                    arrayOf(
                        arrayOf("latitude", location.latitude.toString()),
                        arrayOf("longitude", location.longitude.toString()),
                        arrayOf("altitude", location.altitude.toString()),
                        arrayOf("speed", location.speed.toString()),
                        arrayOf("bearing", location.bearing.toString()),
                        arrayOf("accuracy, horizontal", location.accuracy.toString()),
                        arrayOf("accuracy, vertical", location.verticalAccuracyMeters.toString()),
                        arrayOf("accuracy, speed", location.speedAccuracyMetersPerSecond.toString()),
                        arrayOf("accuracy, bearing", location.bearingAccuracyDegrees.toString()),
                        arrayOf("satellites, in view", satsTotal.toString()),
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
                        arrayOf("accuracy, horizontal", location.accuracy.toString()),
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

            saveFile.writeData(timeStamps, "location - " + location.provider.toString(), data)

            if( location.provider == "gps") GPSFixCount++
            if( location.provider == "network") NETFixCount++
            if( location.provider == "fused") FusedFixCount++
        }
    }

    private val locListener : MyLocationListener = MyLocationListener()
    val CHANNEL_ID = "1"


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i("MyLocation", "Start")


        val loop = Looper.getMainLooper()


        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, locListener, loop)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  0, 0f, locListener, loop)
        //locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0f, this)

        locationManager.registerGnssStatusCallback(object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(s: GnssStatus) {
                gnss = s
            }

            override fun onStopped() {
                super.onStopped()
            }
        })

        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.common_google_signin_btn_icon_light_normal)
            .setContentTitle("Recording")
            .setContentText("Saving Locations")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        createNotificationChannel()
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(0, builder.build())
        }



        return START_STICKY
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onBind(p0: Intent?): IBinder? {
    return null
    }
}