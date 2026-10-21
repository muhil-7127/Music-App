package com.muhil.music.media

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.muhil.music.data.local.preferences.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Representation of an individual equalizer frequency band.
 */
data class EqualizerBandModel(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val levelMb: Short,
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500
) {
    val levelDb: Float get() = levelMb / 100f
    val minLevelDb: Float get() = minLevelMb / 100f
    val maxLevelDb: Float get() = maxLevelMb / 100f

    val formattedFreq: String
        get() = if (centerFreqHz >= 1000) "${centerFreqHz / 1000} kHz" else "$centerFreqHz Hz"
}

/**
 * UI State for the Equalizer Screen.
 */
data class EqualizerUiState(
    val isSupported: Boolean = true,
    val isEnabled: Boolean = false,
    val currentPreset: String = "Flat",
    val bassBoostStrength: Int = 0, // 0 - 1000
    val bands: List<EqualizerBandModel> = defaultBands()
) {
    companion object {
        fun defaultBands(): List<EqualizerBandModel> = listOf(
            EqualizerBandModel(0, 60, 0),
            EqualizerBandModel(1, 230, 0),
            EqualizerBandModel(2, 910, 0),
            EqualizerBandModel(3, 3600, 0),
            EqualizerBandModel(4, 14000, 0)
        )
    }
}

@Singleton
class AudioEqualizerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer,
    private val preferencesManager: UserPreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    companion object {
        val PRESETS: Map<String, List<Short>> = mapOf(
            "Flat" to listOf(0, 0, 0, 0, 0),
            "Bass Boost" to listOf(600, 450, 100, 0, 0),
            "Treble Boost" to listOf(0, 0, 100, 450, 600),
            "Rock" to listOf(450, 250, -100, 200, 400),
            "Pop" to listOf(-100, 200, 500, 250, -100),
            "Classical" to listOf(400, 250, 0, 200, 300),
            "Jazz" to listOf(300, 200, 100, 200, 300),
            "Vocal" to listOf(-200, 100, 450, 300, 0)
        )
    }

    init {
        listenToExoPlayer()
        scope.launch {
            loadSavedPreferences()
        }
    }

    private fun listenToExoPlayer() {
        try {
            val audioSessionId = exoPlayer.audioSessionId
            if (audioSessionId > 0 && audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                initEffects(audioSessionId)
            }

            exoPlayer.addAnalyticsListener(object : AnalyticsListener {
                override fun onAudioSessionIdChanged(
                    eventTime: AnalyticsListener.EventTime,
                    audioSessionId: Int
                ) {
                    if (audioSessionId > 0 && audioSessionId != currentSessionId) {
                        initEffects(audioSessionId)
                    }
                }
            })
        } catch (e: Throwable) {
            Timber.w(e, "Could not attach ExoPlayer audio session listener")
            _uiState.update { it.copy(isSupported = false) }
        }
    }

    @Synchronized
    private fun initEffects(sessionId: Int) {
        if (sessionId <= 0 || sessionId == currentSessionId) return
        currentSessionId = sessionId

        releaseEffects()

        try {
            val eq = Equalizer(0, sessionId)
            val bb = BassBoost(0, sessionId)

            equalizer = eq
            bassBoost = bb

            // Read hardware band frequencies & ranges
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0]
            val maxMb = range[1]

            val bands = mutableListOf<EqualizerBandModel>()
            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val freqHz = (eq.getCenterFreq(bandIndex) / 1000).toInt()
                val currentLevel = eq.getBandLevel(bandIndex)
                bands.add(EqualizerBandModel(bandIndex, freqHz, currentLevel, minMb, maxMb))
            }

            _uiState.update {
                it.copy(
                    isSupported = true,
                    bands = if (bands.isNotEmpty()) bands else EqualizerUiState.defaultBands()
                )
            }

            // Apply previously saved state
            applyStateToHardware()
        } catch (e: Throwable) {
            Timber.w(e, "Hardware Equalizer not supported on this device/audio route")
            equalizer = null
            bassBoost = null
            _uiState.update { it.copy(isSupported = false) }
        }
    }

    private suspend fun loadSavedPreferences() {
        try {
            val enabled = preferencesManager.equalizerEnabled.first()
            val preset = preferencesManager.equalizerPreset.first()
            val bandsMap = preferencesManager.equalizerBands.first()
            val bbStrength = preferencesManager.bassBoostStrength.first()

            _uiState.update { current ->
                val updatedBands = current.bands.map { band ->
                    val savedLevel = bandsMap[band.bandIndex.toInt()]?.toShort()
                    if (savedLevel != null) band.copy(levelMb = savedLevel) else band
                }
                current.copy(
                    isEnabled = enabled,
                    currentPreset = preset,
                    bassBoostStrength = bbStrength,
                    bands = updatedBands
                )
            }

            applyStateToHardware()
        } catch (e: Throwable) {
            Timber.w(e, "Failed to load saved equalizer preferences")
        }
    }

    private fun applyStateToHardware() {
        val state = _uiState.value
        try {
            equalizer?.let { eq ->
                eq.enabled = state.isEnabled
                if (state.isEnabled) {
                    state.bands.forEach { band ->
                        val clamped = band.levelMb.coerceIn(band.minLevelMb, band.maxLevelMb)
                        eq.setBandLevel(band.bandIndex, clamped)
                    }
                }
            }

            bassBoost?.let { bb ->
                bb.enabled = state.isEnabled
                if (state.isEnabled) {
                    bb.setStrength(state.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (e: Throwable) {
            Timber.w(e, "Error applying equalizer state to hardware")
        }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabled = enabled) }
        applyStateToHardware()
        scope.launch {
            preferencesManager.setEqualizerEnabled(enabled)
        }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        _uiState.update { current ->
            val newBands = current.bands.map { band ->
                if (band.bandIndex == bandIndex) {
                    val clamped = levelMb.coerceIn(band.minLevelMb, band.maxLevelMb)
                    band.copy(levelMb = clamped)
                } else band
            }
            current.copy(bands = newBands, currentPreset = "Custom")
        }
        applyStateToHardware()
        saveBandsAndPreset()
    }

    fun setPreset(presetName: String) {
        val presetLevels = PRESETS[presetName] ?: return
        _uiState.update { current ->
            val newBands = current.bands.mapIndexed { index, band ->
                val level = presetLevels.getOrNull(index) ?: 0.toShort()
                val clamped = level.coerceIn(band.minLevelMb, band.maxLevelMb)
                band.copy(levelMb = clamped)
            }
            current.copy(currentPreset = presetName, bands = newBands)
        }
        applyStateToHardware()
        saveBandsAndPreset()
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _uiState.update { it.copy(bassBoostStrength = clamped) }
        try {
            bassBoost?.setStrength(clamped.toShort())
        } catch (e: Throwable) {
            Timber.w(e, "Error setting bass boost strength")
        }
        scope.launch {
            preferencesManager.setBassBoostStrength(clamped)
        }
    }

    fun resetToFlat() {
        setPreset("Flat")
        setBassBoost(0)
    }

    private fun saveBandsAndPreset() {
        val state = _uiState.value
        val map = state.bands.associate { it.bandIndex.toInt() to it.levelMb.toInt() }
        scope.launch {
            preferencesManager.setEqualizerPreset(state.currentPreset)
            preferencesManager.setEqualizerBands(map)
        }
    }

    private fun releaseEffects() {
        try {
            equalizer?.release()
        } catch (_: Throwable) {}
        equalizer = null

        try {
            bassBoost?.release()
        } catch (_: Throwable) {}
        bassBoost = null
    }
}
