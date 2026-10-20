# Nota — P3-A3 Status (قفل الجهاز + حالة الأمان)

## الحالة
- [x] P3-A3 DONE — 3 commits محلية (بلا push)

## الأساس الموجود (أُعيد التحقق منه)
- سويتش "إعادة التعيين عبر قفل الجهاز" (`devcred_*`): افتراضيًا OFF (`?: false` في `PassphraseStore`) + حوار تحذير صريح عند التفعيل (`devcred_warn_msg` يطابق نص المواصفة).
- الشيت يعرض "استخدم قفل الجهاز" فقط عند التفعيل؛ `BiometricPrompt` بـ `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` **بلا `CryptoObject`** (مقايضة موثقة في `AuthManager.deviceCredential`).
- بطاقة Security Status أعلى الإعدادات: 5 صفوف (فتح/مفتاح/جهاز/قفل تلقائي/لقطات) + سجل آخر فتح وعدد الفاشلة؛ ✓/⚠ بألوان هادئة؛ كل صف ينقل لقسمه.
- فحص enrollment: تحذير `sec_enroll_warn` بلا إلغاء تلقائي. تعطيل البصمة: حوار تأكيد `sec_disable_*` + سطر `bio_disable_warn`.

## ما نُفّذ (3 commits)
1. `feat(recovery): device credential reset via device lock`
   - فشل Device Credential كان يعرض نص النظام (`it`) — تسريب oracle يميّز الحالات. الآن رسالة عامة موحدة دائمًا في المواضع الثلاثة (LockScreen + إعادة الضبط + ChangePinBlock)، اتساقًا مع مساري البصمة والمفتاح.
2. `feat(settings): security status card`
   - صف "إعادة التعيين عبر الجهاز" كان `ok = true` دائمًا — صار `ok = !devOn` (المفعّل = ⚠).
   - البطاقة قابلة للطي: كل المؤشرات ✓ = سطر واحد (`sec_all_good`: "أمانك جيد — لا إجراءات مطلوبة") + زر التفاصيل؛ تُفتح/تُطوى (`sec_details`/`sec_hide` — ar+en).
   - دالة نقية `isSecurityAllGood(...)` + ‏6 اختبارات وحدة (`SecurityStatusTest`, pure-JVM, مصفوفة الحالات الست).
3. `feat(settings): warn on biometric disable + check enrollment`
   - نص مسار التفعيل المطلوب (`bio_enable_rec`: "تفعيل البصمة يتيح مسارًا بديلًا لتعيين PIN إذا نسيته." — ar+en) يُعرض عند إتاحة البصمة قبل تفعيلها؛ حوار التعطيل وفحص التسجيل كانا موجودين وتأكدت سلامتهما.

## الاختبارات والبناء (نهاية P3-A كاملًا)
- `./gradlew test`: **BUILD SUCCESSFUL** — ‏44 اختبار × 4 variants = ‏176 تنفيذًا، 0 فشل (29 أساس P2 + 2 صافي P3-0 + 7 RecoveryKey + 6 SecurityStatus).
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL** — ‏APK جديد.
- `./gradlew assembleRelease`: **BUILD SUCCESSFUL** (17m36s) — ‏minify/R8 لم يكسر شيئًا؛ `app-mock-release.apk` جديد. بلا توزيع ولا رفع.
- `grep`: المفاتيح الجديدة (`sec_all_good`/`sec_details`/`sec_hide`/`bio_enable_rec`) في ar+en.

## قرارات لمحمد
1. `isSecurityAllGood` لا يشمل القفل التلقائي (معلومة عرض فقط — حتى "أبدًا" لا تُسقط "جيد"). إن أردت إسقاطها عند `NEVER` أخبرني (P3-C).
2. أخطاء Device Credential صارت عامة تمامًا (كانت تعرض نص النظام المفيد أحيانًا كـ lockout) — مقايضة لصالح عدم التمييز.
3. تحقق `test`: دورة واحدة لكل مرحلة فرعية (A1/A2/A3) لا لكل commit — حسب بند "بعد كل مرحلة فرعية".

## اكتشاف بنية (مهم)
- `~/.gradle/gradle.properties` العام يفرض `-Xmx1g` ويبدو أنه يغلب `-Xmx2500m` في `gradle.properties` للمشروع (الـ daemon كان يقلع بـ 1g ويفشل تجميع Kotlin). كل بناءات P3-0/A مررت `-Dorg.gradle.jvmargs="-Xmx2g ..."` صراحةً. **يُنصح بتوحيد القيمتين** (تعديل ملف عام خارج المشروع — لم أمسّه).

## ما ينتظر محمد (جهاز)
1. سويتش قفل الجهاز OFF افتراضيًا → تفعيل → حوار تحذير → الشيت يعرض "استخدم قفل الجهاز" → PIN جديد.
2. البطاقة: 5 مؤشرات؛ عند الكل ✓ سطر واحد قابل للفتح؛ صف الجهاز ⚠ عند التفعيل.
3. سويتش بصمة ON بلا تسجيل نظام → تحذير `sec_enroll_warn` بلا إلغاء.
4. تعطيل البصمة → حوار `sec_disable_*`؛ قبل التفعيل سطر `bio_enable_rec`.
5. فشل Device Credential (إلغاء/خطأ) → رسالة عامة فقط.
6. **بوابة P3-B**: لا تبدأ B حتى يعتمد محمد اختبار P3-A جهازيًا (حسب الخطة) — الكود جاهز للمراجعة.
