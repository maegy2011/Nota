package com.nota.notes.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v2: عمود color للوحة Sticky Notes (0 = افتراضي)
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
    }
}

// v3: عمود archivedAt للأرشيف (null = نشطة)
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN archivedAt INTEGER")
    }
}

// v4 (أساس المدفوعة 1.1.0-premium-base): ربط الحساب + أساس المجلدات — كلها nullable بلا فقد بيانات
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN ownerAccountId TEXT")
        db.execSQL("ALTER TABLE notes ADD COLUMN folderId INTEGER")
        db.execSQL("CREATE TABLE IF NOT EXISTS folders (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

// v5 (تحسين المجلدات — لون لكل مجلد): عمود color بلا فقد بيانات
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE folders ADD COLUMN color INTEGER NOT NULL DEFAULT 0")
    }
}

// v6 (تثبيت بالدبوس — ملاحظات ومجلدات): عمود pinned بلا فقد بيانات
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE folders ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
    }
}

// v7 (خلفية الملاحظة + القفل الخاص): locked للاثنين وbackground للملاحظات — بلا فقد بيانات
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE notes ADD COLUMN background TEXT")
        db.execSQL("ALTER TABLE folders ADD COLUMN locked INTEGER NOT NULL DEFAULT 0")
    }
}

// v8 (قائمة المفضلة): عمود favorite — بلا فقد بيانات
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0")
    }
}

// v9 (الوسوم Tags): جدول tags + ربط note_tags بحذف متتابع — بلا فقد بيانات
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags (name)")
        db.execSQL("CREATE TABLE IF NOT EXISTS note_tags (noteId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY(noteId, tagId), FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tags_tagId ON note_tags (tagId)")
    }
}

// v10 (التذكير): موعد + تكرار + إشعار مسبق — بلا فقد بيانات
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER")
        db.execSQL("ALTER TABLE notes ADD COLUMN reminderRepeat TEXT NOT NULL DEFAULT 'none'")
        db.execSQL("ALTER TABLE notes ADD COLUMN reminderBeforeMin INTEGER NOT NULL DEFAULT 0")
    }
}

// v11 (الموقع): إحداثيات اختيارية لكل ملاحظة — بلا فقد بيانات
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN latitude REAL")
        db.execSQL("ALTER TABLE notes ADD COLUMN longitude REAL")
    }
}

// v12 (التكرار المخصص): فترة + أيام أسبوع + نمط شهري + نهاية — بلا فقد بيانات
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatInterval INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatDays TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatMonthMode TEXT NOT NULL DEFAULT 'same'")
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatEnd TEXT NOT NULL DEFAULT 'forever'")
        db.execSQL("ALTER TABLE notes ADD COLUMN repeatEndValue INTEGER NOT NULL DEFAULT 0")
    }
}

// v13 (محتوى الملاحظة): مهام checklist + مرفقات صور — جدولان بلا فقد بيانات
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS checklist_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, noteId INTEGER NOT NULL, text TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, sort INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_items_noteId ON checklist_items (noteId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS note_attachments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, noteId INTEGER NOT NULL, file TEXT NOT NULL, kind TEXT NOT NULL DEFAULT 'image', createdAt INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(noteId) REFERENCES notes(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_note_attachments_noteId ON note_attachments (noteId)")
    }
}

// v14 (فصل الحسابات): مالك المجلد — بلا فقد بيانات (اليتيم يُنسب عند أول دخول)
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE folders ADD COLUMN ownerAccountId TEXT")
    }
}

// v15 (التصنيفات): تسمية حرة للملاحظة — nullable بلا فقد بيانات
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN category TEXT")
    }
}

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN geoLat REAL")
        db.execSQL("ALTER TABLE notes ADD COLUMN geoLng REAL")
        db.execSQL("ALTER TABLE notes ADD COLUMN geoRadiusM REAL NOT NULL DEFAULT 200")
        db.execSQL("ALTER TABLE notes ADD COLUMN geoName TEXT NOT NULL DEFAULT ''")
    }
}

// v17 (رسائل النفس — 27 سبتمبر 2026): جدول self_messages — بلا فقد بيانات
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS self_messages (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, text TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

@Database(entities = [Note::class, Folder::class, Tag::class, NoteTag::class, ChecklistItem::class, NoteAttachment::class, SelfMessage::class], version = 17, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun notes(): NoteDao
    abstract fun folders(): FolderDao
    abstract fun tags(): TagDao
    abstract fun content(): ContentDao
    abstract fun selfMessages(): SelfMessageDao
}
