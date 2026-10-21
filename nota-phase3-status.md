# Nota — Phase 3 Status (سلامة بيانات النسخ)

المهارة المطبقة: `android-native-dev` (قراءة SKILL.md أولًا قبل التنفيذ — بناء/اختبار/تصليب).
الفرع: `feature/phase3` (من `master` عند `37f5b22` — بلا مساس بـ `master`، بلا push).
المواصفة: خطة الترقية الشاملة — قسم المرحلة 3 + القيود الصارمة.
قرارات محمد المطبقة: Format v4 مقبول + سياسة التعارض الافتراضية "احتفظ بالمحلي + سجّل".

## القرار العام
- [x] DONE — كودًا وتحققًا (3أ + 3ب + 3ج + 3د + إصلاحا ترجمة)

## ما نُفّذ
| Commit | Hash | الملفات | التفاصيل |
|---|---|---|---|
| `feat(backup): format v4 with UUID per note` | `5e3894d` | BackupCodec.kt, NotesRepository.kt, BackupCodecTest.kt | 3أ: `BackupNote.id` + `V=4` + ترميز v4 فقط + فك v2/v3/v4 + `sha256` في meta + `preview` ترجع version/sha + التصدير بـ id = نص Room ID |
| `feat(backup): conflict resolution with user choice` | `a9ab54a` | BackupCodec/NotesRepository/SettingsStore/VMs/BackupScreen/strings/Test | 3ب: `BackupConflict` + `sameBackupContent` + `noteJson/noteOf` + كشف التعارض في `restoreMerge` (احتفظ بالمحلي + سجّل) + `takeRemote/keepBoth/resolve*` + حفظ التعارضات في `RestoreRecord` (سقف 20) + بطاقة حل في سجل الاستعادة + 6 مفاتيح ar+en |
| `feat(backup): partial restore with selection` | `06d89c3` | NotesRepository/VMs/BackupScreen/strings | 3ج: `selectedIds` في `restoreMerge` + `previewNotes()` في Notes/Cloud VMs + `BackupNoteSelectionList` (checkbox + الكل/إلغاء + عدّاد) في حواري المعاينة (ملف + Drive) + 3 مفاتيح ar+en |
| `feat(backup): undo snackbar after restore` | `ef54fb6` | BackupScreen/strings | 3د: `SnackbarHost` في Scaffold + `onRestored(src, added)` من المسارات الثلاثة + رسالة `restore_undo_snackbar_fmt` بزر `restore_undo_action` لمدة Long (10s) + التراجع عبر `notesVm.undoRestore` + مفتاحان ar+en |
| `fix(backup): expose restoreLogOnce for undo snackbar` | `dcee53e` | NotesViewModel.kt | إصلاح خطأ ترجمة كشفته المحاولة 1 (`restoreLogOnce` غير موجود في الـ VM) |
| `fix(test): pass archivedAt/color in BackupCodecTest` | `67283fe` | BackupCodecTest.kt | إصلاح تحقق لاحق (خطة بخطة): الاختبار الجديد كان يبني `BackupNote` ناقصًا معاملين إجباريين — أُضيفا في 3 مواضع (تعديل اختبار فقط، بلا مساس بالإنتاج) |

## نتيجة الاختبارات والبناءات
- `./gradlew test` (عبر `scripts/nota_build.sh`: قفل حصري + Xmx مرن + worker واحد): **BUILD SUCCESSFUL** — 60 ملف نتائج عبر النسخ الأربع (MockDebug/Release + PlayDebug/Release)، صفر فشل وصفر أخطاء (منها 8 اختبارات `BackupCodecTest` الجديدة: v4/sha256/توافق v2/v3 + noteJson/noteOf/sameBackupContent)
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL**
- ملاحظات بيئية: المحاولات الأولى سقطت بعوائق خارج الكود (local.properties وgoogle-services.json غائبان في نسخة العمل — نُسخا من المشروع الأصلي؛ ثم خطأ الترجمة أعلاه — أُصلح). لا علاقة لها بمنطق المرحلة.
- `grep`: المفاتيح الجديدة الـ 11 موجودة في `values` (ar) و`values-en` (11/11 تطابق)

## ما ينتظر محمد (اختبار جهازي — مرتب بالأولوية)
1. [ ] تصدير نسخة v4 ثم استعادتها على نفس الجهاز → صفر تكرارات (نفس المحتوى يُتخطى عبر id).
2. [ ] استعادة نسخة v3 قديمة → تُدرج بلا كسر (تكرارات محتملة — سلوك موثق).
3. [ ] تعديل ملاحظة محليًا ثم استعادة نسخة تحوي النسخة القديمة منها → بطاقة "N تعارضات" في سجل الاستعادة → "احتفظ بالمحلية" / "خذ من النسخة" / "احتفظ بالاثنين" + "تطبيق على الكل" تعمل.
4. [ ] حوار المعاينة (ملف + Drive) يعرض checkboxes + "استعادة الكل"/"إلغاء الكل" + عدّاد → إلغاء الكل + اختيار واحدة → تُستعاد واحدة فقط.
5. [ ] بعد كل استعادة ناجحة (ملف/Drive/سحابة) → snackbar "تمت استعادة N ملاحظة — [تراجع]" 10 ثوانٍ → الزر ينقل للسلة؛ بعد الاختفاء يبقى زر التراجع في السجل.
6. [ ] استعادة سحابة نوتا (بلا معاينة بعد — P4) → تُستعاد الكل + snackbar يظهر.

## قرارات مأخوذة
1. **id النسخة = نص Room ID** (لا عمود UUID جديد في DB): يتجنب ترحيل Room (17→18) ومخاطره؛ الحقل النصي يقبل UUID مستقبلًا.
2. **v2/v3 بلا id تُدرج دائمًا** مع إبقاء فحص الثلاثي القديم كاحتياط.
3. **سقف 20 تعارضًا** في السجل — يمنع تضخم DataStore.
4. **بطاقة التعارضات expandable داخل سجل الاستعادة** بدل شاشة فرعية مستقلة.
5. **الاختيار الجزئي في معاينتي الملف وDrive فقط** — السحابة بلا معاينة حتى P4 (4د).
6. **رسالة Drive/Snackbar المتكررة بنفس العدد لا تُعرض مرتين** (مفتاح LaunchedEffect).

## اكتشافات غير متوقعة
1. **تلوث فرعي متقاطع (حُلّ):** الـ commit `3c6d214` على `feature/phase4` التقط 6 مفاتيح وسطرين من هذا الفرع عبر worktree مشترك — أزاله وكيل المرحلة 4 في `00d700e` (مؤكد في تقريرها). عند الدمج: يُدمج 3 أولًا ثم 4 فوقه، فأي بقايا تُكتشف في تعارض الدمج.
2. ملفات `nota-backup-audit.md` و`nota-entitlements-audit.md` غير متتبعة في الجذر — لم تُمس.

## انحرافات عن الخطة
- `BackupNote.id` = نص Room ID بدل UUID مولّد (مبرر في القرار 1).
- بطاقة التعارضات داخل السجل لا شاشة فرعية (مبرر في القرار 4).
- مفتاح إضافي `restore_deselect_all` + `conflict_detail_fmt` فوق القائمة (تتمة وظيفية صغيرة).

## ما ينتظر backend
- لا شيء — المرحلة 3 محلية بالكامل (لا endpoints).
