# Nota — حماية الانعكاس والـ JNI من R8
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keep class androidx.room.** { *; }
-keep class com.nota.notes.data.NoteDatabase { *; }
-keep class com.nota.notes.data.NoteDatabase_Impl { *; }
-keep class com.nota.notes.data.** { *; }
-keep class dagger.hilt.** { *; }
-keep class com.nota.notes.Hilt_* { *; }
-keep class com.nota.notes.*_GeneratedInjector { *; }
# P1-C: إبقاء صريح للفئات المُعلَّمة (12 ملفًا يستخدم Hilt) — R8 full-mode قد يجردها رغم consumer-rules المكتبة
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class javax.inject.** { *; }
# WorkManager: لا subclasses في المشروع (الجدولة عبر AlarmManager) — إبقاء إطار العمل فقط
-keep class androidx.work.CoroutineWorker { *; }

-keep class androidx.datastore.** { *; }
-keep class androidx.biometric.** { *; }
# P1-C: Firebase مستخدم (firebase-auth + play-services-auth) — تهيئته بالـ reflection
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
# P1-C: kotlinx.serialization وGson/Moshi غير مستخدمة في الشجرة — بلا keeps لها (غير منفذ).
# P1-C: Compose بلا reflection — بلا keeps (مقصود، التشويش الكامل للـ UI مسموح).
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
# P1-C: أسطر السجلات تقود للكود المشوَّش عبر mapping.txt — التشويش نفسه غير متأثر
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
