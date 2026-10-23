package com.nota.notes.ui

import android.content.Context
import com.nota.notes.billing.Entitlement
import com.nota.notes.billing.EntitlementsApi
import com.nota.notes.billing.EntitlementsRepo
import com.nota.notes.billing.ServerRegister
import com.nota.notes.data.AccountStore
import com.nota.notes.data.Note
import com.nota.notes.data.NotesRepository
import com.nota.notes.data.SettingsStore
import com.nota.notes.data.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotesViewModelTest {

    class FakeRepo(proCalls: MutableList<String>) : NotesRepository(
        com.nota.notes.data.DbHolder(
            RuntimeEnvironment.getApplication(),
            com.nota.notes.security.PassphraseStore(RuntimeEnvironment.getApplication())
        ),
        RuntimeEnvironment.getApplication(),
        com.nota.notes.data.AccountStore(RuntimeEnvironment.getApplication())
    ) {
        val calls = proCalls
        override fun accountChanges(): Flow<String?> = flowOf(null)
        override fun all(): Flow<List<Note>> = flowOf(emptyList())
        override fun search(q: String): Flow<List<Note>> = flowOf(emptyList())
        override fun searchArchived(q: String): Flow<List<Note>> = flowOf(emptyList())
        override fun archived(): Flow<List<Note>> = flowOf(emptyList())
        override fun favorites(): Flow<List<Note>> = flowOf(emptyList())
        override fun searchFavorites(q: String): Flow<List<Note>> = flowOf(emptyList())
        override fun favoritesArchived(): Flow<List<Note>> = flowOf(emptyList())
        override fun searchFavoritesArchived(q: String): Flow<List<Note>> = flowOf(emptyList())
        override fun trashed(): Flow<List<Note>> = flowOf(emptyList())
        override fun searchTrash(q: String): Flow<List<Note>> = flowOf(emptyList())
        override fun allTags(): Flow<List<Tag>> = flowOf(emptyList())
        override fun tagsMap(): Flow<Map<Long, List<String>>> = flowOf(emptyMap())
        override fun tagCounts(): Flow<Map<String, Int>> = flowOf(emptyMap())
        override fun categories(): Flow<List<String>> = flowOf(emptyList())
        override fun categoryCounts(): Flow<Map<String, Int>> = flowOf(emptyMap())
        override fun reminders(): Flow<List<Note>> = flowOf(emptyList())

        override suspend fun setReminder(id: Long, spec: com.nota.notes.data.ReminderSpec) { calls.add("setReminder") }
        override suspend fun clearReminder(id: Long) { calls.add("clearReminder") }
        override suspend fun setCategory(id: Long, category: String?) { calls.add("setCategory") }
        override suspend fun setTags(noteId: Long, names: List<String>) { calls.add("setTags") }
        override suspend fun addTask(noteId: Long, text: String): Long { calls.add("addTask"); return 1L }
        override suspend fun save(id: Long?, title: String, body: String, color: Int, folderId: Long?, background: String?, lat: Double?, lng: Double?, category: String?): Long { calls.add("save"); return 1L }
        override suspend fun archive(id: Long) { calls.add("archive") }
    }

    class FakeEntitlements(pro: Boolean) : EntitlementsRepo(
        RuntimeEnvironment.getApplication(),
        object : EntitlementsApi { override suspend fun getEntitlements(accountId: String): Entitlement = Entitlement.FREE },
        com.nota.notes.data.AccountStore(RuntimeEnvironment.getApplication()),
        com.nota.notes.billing.ServerRegister(
            com.nota.notes.auth.IdTokenProvider(com.nota.notes.data.AccountStore(RuntimeEnvironment.getApplication()))
        )
    ) {
        private val p = pro
        override val cached: Flow<Entitlement> = flowOf(Entitlement(pro = p, features = emptyList(), expiresAt = null))
    }

    private fun vm(pro: Boolean, calls: MutableList<String>): NotesViewModel {
        val ctx = RuntimeEnvironment.getApplication() as Context
        val store = SettingsStore(ctx)
        return NotesViewModel(FakeRepo(calls), store, FakeEntitlements(pro))
    }

    @Test fun `setReminder without pro is no-op`() = runBlocking {
        val calls = mutableListOf<String>()
        val v = vm(false, calls)
        v.setReminderNow(1L, com.nota.notes.data.ReminderSpec(at = 1000L))
        assertTrue("setReminder should not reach repo", "setReminder" !in calls)
    }

    @Test fun `setReminder with pro sets reminder`() = runBlocking {
        val calls = mutableListOf<String>()
        val v = vm(true, calls)
        v.setReminderNow(1L, com.nota.notes.data.ReminderSpec(at = 1000L))
        assertTrue("setReminder should reach repo", "setReminder" in calls)
    }

    @Test fun `setCategory without pro is no-op`() = runBlocking {
        val calls = mutableListOf<String>()
        val v = vm(false, calls)
        v.setCategoryNow(1L, "work")
        assertTrue("setCategory should not reach repo", "setCategory" !in calls)
    }

    @Test fun `saveTags without pro is no-op`() = runBlocking {
        val calls = mutableListOf<String>()
        val v = vm(false, calls)
        v.saveTagsNow(1L, listOf("a"))
        assertTrue("setTags should not reach repo", "setTags" !in calls)
    }

    @Test fun `archive works without pro`() = runBlocking {
        val calls = mutableListOf<String>()
        val v = vm(false, calls)
        v.archiveNow(1L)
        assertTrue("archive should reach repo", "archive" in calls)
    }

    @Test fun `save works without pro`() = runBlocking {
        val calls = mutableListOf<String>()
        val v = vm(false, calls)
        val r = v.saveNow(1L, "t", "b")
        assertTrue("save should reach repo", "save" in calls)
    }

    @Test fun `tasks without pro are no-op`() = runBlocking {
        val calls = mutableListOf<String>()
        val v = vm(false, calls)
        v.addTaskNow(1L, "t")
        assertTrue("addTask should not reach repo", "addTask" !in calls)
    }

    @Test fun `proNow reflects entitlement`() = runBlocking {
        val calls = mutableListOf<String>()
        val vFalse = vm(false, calls)
        val vTrue = vm(true, calls)
        assertTrue(!vFalse.proNow())
        assertTrue(vTrue.proNow())
    }
}
