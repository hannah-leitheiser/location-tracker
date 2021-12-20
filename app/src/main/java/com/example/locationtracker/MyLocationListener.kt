package com.example.locationtracker
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.PowerManager.WakeLock
import android.widget.Toast
import android.location.*
import android.os.*
import androidx.core.util.Consumer
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import java.util.concurrent.Executor

internal class DirectExecutor : Executor {
    override fun execute(r: Runnable) {
        r.run()
    }
}

class MyLocationListener : Service(), LocationListener {
    var gnss : GnssStatus? = null
    lateinit var wl: WakeLock

    override fun onLocationChanged(location: Location) {

        if(!wl.isHeld()) {
            wl.acquire()
        }

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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, this)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,  0, 0f, this)
        //locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 0, 0f, this)

        locationManager.registerGnssStatusCallback(object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(s: GnssStatus) {
                gnss = s
            }

            override fun onStopped() {
                super.onStopped()
            }
        })

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MyLocationListener"
        )
        wl.acquire()

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
    return null
    }
}