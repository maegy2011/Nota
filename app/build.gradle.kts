plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    kotlin("kapt")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    // يقرأ google-services.json ويولد default_web_client_id — بلا مفاتيح في الكود
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nota.notes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nota.notes"
        minSdk = 26
        targetSdk = 35
        // الترقيم الرسمي يبدأ من 1.0.0 — versionCode داخلي متسلسل حتى يقبل أندرويد التحديث فوق النسخ السابقة
        versionCode = 49
        versionName = "1.26.0"
    }

    signingConfigs {
        create("release") {
            // كلمات السر خارج المستودع: env أولًا ثم ملف 600 خارج المشروع — لا قيم مكتوبة هنا أبدًا
            val envFile = rootProject.file(System.getProperty("user.home") + "/.config/nota/signing.env")
            val envMap = if (envFile.exists()) envFile.readLines()
                .mapNotNull { it.split("=", limit = 2).takeIf { p -> p.size == 2 }?.let { p -> p[0].trim() to p[1].trim() } }
                .toMap() else emptyMap()
            val storePw = System.getenv("NOTA_KEYSTORE_PASSWORD") ?: envMap["NOTA_KEYSTORE_PASSWORD"]
                ?: throw GradleException("NOTA_KEYSTORE_PASSWORD missing (env or ~/.config/nota/signing.env)")
            val keyPw = System.getenv("NOTA_KEY_PASSWORD") ?: envMap["NOTA_KEY_PASSWORD"]
                ?: throw GradleException("NOTA_KEY_PASSWORD missing (env or ~/.config/nota/signing.env)")
            // Release keystore is intentionally outside the repository; legacy in-tree keys are unsupported.
            val storePath = System.getenv("NOTA_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
                ?: rootProject.file(System.getProperty("user.home") + "/.config/nota/nota-release.keystore").absolutePath
            val storeFileValue = rootProject.file(storePath)
            require(storeFileValue.isFile) { "Release keystore not found: $storePath (set NOTA_KEYSTORE_PATH)" }
            storeFile = storeFileValue
            storePassword = storePw
            keyAlias = "nota"
            keyPassword = keyPw
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ⚠️ لا تفعّل التصغير: Hilt 2.52 يفلتر NotaApp_GeneratedInjector من الـ pipeline فيفشل R8،
            // والـ dontwarn ينقل العطل لجهاز المستخدم (NoClassDefFoundError عند بدء التشغيل) — موثق 20 سبتمبر 2026 (v1.0.2)
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // نكهات الطبقة (خارطة المدفوعة §4): mock للتجربة الوهمية — play للإنتاج بلا أي Fake في كودها
    flavorDimensions += "tier"
    productFlavors {
        create("mock") {
            dimension = "tier"
            applicationIdSuffix = ".mock"
            versionNameSuffix = "-mock"
            buildConfigField("Boolean", "USE_FAKE_BILLING", "true")
        }
        create("play") {
            dimension = "tier"
            buildConfigField("Boolean", "USE_FAKE_BILLING", "false")
        }
    }

    buildFeatures { compose = true; buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7") // تدوير صور الكاميرا الصحيح عند النسخ
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.material:material-icons-core:1.7.6")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.room:room-ktx:2.7.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    // دخول جوجل الحقيقي (المرحلة 0): Google Maven أولًا في settings — البوابة تحجب مسارات أندرويد على Central أحيانًا
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0") // الموقع الحالي للملاحظات — GPS يعمل أوفلاين
    implementation("com.google.dagger:hilt-android:2.56.2")
    // محرر النص الغني (قرار 26 سبتمبر 2026): rc11 متوافق مع Kotlin 2.1.10 + Compose 1.7.3
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc11")
    ksp("com.google.dagger:hilt-compiler:2.56.2")
    ksp("androidx.room:room-compiler:2.7.2")
}
