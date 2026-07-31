package com.sad.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    suspend fun exportBackup(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())

            // 1. SharedPreferences (player_profile)
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val profileObj = JSONObject().apply {
                put("xp", prefs.getInt("xp", 0))
                put("explored_count", prefs.getInt("explored_count", 0))
                put("visited_dungeons", prefs.getInt("visited_dungeons", 0))
                put("night_explored_count", prefs.getInt("night_explored_count", 0))
                put("morning_explored_count", prefs.getInt("morning_explored_count", 0))
                put("received_rumors_count", prefs.getInt("received_rumors_count", 0))
                put("has_shared", prefs.getBoolean("has_shared", false))
                put("is_dev_mode_unlocked", prefs.getBoolean("is_dev_mode_unlocked", false))

                val achievements = prefs.getStringSet("unlocked_achievements", emptySet()) ?: emptySet()
                val achievementsArr = JSONArray()
                achievements.forEach { achievementsArr.put(it) }
                put("unlocked_achievements", achievementsArr)
            }
            root.put("profile", profileObj)

            // 2. Database (explored_areas & visited_dungeons)
            val gameDb = GameDatabase.getDatabase(context)
            
            val exploredList = gameDb.exploredAreaDao().getAll()
            val exploredArr = JSONArray()
            exploredList.forEach { area ->
                val item = JSONObject().apply {
                    put("lat", area.lat)
                    put("lon", area.lon)
                    put("radius", area.radius)
                    put("timestamp", area.timestamp)
                }
                exploredArr.put(item)
            }
            root.put("explored_areas", exploredArr)

            val visitedList = gameDb.visitedDungeonDao().getAll()
            val visitedArr = JSONArray()
            visitedList.forEach { dungeon ->
                val item = JSONObject().apply {
                    put("osm_id", dungeon.osm_id)
                    put("visitedAt", dungeon.visitedAt)
                    put("xpEarned", dungeon.xpEarned)
                }
                visitedArr.put(item)
            }
            root.put("visited_dungeons", visitedArr)

            // Stream in URI schreiben
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importBackup(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: throw IllegalArgumentException("Datei konnte nicht gelesen werden.")

            val root = JSONObject(jsonString)

            // 1. SharedPreferences wiederherstellen
            if (root.has("profile")) {
                val profileObj = root.getJSONObject("profile")
                val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                
                if (profileObj.has("xp")) editor.putInt("xp", profileObj.getInt("xp"))
                if (profileObj.has("explored_count")) editor.putInt("explored_count", profileObj.getInt("explored_count"))
                if (profileObj.has("visited_dungeons")) editor.putInt("visited_dungeons", profileObj.getInt("visited_dungeons"))
                if (profileObj.has("night_explored_count")) editor.putInt("night_explored_count", profileObj.getInt("night_explored_count"))
                if (profileObj.has("morning_explored_count")) editor.putInt("morning_explored_count", profileObj.getInt("morning_explored_count"))
                if (profileObj.has("received_rumors_count")) editor.putInt("received_rumors_count", profileObj.getInt("received_rumors_count"))
                if (profileObj.has("has_shared")) editor.putBoolean("has_shared", profileObj.getBoolean("has_shared"))
                if (profileObj.has("is_dev_mode_unlocked")) editor.putBoolean("is_dev_mode_unlocked", profileObj.getBoolean("is_dev_mode_unlocked"))

                if (profileObj.has("unlocked_achievements")) {
                    val achievementsArr = profileObj.getJSONArray("unlocked_achievements")
                    val achievementsSet = mutableSetOf<String>()
                    for (i in 0 until achievementsArr.length()) {
                        achievementsSet.add(achievementsArr.getString(i))
                    }
                    editor.putStringSet("unlocked_achievements", achievementsSet)
                }
                editor.commit()
            }

            // 2. Room DB wiederherstellen
            val gameDb = GameDatabase.getDatabase(context)

            if (root.has("explored_areas")) {
                val exploredArr = root.getJSONArray("explored_areas")
                val newExploredList = mutableListOf<ExploredArea>()
                for (i in 0 until exploredArr.length()) {
                    val obj = exploredArr.getJSONObject(i)
                    newExploredList.add(
                        ExploredArea(
                            lat = obj.getDouble("lat"),
                            lon = obj.getDouble("lon"),
                            radius = obj.optDouble("radius", 150.0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                gameDb.exploredAreaDao().deleteAll()
                gameDb.exploredAreaDao().insertAll(newExploredList)
            }

            if (root.has("visited_dungeons")) {
                val visitedArr = root.getJSONArray("visited_dungeons")
                val newVisitedList = mutableListOf<VisitedDungeon>()
                for (i in 0 until visitedArr.length()) {
                    val obj = visitedArr.getJSONObject(i)
                    newVisitedList.add(
                        VisitedDungeon(
                            osm_id = obj.getString("osm_id"),
                            visitedAt = obj.optLong("visitedAt", System.currentTimeMillis()),
                            xpEarned = obj.optInt("xpEarned", 0)
                        )
                    )
                }
                gameDb.visitedDungeonDao().deleteAll()
                gameDb.visitedDungeonDao().insertAll(newVisitedList)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
