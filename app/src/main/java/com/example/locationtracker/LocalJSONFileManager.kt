package com.example.locationtracker
import android.content.Context

class LocalJSONFileManager(context: Context, fileName : String) {
    val fileName = fileName
    val context = context

    public fun writeData(
        timeStampms: Long,
        measurementType: String,
        propertyArray: Array<Array<Array<String>>>
    ) {
        val fileContents =
            formatJSONData(android.os.Build.MODEL, timeStampms, measurementType, propertyArray)
        context.openFileOutput(fileName, Context.MODE_APPEND).use {
            it.write(fileContents.toByteArray())
        }
    }

    private fun formatJSONData(
        phoneName: String,
        timeStampms: Long,
        measurementType: String,
        propertyArray: Array<Array<Array<String>>>
    ): String {
        var outputJSON = "*".repeat(32) + "\n" +
                "{ \"source\"            : \"" + phoneName + "\",\n" +
                "  \"timestamp\"         : \"" + timeStampms.toString() + "\",\n" +
                "  \"measurement_type\"  : \"" + measurementType + "\",\n"

        outputJSON = outputJSON +
                "  \"data\"              : [ \n"

        for (ii in 0..propertyArray.size - 1) {
            outputJSON = outputJSON + "  {\n"
            for (i in 0..propertyArray[ii].size - 1) {

                if (propertyArray[ii][i][1] != "" && propertyArray[ii][i][1] != "{}" && propertyArray[ii][i][1] != "UNAVALIABLE" && propertyArray[ii][i][1] != "null") {
                    outputJSON =
                        outputJSON + "     \"" + propertyArray[ii][i][0] + "\" : \"" + propertyArray[ii][i][1] + "\",\n"
                }
            }
            if (outputJSON.substring(outputJSON.length - 2) == ",\n")
                outputJSON = outputJSON.substring(0, outputJSON.length - 2) + "\n"
            if (ii < propertyArray.size - 1)
                outputJSON = outputJSON + "  }, \n"
            else
                outputJSON = outputJSON + "  }\n"

        }
        outputJSON = outputJSON + " ]\n}\n"
        return outputJSON

    }

}