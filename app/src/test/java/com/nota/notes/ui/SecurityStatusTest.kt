package com.nota.notes.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// P3-A3: pure-JVM tests for the security-status summary predicate.
// The 5 rows themselves (unlock / recovery key / device reset / auto-lock /
// screenshots) are Compose UI verified on-device (nota-p3-a3-status.md).
class SecurityStatusTest {

    @Test fun `all good when unlock strong, key set, device off, shots blocked`() {
        assertThat(isSecurityAllGood(true, true, false, true)).isTrue()
    }

    @Test fun `not good without strong unlock`() {
        assertThat(isSecurityAllGood(false, true, false, true)).isFalse()
    }

    @Test fun `not good without recovery key`() {
        assertThat(isSecurityAllGood(true, false, false, true)).isFalse()
    }

    @Test fun `not good when device reset enabled`() {
        assertThat(isSecurityAllGood(true, true, true, true)).isFalse()
    }

    @Test fun `not good when screenshots allowed`() {
        assertThat(isSecurityAllGood(true, true, false, false)).isFalse()
    }

    @Test fun `not good when everything off`() {
        assertThat(isSecurityAllGood(false, false, true, false)).isFalse()
    }
}
