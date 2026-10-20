# Nota — Phase 1 Status

## القرار العام
- [x] DONE

## ما نُفّذ
| Commit | Hash | الملف | التفاصيل |
|---|---|---|---|
| feat(entitlement): centralize Pro status in PremiumViewModel (1أ) | 0c353a9 | PremiumViewModel.kt + AccountScreen.kt + strings | enum EntitlementSource + source flow + isProNow() + isProForBackup() + سطر مصدر Pro في الحساب |
| refactor(gates): unify all isPro gates behind premiumVm.isPro() (1ب) | cf672b2 | App.kt + BackupScreen.kt | App:420 + App:1209 + BackupScreen:133 (T3 مغلق: Trial يفتح ncPro) |
| feat(pro-gate): enforce Pro inside ViewModels (1ج) | ab10fc6 | NotaCloudViewModel.kt + NotesViewModel.kt + FoldersViewModel.kt + BackupScreen.kt + strings | فحص Pro في backupNow + toggles + duplicate + moveNote + تعليق restoreNow الصريح |

## نتيجة الاختبارات
- `./gradlew test`: BUILD SUCCESSFUL (23m22s — 352 اختبار، 0 فشل، 0 أخطاء، 4 variants)
- `./gradlew assembleDebug`: BUILD SUCCESSFUL (2m4s)
- `./gradlew assembleRelease`: BUILD SUCCESSFUL (mock + play APKs — minify سليم)
- Flaky: لا

## ما ينتظر محمد (اختبار جهازي)
- [ ] فتح التطبيق في وضع trial → شارة "Pro (تجربة)" + سطر "تجربة مجانية" في شاشة الحساب
- [ ] Trial: رفع الوسائط للسحابة يعمل (T3 مغلق — ncPro = isPro)
- [ ] حساب مجاني: رفع الوسائط مرفوض برسالة "رفع الوسائط للسحابة ميزة Pro"
- [ ] حساب مجاني: تثبيت/قفل/مفضلة/تكرار/نقل لمجلد مرفوضة (proLock كالمعتاد)
- [ ] استعادة سحابية بعد downgrade تعمل بلا فحص Pro (قرار صريح)
- [ ] شاشة الحساب بعد شراء حقيقي تعرض "اشتراك ساري"

## قرارات مأخوذة
- **T3 مغلق سلوكيًا** (بقرار محمد المعتمد): `ncPro = premiumVm.isPro()` — التجربة تفتح رفع الوسائط.
- **T2**: `pro` boolean كافٍ — `features` للاحتياط (بقرار محمد المعتمد).
- **بلا حقن PremiumViewModel في VMs أخرى**: نسخة VM منفصلة ستملك `trialActive=false` دائمًا فتكسر التجربة — الفحص من نفس المصادر (كاش المستودع + trialStart) بدلًا من نسخة منفصلة. `Provider` لم يلزم.
- **duplicateNow بلا Pro يُرجع -1L** بدل الرمي: المنادي `duplicateAndOpen` يعالجها بفرعه الآمن، والرمي غير الممسوك كان سيُسقط التطبيق.
- **restoreNow بلا فحص Pro** (قرار الخطة): المستخدم يستعيد بياناته دائمًا حتى بعد downgrade.

## اكتشافات غير متوقعة
- المواصفة افترضت `CloudResult.ProRequired` و`backupNow(allowMedia)` في `CloudBackupViewModel` — الواقع: `NotaCloudViewModel.backupNow(allowMedia)` برسائل نصية (`nc-err-*`) بلا sealed class. طُبّق نفس المعنى بمفتاح `nc-err-pro-required` + ربطه بالنص الجديد.
- المواصفة سمت `NotesViewModel.moveToFolder` — الواقع: `FoldersViewModel.moveNote`. طُبّق الفحص هناك.
- `trialOn` يظهر أيضًا في AccountScreen (متغير محلي من `premiumVm.trialActive` — من المرحلة 0) — ليس مصدرًا منافسًا، بل إعادة تركيب للعرض.

## انحرافات عن الخطة
- **طفيفة ومبررة**: `Provider<PremiumViewModel>` لم يُستخدم (انظر القرارات) — الفحص من `EntitlementsRepo.cached + SettingsStore.trialStart` (نفس مصادر `isPro()` حرفيًا).
- **طفيفة**: بلا `CloudResult` جديد — أُعيد استخدام قناة الرسائل القائمة (`_message`) بدل sealed class غير موجود.
- **طفيفة**: `duplicateNow` تُرجع `-1L` بدل `Result.NotPro`/الرمي (السلامة أولًا — المنادي لا يمسك الاستثناءات).
- لا حذف لأي تعليق قديم — أُضيفت تعليقات المرحلة بجانبها.
- لا منطق تشفير جديد، لا FLAG_SECURE، لا allowBackup، لا درع — التزام كامل.

## ما ينتظر backend (للمرحلتين 5، 6)
- لا شيء من المرحلة 1 — بلا endpoints جديدة.

## تحقق grep (معيار القبول)
- `\.pro == true` → فقط `Entitlements.kt` (الدالة الحرة، parser) + `PremiumViewModel` (isPro/isProNow/resolveSource). صفر في UI/بوابات.
- `trialOn` → فقط `App.kt` (المصدر + paywall) + متغير محلي عرضي في AccountScreen (من المرحلة 0).
- النصوص الجديدة ar+en: `pro_source_server/trial` + `ncloud_err_pro_required` موجودة في `values/` و`values-en/` (وفقط هاتين — سياسة المرحلة 0).

## المهارة المطبقة
- **compose-pro-gating**: توحيد البوابات خلف `isPro` مركزي + trial يفتح الكل + enforcement في الـ ViewModel لا الـ UI فقط.
