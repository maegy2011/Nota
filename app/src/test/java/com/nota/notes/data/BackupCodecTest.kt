package com.nota.notes.data

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// P3-3أ: format v4 — id ثابت + توافق v2/v3 + sha256 في meta.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupCodecTest {

    private fun note(id: String, title: String) = BackupNote(
        id = id, title = title, body = "body-$title", createdAt = 1700000000000L,
        updatedAt = 1700000001000L, color = 1, archivedAt = null
    )

    @Test fun `encode v4 includes ids and sha256`() {
        val json = BackupCodec.encode(listOf(note("42", "a"), note("43", "b")), emptyList(), "1.0")
        val root = JSONObject(json)
        assertThat(root.optInt("v")).isEqualTo(4)
        val notes = root.getJSONArray("notes")
        assertThat(notes.getJSONObject(0).optString("id")).isEqualTo("42")
        assertThat(notes.getJSONObject(1).optString("id")).isEqualTo("43")
        val meta = root.getJSONObject("meta")
        assertThat(meta.optString("sha256", "")).matches("[0-9a-f]{64}")
        assertThat(meta.optInt("count")).isEqualTo(2)
    }

    @Test fun `roundtrip preserves ids`() {
        val src = listOf(note("7", "hello"))
        val back = BackupCodec.decode(BackupCodec.encode(src, emptyList(), "1.0"))
        assertThat(back).hasSize(1)
        assertThat(back[0].id).isEqualTo("7")
        assertThat(back[0].title).isEqualTo("hello")
    }

    @Test fun `v3 without ids gets generated UUIDs`() {
        // نسخة v3 قديمة: نفس الشكل بلا id وبلا sha256.
        val old = JSONObject()
            .put("app", "nota").put("v", 3)
            .put("meta", JSONObject().put("count", 2).put("createdAt", 1700000000000L).put("appVersion", "0.9"))
            .put("folders", org.json.JSONArray())
            .put("notes", org.json.JSONArray()
                .put(JSONObject().put("title", "t1").put("body", "b1").put("createdAt", 1700000000000L).put("updatedAt", 1700000000000L).put("color", 0))
                .put(JSONObject().put("title", "t2").put("body", "b2").put("createdAt", 1700000001000L).put("updatedAt", 1700000001000L).put("color", 0)))
            .toString()
        val back = BackupCodec.decode(old)
        assertThat(back).hasSize(2)
        assertThat(back[0].id).isNotEmpty()
        assertThat(back[1].id).isNotEmpty()
        assertThat(back[0].id).isNotEqualTo(back[1].id)
        // توافق: المحتوى سليم رغم غياب id.
        assertThat(back[0].title).isEqualTo("t1")
    }

    @Test fun `v2 without meta previews as version 2`() {
        val v2 = JSONObject()
            .put("app", "nota")
            .put("notes", org.json.JSONArray()
                .put(JSONObject().put("title", "t").put("body", "b").put("createdAt", 1700000000000L)))
            .toString()
        val meta = BackupCodec.preview(v2)
        assertThat(meta.version).isEqualTo(2)
        assertThat(meta.count).isEqualTo(1)
        assertThat(meta.sha256).isNull()
        val back = BackupCodec.decode(v2)
        assertThat(back).hasSize(1)
        assertThat(back[0].id).isNotEmpty()
    }

    @Test fun `sha256 matches recomputed digest`() {
        val src = listOf(note("9", "x"))
        val json = BackupCodec.encode(src, emptyList(), "1.0")
        val root = JSONObject(json)
        val expect = BackupCodec.sha256Hex(root.getJSONArray("notes").toString())
        assertThat(root.getJSONObject("meta").optString("sha256")).isEqualTo(expect)
    }

    @Test fun `noteJson and noteOf roundtrip preserves full note`() {
        val n = BackupNote(id = "5", title = "t", body = "b", createdAt = 1700000000000L,
            updatedAt = 1700000001000L, color = 2, archivedAt = null, pinned = true, folder = "Work",
            tags = listOf("a"), tasks = listOf(BackupTask("do", true)))
        val back = BackupCodec.noteOf(BackupCodec.noteJson(n))
        assertThat(back).isNotNull()
        assertThat(back!!.id).isEqualTo("5")
        assertThat(back.title).isEqualTo("t")
        assertThat(back.folder).isEqualTo("Work")
        assertThat(back.tasks).hasSize(1)
    }

    @Test fun `sameBackupContent ignores id but detects edits`() {
        val a = BackupNote(id = "1", title = "t", body = "b", createdAt = 10L, updatedAt = 20L, color = 0, archivedAt = null)
        val sameId2 = a.copy(id = "2")
        assertThat(BackupCodec.sameBackupContent(a, sameId2)).isTrue()
        assertThat(BackupCodec.sameBackupContent(a, a.copy(title = "t2"))).isFalse()
        assertThat(BackupCodec.sameBackupContent(a, a.copy(updatedAt = 21L))).isFalse()
        assertThat(BackupCodec.sameBackupContent(a, a.copy(folder = "F"))).isFalse()
    }

    @Test fun `preview v4 returns version and sha`() {
        val json = BackupCodec.encode(listOf(note("1", "p")), emptyList(), "1.0")
        val meta = BackupCodec.preview(json)
        assertThat(meta.version).isEqualTo(4)
        assertThat(meta.sha256).isNotNull()
    }
}
