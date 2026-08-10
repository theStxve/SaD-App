package com.sad.app.data

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val uriString: String
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
    private var progressJob: Job? = null
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

    var currentTrackArtist by mutableStateOf("")
        private set

    var currentPositionMs by mutableLongStateOf(0L)
        private set

    var durationMs by mutableLongStateOf(0L)
        private set

    private var isAppInForeground = false

    fun init(context: Context) {
        val appContext = context.applicationContext

        // Evtl. altes generiertes Synth-WAV aus dem Cache aufräumen
        try {
            val oldSynthFile = File(appContext.cacheDir, "cyberpunk_synth_ambient.wav")
            if (oldSynthFile.exists()) {
                oldSynthFile.delete()
            }
        } catch (e: Exception) {}

        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean(KEY_ENABLED, false)
        volume = prefs.getFloat(KEY_VOLUME, 0.7f)
        val modeStr = prefs.getString(KEY_MODE, PlaybackMode.SEQUENTIAL.name) ?: PlaybackMode.SEQUENTIAL.name
        playbackMode = try { PlaybackMode.valueOf(modeStr) } catch (e: Exception) { PlaybackMode.SEQUENTIAL }
        currentSongIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)

        // Playlist aus SharedPreferences laden
        playlist.clear()
        val savedPlaylistJson = prefs.getString(KEY_PLAYLIST, null)
        if (!savedPlaylistJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedPlaylistJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val uriStr = obj.optString("uriString", "")
                    if (uriStr.isNotBlank()) {
                        playlist.add(
                            SongItem(
                                id = obj.optString("id", "custom_$i"),
                                title = obj.optString("title", "Unbekannter Track"),
                                artist = obj.optString("artist", "Eigener Song"),
                                uriString = uriStr
                            )
                        )
                    }
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
            currentTrackTitle = "Keine Songs"
            currentTrackArtist = "Füge eigene Songs hinzu"
            currentPositionMs = 0L
            durationMs = 0L
        }
    }

    fun toggleEnabled(context: Context, enabled: Boolean) {
        isEnabled = enabled
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()

        if (enabled) {
            if (isAppInForeground && playlist.isNotEmpty()) {
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

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            try {
                val target = positionMs.coerceIn(0L, durationMs).toInt()
                mp.seekTo(target)
                currentPositionMs = target.toLong()
            } catch (e: Exception) {}
        }
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
        if (playlist.isEmpty()) return

        if (!isEnabled) {
            toggleEnabled(context, true)
            return
        }

        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                isPlaying = false
                progressJob?.cancel()
            } else {
                mp.start()
                isPlaying = true
                startProgressTracker()
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
            try {
                appContext.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported by provider
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
                uriString = uri.toString()
            )

            playlist.add(item)
            saveCustomSongs(appContext)

            updateCurrentTrackInfo()
            if (playlist.size == 1 && isEnabled) {
                playTrackAtIndex(context, 0)
            }
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removeCustomSong(context: Context, songId: String) {
        val idx = playlist.indexOfFirst { it.id == songId }
        if (idx != -1) {
            playlist.removeAt(idx)
            saveCustomSongs(context.applicationContext)

            if (currentSongIndex >= playlist.size) {
                currentSongIndex = (playlist.size - 1).coerceAtLeast(0)
            }
            updateCurrentTrackInfo()

            if (playlist.isEmpty()) {
                stopAndRelease()
            } else if (isPlaying && isEnabled) {
                playCurrent(context)
            }
        }
    }

    private fun saveCustomSongs(context: Context) {
        val array = JSONArray()
        for (song in playlist) {
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
        if (isEnabled && playlist.isNotEmpty()) {
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
        if (playlist.isEmpty()) {
            stopAndRelease()
            return
        }
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
            durationMs = mp.duration.toLong().coerceAtLeast(0L)
            updateCurrentTrackInfo()
            startProgressTracker()

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

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isPlaying) {
                mediaPlayer?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            currentPositionMs = mp.currentPosition.toLong()
                            durationMs = mp.duration.toLong().coerceAtLeast(0L)
                        }
                    } catch (e: Exception) {}
                }
                delay(250)
            }
        }
    }

    private fun fadeOutAndPause() {
        fadeJob?.cancel()
        progressJob?.cancel()
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
        progressJob?.cancel()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (e: Exception) {}
        }
        mediaPlayer = null
        isPlaying = false
        currentPositionMs = 0L
    }

    fun formatTime(ms: Long): String {
        if (ms <= 0) return "00:00"
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}
