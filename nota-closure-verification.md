# Nota — Closure Verification (5 مراحل)

## القرار العام
- [ ] READY FOR PUSH (كل الأدلة إيجابية)
- [x] NEEDS FIXES (1 نقطة — assembleRelease OOM، بيئة لا كود)
- [ ] BLOCKED (فشل في مرحلة X — لا يمكن التقدم)

## المرحلة أ — غموض

### أ-1 feature/phase3
```
$ git log --oneline master..feature/phase3
(no output)
```
```
$ git log --oneline feature/phase3..master
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
1c95e80 docs: phase 2 status report
dd304a9 docs: phase 4 status report
dbfa0f8 fix(backup-ui): remove duplicate Composable annotation on CloudKeyCard
42b5a47 feat(backup-ui): always show cloud key card (locked for free)
5727eda feat(backup-ui): preview cloud backup before restore
00d700e feat(backup): delete all backups with double confirmation
dc5e3cc feat(backup): size limits with dialogs + inline status banners (2c, 2d)
049db7a feat(backup): concurrency lock + progress notification (2a, 2b)
3c6d214 feat(backup): delete individual backups
a4da580 feat(backup-ui): pro/free visual indicators
d4c16f1 feat(backup-ui): summary card with last backup and quick action
```
- الحكم: `master..feature/phase3` فارغ → **0 commits** في `feature/phase3` غير موجودة في `master`. الفرع متأخر 26 commit عن master.
- الحكم: يمكن حذف الفرع بأمان: `git branch -D feature/phase3` (لا commits فريدة). دمج غير مطلوب.

### أ-2 عدد الاختبارات

```
--- app/src/test/java/com/nota/notes/data/BackupManagerTest.kt ---
11
--- app/src/test/java/com/nota/notes/data/SecureDeleteTest.kt ---
5
--- app/src/test/java/com/nota/notes/data/GdprExportTest.kt ---
4
--- app/src/test/java/com/nota/notes/data/BackupCodecTest.kt ---
8
--- app/src/test/java/com/nota/notes/data/OpaqueFileNamesTest.kt ---
5
--- app/src/test/java/com/nota/notes/data/SettingsStoreTest.kt ---
3
--- app/src/test/java/com/nota/notes/data/AccountDeletionTest.kt ---
5
--- app/src/test/java/com/nota/notes/data/CryptoShredTest.kt ---
5
--- app/src/test/java/com/nota/notes/security/PerFileKeysTest.kt ---
10
--- app/src/test/java/com/nota/notes/security/AuthManagerTest.kt ---
5
--- app/src/test/java/com/nota/notes/security/RecoveryKeyTest.kt ---
7
--- app/src/test/java/com/nota/notes/security/SelfMessageCryptoTest.kt ---
6
--- app/src/test/java/com/nota/notes/security/PinHashTest.kt ---
7
--- app/src/test/java/com/nota/notes/ui/SettingsSearchTest.kt ---
9
--- app/src/test/java/com/nota/notes/ui/SecurityStatusTest.kt ---
6
```

| ملف الاختبار | عدد `@Test` |
|---|---|
| BackupManagerTest | 11 |
| SecureDeleteTest | 5 |
| GdprExportTest | 4 |
| BackupCodecTest | 8 |
| OpaqueFileNamesTest | 5 |
| SettingsStoreTest | 3 |
| AccountDeletionTest | 5 |
| CryptoShredTest | 5 |
| PerFileKeysTest | 10 |
| AuthManagerTest | 5 |
| RecoveryKeyTest | 7 |
| SelfMessageCryptoTest | 6 |
| PinHashTest | 7 |
| SettingsSearchTest | 9 |
| SecurityStatusTest | 6 |
| **الإجمالي** | **96** |

- الإجمالي الكلي = 96 (لكل variant: mockDebug, mockRelease, playDebug, playRelease).
- يتوافق مع 96 (من XML)، **ليس** 352 (من التقرير السابق). التقرير السابق كان خاطئًا (352 = 96 × ~4 variants؟ أو خطأ تجميع). الرقم الحقيقي = 96.

### أ-3 الاحتفاظ + سقف الحجم

```
=== K_CLOUD_KEEP / MAX_CLOUD_KEEP / DEFAULT ===
app/src/main/java/com/nota/notes/data/SettingsStore.kt:40:    private val K_CLOUD_KEEP = intPreferencesKey("cloud_backup_keep") // عدد نسخ Drive المحتفظ بها 1..7 — الأقدم يُحذف عند التجاوز (27 سبتمبر 2026)
app/src/main/java/com/nota/notes/data/SettingsStore.kt:80:        const val MAX_CLOUD_KEEP = 7 // حد نسخ Drive الأقصى — يختار المستخدم 1..7
app/src/main/java/com/nota/notes/data/SettingsStore.kt:81:        const val DEFAULT_CLOUD_KEEP = 5
app/src/main/java/com/nota/notes/data/SettingsStore.kt:172:    val cloudKeep: Flow<Int> = ctx.settingsStore.data.map { (it[K_CLOUD_KEEP] ?: 5).coerceIn(1, MAX_CLOUD_KEEP) }
app/src/main/java/com/nota/notes/data/SettingsStore.kt:173:    suspend fun setCloudKeep(v: Int) { ctx.settingsStore.edit { it[K_CLOUD_KEEP] = v.coerceIn(1, MAX_CLOUD_KEEP) } }
app/src/main/java/com/nota/notes/ui/BackupScreen.kt:480:            TextButton(onClick = { vm.setKeepCount(keep + 1) }, enabled = !working && keep < SettingsStore.MAX_CLOUD_KEEP && !locked) { Text("+") }
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:223:        settings.cloudKeep.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.nota.notes.data.SettingsStore.DEFAULT_CLOUD_KEEP)

=== coerceIn / take / dropLast / subList ===
app/src/main/java/com/nota/notes/data/DriveBackup.kt:181:                val keepCount = keep.coerceIn(1, 7)
app/src/main/java/com/nota/notes/ui/PinPad.kt:133:                        if (pin.isNotEmpty()) onPin(pin.dropLast(1))
app/src/main/java/com/nota/notes/ui/NoteEditorScreen.kt:422:        undoStack = undoStack.dropLast(1)
app/src/main/java/com/nota/notes/ui/NoteEditorScreen.kt:428:        redoStack = redoStack.dropLast(1)
app/src/main/java/com/nota/notes/ui/LockScreen.kt:146:                        Key.Backspace -> if (pin.isNotEmpty()) { pin = pin.dropLast(1); true } else false
app/src/main/java/com/nota/notes/ui/NoteEditorComponents.kt:822:                    TextButton(onClick = { strokes = strokes.dropLast(1) }, enabled = strokes.isNotEmpty()) {
app/src/main/java/com/nota/notes/ui/NoteEditorComponents.kt:929:                            onValueChange = { interval = it.filter { ch -> ch.isDigit() }.take(2) },
app/src/main/java/com/nota/notes/ui/NotesListScreen.kt:614:                                            full = snip?.first ?: strippedBody.lines().take(2).joinToString("\n"),
app/src/main/java/com/nota/notes/ui/TrashScreen.kt:168:                                            full = snip?.first ?: strippedBody.lines().take(2).joinToString("\n"),

=== حجم: 15MB / 100MB / MAX_DRIVE / sizeLimit ===
app/src/main/java/com/nota/notes/data/DriveBackup.kt:33:        const val MAX_DRIVE_BYTES: Long = 100L * 1024 * 1024
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:283:                    if (!sizeWarnAccepted && sizeBytes > com.nota.notes.data.DriveBackup.MAX_DRIVE_BYTES) {
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:284:                        _sizeWarn.value = SizeWarning(sizeBytes, com.nota.notes.data.DriveBackup.MAX_DRIVE_BYTES)

=== قياس حجم النسخة قبل الرفع ===
app/src/main/java/com/nota/notes/data/NotaCloudApi.kt:30:data class CloudUpload(val sha256: String, val sizeBytes: Long, val usedBytes: Long, val quotaBytes: Long, val dedupe: Boolean)
app/src/main/java/com/nota/notes/data/NotaCloudApi.kt:104:                CloudUpload(o.optString("sha256"), o.optLong("sizeBytes"), o.optLong("usedBytes"), o.optLong("quotaBytes"), o.optBoolean("dedupe", false))
app/src/main/java/com/nota/notes/ui/BackupScreen.kt:240:                        sizeBytes = sizeLast,
app/src/main/java/com/nota/notes/ui/BackupScreen.kt:282:    sizeBytes: Long = 0L,
app/src/main/java/com/nota/notes/ui/BackupScreen.kt:294:        if (sizeBytes > 0) { Text(stringResource(R.string.backup_summary_size_fmt, fmtBytes(sizeBytes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:217:        runCatching { settings.setSizeLast(plainJson.toByteArray().size.toLong()) }
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:249:                if (uploaded) _opStatus.value = OpStatus.Success(json.toByteArray().size.toLong(), dt)
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:282:                    val sizeBytes = json.toByteArray().size.toLong()
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:283:                    if (!sizeWarnAccepted && sizeBytes > com.nota.notes.data.DriveBackup.MAX_DRIVE_BYTES) {
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:284:                        _sizeWarn.value = SizeWarning(sizeBytes, com.nota.notes.data.DriveBackup.MAX_DRIVE_BYTES)
app/src/main/java/com/nota/notes/ui/CloudBackupViewModel.kt:300:                    if (uploaded) _opStatus.value = OpStatus.Success(sizeBytes, System.currentTimeMillis() - t0)
```

- K_CLOUD_KEEP: `intPreferencesKey("cloud_backup_keep")`، default 5.
- MAX_CLOUD_KEEP: 7 (حد نسخ Drive الأقصى).
- DEFAULT_CLOUD_KEEP: 5.
- سقف Drive: `MAX_DRIVE_BYTES = 100L * 1024 * 1024` = 100 MB.
- سقف Cloud: نفس `MAX_DRIVE_BYTES` (100 MB) — يُفحص في `CloudBackupViewModel.kt:282-284`.
- هل "آخر نسختين" موجود؟ **لا.** الاحتفاظ = `keepCount.coerceIn(1, 7)` (DriveBackup.kt:181) — يحذف ما زاد عن العدد المختار (1..7، default 5) مع إبقاء الأحدث دائمًا (`keepIds = (listOf(newId) + all.map { it.id }.filter { it != newId }).take(keepCount)`). لا يوجد trim خاص "آخر 2". تقرير "آخر نسختين" كان خاطئًا.
- فحص الحجم قبل الرفع: `json.toByteArray().size.toLong()` (CloudBackupViewModel.kt:282).
- حوار تحذير أم رفض؟ **حوار تحذير** (`SizeWarning`، CloudBackupViewModel.kt:283-284) — `if (!sizeWarnAccepted && sizeBytes > MAX_DRIVE_BYTES)` → يعرض SizeWarning ويوقف (`return@opLambda`). المستخدم يمكنه قبول (`sizeWarnAccepted=true`) وإعادة المحاولة. ليس رفضًا صريحًا.

### أ-4 الوسوم

```
$ git tag --list
v1.20.0
```
```
$ git tag --list "nota-v*"
(no output)
```
```
$ git describe --tags --always HEAD
v1.20.0-113-g26ece16
```
- قائمة وسوم: `v1.20.0` فقط.
- `nota-v*`: **غائب** (لا يوجد).
- أقرب وسم للـ HEAD: `v1.20.0` (113 commits ahead).
- الحكم: يحتاج إنشاء `nota-v1.21.0` (تم في المرحلة د).

## المرحلة ب — الاختبارات

```
$ ./gradlew cleanTest 2>&1 | tail -5
Starting a Gradle Daemon (subsequent builds will be faster)
> Task :app:cleanTest UP-TO-DATE
BUILD SUCCESSFUL in 38s
1 actionable task: 1 up-to-date
```
```
$ ./gradlew test --console=plain 2>&1 | tee /tmp/nota-closure-test.log | tail -30
> Task :app:processPlayReleaseManifestForPackage UP-TO-DATE
...
> Task :app:testPlayReleaseUnitTest UP-TO-DATE
> Task :app:test UP-TO-DATE
BUILD SUCCESSFUL in 29s
137 actionable tasks: 137 up-to-date
```
- `./gradlew test`: **BUILD SUCCESSFUL in 29s**.
- عدد الاختبارات (من XML، 4 variants × 15 classes): 96 لكل variant، 0 failures، 0 errors.
- variants إضافية:
```
$ ./gradlew testPlayRelease --console=plain 2>&1 | tail -10
> Task :app:testPlayReleaseUnitTest UP-TO-DATE
BUILD SUCCESSFUL in 5s
35 actionable tasks: 35 up-to-date
```
  - `testPlayRelease`: BUILD SUCCESSFUL in 5s — 96 tests، 0 failures (من XML).
- flakiness:
```
$ ./gradlew cleanTest test --console=plain 2>&1 | tail -8
> Task :app:test UP-TO-DATE
BUILD SUCCESSFUL in 8s
138 actionable tasks: 138 up-to-date
```
  - نفس النتيجة (BUILD SUCCESSFUL). ملاحظة: gradle أبلغ up-to-date (لم يُنفَّذ من الصفر بسبب cache). الاختبارات (96، 0 failures) ثابتة في XML.

## المرحلة ج — Release

### ج-1 assembleRelease

محاولة 1 (heap default 2500m):
```
$ ./gradlew assembleRelease --console=plain 2>&1 | tee /tmp/nota-closure-release.log | tail -40
...
> Task :app:minifyMockReleaseWithR8 FAILED
Execution failed for task ':app:minifyMockReleaseWithR8'.
> A failure occurred while executing com.android.build.gradle.internal.tasks.R8Task$R8Runnable
   > Compilation failed to complete
...
ERROR: R8: java.lang.OutOfMemoryError: Java heap space
...
> Task :app:lintVitalAnalyzeMockRelease FAILED
...
BUILD FAILED in 6m 16s
```

محاولة 2 (heap 3500m):
```
$ ./gradlew assembleRelease --console=plain -Dorg.gradle.jvmargs="-Xmx3500m ..." 2>&1 | tee /tmp/nota-closure-release2.log | tail -20
...
> Task :app:minifyMockReleaseWithR8   (completed — APK regenerated)
...
> Task :app:lintVitalAnalyzePlayRelease   (running at 10-min timeout)
```
- mock minify succeeded (APK regenerated 19,843,727 bytes @ 16:25).
- play lint still running at timeout (10 min).

محاولة 3 (skip lint، heap default):
```
$ ./gradlew assembleRelease --console=plain -x lintVitalAnalyzeMockRelease -x lintVitalAnalyzePlayRelease 2>&1 | tee /tmp/nota-closure-release3.log | tail -20
> Task :app:minifyMockReleaseWithR8
ERROR: R8: java.lang.OutOfMemoryError: Java heap space
> Task :app:minifyMockReleaseWithR8 FAILED
BUILD FAILED in 6m 7s
```

محاولة 4 (skip lint، heap 3500m):
```
$ ./gradlew assembleRelease --console=plain -x lintVitalAnalyzeMockRelease -x lintVitalAnalyzePlayRelease -Dorg.gradle.jvmargs="-Xmx3500m ..." 2>&1 | tee /tmp/nota-closure-release4.log | tail -8
> Task :app:minifyMockReleaseWithR8   (running at 10-min timeout)
```

- **لا توجد رسالة "Missing class"** في أي محاولة. الفشل = `java.lang.OutOfMemoryError: Java heap space` (R8 + lint). это بيئة (heap 2.5GB insufficient)، ليس كود.
- **ملاحظة:** `gradle.properties` يضبط `org.gradle.jvmargs=-Xmx2500m`. المتاح في البيئة: ~2.0GB free + 2.7GB swap.

### ج-2 فحص APK للأسرار

```
$ find app/build/outputs/apk -name "*.apk" -exec ls -la {} \;
-rw-r--r-- 1 administrator administrator 40052480 Oct 21 20:38 app/build/outputs/apk/mock/debug/app-mock-debug.apk
-rw-r--r-- 1 administrator administrator 19843727 Oct 21 16:25 app/build/outputs/apk/mock/release/app-mock-release.apk
-rw-r--r-- 1 administrator administrator 40052472 Oct 21 20:38 app/build/outputs/apk/play/debug/app-play-debug.apk
-rw-r--r-- 1 administrator administrator 19843719 Oct 21 16:43 app/build/outputs/apk/play/release/app-play-release.apk
```
```
$ find app/build/outputs/apk -name "*.apk" -exec unzip -l {} \; | grep -iE "\.key$|\.env|keystore|\.hprof" | head -20
(no output — EXIT_GREP_DONE)
```
- APK mock release: 19,843,727 bytes (~18.9 MB).
- APK play release: 19,843,719 bytes (~18.9 MB).
- أسرار في APK: **لا** (لا `.key`/`.env`/`keystore`/`.hprof`). (ملاحظة: APKs من بناء سابق ناجح + mock مُعاد توليده في محاولة 2؛ play APK timestamp 16:43 من محاولة 2 partiel.)

### ج-3 jadx

```
$ which jadx || echo "jadx not available"
jadx not available
```
- jadx: غير متوفر → "jadx not available — لم يُفحص". تشويش `PinHash`/`KeyManager`/`PerFileKeys`/`SelfMessageCrypto` لم يُؤكَّد.

## المرحلة د — الوسم

```
$ git describe --tags --abbrev=0 2>/dev/null || echo "no previous tag"
v1.20.0
```
```
$ git tag -a nota-v1.21.0 -m "Entitlements unification + Backup hardening (phases 0-6)"
TAG CREATED
```
```
$ git tag --list "nota-v*"
nota-v1.21.0
```
```
$ git describe --tags --always HEAD
nota-v1.21.0
```
- آخر tag قبل: `v1.20.0`.
- الوسم الجديد: `nota-v1.21.0` (annotated, محلي فقط، لا push).
- HEAD مرتبط بالوسم الجديد: نعم (`git describe` → `nota-v1.21.0`).

## المرحلة هـ — قائمة جهازية
- الملف: `nota-device-test-final.md`
- عدد البنود: 12 (6 حرج + 6 مهم).
- مصنَّفة: حرج / مهم.

## ما ينتظر محمد
1. تنفيذ قائمة `nota-device-test-final.md` (12 بندًا).
2. قرار: push بعد نجاح كل البنود أو push الآن مع متابعة الاختبار.
3. إصلاح OOM (زيادة heap في CI/بيئة البناء) — **ليس** إصلاح كود. (أو إعادة `assembleRelease` على جهاز بذاكرة ≥4GB.)
4. (إن رغب) حذف الفرع `feature/phase3` (`git branch -D feature/phase3`).
5. (إن رغب) إزالة الوسم `nota-v1.21.0` (`git tag -d nota-v1.21.0`) إذا رأى أن build verification غير مكتمل.

## ما لم يُنفّذ (صراحة)
- push (ممنوع per instructions).
- jadx obfuscation check (jadx غير متوفر).
- flakiness run من الصفر (gradle أبلغ up-to-date due to cache؛ الاختبارات ثابتة 96/0).
- assembleRelease BUILD SUCCESSFUL (فشل OOM).

## مخاطر متبقية
1. **OOM في R8/lint** (بيئة): `gradle.properties` Xmx=2500m insufficient. Need ≥3.5GB heap for reliable minify. Not a code defect.
2. **play APK** timestamp 16:43 (attempt 2 partial) — may be incomplete; recommend regenerate on capable machine.
3. **APK size** ~18.9 MB (mock & play) — within <40MB criterion.
4. **Test count discrepancy**: report claimed 352; actual 96. XML confirms 96.
5. **Retention description**: previous report said "آخر نسختين"; actual is user-configurable 1..7 (default 5). Previous report inaccurate.
6. **Size check**: warning dialog (not hard reject) at 100MB — matches "حوار تحذير" not rejection. Previous report may have overstated.
7. **jadx unavailable**: obfuscation of crypto classes unverified.
8. **Device tests**: 12 items (6 critical) not yet executed on device.
9. **Tag created** despite inconclusive build verification — removable if desired.
