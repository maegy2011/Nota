# Nota — P3-A1 Status (نسيت PIN + بصمة)

## الحالة
- [x] P3-A1 DONE — commit واحد محلي (بلا push)

## الأساس الموجود (عمل سابق، أُعيد التحقق منه سطرًا سطرًا)
- رابط "نسيت PIN؟" (`forgot_pin_link`) ظاهر دائمًا على شاشة القفل حتى أثناء قفل الدرع.
- `RecoverySheet` (عنوان `recover_title`): بصمة (مع `CryptoObject` عبر نفس مسار `ChangePinBlock`) + إعادة ضبط المصنع (بطاقة `errorContainer` هادئة + حواران: تأكيد ثم كتابة `امسح` عبر `recover_wipe_word`).
- نجاح البصمة في الاسترداد لا يصفّر الدرع — التصفير عند `changePin` (تأكيد PIN الجديد) فقط. `notePinFail` يُحتسب لمحاولات الاسترداد الفاشلة.
- `SettingsScreen` → قسم تغيير PIN → عنصر "إعادة ضبط PIN" (`reset_pin_title/sub`) يفتح نفس الشيت (بلا تكرار كود).
- `ChangePinBlock` → رابط "نسيت PIN الحالي؟" (`forgot_current_pin`) قبل إدخال الحالي.
- `BackupScreen`/السحابة: النص "عبارة استرداد سحابة الوسائط" + "هذه العبارة تستعيد وسائطك السحابية فقط — لا تستعيد PIN التطبيق." (`ncloud_key_title/msg`) — موجود مسبقًا.

## ما نُفّذ في هذا الـ commit (فجوات أُغلقت)
1. زر "استخدم البصمة" يظهر **دائمًا** في الشيت (كان يُخفى تمامًا عند غياب البصمة) — معطّل مع نص `recover_bio_unenrolled` ("البصمة غير مسجّلة على الجهاز — أضفها من إعدادات النظام أولًا"):
   - `LockScreen`: `canBio = (act != null)` + `bioEnrolled = showBiometric && bioEnrolled`.
   - `SettingsScreen` (إعادة الضبط): `canBio = (resetAct != null)`.
   - `ChangePinBlock`: `canBio = (activity != null)` + حذف `bioAvailable` غير المستخدمة.
2. زر "لصق" (`recover_paste` — ar+en) في حقل مفتاح الاسترداد داخل الشيت (قراءة الحافظة).
3. تعليق سلسلة fallback في `LockScreen.kt` بالصيغة الحرفية المطلوبة (P3-A1/A2/A3/P0-3).

## الاختبارات
- وحدة: لا جديد قابل للوحدة (كل مسارات A1 واجهات + BiometricPrompt جهازي) — `./gradlew test`: **BUILD SUCCESSFUL** (31×4=124، 0 فشل).
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL**.
- `grep`: `recover_paste` موجود في ar+en. كل مفاتيح A1 (30 مفتاحًا) موجودة ar+en.

## Commits
- `feat(recovery): add forgot-PIN entry point with biometric and app reset`

## ما ينتظر محمد (جهاز)
1. رابط "نسيت PIN" على شاشة القفل (بما فيها أثناء قفل الدرع) → الشيت يعرض الخيارات المتاحة فقط.
2. "استخدم البصمة" (معطّل + نص عند غياب التسجيل) → Prompt → PIN جديد → فتح بالجديد.
3. "إعادة ضبط التطبيق" → حواران (كتابة "امسح") → `factoryReset` → إعداد PIN من جديد.
4. `notePinFail` لا يُصفَّر بنجاح البصمة وحده (يُختبر بمحاولة PIN خاطئة بعد الاسترداد وقبل تعيين الجديد — يجب بقاء العدّاد).
5. "إعادة ضبط PIN" في الإعدادات يفتح نفس الشيت.
6. زر "لصق" يملأ حقل المفتاح من الحافظة.
