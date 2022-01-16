package com.example.locationtracker

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult.*
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.CellInfo

class MyWifiScanListener : Service() {
    private lateinit var wifiManager: WifiManager


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val context: Context = applicationContext
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


                var success = wifiManager.startScan()

                if (success) {
                    // scan failure handling
                    scanSuccess()
                }

                mainHandler.postDelayed(this, 2000)


            }


        })

        return START_STICKY

    }



    private fun scanSuccess() {
        var bestWifiLevel = -1000
        val tsLong = System.currentTimeMillis()
        val bootTime = tsLong - (SystemClock.elapsedRealtimeNanos() / 1000000L)
        var earliestTimestamp = System.currentTimeMillis()
        val results = wifiManager.scanResults
        var dataPayloadMutable: MutableList<Array<Array<String>>> =
            mutableListOf<Array<Array<String>>>()
        Networks = results.size
        for (i in 0..results.size - 1) {
            val wifiTimestamp = ((results[0].timestamp / 1000L) + bootTime)
            if ((wifiTimestamp < earliestTimestamp))
                earliestTimestamp = wifiTimestamp
            if (results[i].level > bestWifiLevel) {
                bestWifiLevel = results[i].level
                bestWifi = results[i].SSID + "_" + results[i].level.toString()
            }
            if (results[i].channelWidth == CHANNEL_WIDTH_20MHZ) {
                dataPayloadMutable.add(
                    arrayOf(
                        arrayOf("BSSID", results[i].BSSID.toString()),
                        arrayOf("SSID", results[i].SSID.toString()),
                        arrayOf("level", results[i].level.toString()),
                        arrayOf("frequency", results[i].frequency.toString()),
                        arrayOf(
                            "bandwidth",
                            saveFile.formatResult(
                                results[i].channelWidth,
                                arrayOf(
                                    CHANNEL_WIDTH_20MHZ,
                                    CHANNEL_WIDTH_40MHZ,
                                    CHANNEL_WIDTH_80MHZ,
                                    CHANNEL_WIDTH_160MHZ,
                                    CHANNEL_WIDTH_80MHZ_PLUS_MHZ
                                ),
                                arrayOf(
                                    "20MHz",
                                    "40MHz",
                                    "80MHz",
                                    "160MHz",
                                    "80MHz+80MHz"
                                )
                            )
                        ),
                        arrayOf(
                            "timestamp",
                            saveFile.convertTimestampToDecimal(wifiTimestamp).toString()
                        )
                    )
                )
            } else {
                if (results[i].channelWidth == CHANNEL_WIDTH_80MHZ_PLUS_MHZ) {

                    dataPayloadMutable.add(
                        arrayOf(
                            arrayOf("BSSID", results[i].BSSID.toString()),
                            arrayOf("SSID", results[i].SSID.toString()),
                            arrayOf("level", results[i].level.toString()),
                            arrayOf("frequency", results[i].frequency.toString()),
                            arrayOf(
                                "bandwidth",
                                saveFile.formatResult(
                                    results[i].channelWidth,
                                    arrayOf(
                                        CHANNEL_WIDTH_20MHZ,
                                        CHANNEL_WIDTH_40MHZ,
                                        CHANNEL_WIDTH_80MHZ,
                                        CHANNEL_WIDTH_160MHZ,
                                        CHANNEL_WIDTH_80MHZ_PLUS_MHZ
                                    ),
                                    arrayOf(
                                        "20MHz",
                                        "40MHz",
                                        "80MHz",
                                        "160MHz",
                                        "80MHz+80MHz"
                                    )
                                )
                            ),
                            arrayOf("frequency, center0", results[i].centerFreq0.toString()),
                            arrayOf("frequency, center1", results[i].centerFreq1.toString()),
                            arrayOf(
                                "timestamp",
                                saveFile.convertTimestampToDecimal(wifiTimestamp).toString()
                            )
                        )
                    )
                } else {
                    dataPayloadMutable.add(
                        arrayOf(
                            arrayOf("BSSID", results[i].BSSID.toString()),
                            arrayOf("SSID", results[i].SSID.toString()),
                            arrayOf("level", results[i].level.toString()),
                            arrayOf("frequency", results[i].frequency.toString()),
                            arrayOf(
                                "bandwidth",
                                saveFile.formatResult(
                                    results[i].channelWidth,
                                    arrayOf(
                                        CHANNEL_WIDTH_20MHZ,
                                        CHANNEL_WIDTH_40MHZ,
                                        CHANNEL_WIDTH_80MHZ,
                                        CHANNEL_WIDTH_160MHZ,
                                        CHANNEL_WIDTH_80MHZ_PLUS_MHZ
                                    ),
                                    arrayOf(
                                        "20MHz",
                                        "40MHz",
                                        "80MHz",
                                        "160MHz",
                                        "80MHz+80MHz"
                                    )
                                )
                            ),
                            arrayOf("frequency, center", results[i].centerFreq0.toString()),
                            arrayOf(
                                "timestamp",
                                saveFile.convertTimestampToDecimal(wifiTimestamp).toString()
                            )
                        )
                    )

                }
            }
        }

        saveFile.writeData(earliestTimestamp, "wifi scan", dataPayloadMutable.toTypedArray())

        WIFIScanCount++


    }

    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        // val results = wifiManager.scanResults
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
