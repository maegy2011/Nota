package com.nota.notes.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

// P3-C3: pure-JVM tests for the settings search matcher.
// UI behaviour (icon, navigation, no sensitive values) verified on-device.
class SettingsSearchTest {

    @Test fun `arabic query finds security section`() {
        val r = searchSettings("بصمة")
        assertThat(r.map { it.route }).contains(SettingsRoute.SECURITY)
    }

    @Test fun `english query finds appearance section`() {
        val r = searchSettings("theme")
        assertThat(r.map { it.route }).contains(SettingsRoute.APPEARANCE)
    }

    @Test fun `ai query routes to AI section`() {
        assertThat(searchSettings("نموذج").map { it.route }).contains(SettingsRoute.AI)
        assertThat(searchSettings("model").map { it.route }).contains(SettingsRoute.AI)
    }

    @Test fun `recovery query routes to security`() {
        val r = searchSettings("استرداد")
        assertThat(r.map { it.route }).contains(SettingsRoute.SECURITY)
    }

    @Test fun `short query returns empty`() {
        assertThat(searchSettings("ب")).isEmpty()
        assertThat(searchSettings("  ")).isEmpty()
    }

    @Test fun `unknown word returns empty`() {
        assertThat(searchSettings("zzzqqq")).isEmpty()
    }

    @Test fun `sensitive values are never indexed`() {
        // مفاتيح AI وكلمات المرور والعبارات قيم — لا تظهر في الفهرس أبدًا.
        val all = defaultSettingsSearchEntries().joinToString(" ") { it.keywords }
        assertThat(all).doesNotContain("sk-")
        assertThat(all).doesNotContain("endpoint-url")
        val r = searchSettings("sk-")
        assertThat(r).isEmpty()
    }

    @Test fun `every non-root route is reachable from the index`() {
        val routes = defaultSettingsSearchEntries().map { it.route }.toSet()
        for (need in listOf(
            SettingsRoute.SECURITY, SettingsRoute.APPEARANCE, SettingsRoute.EXPERIENCE,
            SettingsRoute.AI, SettingsRoute.NOTIF, SettingsRoute.ADVANCED
        )) {
            assertThat(routes).contains(need)
        }
    }

    @Test fun `results have no duplicates`() {
        val r = searchSettings("الأمان")
        val keys = r.map { it.route to it.titleRes }
        assertThat(keys).containsNoDuplicates()
    }
}
