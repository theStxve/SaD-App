package com.sad.app.data

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object RarityColorManager {

    const val DEFAULT_EPIC     = "#FF00E6"
    const val DEFAULT_RARE     = "#FFD700"
    const val DEFAULT_UNCOMMON = "#00FF00"
    const val DEFAULT_COMMON   = "#555555"

    private const val PREFS      = "rarity_colors"
    private const val KEY_EPIC     = "epic"
    private const val KEY_RARE     = "rare"
    private const val KEY_UNCOMMON = "uncommon"
    private const val KEY_COMMON   = "common"

    var epicColor     by mutableStateOf(DEFAULT_EPIC);     private set
    var rareColor     by mutableStateOf(DEFAULT_RARE);     private set
    var uncommonColor by mutableStateOf(DEFAULT_UNCOMMON); private set
    var commonColor   by mutableStateOf(DEFAULT_COMMON);   private set

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        epicColor     = prefs.getString(KEY_EPIC,     DEFAULT_EPIC)     ?: DEFAULT_EPIC
        rareColor     = prefs.getString(KEY_RARE,     DEFAULT_RARE)     ?: DEFAULT_RARE
        uncommonColor = prefs.getString(KEY_UNCOMMON, DEFAULT_UNCOMMON) ?: DEFAULT_UNCOMMON
        commonColor   = prefs.getString(KEY_COMMON,   DEFAULT_COMMON)   ?: DEFAULT_COMMON
    }

    fun setEpic(context: Context, hex: String)     { if (!isValidHex(hex)) return; epicColor = hex; save(context, KEY_EPIC, hex) }
    fun setRare(context: Context, hex: String)     { if (!isValidHex(hex)) return; rareColor = hex; save(context, KEY_RARE, hex) }
    fun setUncommon(context: Context, hex: String) { if (!isValidHex(hex)) return; uncommonColor = hex; save(context, KEY_UNCOMMON, hex) }
    fun setCommon(context: Context, hex: String)   { if (!isValidHex(hex)) return; commonColor = hex; save(context, KEY_COMMON, hex) }

    fun resetAll(context: Context) {
        epicColor = DEFAULT_EPIC; rareColor = DEFAULT_RARE
        uncommonColor = DEFAULT_UNCOMMON; commonColor = DEFAULT_COMMON
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun epicArgb()     = parseArgb(epicColor)
    fun rareArgb()     = parseArgb(rareColor)
    fun uncommonArgb() = parseArgb(uncommonColor)
    fun commonArgb()   = parseArgb(commonColor)

    fun isValidHex(hex: String): Boolean {
        return try { AndroidColor.parseColor(if (hex.startsWith("#")) hex else "#$hex"); true }
        catch (e: IllegalArgumentException) { false }
    }

    private fun save(context: Context, key: String, hex: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, hex).apply()
    }

    private fun parseArgb(hex: String): Int {
        return try { AndroidColor.parseColor(if (hex.startsWith("#")) hex else "#$hex") }
        catch (e: Exception) { AndroidColor.parseColor(DEFAULT_COMMON) }
    }
}
