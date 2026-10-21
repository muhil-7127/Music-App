package com.muhil.music.media

import org.junit.Assert.*
import org.junit.Test

class EqualizerModelTest {

    @Test
    fun bandModel_computesDbAndFormattingAccurately() {
        val bandLow = EqualizerBandModel(
            bandIndex = 0,
            centerFreqHz = 60,
            levelMb = 600,
            minLevelMb = -1500,
            maxLevelMb = 1500
        )
        assertEquals(6.0f, bandLow.levelDb, 0.01f)
        assertEquals(-15.0f, bandLow.minLevelDb, 0.01f)
        assertEquals(15.0f, bandLow.maxLevelDb, 0.01f)
        assertEquals("60 Hz", bandLow.formattedFreq)

        val bandHigh = EqualizerBandModel(
            bandIndex = 4,
            centerFreqHz = 14000,
            levelMb = -450,
            minLevelMb = -1500,
            maxLevelMb = 1500
        )
        assertEquals(-4.5f, bandHigh.levelDb, 0.01f)
        assertEquals("14 kHz", bandHigh.formattedFreq)
    }

    @Test
    fun presets_containExpectedAudioProfiles() {
        val flat = AudioEqualizerManager.PRESETS["Flat"]
        assertNotNull(flat)
        assertEquals(5, flat?.size)
        assertTrue(flat!!.all { it == 0.toShort() })

        val bassBoost = AudioEqualizerManager.PRESETS["Bass Boost"]
        assertNotNull(bassBoost)
        assertTrue(bassBoost!![0] > 0) // Low frequencies amplified

        val trebleBoost = AudioEqualizerManager.PRESETS["Treble Boost"]
        assertNotNull(trebleBoost)
        assertTrue(trebleBoost!![4] > 0) // High frequencies amplified

        val rock = AudioEqualizerManager.PRESETS["Rock"]
        assertNotNull(rock)
        assertEquals(5, rock?.size)

        val vocal = AudioEqualizerManager.PRESETS["Vocal"]
        assertNotNull(vocal)
        assertTrue(vocal!![2] > 0) // Mid/voice presence boosted
    }

    @Test
    fun defaultBands_hasFiveStandardBands() {
        val defaultBands = EqualizerUiState.defaultBands()
        assertEquals(5, defaultBands.size)
        assertEquals(60, defaultBands[0].centerFreqHz)
        assertEquals(230, defaultBands[1].centerFreqHz)
        assertEquals(910, defaultBands[2].centerFreqHz)
        assertEquals(3600, defaultBands[3].centerFreqHz)
        assertEquals(14000, defaultBands[4].centerFreqHz)
    }
}
