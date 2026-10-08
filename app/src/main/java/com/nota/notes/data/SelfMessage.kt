package com.nota.notes.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// رسائل النفس (Message Yourself — 27 سبتمبر 2026): محادثة المستخدم مع نفسه
// على طريقة واتساب/سيجنال — جدول مستقل بلا تشفير إضافي (القاعدة نفسها مشفرة بـ SQLCipher)
@Entity(tableName = "self_messages")
data class SelfMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SelfMessageDao {
    @Query("SELECT * FROM self_messages ORDER BY createdAt ASC, id ASC")
    fun observe(): Flow<List<SelfMessage>>

    @Insert
    suspend fun insert(m: SelfMessage): Long

    @Query("UPDATE self_messages SET text = :text WHERE id = :id")
    suspend fun update(id: Long, text: String)

    @Query("DELETE FROM self_messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM self_messages")
    suspend fun clear()
}
