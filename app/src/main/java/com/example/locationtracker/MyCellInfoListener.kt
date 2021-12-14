package com.example.locationtracker

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.telephony.*
import android.widget.TextView
import androidx.core.content.ContextCompat

fun formatResult(result : Int, specialValues : Array<Int>, specials : Array<String>) : String {
    for(i in 0..specials.size-1) {
        if(result == specialValues[i])
            return specials[i]
    }
    return result.toString()
}

fun formatResultL(result : Long, specialValues : Array<Long>, specials : Array<String>) : String {
    for(i in 0..specials.size-1) {
        if(result == specialValues[i])
            return specials[i]
    }
    return result.toString()
}


fun printIntArray( a : IntArray ) : String {
    var output = ""
    for(i in 0..a.size-1)
    {
        if(i < a.size-1) output = output + a[i].toString() + ", "
        else output = output + a[i].toString()

    }
    return output
}


class MyCellInfoListener : Service()  {

    private lateinit var telephony: TelephonyManager
    private lateinit var lastCellList : MutableList<CellInfo>

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        lastCellList = telephony.allCellInfo

        // I can't get the callback to work, just use a looper
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                var newCells = telephony.allCellInfo
                if(newCells != lastCellList)
                {
                    onCellInfoChanged(newCells)
                    lastCellList = newCells
                }

                mainHandler.postDelayed(this, 1000)
            }
        })

        //val wl: PowerManager.WakeLock
        //val pm = getSystemService(POWER_SERVICE) as PowerManager
        //wl = pm.newWakeLock(
        //    PowerManager.PARTIAL_WAKE_LOCK,
        //    "MyCellInfoListener"
        //)
        //wl.acquire()
        return Service.START_STICKY

    }
    fun onCellInfoChanged(p0: MutableList<CellInfo>) {
        var dataPayloadMutable: MutableList<Array<Array<String>>> =
            mutableListOf<Array<Array<String>>>()

        var cells = p0
        val tsLong = System.currentTimeMillis()
        val bootTime = tsLong - (SystemClock.elapsedRealtimeNanos() / 1000000L)
        var earliestTimestamp = System.currentTimeMillis()
        Towers = cells.size
        for (tower in cells) {
            val towerTimestamp = ((tower.timestampMillis / 1L) + bootTime)
            if ((towerTimestamp < earliestTimestamp))
                earliestTimestamp = towerTimestamp

            val towerInfo = tower.cellIdentity
            val signalStrength = tower.cellSignalStrength
            if (towerInfo is CellIdentityCdma) {
                val t = towerInfo as CellIdentityCdma
                val s = signalStrength as CellSignalStrength
                dataPayloadMutable.add(
                    arrayOf(
                        arrayOf("operator, long", t.operatorAlphaLong.toString()),
                        arrayOf("operator, short", t.operatorAlphaShort.toString()),
                        arrayOf(
                            "CDMA base station id",
                            formatResult(
                                t.basestationId,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "CDMA latitude",
                            formatResult(
                                t.latitude,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "CDMA longitude",
                            formatResult(
                                t.longitude,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "CDMA network id",
                            formatResult(
                                t.networkId,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "CDMA system id",
                            formatResult(
                                t.systemId,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("strength, asu", s.asuLevel.toString()),
                        arrayOf("strength, dbm", s.dbm.toString()),
                        arrayOf(
                            "strength, level", formatResult(
                                s.level, arrayOf(
                                    CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT,
                                    CellSignalStrength.SIGNAL_STRENGTH_MODERATE,
                                    CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR
                                ), arrayOf("GOOD", "GREAT", "MODERATE", "NONE/UNKNOWN", "POOR")
                            )
                        ),
                        arrayOf("timestamp", towerTimestamp.toString())
                    )
                )
            }
            if (towerInfo is CellIdentityGsm && signalStrength is CellSignalStrengthGsm) {
                val t = towerInfo as CellIdentityGsm
                val s = signalStrength as CellSignalStrengthGsm
                dataPayloadMutable.add(
                    arrayOf(
                        arrayOf("operator, long", t.operatorAlphaLong.toString()),
                        arrayOf("operator, short", t.operatorAlphaShort.toString()),
                        arrayOf(
                            "GSM absolute RF channel number",
                            formatResult(
                                t.arfcn,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "GSM base station identity code",
                            formatResult(
                                t.bsic,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "GSM cell identity",
                            formatResult(
                                t.cid,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "GSM location area",
                            formatResult(
                                t.lac,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("GSM mobile country code", t.mccString.toString()),
                        arrayOf("GSM mobile network code", t.mncString.toString()),
                        arrayOf("GSM mobile network operator", t.mobileNetworkOperator.toString()),
                        arrayOf("GSM additional PLMMs", t.additionalPlmns.toString()),
                        arrayOf("strength, asu", s.asuLevel.toString()),
                        arrayOf("strength, dbm", s.dbm.toString()),
                        arrayOf(
                            "strength, level", formatResult(
                                s.level, arrayOf(
                                    CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT,
                                    CellSignalStrength.SIGNAL_STRENGTH_MODERATE,
                                    CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR
                                ), arrayOf("GOOD", "GREAT", "MODERATE", "NONE/UNKNOWN", "POOR")
                            )
                        ),
                        arrayOf(
                            "timing advance",
                            formatResult(
                                s.timingAdvance,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("timestamp", towerTimestamp.toString())
                    )
                )
            }
            if (towerInfo is CellIdentityLte && signalStrength is CellSignalStrengthLte) {
                val t = towerInfo as CellIdentityLte
                val s = signalStrength as CellSignalStrengthLte
                dataPayloadMutable.add(
                    arrayOf(
                        arrayOf("operator, long", t.operatorAlphaLong.toString()),
                        arrayOf("operator, short", t.operatorAlphaShort.toString()),
                        arrayOf("LTE bands", printIntArray(t.bands)),
                        arrayOf(
                            "LTE bandwidth, kHz",
                            formatResult(
                                t.bandwidth,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "LTE cell identity",
                            formatResult(
                                t.ci,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "LTE closed subscriber group",
                            t.closedSubscriberGroupInfo.toString()
                        ),
                        arrayOf(
                            "LTE absolute RF channel number",
                            formatResult(
                                t.earfcn,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("LTE mobile country code", t.mccString.toString()),
                        arrayOf("LTE mobile network code", t.mncString.toString()),
                        arrayOf("LTE mobile network operator", t.mobileNetworkOperator.toString()),
                        arrayOf(
                            "LTE physical cell id",
                            formatResult(
                                t.pci,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "LTE tracking area code",
                            formatResult(
                                t.tac,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("LTE additional PLMMs", t.additionalPlmns.toString()),
                        arrayOf("strength, asu", s.asuLevel.toString()),
                        arrayOf("strength, dbm", s.dbm.toString()),
                        arrayOf(
                            "strength, level", formatResult(
                                s.level, arrayOf(
                                    CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT,
                                    CellSignalStrength.SIGNAL_STRENGTH_MODERATE,
                                    CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR
                                ), arrayOf("GOOD", "GREAT", "MODERATE", "NONE/UNKNOWN", "POOR")
                            )
                        ),
                        arrayOf(
                            "timing advance",
                            formatResult(
                                s.timingAdvance,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("timestamp", towerTimestamp.toString())
                    )
                )
            }
            if (towerInfo is CellIdentityNr && signalStrength is CellSignalStrengthNr) {
                val t = towerInfo as CellIdentityNr
                val s = signalStrength as CellSignalStrengthNr
                dataPayloadMutable.add(
                    arrayOf(
                        arrayOf("operator, long", t.operatorAlphaLong.toString()),
                        arrayOf("operator, short", t.operatorAlphaShort.toString()),
                        arrayOf("NR bands", printIntArray(t.bands)),
                        arrayOf("NR mobile country code", t.mccString.toString()),
                        arrayOf("NR mobile network code", t.mncString.toString()),
                        arrayOf(
                            "NR cell identity",
                            formatResultL(
                                t.nci,
                                arrayOf(CellInfo.UNAVAILABLE_LONG),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "NR absolute RF channel number",
                            formatResult(
                                t.nrarfcn,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "NR physical cell id",
                            formatResult(
                                t.pci,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "NR tracking area code",
                            formatResult(
                                t.tac,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("NR additional PLMMs", t.additionalPlmns.toString()),
                        arrayOf("strength, asu", s.asuLevel.toString()),
                        arrayOf("strength, dbm", s.dbm.toString()),
                        arrayOf(
                            "strength, level", formatResult(
                                s.level, arrayOf(
                                    CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT,
                                    CellSignalStrength.SIGNAL_STRENGTH_MODERATE,
                                    CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR
                                ), arrayOf("GOOD", "GREAT", "MODERATE", "NONE/UNKNOWN", "POOR")
                            )
                        ),
                        arrayOf("timestamp", towerTimestamp.toString())
                    )
                )
            }
            if (towerInfo is CellIdentityTdscdma && signalStrength is CellSignalStrengthTdscdma) {
                val t = towerInfo as CellIdentityTdscdma
                val s = signalStrength as CellSignalStrengthTdscdma
                dataPayloadMutable.add(
                    arrayOf(
                        arrayOf("operator, long", t.operatorAlphaLong.toString()),
                        arrayOf("operator, short", t.operatorAlphaShort.toString()),
                        arrayOf(
                            "TD-SCDMA cell id",
                            formatResult(
                                t.cid,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "TD-SCDMA closed subscriber group",
                            t.closedSubscriberGroupInfo.toString()
                        ),
                        arrayOf(
                            "TD-SCDMA cell parameters id",
                            formatResult(
                                t.cpid,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "TD-SCDMA local area code",
                            formatResult(
                                t.lac,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("TD-SCDMA mobile country code", t.mccString.toString()),
                        arrayOf("TD-SCDMA mobile network code", t.mncString.toString()),
                        arrayOf(
                            "TD-SCDMA mobile network operator",
                            t.mobileNetworkOperator.toString()
                        ),
                        arrayOf(
                            "TD-SCDMA absolute RF channel number",
                            formatResult(
                                t.uarfcn,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "TD-SCDMA cell parameters id",
                            formatResult(
                                t.cpid,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("TD-SCDMA additional PLMMs", t.additionalPlmns.toString()),
                        arrayOf("strength, asu", s.asuLevel.toString()),
                        arrayOf("strength, dbm", s.dbm.toString()),
                        arrayOf(
                            "strength, level", formatResult(
                                s.level, arrayOf(
                                    CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT,
                                    CellSignalStrength.SIGNAL_STRENGTH_MODERATE,
                                    CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR
                                ), arrayOf("GOOD", "GREAT", "MODERATE", "NONE/UNKNOWN", "POOR")
                            )
                        ),
                        arrayOf("timestamp", towerTimestamp.toString())
                    )
                )
            }
            if (towerInfo is CellIdentityWcdma && signalStrength is CellSignalStrengthWcdma) {
                val t = towerInfo as CellIdentityWcdma
                val s = signalStrength as CellSignalStrengthWcdma
                dataPayloadMutable.add(
                    arrayOf(
                        arrayOf("operator, long", t.operatorAlphaLong.toString()),
                        arrayOf("operator, short", t.operatorAlphaShort.toString()),
                        arrayOf("WCDMA additional PLMMs", t.additionalPlmns.toString()),
                        arrayOf(
                            "WCDMA cell id",
                            formatResult(
                                t.cid,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "WCDMA closed subscriber group",
                            t.closedSubscriberGroupInfo.toString()
                        ),
                        arrayOf(
                            "WCDMA location area code",
                            formatResult(
                                t.lac,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("WCDMA mobile country code", t.mccString.toString()),
                        arrayOf("WCDMA mobile network code", t.mncString.toString()),
                        arrayOf(
                            "WCDMA primary scrambling code",
                            formatResult(
                                t.psc,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf(
                            "WCDMA absolute RF channel number",
                            formatResult(
                                t.uarfcn,
                                arrayOf(CellInfo.UNAVAILABLE),
                                arrayOf("UNAVALIABLE")
                            )
                        ),
                        arrayOf("strength, asu", s.asuLevel.toString()),
                        arrayOf("strength, dbm", s.dbm.toString()),
                        arrayOf(
                            "strength, level", formatResult(
                                s.level, arrayOf(
                                    CellSignalStrength.SIGNAL_STRENGTH_GOOD,
                                    CellSignalStrength.SIGNAL_STRENGTH_GREAT,
                                    CellSignalStrength.SIGNAL_STRENGTH_MODERATE,
                                    CellSignalStrength.SIGNAL_STRENGTH_NONE_OR_UNKNOWN,
                                    CellSignalStrength.SIGNAL_STRENGTH_POOR
                                ), arrayOf("GOOD", "GREAT", "MODERATE", "NONE/UNKNOWN", "POOR")
                            )
                        ),
                        arrayOf("timestamp", towerTimestamp.toString())
                    )
                )
            }

        }
        saveFile.writeData(earliestTimestamp, "cell scan", dataPayloadMutable.toTypedArray())

        CellScanCount++

    }

}
