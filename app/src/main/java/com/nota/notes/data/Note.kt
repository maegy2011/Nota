package com.nota.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val color: Int = 0, // فهرس لوحة Sticky Notes — 0 = افتراضي يتبع الثيم
    val archivedAt: Long? = null, // الأرشيف: null = نشطة، غيرها = تاريخ الأرشفة
    val deletedAt: Long? = null, // سلة 30 يوم
    val ownerAccountId: String? = null, // المرحلة 0: ربط الملاحظة بالحساب — null = محلية بلا حساب
    val folderId: Long? = null, // المرحلة 2: المجلد الحاوي — null = بلا مجلد
    val category: String? = null, // التصنيف: تسمية حرة للملاحظة (شخصي/عمل) — null/فارغ = بلا تصنيف — مستقل عن المجلدات والوسوم
    val pinned: Boolean = false, // التثبيت بالدبوس: المثبتة أولًا في كل ترتيب
    val locked: Boolean = false, // قفل خاص: فتحها يتطلب PIN/بصمة حتى والتطبيق مفتوح
    val favorite: Boolean = false, // المفضلة: قلب يضيفها لقائمة المفضلة
    val background: String? = null, // صورة خلفية: اسم ملف داخلي خاص — null = لون فقط
    val reminderAt: Long? = null, // التذكير: تاريخ/وقت الحدث — null = بلا تذكير
    val reminderRepeat: String = "none", // تكرار التنبيه: none|daily|weekly|monthly|yearly
    val reminderBeforeMin: Long = 0L, // الإشعار المسبق بالدقائق قبل الموعد — 0 = في الموعد
    val latitude: Double? = null, // الموقع: خط العرض — null = بلا موقع مرفق (اختياري، ميزة مدفوعة)
    val longitude: Double? = null, // الموقع: خط الطول
    val geoLat: Double? = null, // تذكير الموقع: خط عرض السياج — null = بلا سياج (27 سبتمبر 2026)
    val geoLng: Double? = null, // تذكير الموقع: خط طول السياج
    val geoRadiusM: Float = 200f, // تذكير الموقع: نصف قطر السياج بالمتر
    val geoName: String = "", // تذكير الموقع: اسم يُعرض في الإشعار
    val repeatInterval: Int = 1, // التكرار المخصص: كل N (يوم/أسبوع/شهر/سنة) — v12
    val repeatDays: String = "", // أيام الأسبوع CSV للأسبوعي (1=الأحد..7=السبت) — فاضي = نفس اليوم
    val repeatMonthMode: String = "same", // الشهري: same = نفس اليوم | nth = نفس الترتيب (الثلاثاء الرابع)
    val repeatEnd: String = "forever", // نهاية التكرار: forever|until|count
    val repeatEndValue: Long = 0L // until: تاريخ ms | count: الأحداث المتبقية
)

// عدّاد التصنيفات: الاسم + عدد ملاحظاته الحية — لشاشة الإدارة (نفس نمط TagCount)
data class CategoryCount(val name: String, val count: Int)
