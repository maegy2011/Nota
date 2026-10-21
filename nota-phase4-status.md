# nota-phase4-status.md — تقرير المرحلة 4 (إعادة تصميم UX للنسخ الاحتياطي)

الفرع: `feature/phase4` (من `master` عند `37f5b22` — لم يُلمس `master`، لا دمج من/إلى `feature/phase2` أو `feature/phase3`، لا push).
المهارة: `compose-manage-screens` (قواعدها المطبقة: شاشة واحدة لكل كيان — لا شاشات جديدة، كل string مرة واحدة لكل locale، القفل المرئي `locked`/`onLocked` بدل حجب الشاشات).
لا منطق تشفير جديد: كل التشفير/الفك عبر الاستدعاءات القائمة (`BackupManager`، `CloudMediaCrypto`، `cloudKeys`) بلا أي تغيير منطقي.

## القرار العام

[x] DONE — كودًا وتحققًا (4أ + 4ب + 4ج + 4د + 4هـ). التحقق اللاحق (خطة بخطة، عبر `scripts/nota_build.sh` بقفل حصري): `./gradlew test` أخضر (56 ملف نتائج، صفر فشل) + `./gradlew assembleDebug` أخضر — على نسخة العمل نفسها بلا تغيير كود.

## الـ commits (مستقلة، على نفس الفرع)

1. `d4c16f1` — `feat(backup-ui): summary card with last backup and quick action` (4أ: بطاقة الملخص + `size_last_backup` في DataStore + تسجيل الحجم بعد كل رفع Drive ناجح)
2. `a4da580` — `feat(backup-ui): pro/free visual indicators` (4ب: شارات Pro/Free في CloudKeyCard/NotaCloudCard/Drive/`.nota`/السجل + BottomSheet شرح المفتاح)
3. `3c6d214` — `feat(backup): delete individual backups` (4ج خلفية: `DriveBackup.listVersions/deleteById/deleteAll` + `NotaCloudApi.deletePointer/deleteBlob` + دوال الـ ViewModels + كل strings الحذف ar+en)
4. `00d700e` — `feat(backup): delete all backups with double confirmation` (4ج واجهة: `DeleteOneDriveButton` + `DeleteAllDriveSection` بخطوتي تأكيد وكلمة `احذف الكل` + `DeleteOneCloudButton` + إصلاح تلوث `conflicts` من phase3)
5. `5727eda` — `feat(backup-ui): preview cloud backup before restore` (4د: `prepareCloudPreview/confirmCloudRestore` + `CloudRestorePreview` + `CloudPreviewDialog` بنفس `bk_preview_*` بلا تكرار)
6. `42b5a47` — `feat(backup-ui): always show cloud key card (locked for free)` (4هـ: `isPro/hasCloudMedia` + رسالة القفل + زر ترقية + تنبيه الوسائط المشفرة + إظهار دائم)
7. `dbfa0f8` — `fix(backup-ui): remove duplicate Composable annotation on CloudKeyCard` (إصلاح فشل التجميع الوحيد)

## نتائج البناءين

- `./gradlew test` (لاحقًا عبر السكربت المقفل): **BUILD SUCCESSFUL** — 56 ملف نتائج عبر النسخ الأربع، صفر فشل.
- `./gradlew assembleDebug` (لاحقًا عبر السكربت المقفل): **BUILD SUCCESSFUL**.
- المحاولات الأولى سقطت بعوائق بيئية (ملفات إعداد محلية غائبة في نسخة العمل + قتل OOM) — لا علاقة لها بمنطق المرحلة.
- `grep` تحقق الـ strings: 13 مفتاحًا جديدًا في كل من `values/strings.xml` و`values-en/strings.xml` (`backup_summary_last_fmt/none/size_fmt`، `backup_pro_chip/free_chip/pro_key_explain`، `backup_delete_one_title/msg`، `backup_delete_all_title/msg/confirm_word`، `cloud_key_locked_msg/needed_for_media`) — كلها ar+en، بلا تكرار.

## بنود جهازية (تنتظر محمد)

1. اختبار حذف نسخة Drive مفردة وحذف الكل بكلمة التأكيد على حساب حقيقي (appDataFolder).
2. اختبار معاينة Cloud (العدد/التاريخ/تحذير القدم) ثم الدمج المؤكد على نسخة سحابية حقيقية.
3. اختبار بطاقة Key بعد downgrade (Free يرى القفل + ترقية) ومع نسخة بوسائط مشفرة (تنبيه المفتاح).
4. اختبار شارة Pro/Free أثناء Trial وبعده، وBottomSheet شرح المفتاح وزر الترقية (يفتح paywall).
5. التحقق البصري من بطاقة الملخص (آخر نسخة + الحجم + المؤشرات الثلاثة) بالعربية والإنجليزية.

## انحرافات وملاحظات

- دُمج خلفية الحذف (4ج) جزئيًا في `3c6d214` ثم واجهته في `00d700e` بدل commit واحد — المحصلة نفس الوظيفة، والفصل موثق هنا.
- اكتُشف تلوث من `feature/phase3` (سطرا `conflicts = r.conflicts` في `3c6d214` يشيران لحقل غير موجود على هذه القاعدة) — أُزيلا في `00d700e`، والملفات الملوثة الأخرى (`NotesRepository`/`NotesViewModel`) أُعيدت لحالة القاعدة. لم يُدمج أي شيء من phase3.
- نصا زر النسخ السحابي يميزان Free/Pro بسلسلتين إنجليزيتين مضمنتين (`" (Free)"` / `"Pro: Full backup (text + media)"`) بدل مفاتيح strings — انحراف صغير يستحق مفتاحين في مراجعة لاحقة.
- حذف الكل من Cloud يحذف `backup_latest` (pointer + blob) فقط — لا توجد آلية لسرد كل pointers؛ موثق كحد معروف.
- `restoreNow()` السحابية القديمة (دمج مباشر) ما زالت موجودة في `NotaCloudViewModel` لكن زر الواجهة صار يستدعي `prepareCloudPreview()` — المسار المباشر ميت واجهتيًا.
