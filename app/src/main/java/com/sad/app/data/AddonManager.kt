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

data class AddonMeta(
    val id: String,
    val name: String,
    val placeCount: Int,
    val importedAt: Long,
    val filePath: String
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
                        filePath = obj.getString("filePath")
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
            }
            jsonArray.put(obj)
        }
        context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE).edit()
            .putString(ADDONS_LIST_KEY, jsonArray.toString())
            .apply()
        installedAddons = list
    }

    fun importAddon(context: Context, uri: Uri, customName: String): Result<AddonMeta> {
        return try {
            val addonsDir = File(context.filesDir, "addons").apply { if (!exists()) mkdirs() }
            val addonId = "addon_${System.currentTimeMillis()}"
            val destFile = File(addonsDir, "$addonId.db")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Datei konnte nicht gelesen werden"))

            // Prüfen ob die DB valide ist und Orte zählen
            val placeCount = countPlacesInDb(destFile)

            val meta = AddonMeta(
                id = addonId,
                name = customName.ifBlank { "Addon ${installedAddons.size + 1}" },
                placeCount = placeCount,
                importedAt = System.currentTimeMillis(),
                filePath = destFile.absolutePath
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
        val allAddonPlaces = mutableListOf<PlaceEntity>()
        for (addon in installedAddons) {
            val file = File(addon.filePath)
            if (!file.exists()) continue
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
        return allAddonPlaces
    }
}
