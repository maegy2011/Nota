# Nota — Phase 0 Status

## القرار العام
- [x] DONE
- [ ] PARTIAL (اذكر السبب)
- [ ] FAILED (اذكر السبب)

## ما نُفّذ
| Commit | Hash | الملف | التفاصيل |
|---|---|---|---|
| fix(ui): show Pro badge during trial in account and drawer | 971fe94 | AccountScreen.kt + strings.xml (ar/en) | T4: استبدال `isPro(entitlement)` الحرة بـ `premiumVm.isPro()` المركزي (يشمل التجربة) + شارة "Pro (تجربة)" عند trial-only + نص `acc_plan_pro_trial` جديد ar+en |
| fix(billing): force refresh after successful purchase | 258f260 | PremiumViewModel.kt | C3: `repo.refresh()` → `repo.refresh(force = true)` بعد `BillingResult.Success` |
| feat(billing): display purchase result to user via snackbar | cf17c51 | PremiumViewModel.kt + PaywallScreen.kt + AccountScreen.kt + App.kt + strings.xml (ar/en) | M4: دالة `clearLastResult()` جديدة + composable مشترك `PurchaseResultEffect` + ربطه في ProPaywall وAccountScreen (Snackbar لكل نتيجة؛ الإلغاء يُستهلَك بصمت؛ النجاح يستدعي `forceRefresh()` تلقائيًا) + نصوص `purchase_success` / `purchase_unavailable` / `purchase_failed_fmt` ar+en |
| chore: remove dead entitlement code paths | 4e6fa12 | Entitlements.kt + EntitlementsRepo.kt + BillingGateway.kt + FakeBilling.kt + PlayBillingGateway.kt | 0ب: حذف `canUse(e, name)` الحرة + `EntitlementsRepo.hasFeature(name)` + `queryPurchases()` من الـinterface والتجاوزين — صفر مراجع متبقية للدوال المحذوفة |
| docs(backup): note temporary trial exclusion for ncPro | edd1a0a | BackupScreen.kt | 0ج: تعليق عند `ncPro` يوثّق أن استثناء الـtrial مؤقت وسيُغلَق في المرحلة 1 |

## نتيجة الاختبارات
- `./gradlew test`: BUILD SUCCESSFUL (الـ4 متغيرات: mockDebug + mockRelease + playDebug + playRelease — 56 ملف نتيجة، صفر فشل/خطأ)
- `./gradlew assembleDebug`: BUILD SUCCESSFUL (mock + play)
- `./gradlew assembleRelease`: BUILD SUCCESSFUL (mock + play — R8 + lintVital سليم)
- Flaky: لا

## ما ينتظر محمد (اختبار جهازي)
- [ ] فتح التطبيق في وضع trial → الشارة تقول "Pro (تجربة)" في شاشة الحساب والقائمة الجانبية
- [ ] شراء mock ناجح → Snackbar "تم تفعيل Pro" + تحديث الحالة تلقائيًا
- [ ] شراء mock ملغى → بلا Snackbar وبلا تعليق
- [ ] نكهة play حقيقية: الشراء يرد `Unavailable` → Snackbar "المتجر غير متاح الآن" (يحتاج جهازًا بحساب Play)

## قرارات مُوثّقة من محمد
- **T3 (مؤقت — يُغلَق في المرحلة 1):** التجربة **لا** تفتح `ncPro` حاليًا (بطاقة المفتاح + رفع الوسائط) — استثناء مقصود ومؤقت. المرحلة 1 ستُغيّر `ncPro` ليشمل Trial (Trial يفتح كل شيء بما فيه ncPro). التعليق مثبّت في `BackupScreen.kt` عند `ncPro`.
- **T2:** `pro` boolean كافٍ لاتخاذ القرارات — `features` list للاحتياط المستقبلي فقط (لا تُقرأ في قرارات). أُبقيت الدالة الحرة `hasFeature(features, name)` لأن `canUseFolders()` / `canUseLocation()` و`App.kt:1210` يستخدمونها — لم تُحذَف (الحذف شمل `canUse(e, name)` و`EntitlementsRepo.hasFeature(name)` فقط).
- **سياسة التعارض الافتراضية (للمرحلة 3):** احتفظ بالمحلي + سجّل — ستُطبَّق في 3ب.

## قرارات مأخوذة (أثناء التنفيذ)
- `PurchaseResultEffect` وُضع كـ composable مشترك `internal` في `PaywallScreen.kt` وأُعيد استخدامه في `AccountScreen.kt` بدل تكرار المنطق.
- شارة الـtrial في `AccountScreen` تُحسَب عبر `remember(trialOn, entitlement) { premiumVm.isPro() }` مع جمع `trialActive` كـ State لضمان إعادة التركيب عند تغيّر التجربة (استدعاء `isPro()` وحده لا يُعيد التركيب).

## اكتشافات غير متوقعة
- `NavDrawer.kt` لا يحتوي أي استدعاء للدالة الحرة `isPro(entitlement)` — الشارة فيه تستخدم معامل `isPro: Boolean` القادم من `App.kt:420` وهو شامل للـtrial أصلًا (`entitlementTop?.pro == true || trialOn`). أرقام الأسطر في الخطة قديمة؛ لا تعديل لزم في القائمة الجانبية.
- بقايا `canUse` في `NoteEditorScreen.kt` (سطور 1013/1245/1805/1852) هي معامل `canUse: Boolean` محلي غير مرتبط بالدالة المحذوفة — تُرك كما هو.
- `values/strings.xml` الافتراضي يحوي نصوصًا إنجليزية متفرقة (مثل `acc_plan_free`) — النصوص الجديدة أُضيفت عربيًا فيه وإنجليزيًا في `values-en` حسب القاعدة (ar+en كحد أدنى).

## انحرافات عن الخطة
- تعليق `BackupScreen` صيغ ليصف الاستثناء بأنه **مؤقت** (بقرار محمد) بدل صيغة الخطة الأصلية ("مقصود" دون تأقيت) — المعنى الفني واحد، والتوثيق أدق.
- commit خامس (`docs(backup)`) للتعليق — الخطة حددت رسائل 4 commits فقط؛ التعليق تغيير كودي يستحق تتبعًا مستقلًا بدل دمجه في commit آخر.
- أول تشغيل لـ `./gradlew test` قُطع بانتهاء مهلة العميل (SIGTERM) بعد ~20 دقيقة تجميع؛ أُعيد التشغيل على daemon ساخن ونجح من أول إعادة (محاولة إصلاح واحدة، بلا فشل بناء).

## ما ينتظر backend (للمرحلتين 5، 6)
- لا شيء في المرحلة 0.
