package com.nota.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// المجلدات (ميزة مدفوعة): id/name/createdAt + color (فهرس لوحة Sticky Notes — 0 = افتراضي)
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val color: Int = 0,
    val pinned: Boolean = false, // التثبيت بالدبوس: المثبت أولًا قبل الأقدم
    val locked: Boolean = false, // قفل خاص: فتح التفاصيل يتطلب PIN/بصمة حتى والتطبيق مفتوح
    val createdAt: Long = System.currentTimeMillis(),
    val ownerAccountId: String? = null // فصل الحسابات: null = محلي — والمجلد لا يظهر لحساب آخر
)
