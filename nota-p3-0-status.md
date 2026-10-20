# Nota — P3-0 Status (GCM Hotfix)

## الحالة
- [x] P3-0 DONE — commit واحد محلي (بلا push)

## ما نُفّذ
1. `data/BackupManager.kt` — `decryptEnvelope`:
   - `GeneralSecurityException` (تشمل `AEADBadTagException`) تُغلَّف في `IllegalStateException("فشل فك التشفير — تحقق من كلمة المرور")` مع `cause` الأصلي.
   - `IllegalArgumentException` داخل المقطع المشفر (Base64 مكسور/اقتطاع) تُغلَّف أيضًا — لا تسريب يميّز العبث عن الكلمة الخاطئة.
   - أخطاء الصيغة (`bad format`/`bad iters`/كلمة فارغة) تبقى `IllegalArgumentException` — توافق خلفي مع المتصلين.
   - رسالة واحدة موحدة، لا كشف: كلمة خاطئة أم ملف تالف أم tag مفقود. صيغة الملف بلا تغيير.
2. `CloudMediaCrypto` — لا تغيير: مسار الفك يُرجع `null` أصلًا عند أي فشل (لا استثناء يتسرب) — متوافق مع P3-0.
3. مسار الاستعادة (`CloudBackupViewModel.restoreWithPassword/restoreNow`) — يلتقط `Exception` عامًا ويعرض `err:drive-pw-wrong` (رسالة عامة)، لا crash. `BackupManager.import` يلتقط عامًا أيضًا. لا تغيير لازم.

## الاختبارات الجديدة (3)
- `decrypt with wrong password throws IllegalStateException (not AEADBadTagException)` (+ فحص `cause` من نوع `GeneralSecurityException`)
- `decrypt with tampered ciphertext throws IllegalStateException`
- `decrypt with truncated ciphertext throws IllegalStateException`
- حُدّث اختبار `decrypt with wrong password throws` القديم (كان يوثّق التسرب الخام كسلوك) إلى التوقع الجديد.

## نتائج البناء
- `./gradlew test` (offline): **BUILD SUCCESSFUL** — ‏31 اختبار × 4 variants = ‏124 تنفيذًا، 0 فشل.
- `./gradlew assembleDebug` (offline): **BUILD SUCCESSFUL** — ‏APK جديد `app-mock-debug.apk`.
- ملاحظة بنية: الـ daemon استخدم `-Xmx1g` من `~/.gradle/gradle.properties` العام متجاهلًا `-Xmx2500m` في `gradle.properties` للمشروع، ففشل تجميع Kotlin بـ "Not enough memory". الحل: تمرير `-Dorg.gradle.jvmargs="-Xmx2g ..."` صراحةً (daemon جديد). يُنصح بتوحيد القيمتين لاحقًا — قرار لمحمد.

## Commits
- `fix(backup): wrap AEADBadTagException in IllegalStateException`

## اكتشافات / قرارات لمحمد
- لا شيء جديد — الثغرة أُغلقت كما وُصفت في تقرير P2.

## ما ينتظر محمد (جهاز)
1. استعادة `.nota` بكلمة خاطئة → رسالة خطأ عامة، لا crash.
2. استعادة ملف مُقتطع/مُعدَّل → نفس الرسالة العامة.
