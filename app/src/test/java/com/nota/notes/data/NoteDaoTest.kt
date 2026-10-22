package com.nota.notes.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NoteDaoTest {
    @Test fun `search excludes locked notes`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(ctx, NoteDatabase::class.java).build()
        try {
            val dao = db.notes()
            dao.insert(Note(title = "common title", body = "common body", locked = false))
            dao.insert(Note(title = "common title", body = "secret body", locked = true))
            val results = dao.search("common", null).first()
            assertThat(results).hasSize(1)
            assertThat(results.first().locked).isFalse()
        } finally {
            db.close()
        }
    }
}
