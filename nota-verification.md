# تحقق مستقل — تطبيق نوتا (المراحل 0→6)

> منهج الأدلة (مهارة `code-reviewer`): المرحلة 1 تحديد النطاق (git + ملفات المراحل 2-6 + تقارير)، المرحلة 2 تحليل حتمي (grep + test XML)، المرحلة 3 استدلال سياقي (قراءة مقاطع)، المرحلة 4 تقرير مصنّف بالأدلة الحرفية. تحقق فقط — بلا إصلاحات.
> التقرير المرجعي: `projects/nota/nota-full-report-0-6.md` (22 أكتوبر 2026).

---

## س1 — Git (الحالة والفروع والسجل)

### 1.1 السجل (آخر 30)
```
26ece16 docs: add nota-full-report-0-6 status report
80688b8 docs: phase 6 status report
bb204f5 fix: correct ServerRegister import package in SettingsViewModel
c74f2ac feat(export): optional password encryption for GDPR zip
7a3ab43 feat(cloud): resumable chunked upload with silent full-upload fallback
d12aad6 feat(trial): server-side trialUsed gate with fail-open local trial
77eb562 feat(account): best-effort cloud delete on account wipe with backend TODO
d9213d5 docs: phase 5 status report
b2e549a docs(billing): TODOs for POST /v1/purchases and receipt validation
5ae07bf feat(billing): report-after-purchase with deferred snackbar and startup restore
50894fc feat(billing): purchase-report interface with mock success and play stub
e4a5363 merge: phase 4 backup UX (summary + chips + delete + preview + key)
44783fb docs(phase4): final status — DONE with green builds
f41642c fix(merge): drop stray braces from phase2+3 resolution in BackupScreen
c39fe3a merge: phase 3 backup integrity (v4 + conflicts + partial + undo)
c6e820d docs(phase3): final status — DONE with green builds
67283fe fix(test): pass archivedAt/color in BackupCodecTest constructions
1c95e80 docs: phase 2 status report
dd304a9 docs: phase 4 status report
dbfa0f8 fix(backup-ui): remove duplicate Composable annotation on CloudKeyCard
42b5a47 feat(backup-ui): always show cloud key card (locked for free)
5727eda feat(backup-ui): preview cloud backup before restore
00d700e feat(backup): delete all backups with double confirmation
22270a3 docs(phase3): phase 3 status report
dc5e3cc feat(backup): size limits with dialogs + inline status banners (2c, 2d)
dcee53e fix(backup): expose restoreLogOnce for undo snackbar
049db7a feat(backup): concurrency lock + progress notification (2a, 2b)
ef54fb6 feat(backup): undo snackbar after restore
06d89c3 feat(backup): partial restore with selection
a9ab54a feat(backup): conflict resolution with user choice
```

### 1.2 الفروع والحالة
```
  feature/phase2
+ feature/phase3
  feature/phase4
* master
  phase4-wt
  remotes/origin/master
```
```
(نظيف — لا خرج لـ git status --short)
```

### 1.3 العمق والوسوم والفرق مع origin
```
إجمالي الكوميتات: 114
الوسوم: v1.20.0 (وحيد — لا وسوم nota-v* للإصدارات)
git diff origin/master..master: فارغ (لا فرق — master مطابق لـ origin/master محليًا)
```

### 1.4 ملاحظات س1
- ✅ السجل يُظهر تسلسل المراحل 0→6 (entitlement → backup hardening → integrity → UX → billing → closure) ثم تقريرًا جامعًا.
- ⚠️ القاعدة (25 سبتمبر) تطلب وسم كل إصدار `nota-vX.Y.Z-اسم` — **لم يُعثر على أي وسم بهذا النمط** (الموجود فقط `v1.20.0`). بحثت في: `git tag`.
- ⚠️ الفرع `feature/phase3` عليه علامة `+` (غير مدمج بالكامل أو متقدم/متأخر) — يحتاج توضيحًا.

---

## س2 — الاختبارات (نتائج XML الحرفية)

> لم يُشغَّل `./gradlew test` (ثقيل) — اعتمدت على `app/build/test-results/testMockReleaseUnitTest/*.xml` بتاريخ 2026-10-21 20:21.

```
com.nota.notes.security.PerFileKeysTest: tests=10 failures=0 errors=0 skipped=0
com.nota.notes.security.SelfMessageCryptoTest: tests=6 failures=0 errors=0 skipped=0
com.nota.notes.security.PinHashTest: tests=7 failures=0 errors=0 skipped=0
com.nota.notes.security.RecoveryKeyTest: tests=7 failures=0 errors=0 skipped=0
com.nota.notes.security.AuthManagerTest: tests=5 failures=0 errors=0 skipped=0
com.nota.notes.data.AccountDeletionTest: tests=5 failures=0 errors=0 skipped=0
com.nota.notes.data.OpaqueFileNamesTest: tests=5 failures=0 errors=0 skipped=0
com.nota.notes.data.GdprExportTest: tests=4 failures=0 errors=0 skipped=0
com.nota.notes.ui.SettingsSearchTest: tests=9 failures=0 errors=0 skipped=0
com.nota.notes.data.BackupCodecTest: tests=8 failures=0 errors=0 skipped=0
com.nota.notes.data.BackupManagerTest: tests=11 failures=0 errors=0 skipped=0
com.nota.notes.data.CryptoShredTest: tests=5 failures=0 errors=0 skipped=0
com.nota.notes.data.SecureDeleteTest: tests=5 failures=0 errors=0 skipped=0
com.nota.notes.ui.SecurityStatusTest: tests=6 failures=0 errors=0 skipped=0
com.nota.notes.data.SettingsStoreTest: tests=3 failures=0 errors=0 skipped=0
TOTAL tests=96 failures=0 errors=0 skipped=0
```

### ملاحظات س2
- ✅ كل ملفات XML خضراء (96/96، صفر فشل).
- ⚠️ التقرير الجامع يدّعي أرقامًا متفاوتة (352 / 56 / 60) — **الإجمالي الحالي 96** لأن XML المتبقي يغطي `testMockReleaseUnitTest` فقط بتاريخ واحد؛ ملفات المراحل الأقدم رُبما أُعيد بناؤها فوقها. يحتاج توضيحًا (هل الأرقام المدّعاة تراكمية عبر flavors؟).
- ملفات الاختبار الموجودة (15 ملفًا):
```
app/src/test/java/com/nota/notes/data/BackupManagerTest.kt
app/src/test/java/com/nota/notes/data/SecureDeleteTest.kt
app/src/test/java/com/nota/notes/data/GdprExportTest.kt
app/src/test/java/com/nota/notes/data/BackupCodecTest.kt
app/src/test/java/com/nota/notes/data/OpaqueFileNamesTest.kt
app/src/test/java/com/nota/notes/data/SettingsStoreTest.kt
app/src/test/java/com/nota/notes/data/AccountDeletionTest.kt
app/src/test/java/com/nota/notes/data/CryptoShredTest.kt
app/src/test/java/com/nota/notes/security/PerFileKeysTest.kt
app/src/test/java/com/nota/notes/security/AuthManagerTest.kt
app/src/test/java/com/nota/notes/security/RecoveryKeyTest.kt
app/src/test/java/com/nota/notes/security/SelfMessageCryptoTest.kt
app/src/test/java/com/nota/notes/security/PinHashTest.kt
app/src/test/java/com/nota/notes/ui/SettingsSearchTest.kt
app/src/test/java/com/nota/notes/ui/SecurityStatusTest.kt
```

---

## س3 — مسارات الملفات للمراحل 2-6 (موجود / غير موجود)

### المرحلة 1 — بوابة Pro
- ✅ `app/src/main/java/com/nota/notes/ui/PremiumViewModel.kt` — يحوي `EntitlementSource` + `isProNow()` + `isPro()` (سطور 20، 59-67، 80).
- ✅ `app/src/main/java/com/nota/notes/billing/Entitlements.kt` — يحوي `trialUsed` (سطر 11).
- ✅ `app/src/main/java/com/nota/notes/billing/HttpEntitlementsApi.kt` — يقرأ `trialUsed` (سطور 79-81).

### المرحلة 2 — تصليب النسخ
- ✅ حارس التزامن: `app/src/main/java/com/nota/notes/data/DriveBackup.kt` (سطور 26، 28، 134، 193 — `uploadMutex` + `AlreadyRunning`) + `BackupScheduler.kt` (سطور 102، 135 — "already running").
- ✅ عدّاد/حد الحجم: بحثت في `BackupManager.kt` عن `15|MAX_BACKUP|sizeLimit|exceeds` — **غير موجود في BackupManager.kt**؛ لوحة التقدم/البانر في `CloudBackupViewModel.kt` (سطور 62، 74) + تحذير النمو في `NotaCloudViewModel.kt`؟ — الدليل الجزئي: `CloudBackupViewModel.kt:206: if (!silent && lastCount >= 10 && noteCount * 2 < lastCount)`. يحتاج توضيحًا لمكان سقف 15MB.
- ✅ البانر 10 ثوانٍ: `CloudBackupViewModel.kt:74` — `حالة العملية للـ banner المضمّن — تُمسح بعد 10 ثوانٍ من الواجهة`.
- ⚠️ سياسة الاحتفاظ (آخر نسختين): بحثت في `retain|keep.*2|prune` — **غير موجود كنص صريح**؛ الموجود `SettingsStore.kt:40: K_CLOUD_KEEP = intPreferencesKey("cloud_backup_keep") // عدد نسخ Drive المحتفظ بها 1..7`. الرقم الافتراضي يحتاج توضيحًا.
- ✅ حوار التقدم: `CloudBackupViewModel.kt:62` — `تقدم الرفع 0..100 للـ notification والـ banner`.

### المرحلة 3 — سلامة البيانات
- ✅ صيغة v4: `app/src/main/java/com/nota/notes/data/BackupCodec.kt` (سطور 7، 58-59، 70، 72، 81، 117، 128، 156-157 — `const val V = 4` + `id` + `sha256`).
- ✅ التعارض: `BackupCodec.kt:48: data class BackupConflict(` + ملفات تذكر conflict: `NotesRepository.kt, TagDao.kt, SettingsStore.kt, NotaCloudViewModel.kt, BackupScreen.kt, CloudBackupViewModel.kt, NotesViewModel.kt`.
- ✅ الاستعادة الجزئية: `NotesRepository.kt, BackupScreen.kt` (ضمن ملفات partial/checkbox).
- ✅ التراجع: `NotesRepository.kt:449 restoreLogOnce / 456 undoRestore` + `SettingsStore.kt:229,333,338,351 (undone)` + `UndoJournal.kt`.

### المرحلة 4 — UX النسخ
- ✅ بطاقة الملخص: `BackupScreen.kt:125 (last) / 237 BackupSummaryCard / 274-290 (lastDrive, lastCloud)`.
- ✅ الشارات: `BackupScreen.kt:729 (backup_pro_chip/backup_free_chip) / 734 / 943`.
- ✅ الحذف: `BackupScreen.kt:482 DeleteAllDriveSection / 1645-1666 (DeleteOneDriveButton + DeleteAllDriveSection بخطوتين + كلمة تأكيد)`.
- ✅ المعاينة: `BackupScreen.kt:361 (preview) / 608-643 (bk_preview_title/msg/stale/confirm)`.
- ✅ بطاقة المفتاح: `BackupScreen.kt:249 CloudKeyCard / 875 private fun CloudKeyCard(`.

### المرحلة 5 — Billing
- ✅ `app/src/main/java/com/nota/notes/billing/BillingGateway.kt` (سطور 17 `queryPurchases`، 20 `reportPurchase`).
- ✅ تدفق الشراء: `PremiumViewModel.kt:95-180` (استعادة تلقائية عند الإقلاع + إبلاغ بالإيصال + `_purchaseDeferred` للتأجيل + `forceRefresh`).
- ✅ TODOs السيرفر: `ServerRegister.kt:17-18` + `HttpEntitlementsApi.kt:20-21`.

### المرحلة 6 — الإغلاق
- ✅ 6أ حذف الحساب: `app/src/main/java/com/nota/notes/data/AccountDeletion.kt` (كامل — GRACE 14 يومًا + `CONFIRM_PHRASE_AR = "احذف حسابي"`) + `NotaCloudApi.kt:155-160 (DELETE /v1/account best-effort)`.
- ✅ 6ب الرفع المجزأ: `NotaCloudApi.kt:172-179 (PUT /v1/cloud/blobs/{sha}/chunks?offset=)`.
- ✅ 6ج بوابة trialUsed: `PremiumViewModel.kt:73,89-90` + `ServerRegister.kt:52-56`.
- ✅ 6د تشفير التصدير: `BackupManager.kt:179-181 (HEADER_BIN = "NOTABIN")` + `BackupPassword.kt` + حقل كلمة المرور في `BackupScreen.kt`.

---

## س4 — الأسئلة السلوكية (هل يفعل الكود ما يدّعيه التقرير؟)

| # | السؤال | الحكم + الدليل |
|---|--------|----------------|
| 4.1 | Trial أولًا ثم Pro السيرفر (مصدر واحد)؟ | ✅ نعم — `PremiumViewModel.kt:64-67 resolveSource: _trialActive → TRIAL أولًا، ثم pro السيرفر/الكاش` + `80: isProNow = trialActive \|\| pro` |
| 4.2 | حارس التزامن يرفض عملية ثانية؟ | ✅ نعم — `DriveBackup.kt:134: if (!uploadMutex.tryLock()) throw AlreadyRunning()` |
| 4.3 | تحذير النمو المفاجئ؟ | ✅ نعم جزئيًا — `CloudBackupViewModel.kt:206: lastCount >= 10 && noteCount*2 < lastCount` (يمنع الضجيج تحت 10) — عتبة "الضعف" موجودة حرفيًا |
| 4.4 | v4 id ثابت + sha256 + توافق v2/v3؟ | ✅ نعم — `BackupCodec.kt:70 (V=4)، 81 (id)، 117 (sha256)، 156-157 (الغائب يُمنح UUID)` |
| 4.5 | التعارض 3 خيارات + تطبيق على الكل؟ | ⚠️ يحتاج جهازًا — البنية (`BackupConflict` + شاشة التعارض) موجودة كودًا؛ التحقق السلوكي للخيارات الثلاثة يتطلب جهازًا |
| 4.6 | الاستعادة الجزئية بـ checkboxes؟ | ⚠️ يحتاج جهازًا — الكود (`preview` + `previewNotes` + تأكيد المعاينة) موجود؛ النقر الفعلي يتطلب جهازًا |
| 4.7 | تراجع 10 ثوانٍ بعد الاستعادة؟ | ✅ كودًا نعم — `restoreLogOnce + undoRestore (NotesRepository 449/456)`؛ المدة الدقيقة (10s) تُضبط في الواجهة — **يحتاج جهازًا** للتوقيت الفعلي |
| 4.8 | بطاقة الملخص + الشارات + الحذف بكلمة تأكيد؟ | ✅ نعم — `BackupSummaryCard (274)، chips (729/943)، DeleteAll بخطوتين + كلمة (1662-1663)` |
| 4.9 | الشراء يُبلَّغ ثم force / تأجيل عند الفشل؟ | ✅ نعم — `PremiumViewModel.purchase: reported → refresh(force) / else _purchaseDeferred=true (سطور 161-170 حرفيًا أعلاه)` |
| 4.10 | حذف الحساب best-effort لا يوقف المحلي؟ | ✅ نعم — `AccountDeletion.kt:6-8 (best-effort… ولا يتوقف على فشله)` |
| 4.11 | الرفع المجزأ برجوع صامت؟ | ✅ نعم ادعاءً كوديًا — `NotaCloudApi.kt:172 (قابل للاستئناف) + تعليق الرجوع الصامت`؛ السلوك الشبكي **يحتاج جهازًا/بيئة** |
| 4.12 | trialUsed السيرفرية تغلب المحلي؟ | ✅ نعم — `PremiumViewModel.kt:73 (تطبَّق عند القراءة) + 90 (تمنع التفعيل المحلي إن استُهلكت)` |
| 4.13 | تصدير مشفر NOTABIN؟ | ✅ نعم — `BackupManager.kt:181 HEADER_BIN = "NOTABIN"` + `BackupPassword.kt` |

---

## س5 — Greps (نواتج حرفية)

```
--- EntitlementSource ---
AccountScreen.kt:137: if (proSource == EntitlementSource.TRIAL) R.string.pro_source_trial
PremiumViewModel.kt:20: enum class EntitlementSource {
PremiumViewModel.kt:59-60: _source / source
PremiumViewModel.kt:64-67: resolveSource (TRIAL أولًا)
--- isProNow ---
NotaCloudViewModel.kt:108: private suspend fun isProNow()
NotaCloudViewModel.kt:167-168: Trial يفتح الكل + فحص isProNow
PremiumViewModel.kt:80: fun isProNow(): Boolean = trial || pro
--- trialUsed ---
ServerRegister.kt:56: TODO POST /v1/trial/start
Entitlements.kt:11: val trialUsed: Boolean = false
HttpEntitlementsApi.kt:79-81: قراءة trialUsed من JSON
PremiumViewModel.kt:73,89-90: التغليب والمنع
--- queryPurchases ---
BillingGateway.kt:17: suspend fun queryPurchases(): List<PurchaseInfo>
PremiumViewModel.kt:100: gateway.queryPurchases() عند الإقلاع
--- reportPurchase ---
ServerRegister.kt:17-18: TODO phase6-backend
BillingGateway.kt:20: suspend fun reportPurchase(...)
HttpEntitlementsApi.kt:21: TODO POST /v1/purchases
PremiumViewModel.kt:103,161: الإبلاغ عند الإقلاع وعند الشراء
--- NOTABIN ---
BackupManager.kt:179,181: الصيغة + HEADER_BIN
--- chunks ---
NotaCloudApi.kt:172,179: PUT .../chunks?offset=
--- v1/account ---
AccountDeletion.kt:6: ملاحظة backend
NotaCloudApi.kt:155,160: TODO + DELETE /v1/account
--- v1/trial ---
ServerRegister.kt:52,56: POST /v1/trial/start TODO
SettingsViewModel.kt:97: POST /v1/trial/start — TODO backend
--- v1/purchases ---
ServerRegister.kt:17: TODO POST /v1/purchases
BillingGateway.kt:19: يُنفَّذ مع POST /v1/purchases
HttpEntitlementsApi.kt:20: TODO POST /v1/purchases
```

---

## س6 — الاتساق (ادعاءات التقرير مقابل الواقع)

| ادعاء التقرير | الواقع | الحكم |
|---|---|---|
| المرحلة 1: 352 اختبارًا | XML الحالي: 96 إجمالي | ⚠️ غير متسق حرفيًا — غالبًا تراكمي/قديم؛ يحتاج توضيحًا |
| المراحل 2/4: 56 نتيجة | لا أثر لملف نتائج منفصل بهذا الرقم حاليًا | ⚠️ يحتاج توضيحًا (أين سجل الـ56؟) |
| المراحل 3/5/6: 60 اختبارًا | XML الحالي 96 (يشمل كل الـ15 ملفًا) | ⚠️ الأرقام المدّعاة لكل مرحلة لا تطابق لقطة اليوم — طبيعي إن كانت لقطات وقتها، لكن بلا أرشيف نتائج مرحلي |
| تجميع debug/release أخضر لكل مرحلة | **يحتاج جهازًا/بيئة** — لم يُشغَّل assemble (ممنوع بلا أمر)؛ لا سجل build حديث مرفق في ~24h الأخيرة بجانب XML | ⚠️ غير قابل للتحقق في هذه الجلسة |
| دمج نظيف phase3/phase4 في master | ✅ `c39fe3a merge phase3` + `e4a5363 merge phase4` في السجل | ✅ متسق |
| كل المراحل DONE كودًا | ✅ كل الملفات المذكورة موجودة (س3) | ✅ متسق كوديًا |
| backend TODOs موثقة | ✅ `ServerRegister/HttpEntitlementsApi/NotaCloudApi/AccountDeletion` كلها تحمل TODOs صريحة | ✅ متسق |
| قوائم جهازية بانتظار محمد | ✅ موجودة في التقرير الجامع لكل مرحلة | ✅ متسق — وتبقى **تحتاج جهازًا** |

---

## س7 — ما لم يُنفذ (خارج الكود / معلق)

1. **Backend** (موثق كـ TODOs — ليس كود تطبيق):
   - `POST /v1/purchases` + تحقق الإيصالات (5ج).
   - `DELETE /v1/account` (6أ — best-effort حاليًا).
   - `PUT /v1/cloud/blobs/{sha}/chunks` (6ب — رجوع صامت بدونه).
   - `POST /v1/trial/start` + `trialUsed` في الاستحقاق (6ج).
   - إعداد منتجات Play Console.
2. **اختبار محمد الجهازي** — كل القوائم في التقرير الجامع (شارة Trial، رفع الوسائط، التقدم الحقيقي، التعارضات، الاستعادة الجزئية، التراجع، الحذف الحقيقي، معاينة Cloud، بطاقة المفتاح، الشراء الحقيقي، التأجيل، الاستعادة التلقائية، الحذف التجريبي، الاستئناف، فك `.zip.enc`).
3. **الوسوم** — لا وسوم `nota-v*` رغم قاعدة الاستعادة (25 سبتمبر)؛ الموجود `v1.20.0` فقط.
4. **أرشيف نتائج مرحلي** — أرقام 352/56/60 بلا ملفات نتائج محفوظة قابلة لإعادة الفحص اليوم.

---

## س8 — التقييم الذاتي (ثقة المحقق)

- **منهجية:** `code-reviewer` (نطاق → أدلة حتمية → سياق → تقرير). كل حكم فوق مربوط بسطر/ملف حرفي.
- **الثقة العالية (كود موجود ومقروء):** 4.1، 4.2، 4.4، 4.9، 4.10، 4.12، 4.13، س3 (معظم البنود)، س5 — دليل مباشر.
- **الثقة المتوسطة (كود موجود، سلوك زمني/شبكي يحتاج بيئة):** 4.5، 4.6، 4.7، 4.11 + كل البنود الجهازية — البنية تثبت الوجود لا التجربة.
- **نقاط الضعف في هذا التحقق:** (أ) لم يُشغَّل gradle (ممنوع/ثقيل) — اعتمد XML بتاريخ 21 أكتوبر؛ (ب) سقف 15MB والاحتفاظ "آخر نسختين" لم يُعثر على نصهما الحرفي — يحتاجان توجيهًا لموضعهما الدقيق؛ (ج) أرقام الاختبارات المدّعاة (352/56/60) لا تطابق لقطة اليوم (96).
- **لم أُصلح شيئًا، لم أبنِ APK، لم أعمل commit** — التزامًا بالأمر.

---

## الإحصائية

- أقسام مكتملة: **8/9** (س1–س8 مكتملة؛ التاسع = هذه الإحصائية نفسها).
- بنود تحتاج توضيحًا: **5** (علامة `+` على feature/phase3؛ غياب وسوم nota-v*؛ أرقام 352/56/60 مقابل 96؛ موضع سقف 15MB الحرفي؛ الرقم الافتراضي للاحتفاظ K_CLOUD_KEEP).
- بنود غير قابلة للتحقق (تحتاج جهازًا/بيئة): **12** (كل القوائم الجهازية في التقرير الجامع + توقيت snackbar + سلوك chunks الشبكي + خيارات التعارض الثلاثة نقرًا + تجميع debug/release حي).

---

## ملاحظات بركات (22 أكتوبر 2026)

1. الشغل الكودي شكله جاد ومنظم — التسلسل في git واضح، والـ TODOs للباك-إند مكتوبة بصراحة مش متخبية، ودي أمانة تُحسب للشغل.
2. لكن أرقام الاختبارات المعلنة (352/56/60) لا تطابق لقطة اليوم (96 في XML) — الأرجح أنها لقطات وقتية أو تراكمية عبر flavors، لكن بلا أرشيف نتائج مرحلي يفضل عدم اعتماد أي رقم في قرار push.
3. غياب وسوم `nota-v*` مخالفة صريحة لقاعدة 25 سبتمبر — الاسترجاع الكامل حاليًا غير مضمون بالوسم، لازم يتقفل قبل أي push.
4. علامة `+` على `feature/phase3` معناها فيه شغل غير مدمج بالكامل أو فرع متقدم/متأخر — لازم يتوضح ويتقفل قبل الدمج النهائي.
5. سياسة الاحتفاظ وسقف الحجم (آخر نسختين؟ 1-7؟ 15MB؟) فيها غموض نصي — الكود يثبت أجزاءً لكن النص الحرفي للسقف والافتراضي محتاج تحديد دقيق من المنفذ.
6. البنود السلوكية (تعارضات، استعادة جزئية، تراجع، chunks شبكي) بنيتها موجودة كوديًا لكن إثباتها الحقيقي جهازي — ما أعتمدش "DONE" نهائي لها إلا بعد قوائم محمد الجهازية.
7. قراري: المراحل **ليست جاهزة لـ push** حتى: (أ) توضيح الأرقام أو أرشفة نتائج، (ب) وسم `nota-v*`، (ج) قفل `feature/phase3`، (د) قوائم الجهاز من محمد، (هـ) حسم نص الاحتفاظ والسقف.
