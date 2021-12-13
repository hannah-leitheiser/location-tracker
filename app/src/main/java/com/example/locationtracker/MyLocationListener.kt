package com.example.locationtracker

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.os.IBinder
import android.location.LocationListener
import android.os.PowerManager
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MyLocationListener( saveFile: LocalJSONFileManager) : LocationListener {
    private val saveFile = saveFile
    var gnss : GnssStatus? = null

    var satellitesUsed = 0
    var GPSFixCount = 0
    var NETFixCount = 0
    var FusedFixCount = 0

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

        saveFile.writeData(timeStamps, "location - " + location.provider.toString(), data)

        if( location.provider == "gps") GPSFixCount++
        if( location.provider == "network") NETFixCount++
        if( location.provider == "fused") FusedFixCount++
    }
}