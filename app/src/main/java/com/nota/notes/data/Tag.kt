package com.nota.notes.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// الوسوم: أسماء فريدة (غير حساسة لحالة الأحرف عند الإدخال — تُطبَّع مقصوصة كما كتبها المستخدم أول مرة)
@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

// ربط ملاحظة ↔ وسم — حذف أي طرف يمسح الربط تلقائيًا (CASCADE)
@Entity(
    tableName = "note_tags",
    primaryKeys = ["noteId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = Note::class, parentColumns = ["id"], childColumns = ["noteId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Tag::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("tagId")]
)
data class NoteTag(val noteId: Long, val tagId: Long)

// صف مسطّح لبناء خريطة ملاحظة ← وسومها في الذاكرة
data class NoteTagName(val noteId: Long, val name: String)

// عدّاد الوسوم لشاشة الإدارة — يشمل الصفرية المرتبطة (تُعرض بلا ملاحظات قبل تنظيفها)
data class TagCount(val name: String, val count: Int)
