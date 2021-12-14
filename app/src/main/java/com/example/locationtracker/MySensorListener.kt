package com.example.locationtracker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager

class MySensorListener : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var wl: PowerManager.WakeLock

    fun wakeLockInit() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MySensorListener"
        )
    }

    fun wakeLockAquire(){
        if(!wl.isHeld())
            wl.acquire()
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        sensorManager =  getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {

            val gravSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_PRESSURE)
            // Use the version 3 gravity sensor.
            mSensor = gravSensors[0]

            // Success! There's a magnetometer.
        } else {
            // Failure! No magnetometer.
        }
        wakeLockInit()
        wakeLockAquire()
        sensorManager.registerListener(this, mSensor, 1000000)
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        //TODO("Not yet implemented")

        return null
    }

    override fun onCreate() {
        super.onCreate()



    }



    override fun onSensorChanged(event: SensorEvent) {


        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.

        val lux = event.values[0]
        // Do something with this sensor value.
        val meas = arrayOf(
            arrayOf(
                arrayOf("Value", lux.toString())
            )
        )

        //val fileContents = formatJSONData( android.os.Build.MODEL, System.currentTimeMillis(), "pressure", meas)
        //val filename = "myfile"

        //openFileOutput(filename, Context.MODE_APPEND).use {
        //    it.write(fileContents.toByteArray())
        //}
        saveFile.writeData(System.currentTimeMillis(), "pressure", meas)

        Pressure = Pressure + 1
        wakeLockAquire()

    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //TODO("Not yet implemented")
    }




}