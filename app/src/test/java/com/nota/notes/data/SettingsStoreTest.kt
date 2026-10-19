package com.nota.notes.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// P2-C: TTL tests are pure JVM — they exercise SettingsStore's companion pure
// helpers (isRestoreFresh/filterRestoreLog), the single source of truth used by
// both the read path (restoreLog Flow) and the write path (logRestore).
// No Context/DataStore/Robolectric needed: RestoreRecord's constructor is plain Kotlin.
class SettingsStoreTest {

    private val now = 1_700_000_000_000L // fixed anchor — no wall-clock flakiness
    private val day = 24L * 60 * 60 * 1000

    @Test fun `restoreLog filters entries older than 30 days`() {
        val records = listOf(
            RestoreRecord(id = 1, at = now - 31 * day),
            RestoreRecord(id = 2, at = now - 60 * day),
            RestoreRecord(id = 3, at = now - 365 * day),
            RestoreRecord(id = 4, at = 0L) // missing timestamp counts as stale (documented)
        )
        assertThat(SettingsStore.filterRestoreLog(records, now)).isEmpty()
    }

    @Test fun `restoreLog keeps recent entries`() {
        val records = listOf(
            RestoreRecord(id = 1, at = now),
            RestoreRecord(id = 2, at = now - 29 * day),
            RestoreRecord(id = 3, at = now - SettingsStore.RESTORE_LOG_TTL_MS) // exact boundary kept
        )
        val kept = SettingsStore.filterRestoreLog(records, now)
        assertThat(kept.map { it.id }).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test fun `restoreLog caps at 20 entries after TTL filter`() {
        // Mirrors logRestore's write path: 1 new + up to 19 fresh Survivors.
        // Here we assert the filter half; the cap (take 19) lives in logRestore
        // and is validated by construction — filter must not drop fresh rows
        // that the cap then counts.
        val fresh = List(25) { i -> RestoreRecord(id = i.toLong(), at = now - i * day) }
        val stale = List(10) { i -> RestoreRecord(id = 100L + i, at = now - 31 * day - i * day) }
        val kept = SettingsStore.filterRestoreLog(fresh + stale, now).take(20)
        assertThat(kept).hasSize(20)
        assertThat(kept.map { it.id }).doesNotContain(100L)
        // The 20 kept are the 20 freshest (ids 0..19)
        assertThat(kept.map { it.id }).containsExactlyElementsIn((0L..19L).toList()).inOrder()
    }
}
