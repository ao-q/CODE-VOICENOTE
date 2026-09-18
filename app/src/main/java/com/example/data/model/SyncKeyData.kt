package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class SyncTimestampItem(
    val timeMs: Long,
    val formattedTime: String,
    val label: String
)

data class SyncKeyPayload(
    val version: Int = 1,
    val title: String,
    val audioFileName: String,
    val durationMs: Long,
    val createdAt: Long,
    val noteContent: String,
    val timestamps: List<SyncTimestampItem>
) {
    fun toJsonString(): String {
        val json = JSONObject()
        json.put("version", version)
        json.put("title", title)
        json.put("audioFileName", audioFileName)
        json.put("durationMs", durationMs)
        json.put("createdAt", createdAt)
        json.put("noteContent", noteContent)

        val tsArray = JSONArray()
        for (ts in timestamps) {
            val tsObj = JSONObject()
            tsObj.put("timeMs", ts.timeMs)
            tsObj.put("formattedTime", ts.formattedTime)
            tsObj.put("label", ts.label)
            tsArray.put(tsObj)
        }
        json.put("timestamps", tsArray)
        return json.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): SyncKeyPayload? {
            return try {
                val json = JSONObject(jsonStr)
                val version = json.optInt("version", 1)
                val title = json.optString("title", "Untitled Note")
                val audioFileName = json.optString("audioFileName", "")
                val durationMs = json.optLong("durationMs", 0L)
                val createdAt = json.optLong("createdAt", System.currentTimeMillis())
                val noteContent = json.optString("noteContent", "")

                val tsList = mutableListOf<SyncTimestampItem>()
                val tsArray = json.optJSONArray("timestamps")
                if (tsArray != null) {
                    for (i in 0 until tsArray.length()) {
                        val obj = tsArray.getJSONObject(i)
                        tsList.add(
                            SyncTimestampItem(
                                timeMs = obj.optLong("timeMs", 0L),
                                formattedTime = obj.optString("formattedTime", "00:00"),
                                label = obj.optString("label", "Marker ${i + 1}")
                            )
                        )
                    }
                }
                SyncKeyPayload(
                    version = version,
                    title = title,
                    audioFileName = audioFileName,
                    durationMs = durationMs,
                    createdAt = createdAt,
                    noteContent = noteContent,
                    timestamps = tsList
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
