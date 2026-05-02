package com.sad.app.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.sad.app.data.GameDatabase
import com.sad.app.data.Rumor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.nio.charset.StandardCharsets

/**
 * Verwaltet den P2P-Austausch von Gerüchten über Nearby Connections.
 * Kein Server nötig - läuft komplett über Bluetooth/WiFi Direct.
 */
class RumorSyncManager(
    private val context: Context,
    private val gameDb: GameDatabase,
    private val onStatusChanged: (String) -> Unit
) {
    private val SERVICE_ID = "com.sad.app.rumors"
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Generiere einen anonymen Spielernamen für dieses Gerät
    val localPlayerName: String by lazy {
        val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
        prefs.getString("player_name", null) ?: run {
            val adjectives = listOf("Dunkler", "Kühner", "Stiller", "Wilder", "Flinker")
            val nouns = listOf("Erkunder", "Wanderer", "Jäger", "Läufer", "Sucher")
            val name = "${adjectives.random()} ${nouns.random()}"
            prefs.edit().putString("player_name", name).apply()
            name
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { bytes ->
                val json = String(bytes, StandardCharsets.UTF_8)
                parseAndSaveRumor(json)
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Automatisch akzeptieren
            Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback)
            onStatusChanged("🤝 Verbinde mit ${info.endpointName}...")
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                onStatusChanged("✅ Verbunden mit Spieler!")
                // Gerüchte sofort senden
                sendLatestRumors(endpointId)
            }
        }

        override fun onDisconnected(endpointId: String) {
            onStatusChanged("🔌 Verbindung getrennt")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            onStatusChanged("📡 Spieler entdeckt: ${info.endpointName}")
            Nearby.getConnectionsClient(context).requestConnection(
                localPlayerName, endpointId, connectionLifecycleCallback
            )
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    fun startAdvertisingAndDiscovery() {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        
        Nearby.getConnectionsClient(context).startAdvertising(
            localPlayerName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions
        ).addOnSuccessListener {
            onStatusChanged("📡 Suche nach Spielern in der Nähe...")
        }.addOnFailureListener {
            onStatusChanged("⚠️ Nearby nicht verfügbar")
        }

        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        
        Nearby.getConnectionsClient(context).startDiscovery(
            SERVICE_ID, endpointDiscoveryCallback, discoveryOptions
        )
    }

    fun stopAll() {
        Nearby.getConnectionsClient(context).stopAllEndpoints()
        Nearby.getConnectionsClient(context).stopAdvertising()
        Nearby.getConnectionsClient(context).stopDiscovery()
    }

    private fun sendLatestRumors(endpointId: String) {
        scope.launch {
            val prefs = context.getSharedPreferences("player_profile", Context.MODE_PRIVATE)
            val xp = prefs.getInt("xp", 0)
            val dungeonCount = prefs.getInt("visited_dungeons", 0)
            
            // Hole einen zufälligen bereits besuchten Dungeon
            val visitedDungeons = gameDb.visitedDungeonDao().getAll()
            val appDb = com.sad.app.data.AppDatabase.getDatabase(context)

            val json = JSONObject().apply {
                put("type", "player_rumors")
                put("fromPlayer", localPlayerName)
                put("xp", xp)
                put("dungeonCount", dungeonCount)

                if (visitedDungeons.isNotEmpty()) {
                    val randomId = visitedDungeons.random().osm_id
                    val place = appDb.placeDao().getPlaceById(randomId)
                    if (place != null) {
                        put("lat", place.lat)
                        put("lon", place.lon)
                        put("placeName", place.name.ifEmpty { "Unbekannter Dungeon" })
                        put("placeType", place.type)
                        
                        val hints = listOf(
                            "Dort gibt es guten Loot: ${place.name.ifEmpty{"Unbekannt"}}",
                            "Vorsicht! Ich habe ${place.name.ifEmpty{"etwas"}} erkundet und es war hart.",
                            "Geheimtipp: Bei diesen Koordinaten ist ein ${place.type}.",
                            "Ich habe $dungeonCount Dungeons geschafft. Dieser hier war besonders."
                        )
                        put("message", hints.random())
                    } else {
                        fallbackMessage(dungeonCount, xp)
                    }
                } else {
                    fallbackMessage(dungeonCount, xp)
                }
            }.toString()

            val payload = Payload.fromBytes(json.toByteArray(StandardCharsets.UTF_8))
            Nearby.getConnectionsClient(context).sendPayload(endpointId, payload)
            onStatusChanged("Echtes Gerücht gesendet!")
        }
    }
    
    private fun JSONObject.fallbackMessage(dungeonCount: Int, xp: Int) {
        put("message", "Ich habe ${dungeonCount} Dungeons erkundet und ${xp} XP gesammelt!")
        put("lat", 0.0)
        put("lon", 0.0)
        put("placeName", "Spieler-Update")
        put("placeType", "info")
    }

    private fun parseAndSaveRumor(json: String) {
        try {
            val obj = JSONObject(json)
            val rumor = Rumor(
                placeName = obj.getString("placeName"),
                placeType = obj.getString("placeType"),
                lat = obj.getDouble("lat"),
                lon = obj.getDouble("lon"),
                message = obj.getString("message"),
                fromPlayer = obj.getString("fromPlayer")
            )
            scope.launch {
                gameDb.rumorDao().insert(rumor)
            }
            onStatusChanged("💬 Neues Gerücht erhalten!")
        } catch (e: Exception) {
            Log.e("RumorSync", "Parse error", e)
        }
    }
}
