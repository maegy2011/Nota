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
