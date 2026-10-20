package com.nota.notes.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// P3-B1: اختبارات منطق التصدير النقي (JVM بلا جهاز).
// Robolectric مطلوب لأن الإنتاج يستخدم org.json (نسخة أندرويد غير قابلة للـ mock على JVM).
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GdprExportTest {

    @Test
    fun `zip roundtrip contains expected entries`() {
        val notes = GdprExport.buildNotesJson(listOf(
            GdprExport.noteToJson(1, "t", "b", 100, 200, 0, false, false, false, false, null, null),
            GdprExport.noteToJson(2, "t2", "b2", 100, 200, 1, true, false, false, false, null, null)
        ))
        val zip = GdprExport.assembleZip(mapOf(
            "notes.json" to notes.toByteArray(),
            "attachments/img1.jpg" to byteArrayOf(1, 2, 3),
            "settings.json" to "{}".toByteArray(),
            "restore_log.json" to "[]".toByteArray(),
            "README.txt" to GdprExport.buildReadme().toByteArray()
        ))
        val names = GdprExport.unzipNames(zip)
        assertEquals(
            listOf("README.txt", "attachments/img1.jpg", "notes.json", "restore_log.json", "settings.json"),
            names
        )
        val back = GdprExport.unzipBytes(zip, "notes.json")!!.toString(Charsets.UTF_8)
        assertTrue(back.contains("\"deleted\": true"))
        assertTrue(back.contains("\"deleted\": false"))
    }

    @Test
    fun `settings sanitizer drops sensitive keys`() {
        val all = mapOf(
            "theme" to "dark",
            "ai_key_sealed" to "SECRET",
            "drive_pw_enc" to "SECRET",
            "nota_recovery_key" to "SECRET",
            "nota_recovery_key_bak" to "SECRET",
            "id_token" to "SECRET",
            "account_id" to "SECRET",
            "lang" to "ar"
        )
        val clean = GdprExport.sanitizeSettings(all)
        assertEquals(mapOf("theme" to "dark", "lang" to "ar"), clean)
        val json = GdprExport.buildSettingsJson(clean)
        assertFalse(json.contains("SECRET"))
    }

    @Test
    fun `oversize threshold is 500MB`() {
        assertFalse(GdprExport.isOversize(500L * 1024 * 1024))
        assertTrue(GdprExport.isOversize(500L * 1024 * 1024 + 1))
    }

    @Test
    fun `readme mentions all archive parts`() {
        val r = GdprExport.buildReadme()
        assertTrue(r.contains("notes.json"))
        assertTrue(r.contains("attachments/"))
        assertTrue(r.contains("settings.json"))
        assertTrue(r.contains("restore_log.json"))
    }
}
