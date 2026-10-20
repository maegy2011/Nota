package com.nota.notes.security

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// P3-A2: pure-JVM tests — RecoveryKey has no Android/Keystore dependencies.
// Hash roundtrip (PinHash) and shield accounting (verifyRecoveryKey) are
// covered by PinHashTest + device checklist in nota-p3-a2-status.md.
class RecoveryKeyTest {

    @Test fun `wordlist holds the full BIP39 English set`() {
        assertThat(BIP39_ENGLISH).hasSize(2048)
        assertThat(BIP39_ENGLISH.distinct()).hasSize(2048)
        // Spot-check first/last entries of the official list
        assertThat(BIP39_ENGLISH.first()).isEqualTo("abandon")
        assertThat(BIP39_ENGLISH.last()).isEqualTo("zoo")
    }

    @Test fun `generate returns 12 words from the list`() {
        repeat(20) {
            val words = RecoveryKey.generate()
            assertThat(words).hasSize(12)
            // Per-word membership (the phrase may repeat a word — no multiplicity check)
            words.forEach { assertThat(BIP39_ENGLISH).contains(it) }
        }
    }

    @Test fun `generate is random`() {
        val seen = List(10) { RecoveryKey.generate().joinToString(" ") }.toSet()
        assertThat(seen.size).isGreaterThan(1)
    }

    @Test fun `normalize collapses whitespace and case`() {
        assertThat(RecoveryKey.normalize("  Abandon   ABILITY\nable  ")).isEqualTo("abandon ability able")
        assertThat(RecoveryKey.normalize("")).isEmpty()
    }

    @Test fun `wordCount counts exactly`() {
        assertThat(RecoveryKey.wordCount("")).isEqualTo(0)
        assertThat(RecoveryKey.wordCount("one two")).isEqualTo(2)
        assertThat(RecoveryKey.wordCount(RecoveryKey.generate().joinToString(" "))).isEqualTo(12)
    }

    @Test fun `quizIndices returns 3 distinct sorted indices in range`() {
        repeat(20) {
            val idx = RecoveryKey.quizIndices()
            assertThat(idx).hasSize(3)
            assertThat(idx.distinct()).hasSize(3)
            assertThat(idx.sorted()).isEqualTo(idx)
            assertThat(idx.min()).isAtLeast(0)
            assertThat(idx.max()).isAtMost(11)
        }
    }

    @Test fun `generated phrase survives normalize roundtrip`() {
        val words = RecoveryKey.generate()
        val raw = words.joinToString(" ")
        assertThat(RecoveryKey.normalize(raw)).isEqualTo(raw)
        assertThat(RecoveryKey.wordCount(RecoveryKey.normalize(raw))).isEqualTo(12)
    }
}
