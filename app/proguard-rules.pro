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
-keep class javax.inject.** { *; }
-keep class androidx.work.CoroutineWorker { *; }

-keep class androidx.datastore.** { *; }
-keep class androidx.biometric.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
