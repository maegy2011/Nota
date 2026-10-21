# جرد Backend + UX كامل — شاشة النسخ الاحتياطي (BackupScreen) — تطبيق نوتا

> المنهج: قراءة الكود الفعلي فقط — لا افتراض ولا تخمين. كل بند "غير منفّذ" يعني: بُحث عنه بـ grep وقراءة الملفات ولم يوجد.
> المهارة المطبقة: **code-reviewer** (قرئت `SKILL.md` وطُبقت: تحديد النطاق → فحص آلي مكافئ عبر grep/قراءة → مراجعة سياقية للملفات الستة الحرجة + المخازن + الجدولة + الاختبارات → هذا التقرير كجرد بلا توصيات).
> الملفات المقروءة فعليًا: `ui/BackupScreen.kt` (~1302 سطر)، `ui/CloudBackupViewModel.kt` (~364)، `ui/NotaCloudViewModel.kt` (~323)، `data/BackupManager.kt` (~177)، `data/BackupCodec.kt` (~181)، `data/BackupPassword.kt`، `data/DriveBackup.kt` (~194)، `data/NoteDao.kt` (دوال النسخ)، `data/NotesRepository.kt` (`restoreMerge`/`undoRestore`)، `data/SettingsStore.kt`، `data/GdprExport.kt`، `data/GdprExportRunner.kt`، `security/KeyManager.kt`، `security/PassphraseStore.kt`، `auth/GoogleAuthManager.kt`، `ui/AuthViewModel.kt`، `backup/BackupScheduler.kt` (~135 سطر)، `AndroidManifest.xml`، `res/xml/backup_rules.xml`، ملفات `strings.xml` الستة، ملفات الاختبار الـ14، `PlayBillingGateway.kt` (نكهة play)، `FakeBilling.kt` (نكهة mock)، `EntitlementsRepo`/`HttpEntitlementsApi`/`PremiumViewModel`/`AccountStore`/`TokenVault`/`CloudKeyStore`/`AccountDeletion.kt`/`DeletionWorker.kt`/`NotaApp.kt`.

---

## القسم 1 — الفئات وحدود Free/Pro

| # | العنصر | الملف/السطر التقريبي | Free | Pro | لماذا؟ |
|---|---|---|---|---|---|
| 1 | زر "نسخ الآن" في الملخص (`BackupSummaryCard` ~سطر 241، الزر ~260-267، `onBackupNow = { vm.backupNow() }` أي نسخة Drive ~سطر 206) | `BackupScreen.kt` | ✅ يعمل | ✅ يعمل | لا فحص Pro عليه؛ فقط `enabled = !busy && !locked` |
| 2 | بطاقة Drive (نسخ/استعادة/مجدول/عدد نسخ/كلمة مرور) `DriveCard` ~سطر 291 | `BackupScreen.kt` + `CloudBackupViewModel.kt` | ✅ تعمل كاملة | ✅ تعمل كاملة | لا فحص `ncPro` داخلها إطلاقًا؛ `FreqOption` يعرض أيقونة قفل شكلية فقط إذا `locked` (~سطر 1298) |
| 3 | بطاقة سحابة نوتا (نسخ نصي/استعادة) `NotaCloudCard` ~سطر 559 | `BackupScreen.kt` + `NotaCloudViewModel.kt` | ✅ النسخ النصي يعمل | ✅ + الوسائط | `backupNow(ncPro)` أي `allowMedia = ncPro` فقط؛ لا بوابة تمنع النسخ النصي لغير-Pro في الكود المقروء |
| 4 | بطاقة مفتاح السحابة `CloudKeyCard` ~سطر 698 | `BackupScreen.kt` ~سطر 214 | 🔒 مخفية | ✅ ظاهرة | تُعرض فقط `if (ncPro)` حيث `ncPro = ncEntitlement?.pro == true` (~سطر 143-144) |
| 5 | ملاحظة الوسائط لغير-Pro `ncloud_media_note` ~سطر 612-620 | `BackupScreen.kt` | تظهر | لا تظهر | `if (!ncPro) Text(...)` |
| 6 | ملف `.nota` (تصدير/استعادة) `FileBackupCard` ~سطر 1020 | `BackupScreen.kt` | ✅ يعمل | ✅ يعمل | لا فحص Pro؛ فقط `locked` يعطّل |
| 7 | سجل الاستعادة + تراجع `RestoreLogCard` ~سطر 918 | `BackupScreen.kt` | ✅ يعمل | ✅ يعمل | لا فحص Pro؛ يُفتح عبر رابط `bk_links_log` (~سطر 224) |
| 8 | بانر القفل + شيت الباقات (`ProLockedBanner` خارجي + `ModalBottomSheet` ~سطر 170-191) | `BackupScreen.kt` ~سطر 166-191 | يظهر إذا `locked=true` | لا يظهر | الشيت: عنوان `bk_pro_title` + وصف `bk_pro_msg` + زر `paywall_upgrade` (يغلق ثم `onLocked()`) + زر `cancel` |
| 9 | حالة عدم الدخول (زر تسجيل الدخول) ~سطر 192-201 | `BackupScreen.kt` | تظهر للكل | تظهر للكل | لا فحص Pro |

**الأسئلة الدقيقة:**
- **المجاني يستطيع:** نسخ Drive الآن + مجدول + استعادة Drive + نسخ سحابة نوتا نصي + استعادته + تصدير/استعادة `.nota` + سجل الاستعادة والتراجع. (كله بلا فحص Pro في الكود المقروء.)
- **مستخدم Pro يستطيع إضافيًا:** إظهار/إنشاء/استيراد مفتاح السحابة (`CloudKeyCard`) + إرفاق الوسائط في نسخة السحابة (`allowMedia`).
- **عنصر Free لا يوجد في Pro:** لا يوجد — كل ما في Free موجود في Pro (البطاقة تُخفى عن Free ولا تُسلب من Pro).
- **يُنشأ في Pro ويُقرأ في Free:** نسخة سحابة تحوي وسائط أُنشئت أثناء Pro → الاستعادة في Free تعمل (`restoreNow()` بلا فحص Pro) لكن الوسائط بلا مفتاح تُسجَّل `media:<n>:nokey` وتُتجاهل الملفات الغائبة؛ المفتاح نفسه مخفي عن Free (`CloudKeyCard` مشروطة) فإن فُقد على الجهاز فلا استيراد من الشاشة. نسخة Drive تعمل للكل أصلًا.
- **يُنشأ في Free ولا يُرقَّى:** لا يوجد — لا عنصر Free يُسلب عند الترقية.
- **محتوى الشيت بالنص الحرفي:** مفاتيح `R.string.bk_pro_title` (عنوان) + `R.string.bk_pro_msg` (وصف) + زر `R.string.paywall_upgrade` + زر `R.string.cancel` — النصوص العربية في `strings.xml` (لم تُنسخ حرفيًا هنا؛ المفاتيح هي المرجع).
- **متى يظهر البانر:** كل مرة تُفتح الشاشة بـ `locked=true` (`if (locked) { ProLockedBanner { proSheet=true } }` ~سطر 166) — لا منطق "مرة واحدة" في هذا الملف.
- **هل يظهر في أقسام أخرى:** غير منفّذ — لا يوجد بانر/شيت ثانٍ داخل Drive أو Cloud أو `.nota` — بانر واحد + شيت واحد على مستوى الشاشة.

---

## القسم 2 — آلية التحقق من Pro (Entitlement)

1. **المصدر:** السيرفر — `EntitlementsRepo` + `HttpEntitlementsApi`: `GET https://mohamedadel.cfd/nota-api/v1/entitlements?accountId=` + هيدر `Authorization: Bearer <Firebase ID token>`. الـ parse: `pro = obj.optBoolean("pro", false)`. القاعدة معلَّقة في الكود: "لا isPremium محفوظ محليًا أبدًا — يُسأل من السيرفر مع كاش قصير للأوفلاين". **Play Billing غير مربوط فعليًا:** نكهة play عبارة عن stub يرد دائمًا `BillingResult.Unavailable` (`PlayBillingGateway.kt`: `override suspend fun launchPurchase(productId: String): BillingResult = BillingResult.Unavailable` و`queryPurchases() = emptyList()`)؛ نكهة mock فقط تنجح وهميًا (`FakeBilling.kt`: `delay(300)` ثم `Success("fake-$productId-...")`). **القيمة الافتراضية عند الفشل: `Entitlement.FREE` أي `pro=false`** — ثلاثة مواضع: بلا حساب → FREE؛ استثناء شبكة (غير AuthExpired) → آخر كاش معروف؛ `PremiumViewModel.refresh()` عند فشل غير-auth → `_entitlement.value = Entitlement.FREE`. `AuthExpiredException` تُرفع للواجهة ولا تُدفن.
2. **التحقق قبل كل إجراء أم مرة واحدة:** التحقق الشبكي **ليس** قبل كل إجراء — `EntitlementsRepo.refresh()` يُرجع الكاش فورًا إن طازجًا وإلا يسأل السيرفر؛ `PremiumViewModel.init { refresh() }` مرة عند الإنشاء + `forceRefresh()` يدوي. أما البوابات في الـ Composables (`App.kt` ~سطر 420: `val isPro = entitlementTop?.pro == true || trialOn`) فتُعاد مع كل recomposition. **الكاش:** DataStore `entitlements_cache` — `CACHE_TTL_MS = 24h` لنفس الحساب؛ تبديل الحساب يُسقط الكاش (`acct==null || store[account]!=acct` → FREE). **انتهاء الاشتراك server-side:** يعرفه العميل في الرد التالي بعد انتهاء TTL — نظريًا حتى 24 ساعة (يحتاج جهازًا للتأكيد الفعلي).
3. **حالات الحافة:**
   - Pro منتهي اليوم ونسخة سحابة من أمس → الاستعادة **تعمل** (`restoreNow()` بلا فحص Pro) — لكن بطاقة المفتاح تختفي (`if (ncPro)`)، فإن احتاج المفتاح وما زال مخزنًا محليًا يعمل، وإلا فلا استيراد من الشاشة.
   - Pro على جهاز آخر والجهاز الحالي مغلق منذ أسابيع → بعد أول `refresh` (مع `ServerRegister.ensureRegistered`) يحصل على نفس `pro` — يحتاج جهازًا للتأكيد.
   - Pro + offline → آخر كاش معروف يُستخدم (`forceRefresh` عند الفشل يُبقي آخر استحقاق "لا يُسقَط للـ FREE ظلمًا في الأوفلاين") — يحتاج جهازًا.
   - Refund → غير منفّذ — لا يوجد في الكود أي معالجة refund/cancel؛ السيرفر وحده سيرد `pro:false` لاحقًا. النسخ السحابية: لا حذف تلقائي في الكود. (يحتاج جهازًا للسلوك الفعلي.)
   - Free trial → يوجد trial **محلي** 7 أيام (`SettingsStore`: `TRIAL_DAYS=7`، `trial_start_ms`، `isTrialActive = start>0 && now < start+duration`) + سطر خصم `TRIAL_DISCOUNT_PCT=50` على السنوية ("يُفعَّل من عروض متجر Play" — نص فقط بلا تكامل). ليس trial اشتراك Play.
4. **الترقية:** `PaywallScreen`: بطاقة Pro بزرين شهري/سنوي → `premiumVm.purchase(it)` أي `gateway.launchPurchase(productId)` — في نكهة play الحالية **لا يُفتح أي تدفق شراء فعلي** (Unavailable دائمًا). المنتجات المعرفة: `nota_pro_monthly/yearly`, `nota_lifetime`, `nota_storage_5g/10g_monthly/yearly`. بعد `Success` فقط: `repo.refresh()` (غير force — قد يُرجع الكاش الطازج!) ثم `_entitlement.value = it`. `queryPurchases` **غير مستدعاة إطلاقًا** — لا تحقق من المشتريات. **رسائل النجاح/الفشل: غير منفّذة** — `_lastResult` يُخزَّن ولا يُستهلك في أي UI؛ لا Toast/Dialog للنجاح/الفشل/الإلغاء. زر `paywall_refresh` → `forceRefresh()`. شاشة الحساب تعرض الباقة (`acc_plan_pro/free`) + الانتهاء (`acc_until_fmt`) + `acc_manage_play` (نص: الإدارة عبر Google Play).
5. **Downgrade:** غير منفّذ — لا مسار downgrade في الكود؛ الإلغاء "يُدار عبر Google Play" (نص فقط). النسخ السحابية عند الـ downgrade: لا حذف تلقائي، وتبقى قابلة للاستعادة (`restoreNow` بلا بوابة). لا تنبيه قبل الـ downgrade. لا grace period.
6. **Cross-device:** الاستحقاق لكل `accountId` من السيرفر — جهاز جديد بنفس الحساب يحصل على نفس `pro` بعد أول `refresh`. التجربة المحلية لا تنتقل (DataStore جهازي). الآلية: Play Billing (stub حاليًا) + Firebase Auth (ID token) — لا token مخصص في الكود.

---

## القسم 3 — الموديل والبيانات

1. **محتوى النسخة:** الجداول المضمنة: **الملاحظات كاملة** (`BackupNote`: title/body/createdAt/updatedAt/color/archivedAt/pinned/locked/background/backgroundSha/favorite/category/tags/reminderAt/reminderRepeat/reminderBeforeMin/latitude/longitude/repeatInterval/repeatDays/repeatMonthMode/repeatEnd/repeatEndValue/tasks/attachments/folder/deletedAt/geoLat/geoLng/geoRadiusM/geoName) + **المجلدات** (`BackupFolder(name,color)` — v3 فقط، تُعاد إنشاؤها عند الاستعادة إن غابت `restoreMerge` في `NotesRepository.kt` ~سطر 312) + **الوسوم داخل كل ملاحظة** (`tags: List<String>` — لا جدول tags مستقل). **السلة مضمنة:** نعم — `deletedAt: Long?` يُحفظ ويُستعاد (`backupAllOnce` بلا فلتر deletedAt؛ `allOnce` تستثنيها) وتُستعاد السلة سلةً بتاريخ حذفها. **الإعدادات: غير منفّذة** — JSON يحوي فقط `meta{count,createdAt,appVersion}` + folders + notes. **المرفقات:** metadata فقط (`BackupAttachment(kind=file=اسم داخلي/sha?)` — kind مقيد `image|audio|drawing`) — الغائب على الجهاز **يُتجاهل**: `attachments.filter { NoteMediaFiles.file(appCtx, it.file).exists() }` (`NotesRepository` ~سطر 345). **الخلفيات:** الاسم فقط (`background` + `backgroundSha`) — الملف نفسه لا يُنسخ.
2. **الصيغة (واحدة للكل — Drive وCloud و`.nota`):** ملف `.nota` نصي من أسطر Base64 بترويسة `NOTA` — الصيغ القديمة NOTA1/2/3 محذوفة (تعليق `BackupManager.kt` ~سطر 9). نوعان: `PW` مشفرة بكلمة (6 أسطر: `NOTA\nPW\n<iters>\nsalt\niv\nciphertext` — `encryptEnvelope` ~سطر 140) و`KS` مشفرة بمفتاح الجهاز (4 أسطر: `NOTA\nKS\niv\nciphertext` — `export` ~سطر 15 و~29). الداخل JSON v3: `{"app":"nota","v":3,"meta":{...},"folders":[...],"notes":[...]}` (`BackupCodec.encode` — `const val V = 3`)؛ الفك يقبل v2 (بلا مجلدات). الكتابة ذرية (`.tmp` + `renameTo` ~سطر 49). **لا ZIP ولا SQLCipher ولا JSON مكشوف.**
3. **الإصدارات:** `format_version` = حقل `v` في JSON (`V = 3`) — لا version في الغلاف الخارجي. نسخة أقدم (v2): تُقبل (بلا مجلدات/بطاقة). نسخة أحدث من القارئ: سلوك غير موثق في الكود المقروء. **Migration بين الصيغ: غير منفّذ** — لا كود migration (فقط قبول v2 عند الفك + إزالة NOTA1/2/3).
4. **الحجم:** **لا حساب ولا حد أقصى في الكود** — لا حد لحجم الملف/JSON/عدد الملاحظات. الوحيد: عتبة الانكماش السحابي (تمنع الرفع لا لحد حجم).
5. **الـ metadata:** داخل النسخة `BackupMeta(count, createdAt, appVersion)`؛ محليًا في DataStore: `cloud_backup_last` (آخر رفع ناجح ms) + `cloud_backup_hash` (بصمة آخر JSON — تخطي المكرر) + `cloud_backup_keep` (1..7) + `cloud_backup_count` (عتبة الانكماش) + `cloud_backup_auto/freq` + سجل `restore_log` (آخر الاستعادات + undone). لا checksum داخل ملف `.nota` — السلامة عبر GCM tag فقط. بطاقة الملخص تستخدم أفضل تاريخ متاح (`bk_sum_fmt(تاريخ, مصدر)` / `bk_sum_none`).

---

## القسم 4 — Google Drive

1. **OAuth:** Scope واحد فقط: `https://www.googleapis.com/auth/drive.appdata` (مجلد التطبيق الخاص — `SCOPE_APPDATA` في `DriveBackup.kt`). التوكن عبر `GoogleAuthUtil.getToken` **عند كل عملية** + إعادة واحدة عند 401 (إبطال ثم رمز جديد)؛ `ConsentNeeded(intent)` عند غياب النطاق. مدة الصلاحية/التجديد: توكن OAuth قصير العمر يُطلب عند كل عملية (IO فقط) — لا مدة مكتوبة في الكود. فشل التجديد → رموز `auth-failed`/`consent-needed`/`consent-unavailable`/`not-signed-in`/`no-account`. (السلوك الفعلي على جهاز: يحتاج جهازًا.)
2. **الرفع:** REST مباشر عبر `HttpURLConnection` (لا SDK): `GET drive/v3/files?q=...` + `POST upload/drive/v3/files?uploadType=multipart` + `GET files/{id}?alt=media` + `DELETE files/{id}`. مهلات 20s اتصال / 60s قراءة. **Resume: غير منفّذ** — multipart دفعة واحدة في الذاكرة. لا حد أقصى لحجم النسخة في الكود. **Checksum: نعم بعد الرفع** — `sha256(echo) != sha256(json)` → يحذف المرفوع ويرمي `verify-failed` (`upload` ~سطر 120). فشل منتصف الرفع → رمز `drive-upload:{code}`/`drive-upload-no-id` (والفاشل التحقق يُحذف).
3. **الاحتفاظ 1-7:** `upload(json, keep=5)` + `settings.cloudKeep` (`MAX_CLOUD_KEEP`/`DEFAULT=5` — مقابل `cloud_backup_keep` 1..7 في SettingsStore؛ انظر تناقض C1). ملف مؤرخ جديد كل مرة `nota-backup-<ms>.nota` + حذف ما زاد (`findAll` مرتب modifiedTime desc، يُبقي المرفوع حديثًا دائمًا، ويتخطى التدوير إن لم يظهر الجديد في الفهرس). الثامنة → **الأقدم تُحذف تلقائيًا** (لا رفض). الحماية من الحذف العرضي: إبقاء المرفوع حديثًا + تخطي التدوير إن غاب من الفهرس. **الحذف بلا تأكيد ولا تراجع.**
4. **كلمة مرور Drive:** الاشتقاق PBKDF2 فقط (لا Argon2): `deriveKey` (~سطر 190): `PBEKeySpec(password.toCharArray(), salt, iters, 256)` بـ `PBKDF2WithHmacSHA256` → AES (`SecretKeySpec(tmp.encoded, "AES")`) — `const val ITERS = 600_000` (يُكتب في الغلاف؛ الفك `coerceIn(100_000, 2_000_000)`)، ملح 16 بايت `SecureRandom`، AES-GCM-256 (`GCM_TAG_BITS = 128`). **الحد الأدنى 8 أحرف مطبق فعلًا** (`MIN_PASSWORD_LEN = 8` + `isStrongPassword`: رفض الشائعة/المكرر/المتتاليات) — الفك بلا حد أدنى عمدًا (توافق ملفات 4 أحرف). التوليد: `generateDicewarePassword(words=6)` (~47.5 بت). التخزين: `SettingsStore` (`drive_pw_enc` = `iv.cipher` — مشفرة بمفتاح Keystore `nota_drive_pw`) — "الحماية من قراءة السيرفر لا من الجهاز". **مسار جهاز جديد بإدخال يدوي: موجود** — `pwNeeded==true` يُظهر حقل `cloud_pw_manual_hint` + زر `cloud_pw_manual_btn` → `vm.restoreWithPassword(manualPw)` (خطأ → `err:drive-pw-wrong`).
5. **المعاينة + الانكماش:** المعاينة: `bk_preview_title` + `bk_preview_msg(count, date)` ("ستُدمج بلا مسح لشيء، ونسخة أمان تلقائية تُحفظ أولًا") + تحذير القدم `bk_preview_stale` ("أقدم من بياناتك الحالية — قد يعيد عناصر محذوفة" إذا `meta.createdAt < repo.newestUpdate()`) + تأكيد `bk_preview_confirm` → `vm.confirmRestore()`؛ الإلغاء يمسح المؤقت. **حوار الانكماش:** يُطلق عندما `lastCount >= 10 && noteCount*2 < lastCount` (الجديدة < نصف السابقة) — النص: عنوان `cloud_shrink_title` ("النسخة أصغر بكثير") + رسالة `cloud_shrink_msg(pct)` ("الجديدة %1$d%% فقط من السابقة — حذف جماعي بالخطأ؟ المتابعة ترفعها، والإلغاء يُبقي السحابة كما هي") + `cloud_shrink_confirm` (متابعة الرفع) / `cancel`. **يمكن تجاوزه** (زر المتابعة)؛ الوضع التلقائي (`silent=true`) يتخطى بصمت بلا حوار؛ حارس الفراغ `noteCount==0 && hasBackup()` → تخطٍ.
6. **حالات الحافة:** 0 نسخ → `cloud_never` + `hasBackup()` حارس الرفع الفارغ + `no-backup` عند الاستعادة. Quota ممتلئ → لا معالجة مخصصة (رموز Drive العامة `drive-upload:{code}`) — السلوك الفعلي يحتاج جهازًا. الحساب محذوف → `not-signed-in`/`no-account`/`consent-needed` (جزئيًا كود + يحتاج جهازًا). جهاز جديد بلا توكن → بطاقة الحالة + `cloud_signin_needed` + زر `cloud_signin_btn` عبر `onSignInNeeded`.

---

## القسم 5 — سحابة نوتا (Nota Cloud)

1. **Backend:** خادم مخصص `https://mohamedadel.cfd/nota-api` — REST: `POST /v1/users/register {accountId}` + `GET /v1/entitlements?accountId=` + `GET /v1/cloud/quota?accountId=` + `PUT /v1/cloud/blobs/{sha256}?accountId=&mime=` + `GET /v1/cloud/blobs/{sha256}?accountId=` + `PUT /v1/cloud/pointers/{name}?accountId=&sha=` + `GET /v1/cloud/pointers/{name}?accountId=` (الـ 404 = لا نسخة). كلها `Authorization: Bearer <Firebase ID token>`. مهلة 10ث (رفع/تنزيل 60ث)، تجديد واحد عند 401 ثم `AUTH_EXPIRED`. لا Firebase Storage/Supabase/gRPC في الكود.
2. **Auth:** Firebase ID token من `AccountStore` (مختوم بـ `TokenVault` AES-256-GCM بمفتاح Keystore `nota_token_vault` صيغة `V1:Base64(iv+cipher)`؛ القديم المكشوف يُهاجَر ذاتيًا). `IdTokenProvider.current()` بلا شبكة؛ `refresh()` تجديد قسري + حفظ فوري. صلاحية التوكن: Firebase (ساعة تقريبًا) + تجديد قسري عند الحاجة — لا صلاحية مخصصة في الكود. الفشل mid-upload → `nc-err:<code>` (و`AUTH_EXPIRED` → `ncloud_err_auth`). **الاستئناف بعد انتهاء الجلسة: غير منفّذ** — يجب إعادة الرفع من الصفر.
3. **التخزين:** blobs مشفرة + pointers على الخادم. تشفير server-side إضافي: غير مذكور في الكود (التشفير client-side بالمفتاح المغلف؛ التعليق: "السيرفر لا يرى المفتاح أبدًا"). حذف النسخة عند انتهاء الصلاحية: غير منفّذ في الكود المقروء.
4. **Quota:** الخادم يرد `QUOTA_EXCEEDED` → `nc-err-quota` (أي **الفرض server-side موجود**)؛ الواجهة تعرض `ncloud_used_fmt(used, quota)` + شريط تقدم. حد كل باقة رقميًا: غير مقروء في الكود (يأتي من `/quota`). رسالة التجاوز: `ncloud_err_quota`. لا فرض client-side لحجم في الكود.
5. **مفتاح السحابة:** 32 بايت عشوائية `SecureRandom` (`CloudKeyStore` — DataStore `nota_cloud` + نسخة `nota_cloud_bak`)، **مغلفة** بمفتاح Keystore `nota_cloud_wrap` (AES/GCM). PIN الشاشة "قفل فقط — لا يدخل في تشفير السحابة إطلاقًا". النقل بين الأجهزة: عبارة استرداد (`exportPhrase` = Base64 بشرطات كل 4 — تُعرض مرة واحدة) تُستورد عبر `importPhrase` (يقبل أي فواصل) + حفظ ملف `nota-cloud-key.txt` / استيراد ملف. **Rotation: غير منفّذ** — `createCloudKey()` ينشئ جديدًا لكن لا سياسة rotation/ترحيل للنسخ القديمة في الكود.
6. **حالات الحافة:** رفع ثم انتهاء Pro → النسخة تبقى (لا حذف تلقائي) وقابلة للاستعادة (`restoreNow` بلا بوابة). شراء Pro على جهاز جديد → يستطيع التنزيل بعد استيراد المفتاح (`importPhrase`) — بلا مفتاح وظرف مشفر → `nc-err-nokey` + دليل `ncloud_nokey_guide`. حذف الحساب → النسخ السحابية **لا تُحذف** (لا endpoint حذف server-side في `NotaCloudApi` — quota/blobs/pointers فقط؛ الـ Worker "يوسم التنفيذ فقط... يُوثَّق كـ ينتظر محمد").

---

## القسم 6 — ملف .nota

1. **التصدير:** نفس صيغة القسم 3 (غلاف `NOTA/PW` — PBKDF2-HMAC-SHA256 بـ 600k دورة + AES-GCM-256). الحد الأدنى 8 أحرف + قوة (`length>=8 && isStrongPassword && متطابق`) يُفرض في الزر (`bk_exp_btn`). المشاركة عبر FileProvider/ACTION_SEND بعنوان `bk_share_title` — نجاح `bk_exp_ok` / فشل `bk_failed`.
2. **الاستعادة:** الكشف: `BackupManager.peek` (ترويسة `NOTA` + نوع `PW`/`KS`) — لا extension ولا header bytes ثنائية. ملف تالف → `bk_file_bad` ("ملف النسخة تالف"). كلمة خاطئة → `bk_pw_wrong` / `فشل فك التشفير — تحقق من كلمة المرور` (`IllegalStateException` موحدة P3-0). إصدار أقدم (v2) → يُقبل (بلا مجلدات). **المعاينة قبل الدمج: موجودة دائمًا** — `bk_preview_title/msg(count,date)/stale/confirm`؛ التأكيد يدمج، الإلغاء يحذف المؤقت.
3. **الدمج:** إضافة فقط + تخطي المكرر — لا استبدال/تحديث/حذف (`RestoreResult(added, skipped, addedIds)`). التكرار = نفس الثلاثي `(title, body, createdAt)` بالضبط (`NotesRepository.restoreMerge` ~سطر 310) — لا id ولا hash ولا timestamp للمقارنة، والمستعادة تُدرج **بهوية جديدة**. **التراجع: موجود** — `undoRestore(ids)` ينقل المضافة للسلة (`trashOne`) لا حذفًا نهائيًا (قابلة 30 يومًا) + `safetySnapshot()` قبل أي دمج (نسخة KS في `files/auto-backups` — آخر 3 فقط) + سجل `RestoreRecord`.

---

## القسم 7 — التشفير والمفاتيح

1. **مفاتيح النسخ الثلاثة:** Drive: كلمة المستخدم → PBKDF2 (600k) + AES-GCM (غلاف الملف) + كلمة مخزنة مغلفة بـ Keystore. Cloud: مفتاح عشوائي 32 بايت مغلف بـ Keystore (`nota_cloud_wrap`) — مستقل. `.nota`: كلمة المستخدم → PBKDF2 (600k) + AES-GCM. **لا مفتاح مشترك بين الثلاثة** — ثلاثة مسارات مستقلة. (مفتاح قاعدة البيانات `nota_db_key` مستقل أيضًا.)
2. **التخزين:** DataStore مزدوج لبعضها + تغليف Keystore: `drive_pw_enc` (مفتاح `nota_drive_pw`)، مفتاح السحابة (مفتاح `nota_cloud_wrap`)، مفتاح DB (مفتاح `nota_db_key`)، التوكن (مفتاح `nota_token_vault`). PIN يُقارن بـ PBKDF2 (310k) ولا يدخل في أي تشفير. Recovery key: **hash فقط** (PBKDF2-HMAC-SHA256 310k بملح مستقل) في `PassphraseStore`.
3. **Recovery:** نسيت كلمة Drive → **لا استرداد** — النسخ القديمة تصبح غير قابلة للفك. نسيت مفتاح Cloud → `nc-err-nokey` — لا استرداد server-side (التعليق: السيرفر لا يرى المفتاح). نسيت كلمة `.nota` → لا استرداد. Recovery Key (12 كلمة BIP39) يعيد **PIN فقط** (`AuthViewModel.verifyRecoveryKey` + درع تخمين: من المحاولة 3 قفل 30ث يتضاعف حتى 10دق) + بديل opt-in `device_cred_enabled` (افتراضي OFF) — **لا يعيد أيًّا من مفاتيح النسخ الثلاثة**.
4. **النقل بين الأجهزة:** جهاز جديد + Cloud → استيراد عبارة المفتاح (`importPhrase`) — المفتاح لا ينتقل عبر السحابة نفسها. جهاز جديد + Drive → إدخال كلمة المرور يدويًا (`cloud_pw_manual_btn`). `.nota` → ملف + كلمة فقط (قابل للنقل بالكامل).

---

## القسم 8 — Concurrency و State

1. **عمليات متزامنة:** ضغط "نسخ الآن" مرتين / Drive+Cloud معًا → **لا lock ولا قائمة انتظار** — لا `synchronized`/`Mutex`/`withLock`/`AtomicBoolean`/`isRunning` في مجلد `backup/` ولا `DriveBackup` (grep = صفر). انطلاقان متزامنان (يدوي + مجدول) قد يرفعان معًا.
2. **Background:** النسخ اليومي عبر **AlarmManager فقط (لا WorkManager)** — `BackupScheduler.schedule(ctx, freq)` (~سطر 38): `setAndAllowWhileIdle(RTC_WAKEUP, at, pending)`؛ الموعد 3:07 فجرًا التالي (أسبوعي +7 أيام — `nextRun` ~سطر 50). إعادة الجدولة أولًا دائمًا حتى لو تخطينا (`BackupReceiver.runBackup` ~سطر 90). إغلاق التطبيق أثناء النسخ → `goAsync()` + كوروتين IO؛ لا حفظ حالة جزئية ولا استئناف. إعادة التشغيل → `BootReceiver` (BOOT_COMPLETED + USER_UNLOCKED في `ReminderReceiver.kt` ~سطر 156 + manifest ~سطر 49-64 + إذن RECEIVE_BOOT_COMPLETED). **Progress notification: غير منفّذ** — إشعار **نتيجة فقط** (`notify("nota_backup_sched", 19001)` — `backup_sched_title/ok/fail/consent/noacct`) بلا `setProgress`.
3. **App Kill:** قُتل أثناء الرفع → فشل صامت حتى الموعد القادم (يُشعَر به فقط `backup_sched_fail` إذا وصل للـ catch). **لا recovery mechanism** — إعادة من الصفر. الحالة في DataStore (آخر/عدد/keep/freq) لا في الذاكرة — لكن لا حالة "جاٍرٍ" تُستأنف.

---

## القسم 9 — Failure Modes ورسائل الخطأ

| # | الرسالة (المفتاح/النص) | المُشغِّل | المسار | إمكانية الإصلاح |
|---|---|---|---|---|
| 1 | `cloud_failed` (عام Drive) | أي `err:<msg>` غير مصنف | `DriveCard` ~سطر 320-343 | إعادة المحاولة |
| 2 | `ncloud_err_quota` | `QUOTA_EXCEEDED` من الخادم | `NotaCloudViewModel.backupNow` ~سطر 109 | حذف/ترقية (قرار منتج) |
| 3 | `ncloud_err_nokey` / `ncloud_err_nokey_upload` (+ دليل `ncloud_nokey_guide`) | غياب المفتاح | `backupNow` ~سطر 82 / `restoreNow` | إنشاء/استيراد المفتاح (Pro فقط للبطاقة) |
| 4 | `فشل فك التشفير — تحقق من كلمة المرور` | GCM/كلمة غلط/عبث/اقتطاع | `BackupManager.decrypt` | إعادة إدخال — **تمييز مستحيل عمدًا (P3-0)** |
| 5 | `ملف النسخة تالف` / `bk_file_bad` | غلاف تالف | `import/decrypt` | ملف آخر |
| 6 | `err:drive-pw-wrong` / `cloud_drive_pw_wrong` | كلمة Drive خاطئة عند الفك اليدوي | `restoreWithPassword` ~سطر 246 | إعادة إدخال |
| 7 | `err:drive-pw-weak` / `nota_password_weak` | كلمة ضعيفة | `setDrivePassword` ~سطر 105 | كلمة أقوى/مقترحة (Diceware) |
| 8 | `err:verify-failed` / `cloud_verify_failed` | sha256 غير متطابق بعد الرفع | `DriveBackup.upload` ~سطر 120 (يحذف المرفوع) | إعادة المحاولة |
| 9 | `err:consent-denied` / `cloud_consent_denied` | رفض OAuth | `guardedUpload` | إعادة منح الإذن |
| 10 | `err:no-backup` / `cloud_no_backup` / `nc-err-nobackup` | لا نسخة (404 للسحابة = ليست خطأ) | `restoreNow` | نسخ أولًا |
| 11 | `nc-err:AUTH_EXPIRED` / `ncloud_err_auth` | 401 بعد تجديد واحد | `NotaCloudViewModel` | إعادة الدخول |
| 12 | `ok-backup-same` | البصمة مطابقة (تخطي المكرر) | `guardedUpload` | لا شيء |
| 13 | `bk_restored_fmt(added,skipped)` / `ncloud_ok_restore(a,b)` | نجاح دمج | `confirmRestore` | — |
| 14 | `bk_undo_fmt(n)` ("نُقلت %1$d ملاحظة للسلة — 30 يومًا") | تراجع | `undoRestore` | من السلة |

- تفاصيل الخطأ (stack/server message): تُعرض **عامة فقط** لـ Drive؛ لـ ncloud يوجد `ncDetail` يُعرض + زر نسخ `ncloud_copy` عبر `copySensitive` (~سطر 676-691). **زر "انسخ التفاصيل" للدعم: موجود جزئيًا لـ ncloud فقط.**
- تسجيل الأخطاء في ملف/تصديرها: غير منفّذ — لا file logging (logcat فقط ضمنيًا).
- فشل متكرر (3 مرات): **لا circuit breaker** في الكود.

---

## القسم 10 — الاستعادة (Restore)

1. **سياسة التعارض:** إضافة + تخطي — لا استبدال. الكشف: الثلاثي `(title, body, createdAt)` بالضبط (`NotesRepository.restoreMerge` ~سطر 310: `have = dao.backupAllOnce(acct()).map { Triple(...) }.toSet()`). **لا خيار للمستخدم** — قرار تلقائي. **لا سجل تعارضات** — فقط `RestoreResult(added, skipped, addedIds)` + سجل `restore_log` (تاريخ/مصدر/added/skipped).
2. **Undo:** `undoRestore(ids)` (~سطر 374): ينقل المضافة **للسلة** (`trashOne`) لا للحالة السابقة مباشرة. تبقى قابلة للاستعادة **30 يومًا** (TTL سجل الاستعادة + السلة). بعد 30 يومًا: تُسقط من السجل/السلة (منطق TTL في `SettingsStoreTest` — `filterRestoreLog`: إسقاط >30 يومًا، سقف 20).
3. **حدود الاستعادة:** حد أقصى لعدد المستعادة: غير منفّذ. استعادة جزئية (بعض الملاحظات): غير منفّذ — الكل أو لا شيء (مع تخطي المكرر). ذاكرة غير كافية: لا معالجة في الكود — يحتاج جهازًا.
4. **Trash:** السلة **تُستعاد سلةً** بتاريخ `deletedAt` الأصلي (محفوظ في النسخة). ملاحظة: `NoteDao.restore(id)` = استعادة عنصر واحد من السلة (`SET deletedAt=NULL`) وليست restore نسخة.

---

## القسم 11 — الحذف (Deletion)

1. **حذف نسخة واحدة:** **غير منفّذ** — لا UI ولا دالة `deleteBackup` (الحذف فقط: تدوير تلقائي عند تجاوز keep + حذف المرفوع الفاشل التحقق).
2. **حذف كل النسخ:** **غير منفّذ** — لا زر "حذف كل النسخ الاحتياطية" (لا Drive ولا Cloud ولا `.nota` ولا سجل الاستعادة).
3. **حذف الحساب:** `AuthViewModel` ~سطر 244-289: `requestDeletion()` (يسجل + `DeletionWorker.schedule`) → فترة سماح **14 يومًا** (`AccountDeletion.kt`: `GRACE_MS` + عبارة التأكيد `احذف حسابي`) → `performFirebaseDelete()` (`FirebaseAuth.currentUser.delete()`) + `executeLocalWipe()` (`factoryReset`). **النسخ السحابية لا تُحذف** — لا endpoint حذف server-side (`NotaCloudApi`: quota/blobs/pointers فقط)؛ الـ Worker "يوسم التنفيذ فقط... يُوثَّق كـ ينتظر محمد". Drive appdata: لا حذف في الكود. فترة السماح 14 يومًا: موجودة (للحساب المحلي/Firebase — لا للنسخ السحابية).
4. **Factory Reset:** يمسح محليًا فقط (`AuthViewModel` ~سطر 292-325 + تعليق ~سطر 291: "لا يُمس: نسخ Drive والسحابة (خارجية) — الحوار التحذيري يذكر ذلك صراحة"): DB + `clearAllLockData` + `clearDbPassphrase` + `settings.clearAll()` + `cloudKeys.clearAll()` + مفاتيح Keystore السبعة + ملفات `nota.db*` + مجلدات `note_media, note_bg, auto-backups` + `cacheDir/shared_tmp`. **مفتاح Cloud يُحذف (P0-3) والنسخ السحابية تبقى** (غير قابلة للفك بلا المفتاح ما لم توجد عبارة الاسترداد).

---

## القسم 12 — التفاعلات (Cross-cutting)

1. **حذف الحساب (P3-B2):** لا يُستدعى من شاشة النسخ — من `AuthViewModel` (إعدادات/حساب). Drive appdata **يبقى** (لا حذف في الكود).
2. **GDPR Export (P3-B1):** `GdprExportRunner` يجمع `notes.json` (شامل السلة بـ `deleted` flag) + مرفقات مفكوكة (يستبعد `*.key`) + `settings.json` (بعد sanitize تُسقط 8 مفاتيح حساسة) + `restore_log.json` (آخر 20 — **سجل الاستعادة مشمول**) → ZIP في `cacheDir/nota-export-<ts>.zip`. **النسخ السحابية (Drive/NotaCloud): غير مشمولة.**
3. **Factory Reset:** لا يحذف السحابية (تبقى)؛ **يحذف مفتاح Cloud** (موثق أعلاه).
4. **Auto Backup (Android):** `AndroidManifest.xml` ~سطر 23-24: `allowBackup="false"` + `dataExtractionRules="@xml/backup_rules"` — **لم يتغير**؛ `backup_rules.xml` يستبعد database/file/sharedpref/external/root في `cloud-backup` و`device-transfer` (حظر شامل). مسار تجاوز (device-to-device migration): غير مذكور في الكود. ملاحظة: `NotaApp.kt` ~سطر 12-20 يمسح بقايا `*.nota` من `cacheDir` عند كل تشغيل.
5. **Notifications:** إشعار **نتيجة فقط** للنسخة المجدولة (`backup_sched_ok/fail/consent/noacct` — قناة `backup_sched_channel`) — بلا `setProgress`. التعمية (SECRET/PRIVATE): غير مؤكد في الكود المقروء — يحتاج تحققًا (يُذكر مع "يحتاج جهازًا").

---

## القسم 13 — i18n

1. **الجرد:** مفاتيح `backup_*` = **10** في الافتراضي (22 لغة؟ لا — 6 ملفات): `values/` (الافتراضي = العربية) + `values-en/` + `values-fr/` + `values-it/` + `values-ja/` + `values-es/`. المفاتيح: `backup_title|backup_sub|backup_screen_title|backup_screen_sub|backup_sched_title|backup_sched_ok|backup_sched_fail|backup_sched_consent|backup_sched_noacct|backup_sched_channel`. الإجمالي: الافتراضي 1107 مفتاحًا، en 1107، fr/ja/it 904، **es 261 فقط** (ناقص 8 من العشرة — موجود فقط `backup_sub`, `backup_title`). اللغات المدعومة فعليًا: العربية (افتراضية) + en/fr/it/ja/es. **لا مجلد `values-ar/`** — العربية هي `values/`. اللغة الافتراضية لغير المدعومة: `values/` (العربية).
2. **جودة الترجمة:** نص عربي في الإنجليزي: **غير موجود** (فحص حرف-حرف — العشرة لاتينية خالصة، مثال `backup_sched_fail = "Scheduled backup failed — will retry next time"`). العكس **موجود**: `backup_screen_title = "Backup"` و`backup_screen_sub = "All backup and restore in one place: Google Drive and Nota Cloud."` — إنجليزي خام داخل الملف الافتراضي العربي (غير مترجمين).
3. **النصوص الطويلة:** الأطول `backup_sched_fail` (54 حرفًا: "تعذّر النسخ المجدول — سيُعاد المحاولة في الموعد القادم")؛ خلط اتجاهات (عربي + `Drive` لاتيني) في `ok/consent/noacct` — يعتمد `supportsRtl=true` (manifest ~سطر 25) + bidi.

---

## القسم 14 — Analytics/Telemetry

**لا يوجد أي تتبع.** الـ grep (`analytics|Telemetry|logEvent|funnel|conversion`) — كل النتائج opt-in واجهة فقط بلا إرسال: `SettingsStore.K_ANALYTICS_OPT` + `analyticsOptIn Flow` + `setAnalyticsOptIn` + Switch في `PrivacySection` (`privacy_analytics_title/sub`). **لا `FirebaseAnalytics` ولا `Crashlytics` ولا `logEvent` إطلاقًا** (صفر نتائج). اعتمادية Firebase الوحيدة: `firebase-bom:33.7.0` + `firebase-auth-ktx` (مصادقة فقط). لا conversion funnel ولا عدّ نسخ ناجحة/فاشلة ولا عدّ ترقيات. **متعمد أم نقص:** غير موثق في الكود — لا تعليق يوضح؛ التوافق مع "بلا تتبع" لخصوصية التطبيق **استنتاج ممنوع** — يُسجَّل كقرار منتج معلق (انظر القسم 16).

---

## القسم 15 — الاختبارات

1. **الموجود (14 ملف اختبار):** `BackupManagerTest` (envelope تشفير/فك + `IllegalStateException` الموحدة — Robolectric بسبب `android.util.Base64`)؛ `GdprExportTest` (3 اختبارات Robolectric: zip roundtrip + sanitizer + عتبة 500MB)؛ `SettingsStoreTest` (3 اختبارات JVM لمنطق TTL سجل الاستعادة بمرساة `1_700_000_000_000L`)؛ `AccountDeletionTest` (منطق الحذف النقي — بالاسم)؛ + `SecureDeleteTest/CryptoShredTest/OpaqueFileNamesTest/PerFileKeysTest/AuthManagerTest/RecoveryKeyTest/SelfMessageCryptoTest/PinHashTest/SettingsSearchTest/SecurityStatusTest`.
2. **المفقود (بلا اختبار):** `BackupScheduler.nextRun/schedule/cancel` + `BackupReceiver.runBackup` + `BootReceiver` + `DriveBackup.upload` + `CloudBackupViewModel.guardedUpload` + `GdprExportRunner.export` + `DeletionWorker.doWork/schedule` + `cloudFreq/cloudLast/cloudKeep` في SettingsStore + `deletionRequestedAt`.
3. **يحتاج جهازًا (لا يعمل على JVM):** `GdprExportRunner` (تعليق الملف نفسه) + `BackupReceiver/BootReceiver` (AlarmManager+DataStore+Keystore) + `DriveBackup` (شبكة+حساب Google) + `DeletionWorker` (WorkManager) + `factoryReset` (Keystore/DB/files) + OAuth حقيقي + Play Billing حقيقي + Biometric + ذاكرة غير كافية + Doze/3:07.

---

## القسم 16 — قرارات تنتظر محمد (أسئلة للمنتج — بلا إجابة من الكود)

```markdown
## قرار 1 — نموذج الاشتراك
- Free trial (اشتراك Play) موجود؟ كم يوم؟ [الكود: trial محلي 7 أيام فقط + stub billing]
- Pro شهري؟ سنوي؟ مرة واحدة؟ [الكود: منتجات nota_pro_monthly/yearly + nota_lifetime معرفة بلا تفعيل]
- ما السعر الحالي؟ [غير موجود في الكود]
- هل يوجد "Lifetime"؟ [المنتج معرف — التفعيل غير منفذ]
- هل توجد باقات تخزين؟ [منتجات nota_storage_5g/10g معرفة بلا استهلاك]

## قرار 2 — ما يُحتسب "Pro" في النسخ الاحتياطي
- Drive يومي مجاني؟ أم Pro فقط؟ [الكود الحالي: مجاني فعليًا — لا بوابة]
- Cloud Pro فقط؟ أم مجاني حتى حد معين؟ [الكود الحالي: النصي مجاني + الوسائط Pro]
- .nota مجاني أم Pro؟ [الكود الحالي: مجاني]
- المفتاح السحابي Pro؟ [الكود الحالي: Pro — البطاقة مخفية عن Free]
- سجل الاستعادة مجاني؟ [الكود الحالي: مجاني]

## قرار 3 — عند انتهاء Pro
- النسخ الموجودة تُمسح فورًا؟ أم تبقى؟ [الكود: تبقى — لا حذف]
- هل يوجد grace period (7-30 يوم)؟ [غير منفذ]
- يمكن استعادة نسخة Pro سابقة أثناء Free؟ [الكود: نعم تعمل]
- هل يمكن عرض النسخة السابقة أثناء Free (بلا استعادة)؟ [الكود: نفس الشاشة تعمل]

## قرار 4 — الوعد التسويقي
- ماذا يعرض وصف المتجر عن النسخ الاحتياطي؟ [خارج الكود — يُحتاج نص المتجر]
- هل يعرض "Drive/Cloud مجاني" أم "Pro"؟ [يُحتاج نص المتجر]
- ماذا يعرض لغير Pro؟ [يُحتاج نص المتجر]

## قرار 5 — خصوصية الـ backup
- هل النسخ السحابية E2EE أم تشفير client-side فقط؟ [الكود: client-side بالمفتاح المغلف + "السيرفر لا يرى المفتاح أبدًا" — بلا تدقيق خارجي]
- ما ادعاء التطبيق عن "لا نرى بياناتك"؟ [يُحتاج نص المتجر/السياسة]
- هل الـ backend يستطيع فك التشفير (بلا مفتاح)؟ [الكود لا يُظهر قدرة — التأكيد يحتاج مراجعة server-side]
```

---

## قسم إضافي — contradictions و"موجود لكن لا يعمل" و"يحتاج جهازًا"

### 1) "غير منفّذ — لا يوجد في الكود" (36 بندًا)
N1 معاينة نسخة سحابة نوتا قبل الدمج — N2 حوار تأكيد قبل الرفع — N3 حوار تأكيد مستقل قبل التنزيل (المعاينة تقوم مقامه جزئيًا) — N4 شيت/بانر باقات ثانٍ — N5 فحص Pro على Drive/`.nota`/السجل — N6 الإعدادات داخل النسخة — N7 حد أقصى للحجم — N8 resume/st استئناف الرفع — N9 حذف نسخة معينة من UI — N10 زر حذف كل النسخ — N11 progress notification — N12 قفل concurrency — N13 telemetry إرسال (opt-in بلا إرسال) — N14 معالجة refund/cancel — N15 عرض نتيجة الشراء في UI — N16 استدعاء queryPurchases/التحقق بعد الشراء — N17 استئناف بعد انتهاء الجلسة mid-upload — N18 rotation مفتاح السحابة — N19 شمول GDPR للنسخ السحابية — N20 حذف server-side للنسخ عند حذف الحساب (لا endpoint) — N21 استرداد كلمة Drive — N22 استعادة جزئية — N23 سجل تعارضات — N24 خيار المستخدم عند التعارض — N25 حد أقصى للمستعادة — N26 grace period لانتهاء Pro — N27 تنبيه قبل downgrade — N28 حذف تلقائي عند انتهاء Pro — N29 circuit breaker — N30 تسجيل الأخطاء في ملف/تصديرها — N31 Play Billing فعلي (stub) — N32 Firebase Remote Config — N33 تأكيد حذف نسخة Drive عند التدوير — N34 حد Drive quota مخصص — N35 migration بين الصيغ — N36 مسار تجاوز allowBackup=false.

### 2) "موجود لكن لا يعمل كما يوحي الاسم" (6 بنود)
- M1: بوابة `locked` على بطاقات Drive/`.nota` — تُسمَّى قفل Pro لكن لا علاقة لها بـ `ncPro` داخل هذه البطاقات (التعطيل شكلي عبر `locked` الممرَّر).
- M2: أيقونة القفل في `FreqOption` — شكلية (الخيار يعمل للكل).
- M3: `purchase()` بعد النجاح ينادي `refresh()` غير force — قد يُرجع الكاش الطازج (FREE) فور الدفع.
- M4: `TRIAL_DISCOUNT_PCT=50` ("يُفعَّل من عروض Play") — نص بلا تكامل.
- M5: `quota enforcement` للسحابة server-side حقيقي (`QUOTA_EXCEEDED`) — مقابل غياب أي حد/فرض لـ Drive (تعتمد حصة Drive العامة).
- M6: زر نسخ تفاصيل الخطأ — موجود لـ ncloud فقط (`ncloud_copy`)، غائب لـ Drive.

### 3) "يحتاج جهازًا" (20 بندًا)
H1 المدة الفعلية لمعرفة انتهاء Pro (نظريًا ≤24h) — H2 Pro على جهاز آخر قديم — H3 Pro offline — H4 Refund فعلي — H5 OAuth/تجديد التوكن/فشل التجديد — H6 Play Billing حقيقي — H7 انقطاع منتصف الرفع (Drive) — H8 Biometric/device-cred — H9 `GdprExportRunner` — H10 `BackupReceiver/BootReceiver` — H11 `DriveBackup` شبكة — H12 `DeletionWorker` — H13 `factoryReset` — H14 ذاكرة غير كافية — H15 Drive quota ممتلئ فعليًا — H16 اكتشاف حذف الحساب من Drive — H17 انتهاء الجلسة mid-upload (سحابة) — H18 دقة موعد 3:07 مع Doze — H19 تعمية الإشعار SECRET/PRIVATE — H20 سلوك trial الـ 7 أيام فعليًا.

### 4) تناقضات (10 بنود)
- C1: `MAX_CLOUD_KEEP` — قيمتان ظاهرتان (5 في `DriveBackup.upload(json, keep=5)` مقابل 7 كحد أعلى في SettingsStore) — يحتاج قراءة توحيدية قبل التطوير.
- C2: إزالة أغلفة NOTA1/2/3 نهائيًا مقابل قبول JSON v2 داخليًا — تمييز غلاف/محتوى موثق لكنه مصدر التباس.
- C3: `refresh()` غير force بعد شراء ناجح (M3) — قد يُظهر FREE فور الدفع.
- C4: `_lastResult` يُخزَّن ولا يُعرض في أي شاشة.
- C5: `backup_screen_title/sub` إنجليزي خام داخل `values/` العربي.
- C6: `values-es/` ناقص 8 من 10 مفاتيح `backup_*`.
- C7: `GdprExportTest` يختبر 8 مفاتيح sanitizer بينما القائمة الفعلية 10 (`drive_pw_next`, `cloud_backup_hash` غير مغطاة).
- C8: تعليق "السيرفر لا يرى المفتاح أبدًا" — ادعاء معماري بلا تدقيق server-side مستقل (يُعامل كـ "يحتاج تحقق" لا كحقيقة).
- C9: `safetySnapshot` (آخر 3 في `auto-backups`) تُمسح في `factoryReset` — موثق في الكود لكنه يفاجئ من يظنها "نسخة أمان دائمة".
- C10: معاينة القدم تستخدم `MAX(updatedAt)` — نسخة أقدم من آخر تعديل تُحذَّر حتى لو البيانات متطابقة (تحذير محافظ قد يُربك).

---

## الخلاصة الإحصائية
- المهارة المطبقة: **code-reviewer** (نطاق → فحص → سياق → تقرير).
- الأقسام المكتملة: **16 + القسم الإضافي (كاملة)**.
- بنود "غير منفّذ": **36**.
- بنود "يحتاج جهازًا": **20**.
- تناقضات + "موجود لكن لا يعمل": **10 + 6**.
- أهم 5 نتائج:
  1. Pro الحقيقي من السيرفر (`mohamedadel.cfd/entitlements`) بكاش 24h وافتراضي FREE؛ وPlay Billing **stub غير مربوط** — أي ترقية من المتجر غير فعالة في الكود الحالي.
  2. صيغة واحدة للكل: غلاف `NOTA` نصي (PW: PBKDF2-600k + AES-GCM-256 / KS: Keystore) وداخله JSON v3؛ السلة مضمنة، والإعدادات والمرفقات/الخلفيات **غير مضمنة**.
  3. الدمج إضافة+تخطي بمفتاح `(title,body,createdAt)` بلا id/hash وبلا خيار للمستخدم؛ والتراجع ينقل للسلة + `safetySnapshot` (آخر 3).
  4. Drive: scope appdata فقط + REST بلا resume + احتفاظ 1-7 + بلا قفل تزامن + بلا تقدم؛ السحابة: فرض حصة server-side حقيقي + بلا استئناف + بلا حذف server-side عند حذف الحساب.
  5. **صفر telemetry** (opt-in بلا إرسال)؛ i18n: العربية افتراضية بلا `values-ar` + عنوانا الشاشة إنجليزيان خام + الإسبانية ناقصة 8 مفاتيح.
