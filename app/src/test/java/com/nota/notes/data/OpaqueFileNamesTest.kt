package com.nota.notes.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// P3-D2: pure JVM — no Android APIs.
class OpaqueFileNamesTest {

    @Test fun `random names are 32-hex plus sanitized extension`() {
        val n = OpaqueFileNames.random("jpg")
        assertThat(n).matches("^[0-9a-f]{32}\\.jpg$")
        assertThat(OpaqueFileNames.isOpaque(n)).isTrue()
    }

    @Test fun `random names are unique and extension is sanitized`() {
        val names = (1..32).map { OpaqueFileNames.random("png") }.toSet()
        assertThat(names).hasSize(32)
        assertThat(OpaqueFileNames.random("../../JPG ").takeLast(3)).isEqualTo("jpg")
        assertThat(OpaqueFileNames.random("")).endsWith(".bin")
    }

    @Test fun `legacy timestamp names are detected`() {
        listOf("img_1729000000000.jpg", "file_1729000000000.pdf", "rec_1729000000000.m4a",
            "draw_1729000000000.png", "bg_1729000000000.jpg", "bg_1729000000000_copy.jpg"
        ).forEach { assertThat(OpaqueFileNames.isLegacy(it)).isTrue() }
    }

    @Test fun `opaque names and key sidecars are not legacy`() {
        val n = OpaqueFileNames.random("jpg")
        assertThat(OpaqueFileNames.isLegacy(n)).isFalse()
        assertThat(OpaqueFileNames.isLegacy("img_123.jpg.key")).isFalse()
        assertThat(OpaqueFileNames.isLegacy("view_tmp.jpg")).isFalse()
    }

    @Test fun `extOf preserves real extensions for migration`() {
        assertThat(OpaqueFileNames.extOf("file_123.pdf")).isEqualTo("pdf")
        assertThat(OpaqueFileNames.extOf("rec_123.m4a")).isEqualTo("m4a")
        assertThat(OpaqueFileNames.extOf("noext")).isEqualTo("bin")
    }
}
