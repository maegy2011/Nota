plugins {
    id("com.android.application") version "8.7.3" apply false
    kotlin("android") version "2.1.21" apply false
    kotlin("plugin.compose") version "2.1.21" apply false
    kotlin("kapt") version "2.1.21" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    // KSP بدل kapt لمعالجات Hilt/Room (قرار 26 سبتمبر 2026): kapt لا يقرأ metadata 2.1
    id("com.google.devtools.ksp") version "2.1.21-2.0.2" apply false
    // دخول جوجل (المرحلة 0): متوافق مع AGP 8.5.2 — كل الإعداد من google-services.json
    id("com.google.gms.google-services") version "4.4.2" apply false
}
