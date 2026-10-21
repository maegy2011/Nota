# Nota — Phase 6 Status

## القرار العام
- [x] DONE — كودًا وتحققًا (6أ + 6ب + 6ج + 6د)

## ما نُفّذ
| Commit | الملفات | التفاصيل |
|---|---|---|
| `feat(account): best-effort cloud delete on account wipe with backend TODO` | NotaCloudApi.kt, DeletionWorker.kt, AccountDeletion.kt | 6أ: `deleteAccount()` (DELETE /v1/account + TODO) — يُستدعى أولًا عند التنفيذ النهائي ثم المحلي؛ الفشل يُسجَّل ولا يوقف شيئًا |
| `feat(trial): server-side trialUsed gate with fail-open local trial` | Entitlements.kt, HttpEntitlementsApi.kt, ServerRegister.kt, PremiumViewModel.kt, SettingsViewModel.kt | 6ج: `trialUsed` في الاستحقاق + parser؛ السيرفر يغلب المحلي (fail-closed للتجربة فقط)؛ `reportTrialStart(accountId)` stub + TODO؛ الإبلاغ best-effort والفشل لا يمنع التجربة |
| `feat(cloud): resumable chunked upload with silent full-upload fallback` | NotaCloudApi.kt, SettingsStore.kt, NotaCloudViewModel.kt | 6ب: `uploadChunk()` (PUT chunks?offset= + TODO)؛ حالة الاستئناف في DataStore (sha+offset+عدم-دعم)؛ الرجوع الصامت للكامل بلا endpoint؛ الملفات ≤1MB على المسار المباشر |
| `feat(export): optional password encryption for GDPR zip` | BackupManager.kt, GdprExportRunner.kt, PrivacySection.kt, strings ar+en | 6د: `encryptBytesWithPassword/decryptBytesWithPassword` (PBKDF2-600k + AES-GCM، صيغة NOTABIN — جديدة، المنطق القائم لم يُمس)؛ حقل كلمة مرور اختياري في التصدير؛ `.zip.enc` + نوع مشاركة مناسب |
| `fix: correct ServerRegister import package in SettingsViewModel` | SettingsViewModel.kt | إصلاح تحقق: الاستيراد كان من `data` بدل `billing` — كشفه KSP |

## نتيجة الاختبارات والبناءات
- `./gradlew test` (عبر `nota_build.sh`): BUILD SUCCESSFUL — 60 ملف نتائج، صفر فشل
- `./gradlew assembleDebug`: BUILD SUCCESSFUL
- ملاحظة: كتلة KSP الغامضة (PROCESSING_ERROR) كانت خطأ استيراد حزمة — شخص وفُك في نفس الجلسة

## ما ينتظر محمد (اختبار جهازي — بعد backend)
- [ ] حذف حساب بحساب تجريبي → DELETE يصل (عند توفر endpoint) + المحلي يُمسح حتى مع فشل الشبكة
- [ ] رفع نسخة كبيرة متقطع الشبكة → استئناف من الإزاحة لا من الصفر
- [ ] تصدير مشفر بكلمة مرور → ملف `.zip.enc` + فك ناجح داخل التطبيق
- [ ] حساب استهلك التجربة (trialUsed=true) → لا زر تجربة ولا تفعيل محلي

## قرارات مأخوذة
- السحابة best-effort دائمًا في مسارات الحذف (لا يُعطَّل المحلي أبدًا)
- التجربة fail-open محليًا / fail-closed سيرفريًا (التجربة حق، والالتفاف مرفوض)
- الرفع المجزأ للملفات الكبيرة فقط (>1MB)؛ العلم السلبي يمنع إعادة المحاولة العبثية
- صيغة تشفير جديدة (NOTABIN) بدل zip4j — بلا اعتمادية جديدة، والفك داخل التطبيق

## اكتشافات غير متوقعة
- KSP يبتلع سبب الفشل (PROCESSING_ERROR بلا رسالة) — التشخيص يتطلب تنظيف الكاش + قراءة السجل الكامل

## انحرافات عن الخطة
- لا انحرافات

## ما ينتظر backend (للتنفيذ الكامل)
- [ ] `DELETE /v1/account` — حذف quota/blobs/pointers + إبطال الجلسات
- [ ] `PUT /v1/cloud/blobs/{sha}/chunks?offset=` — تجميع القطع + إعلان الاكتمال
- [ ] `POST /v1/trial/start` — تسجيل الاستهلاك (يُقرأ لاحقًا كـ trialUsed)
