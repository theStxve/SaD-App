package com.sad.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

data class AddonMeta(
    val id: String,
    val name: String,
    val placeCount: Int,
    val importedAt: Long,
    val filePath: String,
    val isEnabled: Boolean = true,
    val fileType: String = "db" // "db" oder "json"
)

data class GlobalAddonRule(
    val iconColor: String? = null,
    val minZoom: Float? = null,
    val rarity: String? = null
)

data class AddonLoadResult(
    val places: List<PlaceEntity>,
    val globalRules: List<GlobalAddonRule>
)

object AddonManager {
    private const val PREFS_KEY = "addon_manager_prefs"
    private const val ADDONS_LIST_KEY = "addons_list"

    var installedAddons by mutableStateOf<List<AddonMeta>>(emptyList())
        private set

    fun init(context: Context) {
        installedAddons = loadAddonsList(context)
    }

    private fun loadAddonsList(context: Context): List<AddonMeta> {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(ADDONS_LIST_KEY, "[]") ?: "[]"
        val list = mutableListOf<AddonMeta>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    AddonMeta(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        placeCount = obj.getInt("placeCount"),
                        importedAt = obj.getLong("importedAt"),
                        filePath = obj.getString("filePath"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        fileType = obj.optString("fileType", if (obj.getString("filePath").endsWith(".json")) "json" else "db")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveAddonsList(context: Context, list: List<AddonMeta>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("placeCount", item.placeCount)
                put("importedAt", item.importedAt)
                put("filePath", item.filePath)
                put("isEnabled", item.isEnabled)
                put("fileType", item.fileType)
            }
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
            .putString(ADDONS_LIST_KEY, jsonArray.toString())
            .apply()
        installedAddons = list
    }

    fun toggleAddon(context: Context, addonId: String, enabled: Boolean) {
        val updated = installedAddons.map {
            if (it.id == addonId) it.copy(isEnabled = enabled) else it
        }
        saveAddonsList(context, updated)
    }

    fun importAddon(context: Context, uri: Uri, customName: String): Result<AddonMeta> {
        return try {
            val addonsDir = File(context.filesDir, "addons").apply { if (!exists()) mkdirs() }
            val addonId = "addon_${System.currentTimeMillis()}"

            // Feststellen ob JSON oder DB
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            val isJson = mimeType.contains("json") || uri.path?.endsWith(".json", ignoreCase = true) == true

            val fileExt = if (isJson) "json" else "db"
            val destFile = File(addonsDir, "$addonId.$fileExt")

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Datei konnte nicht gelesen werden"))

            // Prüfen ob die Datei valide ist und Orte zählen
            val placeCount = if (isJson) countPlacesInJson(destFile) else countPlacesInDb(destFile)

            if (placeCount == 0) {
                destFile.delete()
                return Result.failure(Exception("Keine gültigen Orte im Addon gefunden!"))
            }

            val meta = AddonMeta(
                id = addonId,
                name = customName.ifBlank { "Addon ${installedAddons.size + 1}" },
                placeCount = placeCount,
                importedAt = System.currentTimeMillis(),
                filePath = destFile.absolutePath,
                isEnabled = true,
                fileType = fileExt
            )

            val updated = installedAddons + listOf(meta)
            saveAddonsList(context, updated)

            Result.success(meta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeAddon(context: Context, addonId: String) {
        val addon = installedAddons.find { it.id == addonId }
        if (addon != null) {
            val file = File(addon.filePath)
            if (file.exists()) file.delete()
            val updated = installedAddons.filterNot { it.id == addonId }
            saveAddonsList(context, updated)
        }
    }

    private fun countPlacesInJson(file: File): Int {
        return try {
            val jsonStr = file.readText()
            val jsonArray = JSONArray(jsonStr)
            jsonArray.length()
        } catch (e: Exception) {
            0
        }
    }

    private fun countPlacesInDb(file: File): Int {
        var count = 0
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT COUNT(*) FROM places", null)
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return count
    }

    fun loadAllAddonPlaces(context: Context): List<PlaceEntity> {
        return loadAllAddonData(context).places
    }

    fun loadAllAddonData(context: Context): AddonLoadResult {
        val allAddonPlaces = mutableListOf<PlaceEntity>()
        val globalRules = mutableListOf<GlobalAddonRule>()

        for (addon in installedAddons) {
            if (!addon.isEnabled) continue
            val file = File(addon.filePath)
            if (!file.exists()) continue

            if (addon.fileType == "json") {
                try {
                    val jsonStr = file.readText()
                    val jsonArray = JSONArray(jsonStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)

                        val osmId = obj.optString("osm_id", "")
                        val isOverrideAll = obj.optBoolean("override_all", false) ||
                                            obj.optBoolean("override_all_color", false) ||
                                            osmId == "*" ||
                                            obj.optString("target") == "all"

                        val iconColor = obj.optString("iconColor").ifBlank { null }
                        val minZoom = if (obj.has("minZoom")) obj.getDouble("minZoom").toFloat() else null
                        val rarity = obj.optString("rarity").ifBlank { null }

                        if (isOverrideAll) {
                            globalRules.add(
                                GlobalAddonRule(
                                    iconColor = iconColor,
                                    minZoom = minZoom,
                                    rarity = rarity
                                )
                            )
                        }

                        val lat = obj.optDouble("lat", 0.0)
                        val lon = obj.optDouble("lon", 0.0)

                        // Nur als einzelner Ort hinzufügen wenn lat/lon vorhanden und nicht 0
                        if (lat != 0.0 && lon != 0.0) {
                            val place = PlaceEntity(
                                osm_id = if (osmId.isNotBlank() && osmId != "*") osmId else "addon_${addon.id}_$i",
                                name = obj.optString("name", "Unbekannter Ort"),
                                category = obj.optString("category", "Dungeon"),
                                type = obj.optString("type", "addon"),
                                rarity = rarity ?: "common",
                                lat = lat,
                                lon = lon
                            ).also { p ->
                                p.description = obj.optString("description").ifBlank { null }
                                p.lore = obj.optString("lore").ifBlank { null }
                                p.xpReward = if (obj.has("xpReward")) obj.getInt("xpReward") else null
                                p.questHint = obj.optString("questHint").ifBlank { null }
                                p.iconColor = iconColor
                                p.minZoom = minZoom
                            }
                            allAddonPlaces.add(place)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                var db: SQLiteDatabase? = null
                try {
                    db = SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                    val cursor = db.rawQuery("SELECT osm_id, name, category, type, rarity, lat, lon FROM places", null)
                    while (cursor.moveToNext()) {
                        allAddonPlaces.add(
                            PlaceEntity(
                                osm_id = cursor.getString(0),
                                name = cursor.getString(1) ?: "",
                                category = cursor.getString(2) ?: "Dungeon",
                                type = cursor.getString(3) ?: "",
                                rarity = cursor.getString(4) ?: "common",
                                lat = cursor.getDouble(5),
                                lon = cursor.getDouble(6)
                            )
                        )
                    }
                    cursor.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    db?.close()
                }
            }
        }
        return AddonLoadResult(allAddonPlaces, globalRules)
    }

    fun exportPlacesToJson(context: Context, places: List<PlaceEntity>, uri: Uri): Result<Int> {
        return try {
            val jsonArray = JSONArray()
            for (p in places) {
                val obj = JSONObject().apply {
                    put("osm_id", p.osm_id)
                    put("name", p.name)
                    put("category", p.category)
                    put("type", p.type)
                    put("rarity", p.rarity)
                    put("lat", p.lat)
                    put("lon", p.lon)
                }
                jsonArray.put(obj)
            }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out, Charsets.UTF_8).use { writer ->
                    writer.write(jsonArray.toString(2))
                }
            } ?: return Result.failure(Exception("Stream konnte nicht geöffnet werden"))

            Result.success(places.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
