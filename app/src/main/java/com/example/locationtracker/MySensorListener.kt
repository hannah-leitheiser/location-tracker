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

fun quaternion_mult(q : FloatArray,r : FloatArray) : FloatArray {
    return floatArrayOf(r[0] * q[0] - r[1] * q[1] - r[2] * q[2] - r[3] * q[3],
        r[0] * q[1] + r[1] * q[0] - r[2] * q[3] + r[3] * q[2],
        r[0] * q[2] + r[1] * q[3] + r[2] * q[0] - r[3] * q[1],
        r[0] * q[3] - r[1] * q[2] + r[2] * q[1] + r[3] * q[0])
}

fun point_rotation_by_quaternion(point : FloatArray, q : FloatArray) : FloatArray {
    val r = floatArrayOf( 0f, point[0], point[1], point[2])
    val q_conj = floatArrayOf( q[0], -1 * q[1], -1 * q[2], -1 * q[3] )
    val result =  quaternion_mult(quaternion_mult(q, r), q_conj)
    return floatArrayOf(result[1], result[2], result[3])
}

class MySensorListener : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var pressureSensor: Sensor
    private lateinit var rotationSensor: Sensor
    private lateinit var accelSensor: Sensor
    private lateinit var wl: PowerManager.WakeLock
    private var rotationVector : FloatArray = FloatArray( 0 )

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
            val pSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_PRESSURE)
            pressureSensor = pSensors[0]
            sensorManager.registerListener(this, pressureSensor, 1000000)
        } else {
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
            val pSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR)
            rotationSensor = pSensors[0]
            sensorManager.registerListener(this, rotationSensor, 1000000)
        } else {
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            val pSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION)
            accelSensor = pSensors[0]
            sensorManager.registerListener(this, accelSensor, 1000000)
        } else {
        }

        wakeLockInit()
        wakeLockAquire()
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
        if( event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            rotationVector = floatArrayOf(
                event.values[3], event.values[0],
                event.values[1], event.values[2]
            )
        }

        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        if( event.sensor.type == Sensor.TYPE_PRESSURE)
            if( event.values != null)
                savePressure( event.values )
        if( event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION)
            if( event.values != null && rotationVector.size == 4) {

                val accelerationVector = point_rotation_by_quaternion( event.values,
                    rotationVector)
                saveAcceleration(accelerationVector)
                Accel = accelerationVector



            }

    }

    fun savePressure( values : FloatArray ) {
        val pres = values[0]
        // Do something with this sensor value.
        val meas = arrayOf(
            arrayOf(
                arrayOf("Value", pres.toString())
            )
        )

        saveFile.writeData(System.currentTimeMillis(), "pressure", meas)

        Pressure = Pressure + 1
        PressureC = pres
        wakeLockAquire()
    }

    fun saveAcceleration( values : FloatArray ) {
        // Do something with this sensor value.
        val meas = arrayOf(
            arrayOf(
                arrayOf("x", values[0].toString()),
                arrayOf("y", values[1].toString()),
                arrayOf("z", values[2].toString())
            )
        )

        saveFile.writeData(System.currentTimeMillis(), "acceleration", meas)

        Acceleration = Acceleration + 1
        wakeLockAquire()

    }


    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //TODO("Not yet implemented")
    }




}