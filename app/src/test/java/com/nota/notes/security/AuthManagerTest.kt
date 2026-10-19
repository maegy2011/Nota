package com.nota.notes.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// P2-A decision: pure-function extraction (isAutoLockExpired) instead of
// Robolectric — no Context/ClipboardManager/BiometricPrompt involved in the
// lock-timeout decision, and AuthManager's signature is unchanged.
class AuthManagerTest {

    @Test fun `no lock before timeout elapses`() {
        val now = 1_000_000L
        assertThat(isAutoLockExpired(lastUnlock = now - 10_000L, now = now, autoLockMs = 30_000L, unlocked = true)).isFalse()
    }

    @Test fun `lock after timeout elapses`() {
        val now = 1_000_000L
        assertThat(isAutoLockExpired(lastUnlock = now - 31_000L, now = now, autoLockMs = 30_000L, unlocked = true)).isTrue()
    }

    @Test fun `NEVER mode never locks`() {
        val now = 1_000_000L
        assertThat(isAutoLockExpired(lastUnlock = 0L, now = now, autoLockMs = -1L, unlocked = true)).isFalse()
    }

    @Test fun `already locked stays expired-false`() {
        val now = 1_000_000L
        assertThat(isAutoLockExpired(lastUnlock = now - 999_000L, now = now, autoLockMs = 30_000L, unlocked = false)).isFalse()
    }

    @Test fun `exact boundary does not lock`() {
        // Strict `>` in production logic: now-last == timeout → still open
        val now = 1_000_000L
        assertThat(isAutoLockExpired(lastUnlock = now - 30_000L, now = now, autoLockMs = 30_000L, unlocked = true)).isFalse()
        assertThat(isAutoLockExpired(lastUnlock = now - 30_001L, now = now, autoLockMs = 30_000L, unlocked = true)).isTrue()
    }
}
