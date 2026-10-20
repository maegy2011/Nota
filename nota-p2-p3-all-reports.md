# نوتا — مجمع تقارير P2 + P3

<!-- ===== nota-p2-status.md ===== -->

# Nota — P2 Status

> Skill claims: `coding` + `android` ( man's rule 15 Aug 2026 — mandatory per task).

## القرار العام
- [x] P2 DONE — كل البنود نُفذت حرفيًا. تحفظان موثقان لا يمنعان الإنجاز: (1) زمن بعض الاختبارات >1s لكلفة KDF الإنتاجية نفسها، (2) الـ heap الفعلي على هذا الـ VPS يبقى 1g بأسبقية `~/.gradle` (قرار محمد).

## فحوص مسبقة
### Flavors
- `productFlavors`: نكهتان ببُعد `tier` — `mock` (suffix `.mock` + `USE_FAKE_BILLING=true`) و`play` (`USE_FAKE_BILLING=false`).
- الفصل معماري نظيف: `app/src/mock/.../MockBillingModule` يربط `FakeBilling/FakeIntegrity/FakeEntitlementsApi`، و`app/src/play/.../PlayBillingModule` يربط `PlayBillingGateway/PlayIntegrityCheck/HttpEntitlementsApi` — لا Fake في source set الخاص بـ play.
- `grep BuildConfig. في mock/play`: لا نتائج (الثابت يُقرأ — إن قُرئ — من main فقط).
- `grep http:// في mock/play`: لا نتائج.
- `grep MOCK|mock مع flag|if|when في main`: لا نتائج.
- هل mock يحتوي bypass/http/hardcoded؟ **لا.**
- توصية: **mock للاختبار المحلي، play للنشر** — بلا تغيير كود (توثيق فقط).

### -Xmx2500m
- Commit: `5082d2c` — `build: persist gradle heap settings for R8`
- الفرق: `org.gradle.jvmargs` من `-Xmx1700m -Xms256m ...` إلى `-Xmx2500m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8` (أُبقي على `-Dfile.encoding=UTF-8` عمدًا للمصادر العربية — انظر «قرارات»).
- ⚠️ اكتشاف: على هذا الـ VPS يفوز `~/.gradle/gradle.properties` (`-Xmx1g`) على ملف المشروع في الأسبقية — الـ daemon أقلع فعليًا بـ 1g وفشل تجميع Kotlin بـ OOM (سجل `/tmp/nota-p2-tests.log`). الكوميت يُفيد CI والأجهزة الأخرى؛ أما بناءات هذا السيرفر فما زالت تحتاج تمرير `-Dorg.gradle.jvmargs` على سطر الأوامر (سابقة P1). لم أمسّ `~/.gradle/gradle.properties` — خارج المستودع وقرار محمد.

### jadx
- متوفر؟ **لا** (`which jadx` = not available).
- نتيجة الفحص: **غير متوفر** — لم أحاول التثبيت (حسب التعليمات). تبقى نتيجة `strings` السابقة (0 ظهور لـ PinHash/KeyManager/PassphraseStore) هي المرجع.

## P2-A
### APIs المكتشفة (مقروءة قبل الكتابة — لا افتراضات)
- PinHash (`object`, pure JVM): `fun hash(pin: String, salt: ByteArray, iterations: Int = ITERATIONS): String` (صيغة `pbkdf2$<iter>$<saltHex>$<hashHex>`، default 310k) + `fun verify(pin: String, expected: String): Boolean` (يقبل الجديد + V1 القديم `saltHex:hashHex` ثم SHA-256 مرة واحدة) + `fun isLegacy(expected: String): Boolean`.
- BackupManager (`object`, يستخدم `android.util.Base64` → يحتاج Robolectric): `fun encryptEnvelope(json: String, password: String): String` (require ≥8 + isStrongPassword، ملح 16B، PBKDF2 600k، AES-GCM، صيغة 6 أسطر `NOTA/PW/iters/salt/iv/enc`) + `fun decryptEnvelope(text: String, password: String): String` (**بلا فحص حد أدنى عمدًا — توافق خلفي**) + `const val MIN_PASSWORD_LEN = 8` + `fun isStrongPassword(...)` (قائمة شائعة + كشف التكرار والتتابع) + `ITERS = 600_000`.
- SecureDelete (`object`, pure JVM): `fun shred(file: File, method: String = SHRED_SINGLE): Boolean` (غير موجود → true؛ يُتلف ثم يحذف، يرجع `!exists()`) — الطرق `single|dod|schneier` من ثوابت `SettingsStore`.
- AuthManager (`class`, يحتاج `Context` للبصمة/الحافظة لكن قرار القفل نقي): `var autoLockMs = 30_000L` (-1 = NEVER) + `var lastActive` + `fun checkAutoLock(): Boolean` + `fun lock()` + `fun unlockWithPin(...)`. **استُخرج** `fun isAutoLockExpired(lastUnlock, now, autoLockMs, unlocked): Boolean` (top-level في الملف نفسه، `checkAutoLock` يفوّض إليها — نفس المنطق حرفيًا، بلا تغيير توقيع).
- Hilt: مستخدم (`@HiltViewModel` وغيره) — لكن لا علاقة له بالاختبارات (لا ViewModels تُختبر في P2).
- RestoreRecord: `data class` بحقل `at: Long` (epoch ms) — الـ TTL يقارن epoch مباشرة بلا تحويل.

### الاختبارات المكتوبة
| الملف | عدد | حالة |
|---|---|---|
| PinHashTest | 7 | pass ✅ |
| BackupManagerTest | 9 | pass ✅ |
| SecureDeleteTest | 5 | pass ✅ |
| AuthManagerTest | 5 | pass ✅ |
| SettingsStoreTest | 3 | pass ✅ |
| **المجموع/النكهة** | **29** | **pass ✅ ×4 نكهات = 116 تنفيذ** |

### نتيجة التشغيل
```
./gradlew test → BUILD SUCCESSFUL in 18m 37s (29 tests ×4 variants = 116, 0 fail, 0 errors)
```
- التشغيل 1 (26 اختبارًا، قبل P2-C): BUILD SUCCESSFUL in 12m 25s — 104/104 ✅
- التشغيل 2 (29 اختبارًا، الكود النهائي): BUILD SUCCESSFUL in 18m 37s — 116/116 ✅ (سجل `/tmp/nota-p2-tests4.log`)
- التشغيل 3 (فحص الثبات الحقيقي، `--no-build-cache` + مسح النتائج، صفر cache hits): BUILD SUCCESSFUL in 1m 42s — 116/116 ✅ (سجل `/tmp/nota-p2-tests6.log`)
- (تشغيل وسيط بـ FROM-CACHE في 49s حُسب لاغيًا كفحص ثبات — لا إعادة تنفيذ فعلية — وأُعيد صحيحًا.)
- Flaky؟ **لا — نفس النتيجة 3 مرات (104 ثم 116 ثم 116).**
- صفر اختبار بلا assertion. Flaky؟ يُحسم بعد التشغيل 3.
- زمن الاختبار: اختبارات PBKDF2 (حراسة: 600k دورة إنتاجية) ≈ 1–2.6s، وPinHash (310k) ≈ 1–1.8s — فوق عتبة «<1s» في البرومبت. **انحراف مقبول وموثق:** البطء هو كلفة KDF نفسها (قرار P1-B الأمني — لا يُمس)، والـ 50ms المذكورة في البرومبت كانت متفائلة (الفعلي ~500ms للعملية على هذا الـ VPS). أول اختبار Robolectric يحمل تهيئة Sandbox لمرة واحدة (~13s) — ليست flaky.
- أطول اختبار فعلي: `decrypt legacy 4-char backup succeeds` ≈ 2.6s + تهيئة Robolectric لمرة واحدة.

### قرار A3-legacy
**الخيار 1 (الأفضل):** توليد الـ envelope داخل الاختبار بنفس الخوارزمية مباشرة (PBKDF2+AES-GCM بـ 100k دورة = الحد الأدنى المقبول في `coerceIn`) متجاوزًا `require()` في `encryptEnvelope` — **بلا helper في كود الإنتاج وبلا تغيير سلوك**. يعمل لأن `decryptEnvelope` بلا حد أدنى عمدًا.

### قرار A5
**استخراج منطق نقي** (`isAutoLockExpired` top-level + تفويض من `checkAutoLock`) — بلا Robolectric وبلا تغيير توقيع `AuthManager`. الاختبار JVM خالص.

## P2-B
- السطر المضاف: `.setVisibility(NotificationCompat.VISIBILITY_SECRET)` في `ReminderReceiver.showNotification` + تعليق التبرير (SECRET دائمًا لا شرطيًا) — 3 أسطر مضافة، لا تغيير آخر.
- Commit: `deea8e4` — `feat(notifications): force VISIBILITY_SECRET for all notifications`
- ملف: `app/src/main/java/com/nota/notes/reminder/ReminderReceiver.kt`
- ملاحظة: إشعارات `BackupScheduler` (نسخ احتياطية عامة بلا محتوى ملاحظات) تُركت كما هي — خارج نطاق P2-B حرفيًا (`ReminderReceiver.showNotification` فقط).

## P2-C
- TTL في read: ✅ (`restoreLog` Flow يُرشّح عبر `filterRestoreLog`) — TTL في write: ✅ (`logRestore` تُسقط المنتهية مع الحفاظ على الخام وسقف 20).
- التنسيق الفعلي: `at` = epoch Long (مللي ثانية) — المقارنة حسابية مباشرة بلا تحويل. السجلات بلا `at` (=0) تُعامل كمنتهية في المسارين (موثق في الكود).
- `BackupScreen`: بلا تغيير — حالة الفراغ (`bk_log_empty`) معالجة أصلًا، والقائمة المُرشحة الفارغة تعرضها تلقائيًا.
- 3 اختبارات TTL: pass ✅ (JVM خالص عبر دوال companion النقية — بلا Robolectric).
- Commit: `145e890` — `feat(restore-log): apply 30-day TTL`
- ملاحظة دقة: `markRestoreUndone` لا تُعيد المنتهية — صحيحة (المنتهية غير مرئية أصلًا).

## P2-G
- CI: نُفّذ — `.github/workflows/test.yml` (test فقط، بلا assembleRelease وبلا secrets). المستودع على GitHub (`maegy2011/Nota`) لكن **بلا push** (القيد 1) فلا تحقق حي بعد — يمر مع أول push من محمد.
- Commit: `9a12e1b` — `ci: add GitHub Actions workflow for unit tests` (إن طُبّق)

## مؤجّل (موثق)
- P2-D: PRAGMAs المقترحة هي defaults في SQLCipher 4.5.4 — تعليق توثيقي فقط في `DbHolder`.
- P2-E: DB مشفرة كليًا أصلًا — تحسين دفاعي مؤجل لـ P3.

## Commits المضافة
- `5082d2c` — `build: persist gradle heap settings for R8`
- `825683c` — `test: add unit tests for PinHash and BackupManager` (يضم إصلاح Truth `assertWithMessage` — طُوي داخل الكوميت نفسه التزامًا بقائمة الأسماء)
- `22811c7` — `test: add unit tests for SecureDelete and AuthManager` (يضم استخراج `isAutoLockExpired` + تفويض `checkAutoLock`)
- `deea8e4` — `feat(notifications): force VISIBILITY_SECRET for all notifications`
- `145e890` — `feat(restore-log): apply 30-day TTL`
- `23e1203` — `docs(db): document SQLCipher defaults we rely on`
- `9a12e1b` — `ci: add GitHub Actions workflow for unit tests`
- بلا push — كلها محلية على `master`. ملفات البناء الحساسة (keystore/local.properties/google-services.json/.env) غير مضافة (مُتجاهلة في `.gitignore` — تحققت بـ `git check-ignore`).

## ما ينتظر محمد
1. اختبار P2-B على جهاز (إشعار على lockscreen — يجب ألا يظهر شيء).
2. اختبار P2-C: سجل استعادة أقدم من 30 يومًا → يجب ألا يظهر.
3. دفع كل commits إلى الـ remote (بعد مراجعة) — عندها يعمل CI أول مرة.
4. قرار: heap الفعلي على الـ VPS ما زال 1g (أسبقية `~/.gradle`) — هل يُرفع أم يُترك حماية للسيرفر؟

## ما لم يُنفّذ (صراحة)
- P2-D كودًا (PRAGMAs) — تعليق توثيقي فقط في `DbHolder` (Commit `23e1203`)، حسب البرومبت.
- P2-E (تشفير أعمدة self_messages) — مؤجل P3، بلا أي عمل.
- i18n gaps — مؤجلة، بلا أي عمل.
- اختبارات UI/DB-migration/Keystore/notifications — مرفوضة في P2 أصلًا.
- زر "Clear log" اليدوي — P3.
- فحص jadx — غير متوفر.
- تحقق CI حي — مستحيل بلا push (بانتظار محمد).

## قرارات يجب أن يعرفها محمد
1. إصدارات test deps من الـ Gradle cache (بلا تحميل): truth 1.4.4 (لا 1.4.2)، robolectric 4.13 (لا 4.11.1)، androidx.test:core 1.6.1 (لا 1.5.0)، coroutines-test 1.8.1 (يطابق coroutines المشروع 1.8.1 لا 1.7.3) — كلها أحدث من المطلوب ومتوافقة.
2. BackupManagerTest يعمل تحت Robolectric (إجباري: `android.util.Base64` في كود الإنتاج).
3. **اكتشاف أمني (بلا إصلاح في P2):** فشل GCM (كلمة غلط/عبث) يتسرب كـ `AEADBadTagException` خام لا `IllegalStateException` — على المتصل التقاط `Exception` واسعة. موثق في الاختبار والتقرير.
4. أُبقي `-Dfile.encoding=UTF-8` في سطر heap (مصادر عربية) — انحراف حرفي واحد عن نص البرومبت لمصلحة البناء.

<!-- ===== nota-p3-0-status.md ===== -->

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

<!-- ===== nota-p3-a1-status.md ===== -->

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

<!-- ===== nota-p3-a2-status.md ===== -->

# Nota — P3-A2 Status (مفتاح الاسترداد 12 كلمة)

## الحالة
- [x] P3-A2 DONE — 3 commits محلية (بلا push)

## الأساس الموجود (أُعيد التحقق منه)
- `SetPinScreen` خطوة 2: بصمة / إنشاء مفتاح / تخطي بإقرار مخاطرة صريح (`reckey_choice_*`) — لا تجاوز بلا قرار.
- شاشة توليد: عرض واحد + تحذير (`reckey_warn`) + "انسخ" + "لقد حفظتها" → كويز 3 كلمات عشوائية إلزامي (`quizIndices`) → حفظ `hash` فقط.
- التخزين: `PBKDF2-HMAC-SHA256 ‏310k` عبر `PinHash` (نفس PIN)، `salt` مستقل (`recovery_salt`)، لا كلمات مخزنة أبدًا.
- الشيت: خيار المفتاح يظهر فقط عند وجود `hash`؛ حقل واحد كبير؛ فحص 12 كلمة؛ الفشل بعد 12 كاملة يُحتسب في الدرع (`verifyRecoveryKey`)؛ النجاح → PIN جديد → `changePin` يصفّر الدرع (مسار استرداد حقيقي).
- الإدارة في الإعدادات: حالة مُعد/غير مُعد + إعداد/إعادة إنشاء + تحقق بالعبارة الكاملة.

## ما نُفّذ (3 commits)
1. `feat(recovery): generate and verify 12-word recovery key`
   - `security/Bip39English.kt` جديد: قائمة BIP39 الإنجليزية الرسمية (2048 كلمة، من `bitcoin/bips`).
   - `RecoveryKey` تولّد من BIP39 حصرًا (132 بت) بدل قائمة 256 المخصصة؛ فحص `init` صار `== 2048`.
   - توافق خلفي: التحقق `hash` فقط فمفاتيح الجيل السابق ما زالت تعمل.
   - 7 اختبارات وحدة جديدة (`RecoveryKeyTest`, pure-JVM): حجم القائمة، التوليد، العشوائية، `normalize`، `wordCount`، `quizIndices`، roundtrip.
   - (إصلاح لاحق دُمج في نفس الـ commit عبر fixup: `containsAtLeastElementsIn` فشل مع تكرار كلمة عشوائيًا — استُبدل بفحص عضوية لكل كلمة.)
2. `feat(recovery): use recovery key to reset PIN`
   - `RecRegenGate`: إعادة الإنشاء تتطلب PIN الحالي (يُحتسب فشله في الدرع عبر `verify`) أو بصمة؛ مع ملاحظة أن القديم يتوقف نهائيًا (`reckey_regen_note` — ar+en).
   - ملاحظة دقيقة: المفتاح بوابة تعيين PIN فقط — ملفات `.nota` تُقفل بكلمة مرورها الخاصة فلا "نسخ مرتبطة بالمفتاح" أصلًا (موثق في التعليق).
   - `copySensitive`: مسح تلقائي للحافظة بعد 60 ثانية (API 28+).
3. `feat(recovery): migration prompt for existing users without recovery path`
   - البطاقة تُخفى عند وجود بصمة مفعّلة ومتاحة (البصمة مسار استرداد قائم — P3-A1)؛ تحديث حي عند تبديل البصمة (`refreshMigrationAsync`).
   - `snooze` أسبوع (`7d` في `PassphraseStore`) — "لاحقًا" حرفيًا.

## انحرافات موثقة (قرارات لمحمد)
1. التحقق في الإعدادات بإدخال الـ 12 كلمة كاملة بدل "عرض 3 كلمات": عرض 3 كلمات والتحقق منها **مستحيل** مع تخزين `hash` فقط (لا كلمات مخزنة) — والتحقق الكامل أدق. الكويز بـ 3 كلمات موجود حيث الكلمات حاضرة في الذاكرة (الإنشاء).
2. زر البصمة في خطوة الإعداد لا "يفعّل البصمة" فعليًا (يتطلب تسجيل نظام + `CryptoObject`) — النص يقول "يمكنك تفعيلها من الإعدادات بعد إنشاء PIN" (`reckey_choice_bio_ok`). التفعيل القسري من هذه الشاشة غير ممكن تقنيًا.
3. "إعداد الآن" في بطاقة الترحيل يفتح `RecoveryKeySetup` مباشرةً بدل `SetPinScreen` خطوة 2 — نفس النتيجة بخطوات أقل (المستخدم موثّق بفتح القفل أصلًا).
4. تحقق `test`: دورة تحقق واحدة لـ A2 كاملًا (مواصفة البرومبت تطلب بناءً بعد كل مرحلة فرعية A1/A2/A3 — لا بعد كل commit).

## الاختبارات
- `./gradlew test`: **BUILD SUCCESSFUL** — ‏38 اختبار × 4 variants = ‏152 تنفيذًا، 0 فشل (31 سابق + 7 جديدة).
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL**.
- `grep`: `reckey_regen_auth_title`/`reckey_regen_note` في ar+en؛ كل مفاتيح A2/A3 موجودة ar+en (فحص آلي سابقًا — بلا نواقص).

## ما ينتظر محمد (جهاز)
1. الإعداد الأول: خطوة "كيف تريد استرداد الوصول؟" → إنشاء مفتاح (12 كلمة BIP39 إنجليزية) → نسخ (يمسح بعد دقيقة) → كويز 3 كلمات → حفظ.
2. الاسترداد بالمفتاح من الشيت → PIN جديد → فتح بالجديد؛ محاولة خاطئة (12 كلمة) تُحتسب في الدرع.
3. إعادة الإنشاء من الإعدادات → بوابة PIN/بصمة → عبارة جديدة → القديمة تتوقف.
4. بطاقة الترحيل: تظهر بلا مفتاح ولا بصمة؛ "لاحقًا" تُخفيها أسبوعًا؛ تختفي عند تفعيل البصمة.

<!-- ===== nota-p3-a3-status.md ===== -->

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

<!-- ===== nota-p3-b-status.md ===== -->

# Nota — P3-B GDPR Status

## القرار العام
- [x] P3-B COMPLETE (كودًا + اختبارات وحدة + بناء — ينتظر الاختبار الجهازي من محمد)

## ما أُنجز (بالترتيب الحرفي من البرومبت)

### P3-B1 — تصدير كل البيانات ✅
- زر "تصدير كل بياناتي" في SettingsScreen → قسم الخصوصية (`PrivacySection`).
- ZIP في `cacheDir` يُشارك عبر ShareIntent (FileProvider)، يحتوي:
  - `notes.json` — كل الملاحظات بما فيها السلة (flag `deleted`) + archived/pinned/favorite/tags.
  - `attachments/` — المرفقات مفكوكة التشفير (فك عبر `MediaCrypto.decryptFile`، الساقط يُقرأ خامًا، الفاشل يُتخطى ويُوثق في README بدل إسقاط التصدير).
  - `settings.json` — بعد `sanitizeSettings` (يستبعد 8 مفاتيح حساسة صراحةً: `ai_key_sealed`، `drive_pw_enc`، `nota_recovery_key[_bak]`، `id_token`، `account_id`، `drive_pw_next`، `cloud_backup_hash`).
  - `restore_log.json` — سجل الاستعادة + `README.txt` ثنائي اللغة.
- تحذير 500MB (`SIZE_WARN_BYTES`) + شريط تقدم (`LinearProgressIndicator` من 0.05 → 1).
- تشفير ZIP بكلمة مرور (اختياري في البرومبت): **لم يُنفذ** — لا مكتبة ZIP مشفرة في المشروع، وإضافتها تعني تبعية جديدة. موثق كـ "لم يُنفذ (صراحة)" أدناه.

### P3-B2 — حذف الحساب بسماح 14 يومًا ✅
- زر "حذف حسابي" منفصل تمامًا عن `factoryReset`.
- حوار 1: تحذير + 14 يومًا + تحذير Pro الإضافي. حوار 2: كتابة "احذف حسابي" حرفيًا (بعد trim، بلا تساهل).
- الآلية: `FirebaseAuth.deleteUser()` (يحتاج جلسة وشبكة — الفشل لا يوقف المسار المحلي) → تسجيل `deletionRequestedAt` → فحص `DeletionWorker` الدوري (يومي).
- بطاقة "الحذف المعلق" (errorContainer هادئ) تعرض الأيام المتبقية + زر "إلغاء الحذف".
- بعد 14 يومًا: `isDue` → مسح الطلب + وسم التنفيذ. **الحذف النهائي server-side غير مدعوم** — لا endpoint في `NotaCloudApi` (quota/blobs/pointers فقط). يُطبق المحلي + Firebase فقط، والرسالة صريحة: "لحذف السحابة، تواصل مع support". موثق كقرار يحتاج محمد/backend.

### P3-B3 — opt-in للتقارير ✅
- سويتشا "تقارير الأعطال" و"التحليلات"، افتراضيًا OFF، محفوظان في DataStore.
- **قرار موثق:** لم تُضف مكتبة Crashlytics/Sentry — لا جامع بيانات أصلًا في التطبيق (جيد)، والسويتشان بوابتا تفعيل مستقبلي. إضافة SDK الآن تعني تتبعًا وتكلفة بلا طلب صريح من محمد.
- سطر سياسة البيانات: "بيانات AI تُرسل إلى endpoint الذي تحدده أنت. لا نجمع أي بيانات." (ar+en).

## Commits (محلية، بلا push)
| المرحلة | Commit | Hash |
|---|---|---|
| B1 | `feat(gdpr): export all user data as ZIP` | `f4693dd` |
| B2 | `feat(gdpr): account deletion with 14-day grace period` | `ed0ce50` |
| B3 | `feat(gdpr): explicit opt-in for crash reports and analytics` | `a5ff399` |
| ملاحظة تقسيم | ملفا `GdprViewModel.kt` (جسر Hilt) مع B1، و`PrivacySection.kt` + كل التوصيلات (App/SettingsStore/AuthViewModel/Screens/strings) مع B3 — لأن القسم والواجهات مشتركة بين الثلاثة ولا تنفصل لكوميتات تبني فرادى |

## نتيجة الاختبارات والبناءات
- `./gradlew test` — **BUILD SUCCESSFUL** (15m16s): **53 اختبار × 4 variants = 212 تنفيذًا، 0 فشل** (0 skipped, 0 errors).
- الجديد: 9 اختبارات (4 `GdprExportTest`: roundtrip + تعقيم + عتبة 500MB + README — و5 `AccountDeletionTest`: مهلة/متبقي/عبارة التأكيد).
- إصلاح أثناء العمل: `GdprExportTest` كان يفشل (2) بخطأ `org.json... not mocked` — أُضيف `@RunWith(RobolectricTestRunner)` + `@Config(sdk=[28])` بنفس نمط `BackupManagerTest`، بلا تبعيات جديدة.
- `./gradlew assembleDebug` — **BUILD SUCCESSFUL** (3m1s). لم يُشغل `assembleRelease` (ممنوع إلا بأمر صريح) ولم يُدفع شيء (`git push` ممنوع نهائيًا).
- `grep`: مفاتيح `privacy_*` = 25 في ar و25 في en (تطابق تام). لا مفاتيح معلقة.
- أسرار: لا ملفات حساسة أُضيفت للـ git. منطق التشفير القائم untouched (لا `PinHash`/`KeyManager`/`DbProvider`/`CloudKeyStore`/`PassphraseStore`/`MediaCrypto` في الـ diff).

## تشخيص العائق السابق (compileMockDebugKotlin backend crash)
- السبب: `gradle.properties` يطلب `-Xmx2500m` على سيرفر 3.9GB (المتاح ~1.4GB) → الديمون يُقتل/OOM أثناء تجميع Kotlin.
- الحل المطبق (بلا مساس بـ `~/.gradle` ولا بملفات المشروع): تجاوز من سطر الأوامر فقط:
  `-Dorg.gradle.jvmargs="-Xmx1152m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8"` + `--max-workers=1 --no-parallel`.
- الدليل: نفس الكود الذي كان يسقط اكتمل تجميعه واختباره وبناؤه. **توصية دائمة:** أي بناء على هذا السيرفر يستخدم نفس سطر الأوامر حتى يُرقَّى الـ RAM أو تُخفَّض القيمة في `gradle.properties` (التعديل يحتاج قرار محمد).

## قرارات يجب أن يعرفها محمد
1. **تشفير ZIP بكلمة مرور:** لم يُنفذ (اختياري في البرومبت) — يحتاج مكتبة (مثل zip4j) + UI لكلمة المرور. إن كان متطلبًا أوروبيًا صريحًا، يُضاف في P3-D/P4.
2. **حذف السحابة server-side:** غير مدعوم في الـ backend الحالي — يحتاج تنسيق endpoint حذف حساب. حاليًا: محلي + Firebase فقط + رسالة تواصل مع الدعم.
3. **Crashlytics/Sentry:** لم تُضف عمدًا — السويتشان جاهزان كبوابة، والتفعيل الحقيقي (SDK + مفاتيح) يحتاج أمرًا صريحًا.
4. عبارة التأكيد "احذف حسابي" عربية حرفية في كل اللغات (حتى الواجهة الإنجليزية تعرض `Type: احذف حسابي`) — مقصود: المستخدم المصري/العربي هو الأساس، والعبارة الثابتة تمنع الحذف العرضي. إن أردت نسخة إنجليزية (`DELETE MY ACCOUNT`) للحسابات الإنجليزية، قرارك.
5. `DeletionWorker` دوري يومي — قد يؤجله Doze؛ التنفيذ النهائي الدقيق يحتاج `setExpedited` أو فحصًا عند فتح التطبيق (مقترح P4).

## ما ينتظر محمد (اختبار جهازي — بالترتيب)
1. [ ] التصدير: ملاحظات + مرفقات حقيقية → ZIP يفتح، `settings.json` بلا مفاتيح حساسة، تحذير الحجم يظهر فوق 500MB، المشاركة عبر ShareIntent تعمل.
2. [ ] حذف Firebase حقيقي بجلسة وشبكة → يُحذف الحساب فعلًا (المحاكي/JVM لا يغطيها).
3. [ ] بطاقة "الحذف المعلق" تظهر بعد الطلب، و"إلغاء الحذف" يمسحها ويوقف الـ Worker.
4. [ ] سويتشا opt-in يحفظان القيمة بعد إغلاق التطبيق وإعادة فتحه.
5. [ ] قسم الخصوصية يظهر في موضعه الصحيح داخل الإعدادات ولا يكسر التمرير/الطي.
6. [ ] (P3-A السابق) ما زال ينتظر جهازك: رابط "نسيت PIN" + Recovery Key + Device Credential على جهاز حقيقي ببصمة مسجلة.

## ما لم يُنفذ (صراحة)
- تشفير ZIP بكلمة مرور (اختياري — §B1).
- حذف server-side للسحابة (يحتاج backend — §B2).
- إضافة Crashlytics/Sentry SDK (قرار — §B3).
- `assembleRelease` + الاختبار الجهازي (ممنوع/مستحيل هنا — بانتظار محمد).

<!-- ===== nota-p3-c-status.md ===== -->

# Nota — P3-C Status (UX Restructuring)

## القرار العام
- [x] P3-C COMPLETE (كودًا + اختبار وحدة + بناء)
- [ ] الاختبار الجهازي — ينتظر محمد (قسم أدناه)

## ما نُفّذ

### P3-C1 — هيكلة الإعدادات 3 مستويات ✅
- الجذر يعرض: بطاقة Security Status (قابلة للطي) + حقل بحث + 6 صفوف أقسام.
- المسارات: الأمان والخصوصية / المظهر والهوية / التجربة / الذكاء الاصطناعي / الإشعارات / المتقدمة.
- «الفتح السريع» انتقل من الترحيب إلى الأمان (تجاوز قفل).
- «الإتلاف الآمن» خرج من بطاقة الأمان إلى بطاقة مستقلة بعد الخصوصية (إجراء حذف).
- «تغيير PIN + إعادة ضبط PIN» قبل الخصوصية داخل مسار الأمان.
- «المتقدمة» = سجل الوصول (آخر فتح ناجح + محاولات فاشلة — نُقل من بطاقة الأمان) + إصدار التطبيق.
- لا وضع مطور ولا سجلات تشخيصية بعد — موثق كعمل مستقبلي (مرشح P4).
- روابط بطاقة الأمان تنقل لمسار الأمان (بدل التمرير القديم — BringIntoView أُزيل من الشاشة).

### P3-C2 — هيكلة النسخ الاحتياطي ✅
- بطاقة ملخص: «آخر نسخة: {تاريخ} — {Drive/سحابة}» + زر «نسخ الآن» (Drive).
- 3 طرق قابلة للطي (مفتوحة افتراضيًا): Drive / سحابة نوتا (+ بطاقة المفتاح لـ Pro) / ملف .nota.
- Drive: التكرار + عدد النسخ + كلمة المرور مطوية في «إعدادات متقدمة».
- سحابة نوتا: رسائل الخطأ inline عند الحدوث فقط (تحقق — كانت كذلك أصلًا، بلا تغيير سلوك).
- بانر Pro يفتح BottomSheet «تعرّف على الباقات» (عنوان + شرح + زر ترقية + إلغاء) بدل الانتقال المباشر.
- ملف .nota: بطاقتان منفصلتان «تصدير ملف .nota» و«استعادة من ملف .nota» بحقلي كلمة مرور مستقلين.
- روابط سفلية: «سجل الاستعادة» (طي/فتح) + «إعدادات متقدمة» (تفتح متقدمة Drive).

### P3-C3 — بحث الإعدادات ✅
- حقل بحث أعلى جذر الإعدادات (أيقونة 🔍) — يبحث في العناوين والأسماء والكلمات المفتاحية ar+en.
- النتائج قابلة للنقر وتنقل للقسم.
- الفهرس (`SettingsSearch.kt`) لا يحوي أي قيم حساسة — عناوين فقط (مفاتيح AI/كلمات المرور/العبارات غير مفهرسة — يغطيها اختبار وحدة).
- استعلام أقصر من حرفين → فارغ (منع الضجيج).

## Commits (محلية فقط — بلا push)
| Commit | المحتوى |
|---|---|
| `74cee81` refactor(settings) | هيكلة C1 + فهرس البحث + نصوص ar+en |
| `8b7635a` refactor(backup) | هيكلة C2 |
| `0495553` feat(settings): search | اختبار `SettingsSearchTest` (9 اختبارات) |

## نتيجة الاختبارات والبناءات
- `./gradlew test` (عبر variant واحدًا واحدًا — السبب أدناه): **248/248 ناجحة (62×4)، صفر فشل** — تشمل 9 اختبارات `searchSettings` جديدة.
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL**.
- `assembleRelease`: ممنوع في P3-C (مسموح test + assembleDebug فقط) — لم يُشغَّل.
- `grep`: كل المفاتيح الجديدة الـ30 موجودة في ar + en (صفر مفقود).
- توازن الأقواس: متحقق آليًا في الملفين بعد كل تعديل.

## اكتشافات غير متوقعة
1. **ذاكرة البناء (حرج بيئي، ليس كودًا):** الجهاز 3GB RAM، وإعداد المستخدم العام `~/.gradlew/gradle.properties` يفرض `-Xmx1g` متجاوزًا إعداد المشروع (`-Xmx2500m`) — أول `./gradlew test` كامل سقط بـ OOM في `compileMockDebugKotlin`، وتشغيل لاحق سقط في `compileMockReleaseKotlin`. **لم أغيّر أي إعداد** — تجاوزت بتشغيل variant واحدًا في كل مرة (كلها خضراء). التوصية: تشغيل CI على جهاز أقوى، أو توحيد الإعداد لاحقًا بأمر صريح.
2. ملفا `java_pid*.hprof` (مخلفات OOM) حُذفا ولم يُلتزما.
3. `Screen.ADVANCED` في `App.kt` ميت (enum بلا توجيه) — لم ألمسه؛ مسار «المتقدمة» الجديد داخل `SettingsScreen` فقط.
4. تحذيرات الترجمة القائمة (أيقونات/deprecations) لم تُلمس — خارج نطاق P3-C.

## قرارات يجب أن يعرفها محمد
1. **تقسيم الـ commits:** ملف الاختبار `SettingsSearchTest.kt` في commit منفصل (`feat(settings): search`) لاحترام الأسماء المطلوبة — كود البحث نفسه في commit الهيكلة (إجبار ترجمة).
2. **ترتيب مسار الأمان:** أمان → PIN → خصوصية(GDPR) → إتلاف — يطابق ترتيب البرومبت.
3. **المتقدمة:** سجل الوصول + الإصدار فقط — وضع المطور/السجلات التشخيصية عمل مستقبلي.
4. **أخطاء السحابة:** كانت inline مشروطة أصلًا — لم أغيّر سلوكها، فقط وثقت التحقق.
5. **الطي يخفي الحالة:** طي بطاقة Drive يصفي حقول كلمة المرور المؤقتة (سلوك Compose طبيعي) — مقبول لأن المفتوح هو الافتراضي.
6. لا تغيير في أي منطق تشفير — PIN بوابة فقط — الدرع لم يُلمس.

## ما ينتظر محمد (اختبار جهازي — مرتب بالأولوية)
1. [ ] الجذر: 6 أقسام + بحث — التنقل بين المسارات والرجوع يعمل، والبطاقة تطوى عند اكتمال المؤشرات.
2. [ ] البحث: «بصمة» و«theme» و«نموذج» تنقل للأقسام الصحيحة؛ استعلام «sk-» أو أي قيمة حساسة → لا نتائج.
3. [ ] مسار الأمان: الفتح السريع + الإتلاف + PIN + الخصوصية كلها ظاهرة وبترتيبها.
4. [ ] النسخ: الملخص يعرض آخر نسخة صحيحة + «نسخ الآن» يعمل؛ الطي والروابط السفلية تعمل.
5. [ ] شيت الباقات يفتح من البانر (وضع locked) وزر الترقية يقود للـ paywall.
6. [ ] بطاقتا .nota منفصلتان — تصدير ثم استعادة بكلمتي مرور مختلفتين.
7. [ ] تدوير الشاشة داخل مسار فرعي (يفقد المسار — `remember` فقط؛ مقبول أم يحتاج حفظ حالة؟ قرار لمحمد).

## ما لم يُنفّذ (صراحة)
- P3-D كاملًا — لم يبدأ (حسب الأمر).
- وضع المطور / السجلات التشخيصية في المتقدمة (مستقبلي).
- الاختبار الجهازي (لا جهاز لدى الوكيل).

## المخاطر المتبقية
- بيئة البناء الهشة (ذاكرة 1g) قد تسقط أي `./gradlew test` كامل مستقبلًا — يُفضل CI أو جهاز أقوى.
- حالة مسار الإعدادات لا تنجو من تدوير الشاشة (بند 7 أعلاه).

<!-- ===== nota-p3-d-status.md ===== -->

# Nota — P3-D Security Deep — Status Report

المهارة المطبقة: `android` (قراءة SKILL.md قبل التنفيذ — بناء/تشغيل/تصليب + طبقة runtime-first).
الفرع: master (محلي فقط — بلا push). البناءات المسموحة: test + assembleDebug فقط.

## D1 — Per-file keys للمرفقات ✅ done

- ملف جديد `security/PerFileKeys.kt`: توليد مفتاح AES-256 عشوائي لكل ملف + تغليف (wrap) بالمفتاح الرئيسي `nota_media_key` (يعمل كـ KEK) عبر AES/GCM.
- ملف جديد `security/PerFileMedia.kt`: ظرف `NOTAM2` للملفات + ملف مفتاح جانبي `<name>.key` بصيغة `NOTAK1` (مفتاح الملف مغلّف بالرئيسي).
- **قرار موثق (بديل عن صفوف DB):** المفاتيح المغلّفة تُخزَّن ملفات جانبية في نفس المجلد الخاص (filesDir — نفس حماية DB) بدل جدول Room. الأسباب: بلا ترحيل Room، بلا إغلاق DB، وحذف المفتاح = crypto-shred طبيعي. الخاصية الأمنية مطابقة (السرية من المفتاح الرئيسي لا من مكان التخزين).
- **قرار حرج:** لا يُتحقق من `encoded.size` للمفتاح الرئيسي — مفاتيح Keystore غير قابلة للاستخراج (`encoded=null`) وكان التحقق سيكسر الجهاز؛ التحقق فقط لمفتاح الملف البرمجي.
- الربط: `NoteMediaFiles` (نسخ/تحميل/مؤقت/حجم/ختم/رسم) + `NoteBgFiles` (نسخ/تحميل/تكرار) تكتب ملفات جديدة بمفتاح خاص مع سقوط تلقائي للمسار القديم عند تعذر Keystore. `readAll` يفك الجديد والقديم معًا + **ترحيل كسول** للقديم عند أول قراءة ناجحة (بلا إغلاق DB — رد على شرط الخطة).
- `NotaCloudViewModel` (موقِعان) و`GdprExportRunner` انتقلا لـ `readAll` — **اكتشاف:** `GdprExportRunner` كان سيسرّب `*.key` (مادة مفاتيح) داخل ZIP التصدير — أُضيف استبعاد صريح `isKeySidecar` (راجع "اكتشافات").
- لم يُمس: KeyManager / MediaCrypto / DbProvider / CloudKeyStore / PassphraseStore (سوى استدعاءات، بلا تغيير منطق).
- توثيق صريح: حذف مفتاح ملف = فقدان أبدي لذلك الملف — هذا هو الهدف.
- اختبارات: `PerFileKeysTest` (10 اختبارات JVM خالصة — توليد/تغليف/فك/مفتاح خاطئ/عبث/تظرف/ملفات مؤقتة/ترحيل legacy/صمود بعد حذف المفتاح).

## D2 — تشفير أسماء الملفات ✅ done

- ملف جديد `data/OpaqueFileNames.kt`: أسماء `32-hex + ext` (مثال `a7f3e9c1….jpg`) + كشف `isLegacy` (بادئات `img_/file_/rec_/draw_/bg_`) + `isOpaque` + `extOf`.
- المواقع الستة الجديدة كلها معتمة: صور/ملفات/صوت/رسم الملاحظات + نسخ/تكرار الخلفيات.
- **الخريطة hash→note موجودة أصلًا ولا تحتاج جدولًا:** `note_attachments.file → noteId` و`notes.background → id` — وُثّق ذلك بدل إنشاء جدول.
- الترحيل: استعلاما `renameFile` (ContentDao) و`renameBackground` (NoteDao) — بلا تغيير سكيما — + `NotesRepository.migrateMediaFilenames()` (idempotent: الملف + جانبي `.key` معًا ثم مراجع DB، واليتيم يُعتَّم بلا ضرر) — تُستدعى مع صيانة فتح القفل (`purgeTrash`) وكل خطوة `runCatching`.
- اختبارات: `OpaqueFileNamesTest` (5 اختبارات JVM).

## D3 — SecureDelete → Crypto-shred ✅ done (D1 منجز — بلا حاجة لمسار المفتاح المشترك)

- `SettingsStore`: ثابت جديد `SHRED_CRYPTOSHRED` + `SHRED_OPTIONS = [cryptoshred]` فقط؛ `SHRED_DOD/SCHNEIER` موسومة `@Deprecated` (محفوظة للتوافق)؛ `migrateShredMethod()` تهاجر أي قيمة قديمة/غريبة → cryptoshred (على القراءة والكتابة معًا — شفاء ذاتي بلا كسر القيم المخزنة).
- `SecureDelete.cryptoShred(file, wipeKey)`: المفتاح أولًا ثم الملف، بلا تمريرات (تعليل موثق: التمريرات خرافة على الفلاش)؛ `shred()` القديمة باقية للتوافق وتُوجَّه لـ cryptoShred عند الطلب.
- `NoteMediaFiles.shred` / `NoteBgFiles.shred`: عند cryptoshred → `PerFileMedia.cryptoShred` (يحذف `.key` ثم الملف).
- الواجهة: `ShredMethodOptions` تعرض سطر crypto-shred الوحيد + وصفه (بلا خيارات) — `ThemeOption` باقية للاستخدامات الأخرى.
- نصوص جديدة ar+en: `shred_cryptoshred` + `shred_cryptoshred_desc` + تحديث `shred_sub` (تحقق grep: موجودة في ar+en؛ مفاتيح `shred_single/dod/schneier` أُبقيت خامدة بلا مرجع كودي — لا مفاتيح معلقة جديدة).
- ملاحظة残留: ملفات legacy غير المُرحّلة بعد بلا مفتاح جانبي — crypto-shred لها = حذف عادي (مخاطر موثقة أدناه).
- اختبارات: `CryptoShredTest` (5 اختبارات JVM — الترتيب مفتاح-أولًا، ملف مفقود، توجيه legacy، هجرة القيم).

## D4 — تشفير عمود self_messages ✅ done

- ملف جديد `security/SelfMessageCrypto.kt`: مفتاح Keystore مستقل `nota_self_msg_key` (ملف جديد — لا يمس KeyManager) + ظرف `NOTAS1` (java.util.Base64 — آمن JVM).
- **تصحيح للخطة (موثق):** جدول `self_messages` فيه عمود `text` فقط — لا توجد أعمدة `location/geofence` — فشُفّر `text` وحده. بلا تغيير سكيما (الظرف نص في نفس العمود).
- الربط في `SelfChatViewModel`: ختم عند `send/edit` (سقوط للعادي عند تعذر Keystore — SQLCipher يبقى الضابط الأساسي، والدردشة لا تُكسر) + فك عند العرض (`decryptOrLegacy` لا ترمي أبدًا) + **ترحيل كسول** للصفوف القديمة بعد أول قراءة (idempotent).
- اختبارات: `SelfMessageCryptoTest` (6 اختبارات JVM — ذهاب/عودة، IV fresh، مفتاح خاطئ، عبث، legacy passthrough، فارغ).

## D5 — إغلاق i18n gaps ✅ done

- القياس قبل الحذف: `values-hi` (907 أسطر — 455 سطرًا بتلوث CJK/كوري/ياباني/ألماني)، `values-ru` (909 أسطر — 730 سطرًا عربيًا)، `values-zh` (904 أسطر — 741 سطرًا عربيًا). التلوث أغلبي لا هامشي → **حذف الملفات الثلاثة كاملة** (fallback للعربية الافتراضية — أفضل من واجهة روسية بنص عربي، حسب الخطة).
- `values-ms` و`values-tr`: مجلدان فارغان (غير متتبعين) — أُزيلا من القرص.
- الباقي: `values` (عربي افتراضي) + `en/es/fr/it/ja` — لم تُمس.
- لا مرجع كودي للغات المحذوفة (أندرويد يكتشف تلقائيًا).

## Commits (محلية فقط — بلا push)

1. `6a3f0ef` feat(security): per-file media encryption keys
2. `edc8995` feat(security): encrypt media filenames
3. `4ab6cc2` feat(security): replace DOD/Schneier with crypto-shred
4. `6cb327e` feat(security): column-level encryption for self_messages
5. `2fb4ed3` i18n: fix broken locale files (hi/ru/zh/ms/tr)

## نتيجة الاختبارات

- اختبارات جديدة: 26 (PerFileKeys 10 + Opaque 5 + CryptoShred 5 + SelfMessage 6) — كلها JVM خالصة (java.util.Base64 حصرًا في الكود الجديد — بلا Robolectric).
- `./gradlew test`: **BUILD SUCCESSFUL في الـvariants الأربعة — 88 اختبار × 4 = 352 تنفيذًا، 0 فشل/0 skipped/0 errors** (62 سابق + 26 جديدة). ملاحظة بيئية: الاستدعاء الكامل الموحد سقط بـ OOM في `compileMockDebugKotlin` — نفس الفخ الموثق في P3-C (إعداد المستخدم العام `~/.gradle` يفرض `-Xmx1g` متجاوزًا `-Xmx2500m` للمشروع، وجهاز 3.8GB) — **لم يُغيَّر أي إعداد**، ونُفّذ variant واحدًا واحدًا (`testMockDebug` 5m22s ثم `testMockRelease` 4m35s ثم `testPlayDebug` 5m23s ثم `testPlayRelease` 4m54s) — نفس التغطية تمامًا.
- `./gradlew assembleDebug` (مقسمًا لنفس السبب): **BUILD SUCCESSFUL** — `assembleMockDebug` (1m37s) + `assemblePlayDebug` (40s) — APKs جديدة (40MB لكل variant). ممنوع `assembleRelease`/الرفع — التُزم.
- تحذيرات فقط: deprecation مزدوجة في `SecureDeleteTest` (تستخدم SHRED_DOD/SCHNEIER القديمين — مقصود للتوافق، والاختبارات خضراء).
- ملفات الأسرار: لم يُضف أي ملف جديد للـ git (فقط الكود/الاختبارات/التقارير).

## اكتشافات غير متوقعة

1. **(حساس — أُصلح فورًا) `GdprExportRunner` كان سيصدّر مادة المفاتيح:** يسرد كل مجلد `note_media` — مع D1 ظهرت ملفات `<name>.key` (مفاتيح مغلّفة) وكان سيضمّنها في ZIP "بياناتي" ويفك المشفر منها كـ"خام" عند الفشل. أُضيف استبعاد `isKeySidecar` + فك عبر `readAll`. التأثير قبل الإصلاح: أي تصدير GDPR بعد D1 كان سيسلّم مفاتيح ملفات المستخدم (مغلّفة بالرئيسي — ليست كارثة فورية لكن تسريب مادة مفاتيح مرفوض).
2. **فخ `sealIfPlain` على ملفات NOTAM2:** يقبل أي ملف لا يبدأ بـ `NOTAM1` كـ"عادي" وكان سيُعيد تشفير ملفات المفتاح-الخاص بالمفتاح المشترك (يُيتم `.key` + خفض أمني صامت). كل مسارات الوسائط انتقلت لـ `PerFileMedia.sealPlain` الذي يميّز الثلاثي (عادي/legacy/NOTAM2).
3. **فخ `Keystore.encoded=null`:** أي تحقق من طول المفتاح الرئيسي عبر `encoded` كان سيكسر كل أجهزة الإنتاج (مفاتيح Keystore غير قابلة للاستخراج) — وُثّق في `PerFileKeys`.
4. **خطة D4 ذكرت أعمدة `location/geofence` غير موجودة** في `self_messages` (فقط `text`) — شُفّر الموجود وُوُثّق.
5. **خطة D1 افترضت تخزين المفاتيح في DB + إغلاق DB للترحيل** — نُفّذ بديل الملفات الجانبية + ترحيل كسول (مبررات أعلاه).

## قرارات يجب أن يعرفها محمد

1. مفاتيح الملفات **جانبية مشفرة** لا صفوف DB (مبررات في D1) — إن أردت نقلها لـ Room لاحقًا فالبنية (`PerFileKeys` الخالصة) تسمح بتبديل المخزن فقط.
2. سياسة فشل D4 **fail-open للعادي** عند تعذر Keystore (بدل كسر الدردشة) — SQLCipher هو الضابط الأساسي. إن أردت fail-closed صارمًا، التغيير سطر واحد في `sealOrPlain`.
3. حذف ملفات `hi/ru/zh` كاملة (لا إصلاح جزئي) — المستخدمون بهذه اللغات يرون العربية الآن. الترجمة الحقيقية لاحقًا تعيد الملفات.
4. مفاتيح `shred_single/dod/schneier` في XML أُبقيت خامدة (للغات es/fr/it/ja) — تنظيفها الكامل اختياري لاحقًا.

## ما ينتظر محمد (اختبار جهازي — مرتب بالأولوية)

1. [ ] ملف وسائط جديد (صورة) → يوجد `<name>.key` بجانبه + ظرف NOTAM2 + الاسم 32-hex — والعرض يعمل.
2. [ ] صورة legacy (من نسخة سابقة) تُفتح → تُرحَّل تلقائيًا (NOTAM2 + ‎.key) وتستمر بالعرض.
3. [ ] حذف مرفق مع تفعيل المسح الآمن → الملف + ‎.key اختفيا (مدير ملفات/فحص filesDir).
4. [ ] ملف legacy أُعيد تسميته لمعتم بعد فتح القفل (فحص filesDir + المرفق ما زال يُعرض).
5. [ ] نسخ احتياطي سحابي لملف مفتاح-خاص → يُرفع ويُستعاد ويُعرض (مسار `readAll`).
6. [ ] تصدير GDPR → ZIP بلا أي `*.key` + الصور تُفتح.
7. [ ] رسالة نفس جديدة → صف DB يبدأ بـ NOTAS1 + العرض سليم + رسالة legacy تُرحَّل بعد الفتح.
8. [ ] الإعدادات → الأمان: سطر crypto-shred الوحيد يظهر (بلا خيارات DOD/Schneier) + إعداد قديم (dod) يظهر مهاجرًا.
9. [ ] تبديل لغة الجهاز للروسية/الصينية/الهندية → الواجهة عربية (fallback) بلا crash.
10. [ ] سيناريو Keystore مقفل/تالف: الدردشة الذاتية والوسائط تسقط للمسارات القديمة بلا crash (fail-open موثق).

## المخاطر المتبقية (P4؟)

- ملفات legacy غير المُرحّلة بعد تشترك بالمفتاح الرئيسي (نافذة تنغلق بالترحيل الكسول) — وcrypto-shred لها حذف عادي فقط.
- الصوتيات (`rec_*.m4a`) تُسجَّل خامًا أثناء التسجيل ثم تُختم بعده — نافذة قصيرة لملف خام على القرص (قائمة من قبل P3-D — لم تُوسَّع).
- `decryptedTemp` (نسخ العرض المؤقتة) خارج نطاق P3-D — تُمسح بالاستبدال لكنها خام في cache.
- لا كشف عبث يُنبّه المستخدم عند فشل فك NOTAM2 (فشل صامت → null) — مقصود (بلا تسريب) لكن يستحق رسالة UI لاحقًا.

<!-- ===== nota-p3-final-status.md ===== -->

# Nota — P3 Final Status

المهارة المطبقة عبر P3-D (مرحلتي): `android` (قراءة SKILL.md قبل التنفيذ). الفرع: master محليًا — **بلا push إطلاقًا**. كل التقارير في `nota-p3-*-status.md`.

## القرار العام

- [x] P3 COMPLETE — كل المراحل الفرعية منجزة كودًا ووحدةً وبناءً (debug) محليًا.
- [ ] P3 PARTIAL — لا شيء معلّق كوديًا؛ المعلّق الوحيد هو **الاختبار الجهازي** (لا جهاز لدى الوكيل — 30 بندًا أدناه تنتظر محمد).

## المراحل الفرعية

| المرحلة | الحالة | Commits |
|---|---|---|
| P3-0 (إغلاق ثغرة GCM) | done | 1 (`a487894`) |
| P3-A1 (نسيت PIN + بصمة) | done | 1 (`2eec696`) |
| P3-A2 (Recovery Key) | done | 3 (`b27f999`, `47235ff`, `dae79ba`) + تقرير (`53fc29e`) |
| P3-A3 (Device Credential + بطاقة الأمان) | done | 3 (`e2d9875`, `7613d81`, `dd1a762`) + تقرير (`fa89171`) |
| P3-B1 (تصدير GDPR) | done | 1 (`f4693dd`) |
| P3-B2 (حذف الحساب) | done | 1 (`ed0ce50`) |
| P3-B3 (opt-in الخصوصية) | done | 1 (`a5ff399`) + تقرير B (`b7d75be`) |
| P3-C1 (هيكلة الإعدادات) | done | 1 (`74cee81`) |
| P3-C2 (هيكلة النسخ) | done | 1 (`8b7635a`) |
| P3-C3 (بحث الإعدادات) | done | 1 (`0495553`) + تقرير C (`59693f0`) |
| P3-D1 (مفاتيح لكل ملف) | done | 1 (`6a3f0ef`) |
| P3-D2 (تعتيم الأسماء) | done | 1 (`edc8995`) |
| P3-D3 (crypto-shred) | done | 1 (`4ab6cc2`) |
| P3-D4 (تشفير self_messages) | done | 1 (`6cb327e`) |
| P3-D5 (إصلاح اللغات) | done | 1 (`2fb4ed3`) |

إجمالي P3: **19 commit كود/تقارير** (1+1+3+3+1+1+1+1+1+1+5) — كلها محلية، بلا push.

## نتيجة الاختبارات

- إجمالي الاختبارات الجديدة في P3: **60** (P3-0: 3 GCM + A1: 0 (واجهات جهازية) + A2: 7 RecoveryKey + A3: 6 SecurityStatus + B: 9 GDPR + C: 9 searchSettings + D: 26 [PerFileKeys 10 + Opaque 5 + CryptoShred 5 + SelfMessage 6]).
- نتيجة `./gradlew test` (بعد P3-D): **BUILD SUCCESSFUL — 88 اختبار × 4 variants = 352 تنفيذًا، 0 فشل** (62 سابق + 26 جديدة).
- Flaky؟ **لا** — أربع runs متتالية للـvariants كلها خضراء من أول مرة.
- نتيجة `./gradlew assembleDebug`: **نجاح** (mock + play — APKs جديدة ~40MB).
- نتيجة `./gradlew assembleRelease`: نُفّذت في P3-A3 (نجاح — minify/R8 سليم) ولم تُعَد بعد D (ممنوع البناء إلا test/debug في هذه المهمة — **يُعاد تشغيلها بأمر صريح قبل أي نشر** لأن D غيّر مسارات التشفير).
- ملاحظة بيئية (موثقة من P3-C وتكررت): الاستدعاء الموحد الكامل يسقط OOM (`~/.gradle` العام يفرض `-Xmx1g`) — نُفّذ variant واحدًا واحدًا بلا تغيير أي إعداد.

## اكتشافات غير متوقعة (أبرزها — التفاصيل في تقارير المراحل)

1. **P3-D: تسريب مفاتيح في تصدير GDPR (أُصلح):** `GdprExportRunner` كان سيسرّب ملفات `*.key` (مادة مفاتيح) داخل ZIP "بياناتي" — أُضيف استبعاد + فك عبر `readAll`.
2. **P3-D: فخ `sealIfPlain` على ملفات NOTAM2 (أُصلح):** كان سيُعيد تشفير ملفات المفتاح-الخاص بالمشترك (يُيتم `.key`) — كل المسارات انتقلت لـ `PerFileMedia.sealPlain`.
3. **P3-D: فخ `Keystore.encoded=null` (تفاداه التصميم):** أي تحقق طول عبر `encoded` كان سيكسر كل الأجهزة — وُثّق.
4. **P3-D: خطة D4 ذكرت أعمدة `location/geofence` غير موجودة** في `self_messages` — شُفّر `text` الموجود.
5. **P2 (موروث):** ثغرة `AEADBadTagException` الخام — **أُغلقت في P3-0** (رسالة موحدة + 3 اختبارات).
6. بيئة البناء الهشة (1g) — توصية P3-C المستمرة: CI أو جهاز أقوى.

## قرارات يجب أن يعرفها محمد

1. **D1:** مفاتيح الملفات **جانبية مشفرة** لا صفوف DB (بلا ترحيل Room/إغلاق DB — نفس الخاصية الأمنية). التبديل لـ Room لاحقًا = تبديل مخزن فقط.
2. **D3:** DOD/Schneier أُزيلتا من الواجهة (خرافة على الفلاش) — الثوابت باقية `@Deprecated` للتوافق، والقيم المخزنة تُهاجَر ذاتيًا. D1 منجز فلا حاجة لمسار "المفتاح المشترك".
3. **D4:** سياسة الفشل **fail-open للعادي** عند تعذر Keystore (SQLCipher هو الأساس) — التحويل لـ fail-closed = سطر واحد في `sealOrPlain` إن أردت.
4. **D5:** حُذفت ملفات `hi/ru/zh` كاملة (التلوث أغلبي: 455/730/741 سطرًا) + مجلدا `ms/tr` الفارغان — هذه اللغات تسقط للعربية الآن.
5. **P3-B:** ما لم يُنفذ: تشفير ZIP بكلمة مرور + حذف server-side + Crashlytics/Sentry (قرارات.only — في تقرير B).
6. **P3-C:** تدوير الشاشة داخل مسار فرعي يفقد المسار (`remember` فقط) — قرارك: مقبول أم حفظ حالة؟
7. **الـ PIN بوابة وصول فقط في كل P3** — لم يُجعَل مفتاح تشفير في أي مسار؛ الدرع (`notePinFail`) لم يُكسر.

## ما ينتظر محمد (اختبار جهازي — 30 بندًا، مرتب بالأولوية)

**P3-0 (حرج — ثغرة):**
1. [ ] استعادة `.nota` بكلمة خاطئة → رسالة عامة، لا crash.
2. [ ] استعادة ملف مُقتطع/مُعدَّل → نفس الرسالة العامة.

**P3-A (وجودي — أولوية قصوى):**
3. [ ] رابط "نسيت PIN" على القفل (حتى أثناء الدرع) → الشيت بالخيارات المتاحة فقط.
4. [ ] بصمة → PIN جديد → فتح بالجديد؛ العدّاد لا يُصفَّر وحده.
5. [ ] إعادة الضبط (حواران + "امسح") → `factoryReset` → إعداد جديد.
6. [ ] Recovery Key: توليد (12 كلمة) → كويز → استرداد → PIN جديد؛ الخاطئة تُحتسب درعيًا.
7. [ ] إعادة إنشاء المفتاح → القديم يتوقف؛ بطاقة الترحيل (تظهر/تُخفى أسبوعًا).
8. [ ] قفل الجهاز: OFF افتراضيًا → تحذير → "استخدم قفل الجهاز" → PIN جديد.
9. [ ] البطاقة (5 مؤشرات + طي)؛ تحذير البصمة بلا تسجيل؛ حوار تعطيل البصمة.

**P3-B (قانوني):**
10. [ ] تصدير حقيقي → ZIP يفتح + `settings.json` بلا أسرار + تحذير 500MB + ShareIntent.
11. [ ] حذف Firebase حقيقي (شبكة) + بطاقة "الحذف المعلق" + "إلغاء الحذف".
12. [ ] سويتشا opt-in يصمدان بعد إعادة الفتح؛ قسم الخصوصية سليم الموضع.

**P3-C (تجربة):**
13. [ ] الجذر (6 أقسام + بحث) وتنقلها؛ البحث («بصمة»/«theme») + حجب الحساس («sk-» → لا نتائج).
14. [ ] مسار الأمان الكامل؛ ملخص النسخ + «نسخ الآن»؛ شيت الباقات؛ بطاقتا .nota المنفصلتان.

**P3-D (دفاعي — بنود تقرير D العشرة):**
15. [ ] صورة جديدة → `<name>.key` + NOTAM2 + اسم 32-hex + عرض سليم.
16. [ ] صورة legacy تُفتح → ترحيل تلقائي + استمرار العرض.
17. [ ] حذف مرفق (المسح مفعّل) → الملف + ‎.key اختفيا.
18. [ ] legacy يُعتَّم بعد فتح القفل + المرفق ما زال يُعرض.
19. [ ] نسخ سحابي لملف مفتاح-خاص → رفع/استعادة/عرض.
20. [ ] GDPR → ZIP بلا `*.key` + الصور تُفتح.
21. [ ] رسالة نفس جديدة → صف NOTAS1 + عرض سليم + ترحيل legacy.
22. [ ] سطر crypto-shred الوحيد (بلا DOD) + إعداد dod قديم يظهر مهاجرًا.
23. [ ] لغات hi/ru/zh → fallback عربي بلا crash.
24. [ ] Keystore تالف: سقوط ناعم بلا crash (وسائط + دردشة).
25. [ ] **قبل أي نشر: `assembleRelease` + تثبيت نسخة release على جهاز** (D غيّر مسارات التشفير بعد آخر release ناجح في P3-A3).

**عام:**
26. [ ] بوابة الإصدار: اعتماد P3-A جهازيًا كان شرط B/C/D كوديًا (أُنجز الكود بلا الاعتماد — راجع إن لزم).
27. [ ] CI (أُضيف في P3، لم يُشغَّل حيًا — يحتاج push بقرارك).
28. [ ] قرار تدوير الشاشة (بند 6 أعلاه).
29. [ ] قرارات B المؤجلة (تشفير ZIP/حذف سيرفر/Crashlytics).
30. [ ] قرار fail-open/fail-closed لـ D4 (بند 3 أعلاه).

## ما لم يُنفّذ (صراحة — كامل P3)

- تشفير ZIP بكلمة مرور (B1 — اختياري).
- حذف server-side للسحابة (B2 — يحتاج backend).
- Crashlytics/Sentry SDK (B3 — قرار مؤجل).
- وضع المطور/السجلات التشخيصية (C — مستقبلي).
- ترجمات hi/ru/zh/ms/tr الحقيقية (D5 — حُذفت ملفاتها؛ تعود عند توفر مترجم).
- تنظيف مفاتيح `shred_single/dod/schneier` الخامدة من XML (D3 — تجميلي).
- `assembleRelease` بعد P3-D + الاختبار الجهازي كله (30 بندًا أعلاه).
- `git push` / CI حي / أي رفع (ممنوع حتى أمر صريح).

## المخاطر المتبقية (P4؟)

- نافذة ملفات legacy بالمفتاح المشترك (تنغلق بالترحيل الكسول)؛ crypto-shred لها حذف عادي فقط.
- الصوتيات تُسجَّل خامًا لحظيًا قبل الختم؛ `decryptedTemp` خام في cache (خارج نطاق P3-D).
- فشل فك NOTAM2 صامت (مقصود ضد التسريب) — يستحق رسالة UI لاحقًا.
- بيئة البناء (1g) — CI/جهاز أقوى موصى به.

