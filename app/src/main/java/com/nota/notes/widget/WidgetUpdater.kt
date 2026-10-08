package com.nota.notes.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetStore by preferencesDataStore("nota_widget")

// كاش الويدجت: عناوين فقط في مخزن خاص — يُكتب بعد فتح القفل/الحفظ،
// والـ provider يقرأ الكاش ولا يفك تشفير القاعدة أبدًا (خصوصية أولًا)
object WidgetUpdater {
    const val MAX_TITLES = 3
    private val K_N = intPreferencesKey("n")
    private fun key(i: Int) = stringPreferencesKey("t$i")

    suspend fun push(ctx: Context, titles: List<String>) {
        val take = titles.take(MAX_TITLES)
        ctx.widgetStore.edit { p ->
            p[K_N] = take.size
            take.forEachIndexed { i, t -> p[key(i)] = t }
        }
    }

    suspend fun snapshot(ctx: Context): List<String> {
        val p = ctx.widgetStore.data.map { (it[K_N] ?: 0) to it }.first()
        val n = p.first
        if (n <= 0) return emptyList()
        return (0 until minOf(n, MAX_TITLES)).mapNotNull { p.second[key(it)] }
    }
}
