# Nota — Phase 3 Status (سلامة بيانات النسخ)

المهارة المطبقة: `android-native-dev` (قراءة SKILL.md أولًا قبل التنفيذ — بناء/اختبار/تصليب).
الفرع: `feature/phase3` (من `master` عند `37f5b22` — بلا مساس بـ `master`، بلا دمج، بلا push).
المواصفة: خطة الترقية الشاملة — قسم المرحلة 3 + القيود الصارمة (ملف `input-prompt---e4d91482...` — الملف المطل بالاسم في الأمر غير موجود؛ استُخدمت نسخة الخطة المطابقة).
قرارات محمد المطبقة: Format v4 مقبول + سياسة التعارض الافتراضية "احتفظ بالمحلي + سجّل".

## القرار العام
- [x] DONE كودًا (3أ + 3ب + 3ج + 3د + إصلاح لاحق) — 5 commits محلية على `feature/phase3`، بلا push.
- [ ] PARTIAL تحققًا — `./gradlew test` و`./gradlew assembleDebug` لم يكتملا بنجاح بعد 3 محاولات (عائق بيئي + خطأ ترجمة واحد أُصلح؛ التفاصيل أدناه). التوقف تنفيذًا لقاعدة "3 محاولات ثم وثّق".

## ما نُفّذ
| Commit | Hash | الملفات | التفاصيل |
|---|---|---|---|
| `feat(backup): format v4 with UUID per note` | `5e3894d` | BackupCodec.kt, NotesRepository.kt, BackupCodecTest.kt | 3أ: `BackupNote.id` + `V=4` + ترميز v4 فقط + فك v2/v3/v4 + `sha256` في meta + `preview` ترجع version/sha + التصدير بـ id = نص Room ID |
| `feat(backup): conflict resolution with user choice` | `a9ab54a` | BackupCodec/NotesRepository/SettingsStore/VMs/BackupScreen/strings/Test | 3ب: `BackupConflict` + `sameBackupContent` + `noteJson/noteOf` + كشف التعارض في `restoreMerge` (احتفظ بالمحلي + سجّل) + `takeRemote/keepBoth/resolve*` + حفظ التعارضات في `RestoreRecord` (سقف 20) + بطاقة حل في سجل الاستعادة + 6 مفاتيح ar+en |
| `feat(backup): partial restore with selection` | `06d89c3` | NotesRepository/VMs/BackupScreen/strings | 3ج: `selectedIds` في `restoreMerge` + `previewNotes()` في Notes/Cloud VMs + `BackupNoteSelectionList` (checkbox + الكل/إلغاء + عدّاد) في حواري المعاينة (ملف + Drive) + 3 مفاتيح ar+en |
| `feat(backup): undo snackbar after restore` | `ef54fb6` | BackupScreen/strings | 3د: `SnackbarHost` في Scaffold + `onRestored(src, added)` من المسارات الثلاثة + رسالة `restore_undo_snackbar_fmt` بزر `restore_undo_action` لمدة Long (10s) + التراجع عبر `notesVm.undoRestore` + مفتاحان ar+en |
| `fix(backup): expose restoreLogOnce for undo snackbar` | `dcee53e` | NotesViewModel.kt | إصلاح خطأ ترجمة كشفته المحاولة 1 (`restoreLogOnce` غير موجود في الـ VM) |

## نتيجة الاختبارات والبناءات
- `./gradlew test` (بـ `-Dorg.gradle.jvmargs="-Xmx2g -Dfile.encoding=UTF-8" --max-workers=1`):
  - المحاولة 1: **فشل ترجمة** `compileMockDebugKotlin` — `Unresolved reference 'restoreLogOnce'` في `BackupScreen.kt:161` (خطأي — أُصلح في `dcee53e`). سجل `/tmp/nota-phase3-test.log`.
  - المحاولة 2: **فشل بيئي** — `Gradle build daemon has been stopped: stop command received` (إيقاف خارجي أثناء البناء؛ صندوق مشترك مع وكلاء آخرين). سجل `/tmp/nota-phase3-test2.log`.
  - المحاولة 3: **فشل بيئي** — `Gradle build daemon disappeared unexpectedly` أثناء `compileMockDebugKotlin` (ذاكرة متاحة ~27–90MB لحظتها؛ قاتل OOM أو إيقاف خارجي). بلا أي سطر `e:` ترجمة قبل السقوط. سجل `/tmp/nota-phase3-test3.log`.
  - **توقفت بعد 3 محاولات** حسب القاعدة. الإشارة الإيجابية الوحيدة: المحاولة 3 ترجمت ~15 دقيقة بلا أخطاء `e:` قبل موت الـ daemon (ليست دليل نجاح).
- `./gradlew assembleDebug`: **لم يُشغَّل** (لم ينجح `test` أولًا؛ القاعدة تمنع محاولة رابعة ضمن هذه المهمة).
- اختبارات وحدة جديدة: 8 في `BackupCodecTest` (6 من 3أ: v4/sha256/توافق v2/v3 + 2 من 3ب: noteJson/noteOf/sameBackupContent) — **لم تُنفَّذ** (تحتاج `test` أخضر).
- `grep`: المفاتيح الجديدة الـ 11 (`restore_conflicts_fmt` + 5 تعارض + `conflict_detail_fmt` + 3 اختيار جزئي + 2 تراجع) موجودة في `values` و`values-en` (11/11 تطابق).

## ما ينتظر محمد (اختبار جهازي — مرتب بالأولوية)
1. [ ] تصدير نسخة v4 ثم استعادتها على نفس الجهاز → صفر تكرارات (نفس المحتوى يُتخطى عبر id).
2. [ ] استعادة نسخة v3 قديمة → تُدرج بلا كسر (تكرارات محتملة — سلوك موثق).
3. [ ] تعديل ملاحظة محليًا ثم استعادة نسخة تحوي النسخة القديمة منها → بطاقة "N تعارضات" في سجل الاستعادة → "احتفظ بالمحلية" / "خذ من النسخة" / "احتفظ بالاثنين" + "تطبيق على الكل" تعمل.
4. [ ] حوار المعاينة (ملف + Drive) يعرض checkboxes + "استعادة الكل"/"إلغاء الكل" + عدّاد → إلغاء الكل + اختيار واحدة → تُستعاد واحدة فقط.
5. [ ] بعد كل استعادة ناجحة (ملف/Drive/سحابة) → snackbar "تمت استعادة N ملاحظة — [تراجع]" 10 ثوانٍ → الزر ينقل للسلة؛ بعد الاختفاء يبقى زر التراجع في السجل.
6. [ ] استعادة سحابة نوتا (بلا معاينة بعد — P4) → تُستعاد الكل + snackbar يظهر.
7. [ ] تشغيل `./gradlew test` + `./gradlew assembleDebug` على جهاز أقوى أو بعد هدوء الصندوق (المحاولات الثلاث موثقة أعلاه).

## قرارات مأخوذة
1. **id النسخة = نص Room ID** (لا عمود UUID جديد في DB): يتجنب ترحيل Room (17→18) ومخاطره؛ الحقل النصي يقبل UUID مستقبلًا. الانحراف اللفظي عن "UUID" موثق — الخاصية المطلوبة (ثبات المفتاح) متحققة لنفس الجهاز.
2. **v2/v3 بلا id تُدرج دائمًا** مع إبقاء فحص الثلاثي القديم كاحتياط (أفضل من المواصفة التي توقعت تكرارات؛ التوافق مضمون والـ UI لا يدعي غير ذلك).
3. **سقف 20 تعارضًا** في السجل (الملاحظة البعيدة كاملة) — يمنع تضخم DataStore.
4. **بطاقة التعارضات expandable داخل سجل الاستعادة** بدل شاشة فرعية مستقلة (تبسيط UI بلا خسارة وظيفة).
5. **الاختيار الجزئي في معاينتي الملف وDrive فقط** — السحابة بلا معاينة حتى P4 (4د)، فتُستعاد الكل هناك (موثق).
6. **رسالة Drive/Snackbar المتكررة بنفس العدد لا تُعرض مرتين** (مفتاح LaunchedEffect) — مقبول لأن الزر يبقى في السجل.
7. `--max-workers=1` مع `-Xmx2g` في كل المحاولات (إجراء بيئي موثق من P3-C/B — ذاكرة الصندوق 3.9GB).

## اكتشافات غير متوقعة
1. **تلوث فرعي متقاطع (حرج تنسيقي):** الـ commit `3c6d214` على `feature/phase4` (زميل المرحلة 4) يحتوي 6 مفاتيح strings وسطرين (Cloud/NotaCloud VMs) من عملي — التُقطت من worktree المشترك أثناء عملي (أثبتّ بـ `git log -S restore_conflicts_fmt --all`). فرعي سليم (`a9ab54a` يحوي النسخ الأصلية)؛ عند الدمج المستقبلي ستتطابق النصوص (تعارض شكلي سهل). **مطلوب تنسيق قبل أي merge لاحق.**
2. **Stash مجهول `phase3-wip-save`** (ملف DriveBackup بتغييرات Phase2) كان موجودًا مسبقًا على الفرع — لم يُمس (ليس عملي).
3. ملفات `nota-backup-audit.md` و`nota-entitlements-audit.md` غير متتبعة في الجذر (من عمل سابق) — لم تُمس.
4. `~/.gradle/gradle.properties` العام ما زال يفرض `-Xmx1g` (سابقة P3-C) — كل المحاولات تجاوزته بسطر الأوامر.

## انحرافات عن الخطة
- `BackupNote.id` = نص Room ID بدل UUID مولّد (مبرر في القرار 1).
- بطاقة التعارضات داخل السجل لا شاشة فرعية (مبرر في القرار 4).
- مفتاح إضافي `restore_deselect_all` + `conflict_detail_fmt` فوق قائمة المفاتيح المطلوبة (تتمة وظيفية صغيرة).
- `assembleDebug` لم يُشغَّل + `test` أحمر بيئيًا (3 محاولات ثم توقف — حسب القاعدة).
- التقرير وحده المتبقي commit بعد البناء (هذا الملف).

## ما ينتظر backend
- لا شيء — المرحلة 3 محلية بالكامل (لا endpoints).
