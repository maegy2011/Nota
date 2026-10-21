# Nota — Phase 5 Status

## القرار العام
- [x] DONE — كودًا وتحققًا (5أ + 5ب + 5ج)

## ما نُفّذ
| Commit | الملفات | التفاصيل |
|---|---|---|
| `feat(billing): purchase-report interface with mock success and play stub` | BillingGateway.kt, FakeBilling.kt, PlayBillingGateway.kt | 5أ: `PurchaseInfo` + `queryPurchases()` + `reportPurchase()` — mock: استعادة [] وإبلاغ ناجح؛ play: الاثنان عاجزان + TODO عميل Billing الحقيقي |
| `feat(billing): report-after-purchase with deferred snackbar and startup restore` | PremiumViewModel.kt, PaywallScreen.kt, App.kt, strings ar+en | 5ب: بعد النجاح → إبلاغ بالإيصال → force عند النجاح / snackbar "مؤجل" عند الفشل؛ استعادة تلقائية عند الإقلاع (بلا شبكة إضافية في mock)؛ `purchase_deferred_fmt` ar+en |
| `docs(billing): TODOs for POST /v1/purchases and receipt validation` | HttpEntitlementsApi.kt, ServerRegister.kt | 5ج: TODOs الخادم (endpoint + تحقق الإيصالات) |

## نتيجة الاختبارات والبناءات
- `./gradlew test` (عبر `nota_build.sh`): BUILD SUCCESSFUL — 60 ملف نتائج، صفر فشل
- `./gradlew assembleDebug`: BUILD SUCCESSFUL — mock+play APKs حديثة
- لا اختبارات جديدة (المرحلة هيكلية — المسارات تُختبر فعليًا مع backend والـ Console)

## ما ينتظر محمد (اختبار جهازي — بعد backend + Console)
- [ ] شراء حقيقي في نكهة play → Snackbar نجاح + Pro يظهر بعد force refresh
- [ ] قطع الشبكة أثناء الإبلاغ → Snackbar "التفعيل مؤجل"
- [ ] إعادة فتح التطبيق بمشتريات سابقة → استعادة تلقائية بلا ضغط زر

## قرارات مأخوذة
- الإبلاغ يُستهلك بالإيصال (`getOrThrow`) لا بنجاح الاستدعاء — `Result<Result>` كانت ستكذب
- الاستعادة عند الإقلاع تُحدِّث السيرفر فقط بوجود مشتريات (بلا شبكة زائدة في mock)
- `purchaseToken` الفارغ = مؤجل لا ناجح (دفاع ضد نجاح بلا دليل)

## اكتشافات غير متوقعة
- لا شيء — البناءات خضراء من أول مرة بعد تثبيت السكربت المقفل

## انحرافات عن الخطة
- لا انحرافات

## ما ينتظر backend (للتنفيذ الكامل)
- [ ] `POST /v1/purchases` — يستقبل {accountId, productId, purchaseToken} + تحقق Play + ترقية الاستحقاق
- [ ] تحقق الإيصالات سيرفريًا (receipt validation)
- [ ] RTDN (تجديد/إلغاء/استرداد) — webhook
- [ ] إعداد منتجات Play Console (IDs مطابقة للثوابت) + منح trial في Play
