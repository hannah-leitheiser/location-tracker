package com.example.locationtracker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.hardware.TriggerEvent

import android.hardware.TriggerEventListener

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
    var lastTimeRan : Long = 0L

    private var accelerationArray : MutableList<FloatArray> = mutableListOf()
    private var accelerationTimestamps : MutableList<Long> = mutableListOf<Long>()

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


    fun flush()
    {
        sensorManager.flush(this)

    }

    fun registerSensorListeners() {

   //     sensorManager.unregisterListener(this)


           sensorManager.registerListener(this, pressureSensor, 1000000)
            sensorManager.registerListener(this, rotationSensor, 10000)
            sensorManager.registerListener(this, accelSensor, 10000)

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
            sensorManager.registerListener(this, rotationSensor, 10000)
        } else {
        }

        if (sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            val pSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION)
            accelSensor = pSensors[0]
            sensorManager.registerListener(this, accelSensor, 10000)
        } else {
        }

        wakeLockInit()
        wakeLockAquire()

        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {

                /*if(System.currentTimeMillis() - lastTimeRan > 700 ) {
                    Pressure=Pressure+1
                    lastTimeRan = System.currentTimeMillis()
                }*/

                registerSensorListeners()
                flush()

                wakeLockAquire()
                if(accelerationTimestamps.size > 1) {

                    var accelX = 0f
                    var accelY = 0f
                    var accelZ = 0f

                    for(i in 0..accelerationTimestamps.size-2)
                        {
                          accelX = accelX + accelerationArray[i][0] *
                                  ( accelerationTimestamps[i+1] -
                                          accelerationTimestamps[i])
                            accelY = accelY + accelerationArray[i][1] *
                                    ( accelerationTimestamps[i+1] -
                                            accelerationTimestamps[i])
                            accelZ = accelZ + accelerationArray[i][2] *
                                    ( accelerationTimestamps[i+1] -
                                            accelerationTimestamps[i])
                        }
                    val ADuration = accelerationTimestamps[ accelerationTimestamps.size-1 ] -
                                    accelerationTimestamps[0]
                    Accel = floatArrayOf(   accelX / ADuration,
                        accelY / ADuration,
                        accelZ / ADuration )
                    saveAcceleration(Accel ,
                                        ADuration)
                    if(rotationVector.size == 5)
                        saveRotationVector( rotationVector)

                    accelerationArray = mutableListOf( accelerationArray[ accelerationArray.size - 1 ] )
                    accelerationTimestamps = mutableListOf<Long>( accelerationTimestamps[ accelerationTimestamps.size - 1] )


                }


                mainHandler.postDelayed(this, 1000)
            }
        })



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
            rotationVector = event.values
        }

        if( event.sensor.type == Sensor.TYPE_PRESSURE)
            if( event.values != null)
                savePressure( event.values )
        if( event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION)
            if( event.values != null && rotationVector.size == 5) {

                val accelerationVector = point_rotation_by_quaternion( event.values,
                    floatArrayOf( rotationVector[3],
                                     rotationVector[0],
                                     rotationVector[1],
                                     rotationVector[2]) )
                accelerationArray.add( accelerationVector)
                accelerationTimestamps.add( System.currentTimeMillis())
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

    fun saveAcceleration( values : FloatArray, duration : Long ) {
        // Do something with this sensor value.
        val meas = arrayOf(
            arrayOf(
                arrayOf("x", values[0].toString()),
                arrayOf("y", values[1].toString()),
                arrayOf("z", values[2].toString()),
                arrayOf("duration", saveFile.convertTimestampToDecimal(duration))
            )
        )

        saveFile.writeData(System.currentTimeMillis(), "acceleration", meas)

        Acceleration = Acceleration + 1

    }

    fun saveRotationVector( values : FloatArray ) {
        // Do something with this sensor value.
        val meas = arrayOf(
            arrayOf(
                arrayOf("x", values[3].toString()),
                arrayOf("i", values[0].toString()),
                arrayOf("j", values[1].toString()),
                arrayOf("k", values[2].toString()),
                arrayOf("bearing accuracy", values[4].toString())


        )
        )

        saveFile.writeData(System.currentTimeMillis(), "rotation vector", meas)

        wakeLockAquire()

    }



    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        //sensorManager.unregisterListener(this, pressureSensor)
        //sensorManager.unregisterListener(this, accelSensor)
        //sensorManager.unregisterListener(this, rotationSensor)
        registerSensorListeners()
     }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        wl.release()
    }
}