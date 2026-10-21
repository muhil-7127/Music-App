package com.muhil.music.media

import org.junit.Assert.*
import org.junit.Test

class SleepTimerTest {

    @Test
    fun defaultState_isInactiveWithEmptyRemaining() {
        val state = SleepTimerState()
        assertFalse(state.isActive)
        assertEquals(0L, state.remainingSeconds)
        assertEquals(0, state.totalMinutes)
        assertEquals("", state.formattedRemaining)
    }

    @Test
    fun activeState_formatsRemainingMinutesAndSecondsCorrectly() {
        // 14 minutes and 32 seconds = 872 seconds
        val state1 = SleepTimerState(isActive = true, remainingSeconds = 872L, totalMinutes = 15)
        assertTrue(state1.isActive)
        assertEquals("14:32", state1.formattedRemaining)

        // Exactly 5 minutes = 300 seconds
        val state2 = SleepTimerState(isActive = true, remainingSeconds = 300L, totalMinutes = 5)
        assertEquals("05:00", state2.formattedRemaining)

        // 45 seconds
        val state3 = SleepTimerState(isActive = true, remainingSeconds = 45L, totalMinutes = 1)
        assertEquals("00:45", state3.formattedRemaining)
    }

    @Test
    fun expiredState_returnsEmptyFormattedTime() {
        val state = SleepTimerState(isActive = false, remainingSeconds = 0L, totalMinutes = 0)
        assertEquals("", state.formattedRemaining)
    }
}
