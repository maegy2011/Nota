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
