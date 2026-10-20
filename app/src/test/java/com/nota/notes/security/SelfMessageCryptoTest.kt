package com.nota.notes.security

import com.google.common.truth.Truth.assertThat
import javax.crypto.KeyGenerator
import org.junit.Test

// P3-D4: pure JVM — seal/open/decryptOrLegacy take an injected SecretKey (software AES here).
// The thin Keystore layer (deviceKey) is device-only by design and out of scope.
class SelfMessageCryptoTest {

    private fun softwareKey(): javax.crypto.SecretKey {
        val gen = KeyGenerator.getInstance("AES")
        gen.init(256)
        return gen.generateKey()
    }

    @Test fun `seal then open roundtrips text`() {
        val key = softwareKey()
        val sealed = SelfMessageCrypto.seal(key, "رسالة سرية 123")
        assertThat(SelfMessageCrypto.isSealed(sealed)).isTrue()
        assertThat(SelfMessageCrypto.open(key, sealed)).isEqualTo("رسالة سرية 123")
    }

    @Test fun `sealed envelopes are non-deterministic (fresh IV)`() {
        val key = softwareKey()
        assertThat(SelfMessageCrypto.seal(key, "same")).isNotEqualTo(SelfMessageCrypto.seal(key, "same"))
    }

    @Test fun `open with wrong key fails closed`() {
        val sealed = SelfMessageCrypto.seal(softwareKey(), "secret")
        assertThat(SelfMessageCrypto.open(softwareKey(), sealed)).isNull()
    }

    @Test fun `open rejects tampered and malformed input`() {
        val key = softwareKey()
        val sealed = SelfMessageCrypto.seal(key, "secret")
        val tampered = sealed.dropLast(2) + if (sealed.last() == 'A') 'B' else 'A'
        assertThat(SelfMessageCrypto.open(key, tampered)).isNull()
        assertThat(SelfMessageCrypto.open(key, "plain legacy")).isNull()
        assertThat(SelfMessageCrypto.open(key, "")).isNull()
    }

    @Test fun `decryptOrLegacy passes legacy through and never throws`() {
        val key = softwareKey()
        assertThat(SelfMessageCrypto.decryptOrLegacy(key, "legacy plain")).isEqualTo("legacy plain")
        assertThat(SelfMessageCrypto.decryptOrLegacy(null, "legacy plain")).isEqualTo("legacy plain")
        val sealed = SelfMessageCrypto.seal(key, "hi")
        assertThat(SelfMessageCrypto.decryptOrLegacy(key, sealed)).isEqualTo("hi")
        // corrupt sealed falls back to stored-as-is, never throws
        assertThat(SelfMessageCrypto.decryptOrLegacy(key, sealed.dropLast(4) + "AAAA")).isNotNull()
        assertThat(SelfMessageCrypto.decryptOrLegacy(null, sealed)).isEqualTo(sealed)
    }

    @Test fun `empty text seals and opens`() {
        val key = softwareKey()
        assertThat(SelfMessageCrypto.open(key, SelfMessageCrypto.seal(key, ""))).isEqualTo("")
    }
}
