package com.sad.app.data

import android.content.Context
import android.location.Location
import com.sad.app.notifications.DungeonNotifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.cos

/**
 * Zentrale Spiel-Logik für jeden GPS-Fix.
 *
 * Sowohl der [com.sad.app.LocationTrackingService] als auch der MapScreen bekommen Updates
 * vom Fused Location Provider. Früher hat jeder davon selbst Nebel, Strecke und Dungeons
 * verarbeitet, wodurch XP und Kilometer doppelt gezählt wurden. Jetzt laufen alle Fixes
 * durch diese eine Stelle: serialisiert per Mutex und dedupliziert über den Zeitstempel.
 */
object GameEngine {

    const val DETECTION_RADIUS_M = 25f
    const val MAGNET_RADIUS_M = 500f
    private const val MAX_PLAUSIBLE_STEP_M = 500f
    private const val MAX_CONNECTION_GAP_M = 2000f

    data class Result(
        /** Neu aufgedeckte Bereiche (inkl. interpolierter Verbindungspunkte). */
        val newExplored: List<ExploredArea> = emptyList(),
        /** In diesem Schritt neu geplünderte Dungeons. */
        val looted: List<PlaceEntity> = emptyList()
    )

    private val mutex = Mutex()
    private var lastFixTime = 0L
    private var lastLat: Double? = null
    private var lastLon: Double? = null

    // Addon-Orte cachen, damit nicht bei jedem Fix alle Addon-Dateien gelesen werden
    private var addonCacheKey: List<AddonMeta>? = null
    private var addonPlaces: List<PlaceEntity> = emptyList()

    fun xpFor(place: PlaceEntity): Int = place.xpReward ?: xpForRarity(place.rarity)

    fun xpForRarity(rarity: String): Int = when (rarity) {
        "epic" -> 200
        "rare" -> 100
        "uncommon" -> 50
        else -> 25
    }

    fun visionRadius(context: Context): Double {
        val mapPrefs = context.getSharedPreferences("map_customization_prefs", Context.MODE_PRIVATE)
        return if (mapPrefs.getBoolean("precisionModeEnabled", false)) 20.0 else 150.0
    }

    /** Ab wann ein Punkt als "schon erkundet" gilt: halber Sichtradius. */
    private suspend fun isExplored(dao: ExploredAreaDao, lat: Double, lon: Double, radius: Double): Boolean {
        val dLat = (radius * 0.5) / 111_000.0
        val dLon = dLat / cos(Math.toRadians(lat)).coerceAtLeast(0.01)
        return dao.countInBox(lat - dLat, lat + dLat, lon - dLon, lon + dLon) > 0
    }

    private fun currentAddonPlaces(context: Context): List<PlaceEntity> {
        AddonManager.ensureInit(context)
        val key = AddonManager.installedAddons
        if (key !== addonCacheKey) {
            addonPlaces = AddonManager.loadAllAddonPlaces(context)
            addonCacheKey = key
        }
        return addonPlaces
    }

    suspend fun processLocation(
        context: Context,
        rawLat: Double,
        rawLon: Double,
        fixTimeMs: Long
    ): Result = mutex.withLock {
        // Derselbe Fix kommt ggf. sowohl beim Service als auch beim MapScreen an
        if (fixTimeMs <= lastFixTime) return@withLock Result()
        lastFixTime = fixTimeMs

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        val lat = rawLat + prefs.getFloat("lat_offset", 0f)
        val lon = rawLon + prefs.getFloat("lon_offset", 0f)

        val gameDb = GameDatabase.getDatabase(appContext)
        val placeDb = AppDatabase.getDatabase(appContext)
        val exploredDao = gameDb.exploredAreaDao()
        val radius = visionRadius(appContext)
        val connectionMode = appContext
            .getSharedPreferences("map_customization_prefs", Context.MODE_PRIVATE)
            .getBoolean("connectionModeEnabled", false)

        val newExplored = mutableListOf<ExploredArea>()
        val dist = FloatArray(1)

        // 1. Strecke messen + optional Verbindungslinie aufdecken
        val pLat = lastLat
        val pLon = lastLon
        if (pLat != null && pLon != null) {
            Location.distanceBetween(pLat, pLon, lat, lon, dist)
            val d = dist[0]
            if (d in 2f..MAX_PLAUSIBLE_STEP_M) {
                PlayerProfile.addDistanceMeters(appContext, d)
            }
            if (connectionMode && d > radius && d <= MAX_CONNECTION_GAP_M) {
                val steps = (d / radius).toInt()
                for (i in 1 until steps) {
                    val f = i.toDouble() / steps
                    val iLat = pLat + f * (lat - pLat)
                    val iLon = pLon + f * (lon - pLon)
                    if (!isExplored(exploredDao, iLat, iLon, radius)) {
                        val area = ExploredArea(lat = iLat, lon = iLon, radius = radius)
                        exploredDao.insert(area)
                        newExplored += area
                    }
                }
            }
        }
        lastLat = lat
        lastLon = lon

        // 2. Nebel lüften
        if (!isExplored(exploredDao, lat, lon, radius)) {
            val area = ExploredArea(lat = lat, lon = lon, radius = radius)
            exploredDao.insert(area)
            newExplored += area
            PlayerProfile.incrementExplored(appContext)
        }

        // 3. Dungeons in Reichweite automatisch looten (Haupt-DB + aktive Addons)
        val detectionRadius =
            if (prefs.getBoolean("dev_magnet_mode", false)) MAGNET_RADIUS_M else DETECTION_RADIUS_M
        val box = 0.005
        val candidates = placeDb.placeDao().getPlacesInArea(lat - box, lat + box, lon - box, lon + box) +
            currentAddonPlaces(appContext).filter {
                it.lat in (lat - box)..(lat + box) && it.lon in (lon - box)..(lon + box)
            }

        val looted = mutableListOf<PlaceEntity>()
        for (place in candidates.distinctBy { it.osm_id }) {
            Location.distanceBetween(lat, lon, place.lat, place.lon, dist)
            if (dist[0] > detectionRadius) continue
            if (gameDb.visitedDungeonDao().alreadyVisited(place.osm_id) > 0) continue

            val xp = xpFor(place)
            gameDb.visitedDungeonDao().insert(VisitedDungeon(osm_id = place.osm_id, xpEarned = xp))
            PlayerProfile.incrementDungeons(appContext)
            PlayerProfile.addXP(appContext, xp)
            DungeonNotifier.notifyDungeonLooted(appContext, place, xp)
            looted += place
        }

        Result(newExplored, looted)
    }
}
