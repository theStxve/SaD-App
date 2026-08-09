package com.sad.app.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val uriString: String,
    val isBuiltIn: Boolean = false
)

enum class PlaybackMode(val label: String) {
    SINGLE_LOOP("Einzel-Loop"),
    SEQUENTIAL("Reihenfolge"),
    SHUFFLE("Shuffle")
}

object MusicManager {
    private const val PREFS_NAME = "bgm_settings"
    private const val KEY_ENABLED = "bgm_enabled"
    private const val KEY_VOLUME = "bgm_volume"
    private const val KEY_MODE = "bgm_mode"
    private const val KEY_PLAYLIST = "bgm_playlist"
    private const val KEY_CURRENT_INDEX = "bgm_current_index"

    private var mediaPlayer: MediaPlayer? = null
    private var fadeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Reactive Compose States
    var isEnabled by mutableStateOf(false)
        private set

    var volume by mutableFloatStateOf(0.7f)
        private set

    var playbackMode by mutableStateOf(PlaybackMode.SEQUENTIAL)
        private set

    val playlist = mutableStateListOf<SongItem>()

    var currentSongIndex by mutableIntStateOf(0)
        private set

    var isPlaying by mutableStateOf(false)
        private set

    var currentTrackTitle by mutableStateOf("Kein Song")
        private set

    var currentTrackArtist by mutableStateOf("SAD Audio")
        private set

    private var isAppInForeground = false
    private var builtInSongFile: File? = null

    fun init(context: Context) {
        val appContext = context.applicationContext

        // Built-in Synth Ambient generieren falls nötig
        ensureBuiltInAmbientTrack(appContext)

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean(KEY_ENABLED, false)
        volume = prefs.getFloat(KEY_VOLUME, 0.7f)
        val modeStr = prefs.getString(KEY_MODE, PlaybackMode.SEQUENTIAL.name) ?: PlaybackMode.SEQUENTIAL.name
        playbackMode = try { PlaybackMode.valueOf(modeStr) } catch (e: Exception) { PlaybackMode.SEQUENTIAL }
        currentSongIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)

        // Playlist laden
        playlist.clear()

        // 1. Built-in Synth Song immer als ersten Track hinzufügen
        builtInSongFile?.let { file ->
            playlist.add(
                SongItem(
                    id = "builtin_ambient_01",
                    title = "Cyberpunk Synth Ambient",
                    artist = "SAD Underground Protocol",
                    uriString = Uri.fromFile(file).toString(),
                    isBuiltIn = true
                )
            )
        }

        // 2. Eigene Songs aus SharedPreferences laden
        val savedPlaylistJson = prefs.getString(KEY_PLAYLIST, null)
        if (!savedPlaylistJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedPlaylistJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    playlist.add(
                        SongItem(
                            id = obj.optString("id", "custom_$i"),
                            title = obj.optString("title", "Unbekannter Track"),
                            artist = obj.optString("artist", "Eigener Song"),
                            uriString = obj.optString("uriString", ""),
                            isBuiltIn = false
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (currentSongIndex >= playlist.size) {
            currentSongIndex = 0
        }

        updateCurrentTrackInfo()
    }

    private fun updateCurrentTrackInfo() {
        if (playlist.isNotEmpty() && currentSongIndex in playlist.indices) {
            val track = playlist[currentSongIndex]
            currentTrackTitle = track.title
            currentTrackArtist = track.artist
        } else {
            currentTrackTitle = "Kein Song"
            currentTrackArtist = ""
        }
    }

    fun toggleEnabled(context: Context, enabled: Boolean) {
        isEnabled = enabled
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

        if (enabled) {
            if (isAppInForeground) {
                playCurrent(context)
            }
        } else {
            stopAndRelease()
        }
    }

    fun setVolume(context: Context, newVolume: Float) {
        volume = newVolume.coerceIn(0.0f, 1.0f)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_VOLUME, volume).apply()
        mediaPlayer?.setVolume(volume, volume)
    }

    fun setMode(context: Context, mode: PlaybackMode) {
        playbackMode = mode
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun playTrackAtIndex(context: Context, index: Int) {
        if (index !in playlist.indices) return
        currentSongIndex = index
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CURRENT_INDEX, currentSongIndex).apply()
        updateCurrentTrackInfo()

        if (isEnabled && isAppInForeground) {
            playCurrent(context)
        }
    }

    fun togglePlayPause(context: Context) {
        if (!isEnabled) {
            toggleEnabled(context, true)
            return
        }

        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
            } else {
                mp.start()
                isPlaying = true
            }
        } ?: run {
            playCurrent(context)
        }
    }

    fun nextTrack(context: Context) {
        if (playlist.isEmpty()) return
        val nextIdx = when (playbackMode) {
            PlaybackMode.SINGLE_LOOP -> (currentSongIndex + 1) % playlist.size
            PlaybackMode.SEQUENTIAL -> (currentSongIndex + 1) % playlist.size
            PlaybackMode.SHUFFLE -> (0 until playlist.size).filter { it != currentSongIndex }.randomOrNull() ?: 0
        }
        playTrackAtIndex(context, nextIdx)
    }

    fun previousTrack(context: Context) {
        if (playlist.isEmpty()) return
        val prevIdx = if (currentSongIndex - 1 < 0) playlist.size - 1 else currentSongIndex - 1
        playTrackAtIndex(context, prevIdx)
    }

    fun addCustomSong(context: Context, uri: Uri): Result<SongItem> {
        val appContext = context.applicationContext
        return try {
            // Persistable Permission anfordern falls SAF Uri
            try {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Kann bei manchen Providern auftreten, ignorieren
            }

            var title = "Unbekannter Track"
            var artist = "Eigener Song"

            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(appContext, uri)
                val mTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val mArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                if (!mTitle.isNullOrBlank()) title = mTitle
                if (!mArtist.isNullOrBlank()) artist = mArtist
            } catch (e: Exception) {
                // Fallback: Dateiname aus Uri lesen
                uri.lastPathSegment?.let { segment ->
                    val clean = segment.substringAfterLast("/")
                    if (clean.isNotBlank()) title = clean
                }
            } finally {
                try { mmr.release() } catch (e: Exception) {}
            }

            val item = SongItem(
                id = "custom_${System.currentTimeMillis()}",
                title = title,
                artist = artist,
                uriString = uri.toString(),
                isBuiltIn = false
            )

            playlist.add(item)
            saveCustomSongs(appContext)

            // Falls es der erste Song war oder Musik aktiv ist, bereit machen
            updateCurrentTrackInfo()
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeCustomSong(context: Context, songId: String) {
        val idx = playlist.indexOfFirst { it.id == songId }
        if (idx != -1 && !playlist[idx].isBuiltIn) {
            playlist.removeAt(idx)
            saveCustomSongs(context.applicationContext)

            if (currentSongIndex >= playlist.size) {
                currentSongIndex = (playlist.size - 1).coerceAtLeast(0)
            }
            updateCurrentTrackInfo()

            if (isPlaying && isEnabled) {
                playCurrent(context)
            }
        }
    }

    private fun saveCustomSongs(context: Context) {
        val customList = playlist.filter { !it.isBuiltIn }
        val array = JSONArray()
        for (song in customList) {
            val obj = JSONObject()
            obj.put("id", song.id)
            obj.put("title", song.title)
            obj.put("artist", song.artist)
            obj.put("uriString", song.uriString)
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PLAYLIST, array.toString()).apply()
    }

    // App Foreground / Background Hooks
    fun onAppForeground(context: Context) {
        isAppInForeground = true
        if (isEnabled) {
            playCurrent(context, fadeIn = true)
        }
    }

    fun onAppBackground() {
        isAppInForeground = false
        if (isPlaying) {
            fadeOutAndPause()
        }
    }

    private fun playCurrent(context: Context, fadeIn: Boolean = false) {
        if (playlist.isEmpty()) return
        val currentTrack = playlist.getOrNull(currentSongIndex) ?: return

        fadeJob?.cancel()

        try {
            stopAndRelease()

            val mp = MediaPlayer()
            val uri = Uri.parse(currentTrack.uriString)
            mp.setDataSource(context.applicationContext, uri)
            mp.prepare()

            val initialVol = if (fadeIn) 0.0f else volume
            mp.setVolume(initialVol, initialVol)

            mp.isLooping = (playbackMode == PlaybackMode.SINGLE_LOOP)

            mp.setOnCompletionListener {
                if (playbackMode != PlaybackMode.SINGLE_LOOP) {
                    nextTrack(context)
                }
            }

            mp.start()
            mediaPlayer = mp
            isPlaying = true
            updateCurrentTrackInfo()

            if (fadeIn) {
                fadeJob = scope.launch {
                    val steps = 10
                    val targetVol = volume
                    for (i in 1..steps) {
                        delay(50)
                        val stepVol = targetVol * (i.toFloat() / steps)
                        mediaPlayer?.setVolume(stepVol, stepVol)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    private fun fadeOutAndPause() {
        fadeJob?.cancel()
        val mp = mediaPlayer ?: return
        if (!mp.isPlaying) return

        fadeJob = scope.launch {
            val steps = 10
            val startVol = volume
            for (i in steps downTo 0) {
                delay(40)
                val stepVol = startVol * (i.toFloat() / steps)
                try {
                    mediaPlayer?.setVolume(stepVol, stepVol)
                } catch (e: Exception) {}
            }
            try {
                mediaPlayer?.pause()
            } catch (e: Exception) {}
            isPlaying = false
        }
    }

    private fun stopAndRelease() {
        fadeJob?.cancel()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (e: Exception) {}
        }
        mediaPlayer = null
        isPlaying = false
    }

    /**
     * Synthesizes a atmospheric 10-second Cyberpunk Synth Ambient WAV loop into app cache if missing.
     * Uses a sub-bass drone (55Hz) + dual detuned synth pads (110Hz / 112Hz) with smooth modulation.
     */
    private fun ensureBuiltInAmbientTrack(context: Context) {
        val file = File(context.cacheDir, "cyberpunk_synth_ambient.wav")
        builtInSongFile = file
        if (file.exists() && file.length() > 1000) return

        scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 44100
                val durationSec = 12
                val numSamples = sampleRate * durationSec
                val pcmData = ShortArray(numSamples)

                val freqSub = 55.0 // A1 Sub-bass
                val freqPad1 = 110.0 // A2
                val freqPad2 = 164.81 // E3 (fifth)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Envelope for seamless loop fading
                    val loopEnv = Math.sin(Math.PI * (t / durationSec))

                    // Low LFO for pulsing filter effect
                    val lfo = 0.6 + 0.4 * Math.sin(2 * Math.PI * 0.25 * t)

                    // Sub-bass tone
                    val sub = Math.sin(2 * Math.PI * freqSub * t) * 0.45

                    // Warm detuned synth pad
                    val pad1 = Math.sin(2 * Math.PI * freqPad1 * t) * 0.25
                    val pad2 = Math.sin(2 * Math.PI * (freqPad2 + 0.5) * t) * 0.20

                    val mix = (sub + (pad1 + pad2) * lfo) * loopEnv * 0.7
                    pcmData[i] = (mix * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                // Write WAV file header + PCM data
                val byteBuffer = ByteBuffer.allocate(44 + numSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
                // RIFF header
                byteBuffer.put("RIFF".toByteArray())
                byteBuffer.putInt(36 + numSamples * 2)
                byteBuffer.put("WAVE".toByteArray())
                // fmt chunk
                byteBuffer.put("fmt ".toByteArray())
                byteBuffer.putInt(16) // Subchunk1Size
                byteBuffer.putShort(1.toShort()) // AudioFormat (PCM)
                byteBuffer.putShort(1.toShort()) // NumChannels (Mono)
                byteBuffer.putInt(sampleRate)
                byteBuffer.putInt(sampleRate * 2) // ByteRate
                byteBuffer.putShort(2.toShort()) // BlockAlign
                byteBuffer.putShort(16.toShort()) // BitsPerSample
                // data chunk
                byteBuffer.put("data".toByteArray())
                byteBuffer.putInt(numSamples * 2)

                for (sample in pcmData) {
                    byteBuffer.putShort(sample)
                }

                FileOutputStream(file).use { fos ->
                    fos.write(byteBuffer.array())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
