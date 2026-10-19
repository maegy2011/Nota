package com.nota.notes.security

import com.google.common.truth.Truth.assertThat
import java.security.SecureRandom
import org.junit.Test

// P2-A: behavioral tests only — no asserts on iteration counts or internals.
class PinHashTest {

    private fun salt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    @Test fun `verify returns true for correct pin`() {
        val h = PinHash.hash("4821", salt())
        assertThat(PinHash.verify("4821", h)).isTrue()
    }

    @Test fun `verify returns false for wrong pin`() {
        val h = PinHash.hash("4821", salt())
        assertThat(PinHash.verify("4822", h)).isFalse()
    }

    @Test fun `verify returns false for empty pin`() {
        val h = PinHash.hash("4821", salt())
        assertThat(PinHash.verify("", h)).isFalse()
    }

    @Test fun `hash produces different output for different pins`() {
        val h1 = PinHash.hash("1111", salt())
        val h2 = PinHash.hash("2222", salt())
        assertThat(h1).isNotEqualTo(h2)
    }

    @Test fun `hash is deterministic for same pin and salt`() {
        val s = salt()
        assertThat(PinHash.hash("4821", s)).isEqualTo(PinHash.hash("4821", s))
    }

    @Test fun `verify handles malformed hash format gracefully`() {
        val h = PinHash.hash("4821", salt())
        // No crash — always false:
        assertThat(PinHash.verify("4821", "")).isFalse()
        assertThat(PinHash.verify("4821", "garbage-no-format")).isFalse()
        assertThat(PinHash.verify("4821", "a:b:c")).isFalse()
        assertThat(PinHash.verify("4821", "pbkdf2\$abc\$zz\$qq")).isFalse()
        assertThat(PinHash.verify("4821", "pbkdf2\$0\$\$")).isFalse()
        assertThat(PinHash.verify("4821", "pbkdf2\$-5\$aa\$bb")).isFalse()
        // Sanity: the valid hash still verifies after the malformed attempts
        assertThat(PinHash.verify("4821", h)).isTrue()
    }

    @Test fun `legacy v1 format still verifies`() {
        // V1 = saltHex:sha256(salt+pin) — accepted for backward compat
        val s = salt()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        md.update(s)
        md.update("4821".toByteArray())
        val hex = { b: ByteArray -> b.joinToString("") { "%02x".format(it) } }
        val legacy = "${hex(s)}:${hex(md.digest())}"
        assertThat(PinHash.isLegacy(legacy)).isTrue()
        assertThat(PinHash.verify("4821", legacy)).isTrue()
        assertThat(PinHash.verify("wrong", legacy)).isFalse()
    }
}
