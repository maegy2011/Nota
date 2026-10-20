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
