# جرد subsystem الاستحقاق (Entitlement) — تطبيق نوتا

> المنهج: مهارة `code-reviewer` (تحديد النطاق → قراءة فعلية → جرد بلا توصيات). كل ما يلي مقروء من الكود فعليًا — لا افتراضات.
> النطاق: `app/src/main/java/com/nota/notes/billing/` + `PremiumViewModel` + `PaywallScreen` + كل مواضع البوابة في `App.kt` والشاشات. لا تكرار لما في `nota-backup-audit.md` إلا بقدر التماس المباشر.
> تاريخ الجرد: 20 أكتوبر 2026. الفرع والملفات كما في `projects/nota`.

## جدول المحتويات

1. [القسم 1 — نموذج البيانات](#القسم-1--نموذج-البيانات-data-model)
2. [القسم 2 — عقد السيرفر](#القسم-2--عقد-السيرفر-server-contract)
3. [القسم 3 — آلة الحالة](#القسم-3--آلة-الحالة-state-machine)
4. [القسم 4 — سطح البوابة](#القسم-4--سطح-البوابة-feature-gating-surface)
5. [القسم 5 — مسار الشراء الحالي](#القسم-5--مسار-الشراء-الحالي-stub)
6. [القسم 6 — مسار الشراء المقصود](#القسم-6--مسار-الشراء-المقصود-عندما-يُربط-play-billing)
7. [القسم 7 — التجربة](#القسم-7--التجربة-trial)
8. [القسم 8 — Cross-Device والاستحقاق](#القسم-8--cross-device-والاستحقاق)
9. [القسم 9 — حالات الفشل](#القسم-9--حالات-الفشل-failure-modes)
10. [القسم 10 — ما ينقص لتحقيق "مدفوع حقيقي"](#القسم-10--ما-ينقص-لتحقيق-مدفوع-حقيقي)
11. [القسم 11 — تفاعل Entitlement مع النسخ الاحتياطي](#القسم-11--تفاعل-entitlement-مع-النسخ-الاحتياطي-لتصميم-لاحق)
12. [القسم 12 — قرارات منتج تنتظر محمد](#القسم-12--قرارات-منتج-تنتظر-محمد-أسئلة-محددة)
13. [قسم إضافي — موجود لكن لا يعمل كما يوحي الاسم](#قسم-إضافي--موجود-لكن-لا-يعمل-كما-يوحي-الاسم)
14. [قسم إضافي — يحتاج جهازًا](#قسم-إضافي--يحتاج-جهازًا)
15. [قسم إضافي — تناقضات](#قسم-إضافي--تناقضات)
16. [الجرد الرباعي](#الجرد-الرباعي)

---

## القسم 1 — نموذج البيانات (Data Model)

### 1أ — `Entitlement` (الكائن)

- **يُعرَّف في:** `app/src/main/java/com/nota/notes/billing/Entitlements.kt` — السطور 1–26.
- **النوع:** `data class` — ليست `sealed class` ولا `enum`.
- **الحقول:** ثلاثة فقط — `pro: Boolean` (افتراضي `false`)، `features: List<String>` (افتراضي فارغة)، `expiresAt: Long?` (افتراضي `null`).
- **لا يوجد:** `tier`، `quota`، `maxDevices`، `trial`، `source` (play/server/manual). الاستحقاق بُعد واحد (`pro`) + قائمة مزايا اسمية + انتهاء اختياري.
- **النقل الحرفي:**

```kotlin
data class Entitlement(
    val pro: Boolean = false,
    val features: List<String> = emptyList(),
    val expiresAt: Long? = null
) {
    companion object {
        const val FEATURE_FOLDERS = "folders"
        const val FEATURE_LOCATION = "location"
        // كل مزايا ما بعد إغلاق المجانية (قرار 20 سبتمبر 2026): مدفوعة — تُفتح بـ pro من السيرفر
        const val FEATURE_TAGS = "tags"
        const val FEATURE_REMINDERS = "reminders"
        const val FEATURE_PDF = "pdf"
        const val FEATURE_LOCK = "lock"
        const val FEATURE_FAVORITES = "favorites"
        const val FEATURE_PIN = "pin"
        const val FEATURE_WIDGET = "widget"
        const val FEATURE_CLOUD = "cloud"
        const val FEATURE_BACKGROUNDS = "backgrounds"
        const val FEATURE_MEDIA = "media"
        const val FEATURE_SHRED = "shred"
        val FREE = Entitlement(pro = false)
    }
}
```

- **الدوال الحرة في نفس الملف (سطور 28–41) — تُنقل حرفيًا:**

```kotlin
// أسماء المزايا المعروفة — أولها المجلدات
fun hasFeature(features: List<String>, name: String): Boolean = features.contains(name)

// واجهة سؤال الاستحقاق — التطبيق يسأل GET /v1/entitlements (سيرفر VPS لاحقًا)
interface EntitlementsApi {
    suspend fun getEntitlements(accountId: String): Entitlement
}

// بوابة PRO: السيرفر يرد pro:true لأي حساب له صف استحقاق ساري (isProRow) — المدفوعة كلها تُفتح به
fun isPro(e: Entitlement?): Boolean = e?.pro == true

fun canUse(e: Entitlement?, name: String): Boolean =
    e?.pro == true || hasFeature(e?.features ?: emptyList(), name)
```

- ملاحظة: الدالة الحرة `canUse(e, name)` **غير مستدعاة في أي مكان** (البوابات تستخدم `PremiumViewModel.isPro()` / `canUseFolders()` / `canUseLocation()` أو فحصًا مباشرًا في `App.kt`). موجودة لكن لا تعمل كمسار فعلي.

### 1ب — `PremiumViewModel`

- **الملف:** `app/src/main/java/com/nota/notes/ui/PremiumViewModel.kt` — 102 سطرًا (يُنقل كاملًا أدناه).
- **`StateFlow` المكشوفة وأنواعها:**

| الـ Flow | النوع | الغرض |
|---|---|---|
| `entitlement` | `StateFlow<Entitlement?>` | آخر استحقاق معروف (`null` = لم يُسأل بعد) |
| `buying` | `StateFlow<Boolean>` | شراء جارٍ |
| `refreshing` | `StateFlow<Boolean>` | تحديث force جارٍ |
| `authExpired` | `StateFlow<Boolean>` | الجلسة منتهية — تُعرض طلب إعادة الدخول |
| `lastResult` | `StateFlow<BillingResult?>` | نتيجة آخر شراء |
| `trialActive` | `StateFlow<Boolean>` | التجربة نشطة (تُغذَّى من `App.kt` عبر `setTrialActive`) |

- **`_lastResult`:** نوعه `BillingResult?`، يُحدَّث في `purchase()` فقط (`_lastResult.value = r`)، و**لا يُقرأ في أي Composable أو ViewModel آخر** — يُخزَّن ولا يُعرض (انظر القسم الإضافي M4/C4).
- **التفاعل مع `EntitlementsRepo`:** عبر `repo.refresh()` في `refresh()` و`purchase()`، و`repo.refresh(force = true)` في `forceRefresh()`. لا يستدعي `repo.hasFeature(name)` إطلاقًا.
- **النقل الحرفي (كاملًا — 102 سطرًا):**

```kotlin
package com.nota.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nota.notes.billing.AuthExpiredException
import com.nota.notes.billing.BillingGateway
import com.nota.notes.billing.BillingResult
import com.nota.notes.billing.Entitlement
import com.nota.notes.billing.EntitlementsRepo
import com.nota.notes.billing.hasFeature
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// حالة الترقية للـ Paywall — بلا runBlocking، كل شيء معلّق
@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repo: EntitlementsRepo,
    private val gateway: BillingGateway
) : ViewModel() {
    private val _entitlement = MutableStateFlow<Entitlement?>(null)
    val entitlement: StateFlow<Entitlement?> = _entitlement.asStateFlow()

    private val _buying = MutableStateFlow(false)
    val buying: StateFlow<Boolean> = _buying.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    // الجلسة منتهية على الجهاز: تُعرض الواجهة طلب إعادة الدخول بدل السقوط الصامت للـ FREE
    private val _authExpired = MutableStateFlow(false)
    val authExpired: StateFlow<Boolean> = _authExpired.asStateFlow()

    private val _lastResult = MutableStateFlow<BillingResult?>(null)
    val lastResult: StateFlow<BillingResult?> = _lastResult.asStateFlow()

    // التجربة المجانية (27 سبتمبر 2026): تُغذَّى من الإعدادات — تفتح كل البوابات كـ Pro حتى انتهائها
    private val _trialActive = MutableStateFlow(false)
    val trialActive: StateFlow<Boolean> = _trialActive.asStateFlow()
    fun setTrialActive(v: Boolean) { _trialActive.value = v }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.refresh() }
                .onSuccess { _entitlement.value = it; _authExpired.value = false }
                .onFailure { e ->
                    if (e is AuthExpiredException) _authExpired.value = true
                    else _entitlement.value = Entitlement.FREE
                }
        }
    }

    // تحديث الحالة من السيرفر متجاهلًا الكاش — بعد الدفع يحتاج المشتري هذا طبيعيًا.
    // عند الفشل يُبقَى آخر استحقاق معروف (لا يُسقَط للـ FREE ظلمًا في الأوفلاين).
    fun forceRefresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            runCatching { repo.refresh(force = true) }
                .onSuccess { _entitlement.value = it; _authExpired.value = false }
                .onFailure { e -> if (e is AuthExpiredException) _authExpired.value = true }
            _refreshing.value = false
        }
    }

    fun canUseFolders(): Boolean {
        if (_trialActive.value) return true
        val e = _entitlement.value ?: return false
        return e.pro || hasFeature(e.features, Entitlement.FEATURE_FOLDERS)
    }

    // بوابة PRO العامة: كل مزايا ما بعد إغلاق المجانية (وسوم/تذكير/PDF/قفل خاص/مفضلة/تثبيت/ويدجت/سحابة/خلفيات/وسائط)
    fun isPro(): Boolean = _trialActive.value || _entitlement.value?.pro == true

    // الموقع المرفق ميزة مدفوعة — pro أو علم location من السيرفر
    fun canUseLocation(): Boolean {
        if (_trialActive.value) return true
        val e = _entitlement.value ?: return false
        return e.pro || hasFeature(e.features, Entitlement.FEATURE_LOCATION)
    }

    fun purchase(productId: String) {
        if (_buying.value) return
        viewModelScope.launch {
            _buying.value = true
            val r = runCatching { gateway.launchPurchase(productId) }
                .getOrElse { BillingResult.Failed(it.message ?: "error") }
            _lastResult.value = r
            if (r is BillingResult.Success) {
                runCatching { repo.refresh() }.onSuccess { _entitlement.value = it }
            }
            _buying.value = false
        }
    }
}
```

### 1ج — `EntitlementsRepo`

- **الملف:** `app/src/main/java/com/nota/notes/billing/EntitlementsRepo.kt` — 102 سطرًا.
- **الدوال العامة:** `cached: Flow<Entitlement>`، `refresh(force: Boolean = false)`، `hasFeature(name: String)`. **لا توجد دالة `forceRefresh()` في الـ Repo** — الـ force تمرَّر كمعامل لـ `refresh()`، ودالة `forceRefresh()` موجودة في `PremiumViewModel` فقط وتستدعي `repo.refresh(force = true)`.
- **`refresh()`:** تقرأ الكاش أولًا — إن كان الكاش لنفس الحساب وطازجًا (`isFresh`) يُعاد فورًا بلا شبكة. وإلا: `ensureRegistered` ثم `getEntitlements` ثم حفظ الكاش.
- **`CACHE_TTL_MS`:** `24L * 60L * 60L * 1000L` (24 ساعة بالضبط) — في `companion object` بنفس الملف.
- **مفتاح الكاش:** `K_ACCOUNT` (معرف الحساب) + `K_PRO` + `K_FEATURES` (CSV) + `K_EXPIRES` + `K_CACHED_AT` في DataStore باسم `entitlements_cache`. عند تغيّر `accountId` يُسقط الكاش (`cached` تعيد `FREE`، و`refresh` تتجاهل الطزاجة لأن `store[K_ACCOUNT] != accountId`).
- **النقل الحرفي لـ `refresh()` و`hasFeature()` (لا توجد `forceRefresh` في هذا الملف):**

```kotlin
    // تحديث من السيرفر: كاش طازج يُعاد فورًا (ما لم يُطلب force)، وإلا سؤال جديد ثم حفظ.
    // شفاء ذاتي: قبل أي سؤال شبكة يُضمَن التسجيل أولًا (idempotent) فيُنشَأ أي حساب مفقود.
    suspend fun refresh(force: Boolean = false): Entitlement = withContext(Dispatchers.IO) {
        val accountId = accounts.currentAccountId() ?: return@withContext Entitlement.FREE
        val store = ctx.entitlementsCache.data.first()
        val cachedAt = store[K_CACHED_AT] ?: 0L
        // الطزاجة لا تُحتسب إلا لنفس الحساب — كاش حساب آخر كأنه غير موجود
        if (!force && store[K_ACCOUNT] == accountId && isFresh(cachedAt)) {
            return@withContext Entitlement(
                pro = store[K_PRO] ?: false,
                features = store[K_FEATURES]?.split(",")?.filter { f -> f.isNotBlank() } ?: emptyList(),
                expiresAt = store[K_EXPIRES]?.takeIf { v -> v > 0L }
            )
        }
        if (registrar.ensureRegistered(accountId)) {
            accounts.setPendingRegister(false)
        }
        val fresh = try {
            api.getEntitlements(accountId)
        } catch (e: AuthExpiredException) {
            // الجلسة منتهية: تُرفع للواجهة لتطلب إعادة الدخول — لا تُدفن في الكاش
            throw e
        } catch (e: Exception) {
            // أوفلاين: آخر كاش معروف بدل الفشل
            return@withContext Entitlement(
                pro = store[K_PRO] ?: false,
                features = store[K_FEATURES]?.split(",")?.filter { f -> f.isNotBlank() } ?: emptyList(),
                expiresAt = store[K_EXPIRES]?.takeIf { v -> v > 0L }
            )
        }
        ctx.entitlementsCache.edit {
            it[K_ACCOUNT] = accountId
            it[K_PRO] = fresh.pro
            it[K_FEATURES] = fresh.features.joinToString(",")
            if (fresh.expiresAt == null) it.remove(K_EXPIRES) else it[K_EXPIRES] = fresh.expiresAt
            it[K_CACHED_AT] = System.currentTimeMillis()
        }
        fresh
    }

    suspend fun hasFeature(name: String): Boolean = withContext(Dispatchers.IO) {
        val current = try {
            refresh()
        } catch (e: Exception) {
            cached.first()
        }
        hasFeature(current.features, name) || (current.pro && name == Entitlement.FEATURE_FOLDERS)
    }
```

- ملاحظة جردية: `repo.hasFeature` **غير مستدعاة في أي مكان** (البوابات تستخدم `PremiumViewModel.canUseFolders()`). وفيها تخصيص غريب: `pro` وحده لا يكفي إلا إذا كان `name == FEATURE_FOLDERS` — أي `hasFeature("location")` على حساب `pro:true` بلا علم location **تعيد false** هنا، بينما `PremiumViewModel.canUseLocation()` تعيد true. (انظر التناقضات.)

### 1د — `trialOn`

- **كيف يُحسب:** في `App.kt` سطر 417: `val trialOn = SettingsStore.isTrialActive(trialStartTop, trialNow)` — و`isTrialActive` في `SettingsStore.kt` سطور 101–102: `startMs > 0L && nowMs < startMs + trialDurationMs()`.
- **من `trial_start_ms`:** نعم — `K_TRIAL_START` (`trial_start_ms`) في `SettingsStore` (سطر 48)، يُقرأ كـ Flow `trialStart`، ويُكتب في `startTrial()` (سطر 283): `trial_start_ms = now + trial_offered = true`.
- **ماذا يمنح فعليًا:** **كل شيء.** `PremiumViewModel.isPro()` = `trialActive || pro`، و`canUseFolders()` و`canUseLocation()` تعيدان `true` فورًا عند `trialActive`. و`App.kt` سطر 420: `isPro = entitlement?.pro == true || trialOn` — فكل بوابة `isPro` مفتوحة أثناء التجربة. **يمنح ميزات Pro كاملة — لا يقتصر على خصم.**
- **التفاعل مع `pro`:** OR كامل — `trialOn` و`pro` يُجمعان بـ `||` في كل موضع (البوابة العليا سطر 420 + داخل الـ ViewModel). ليسا مستقلَّين.
- **المدة:** `TRIAL_DAYS = 7` (`SettingsStore.kt` سطر 98)، `trialDurationMs() = 7 * 24 * 3600 * 1000`. تُعاد كل دقيقة (`trialTick` + `delay(60_000)` في `App.kt` سطور 410–416) لالتقاط الانتهاء.
- **إعادة التثبيت:** **تُصفَّر.** `trial_start_ms` في DataStore محلي (`settingsStore`) بلا نسخ سحابي — مسح البيانات/إعادة التثبيت يعيدها `0` (لم تبدأ)، لكن `trial_offered` يُصفَّر أيضًا فيُعرض العرض مجددًا. غير واضح في الكود أي حماية ضد إعادة التثبيت المتكرر — **لا يوجد ربط سيرفري للتجربة إطلاقًا.**
- **`TRIAL_DISCOUNT_PCT = 50`:** **نص فقط.** يُمرَّر كمعامل افتراضي `discountPct` في `PaywallScreen` (سطر `discountPct: Int = SettingsStore.TRIAL_DISCOUNT_PCT`) ويُعرض في سطر `trial_discount_note` ("خصم 50% على الباقة السنوية"). **لا يوجد أي تكامل مع عروض Play** (التعليق نفسه يقول "يُفعَّل من عروض متجر Play" — أي مستقبلًا). لا يُطبَّق سعريًا في أي كود.

---

## القسم 2 — عقد السيرفر (Server Contract)

### 2أ — الـ endpoints المستخدمة

| # | الطريقة | المسار الكامل | الملف | توقيع الدالة | Headers | Body |
|---|---|---|---|---|---|---|
| 1 | GET | `https://mohamedadel.cfd/nota-api/v1/entitlements?accountId=` | `HttpEntitlementsApi.kt` سطر 24 | `suspend fun getEntitlements(accountId: String): Entitlement` | `Accept: application/json` + `Authorization: Bearer <idToken>` (إن وُجد) | لا يوجد |
| 2 | POST | `https://mohamedadel.cfd/nota-api/v1/users/register` | `ServerRegister.kt` سطر 19 | `suspend fun ensureRegistered(accountId: String): Boolean` | `Content-Type: application/json` + `Authorization: Bearer <idToken>` (إن وُجد) | `{"accountId": "..."}` فقط |
| 3 | GET/PUT | `.../v1/cloud/quota`, `/v1/cloud/blobs/{sha}`, `/v1/cloud/pointers/{name}` | `NotaCloudApi.kt` | `getQuota`, `uploadBlob`, `downloadBlob`, `setPointer`, `getPointer` | نفس نمط التوكن | حسب العملية |

- **لا يوجد** في الكود أي `POST /v1/purchases` أو endpoint للتحقق من الإيصالات أو webhook لـ RTDN. (انظر القسم 6.)
- **النقل الحرفي لتوقيعَي الدالتين** مذكور أعلاه (جدول) + الكود الكامل في القسم 1 (لـ `HttpEntitlementsApi`) وأدناه لـ `ServerRegister`:

```kotlin
suspend fun ensureRegistered(accountId: String): Boolean = withContext(Dispatchers.IO) {
    var token = tokens.current()
    repeat(2) { attempt ->
        try {
            val url = URL(REGISTER_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            val body = JSONObject().apply { put("accountId", accountId) }.toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            conn.disconnect()
            if (code == 401 && attempt == 0) {
                token = tokens.refresh()
                if (!token.isNullOrBlank()) return@repeat
            }
            if (code in 200..299) return@withContext true
        } catch (e: Exception) {
            Log.w(TAG, "ensureRegistered failed: ${e.message}")
        }
    }
    false
}
```

### 2ب — شكل الاستجابة (Response Schema)

- من الكود (`HttpEntitlementsApi.parse` — سطور 59–78): تُقرأ **ثلاثة حقول فقط**: `pro` عبر `obj.optBoolean("pro", false)`، و`features` عبر `optJSONArray("features")` (سلسلة نصوص)، و`expiresAt` (رقم millis أو نص ISO-8601 عبر `Instant.parse` أو `null`).
- **لا يوجد** `meta` ولا `_links` ولا `tier` ولا `quota` ولا `storage` في كود القراءة.
- **كود السيرفر غير موجود في المستودع** — "not in repository" (لا يوجد مجلد `nota-api`؛ لا يوجد أي كود سيرفر). فالـ schema الفعلي للسيرفر **غير واضح في الكود** beyond ما يقرأه الـ parser أعلاه. مثال استجابة من الكود: غير موجود كنص ثابت — الـ parser هو المرجع الوحيد.
- النقل الحرفي لـ `parse`:

```kotlin
private fun parse(body: String): Entitlement {
    val obj = JSONObject(body)
    val pro = obj.optBoolean("pro", false)
    val features = mutableListOf<String>()
    val arr = obj.optJSONArray("features")
    if (arr != null) {
        for (i in 0 until arr.length) {
            val f = arr.optString(i, "")
            if (f.isNotBlank()) features.add(f)
        }
    }
    // السيرفر يرد expiresAt نص ISO أو null (ورقم millis إن تغيّر لاحقًا)
    val raw = obj.opt("expiresAt")
    val expiresAt: Long? = when {
        raw is Number -> raw.toLong().takeIf { v -> v > 0L }
        raw is String && raw.isNotBlank() -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull()
        else -> null
    }
    return Entitlement(pro = pro, features = features, expiresAt = expiresAt)
}
```

### 2ج — الأخطاء (Error Responses)

| كود HTTP | كيف يترجمه التطبيق |
|---|---|
| 200–299 | parse عادي |
| 401 (المحاولة 1) | تجديد التوكن (`tokens.refresh()`) وإعادة المحاولة مرة واحدة |
| 401 (بعد التجديد أو بلا توكن) | `throw AuthExpiredException()` — تُرفع للواجهة لتطلب إعادة الدخول، لا تُدفن في الكاش |
| غير 2xx آخر | `throw IOException("entitlements HTTP $code")` — يلتقطها `refresh()` كأوفلاين/فشل → آخر كاش معروف |
| JSON خاطئ | `Log.w` + `throw IOException("entitlements parse failed")` → نفس مسار الكاش |

- **`AuthExpiredException`:** في `billing/AuthExpiredException.kt` — `class AuthExpiredException : IOException("auth session expired")`. تُرفع عند: 401 متكرر في `HttpEntitlementsApi`، أو غياب جلسة Firebase على الجهاز (التجديد `null`).

### 2د — `ServerRegister.ensureRegistered`

- **ماذا يفعل:** POST `{accountId}` + توكن Firebase إلى `/v1/users/register` (إيدمبوتنت — يُعاد المحاولة عند 401 بعد تجديد التوكن؛ أي استثناء شبكة يُبتلع كـ `false`).
- **الأين:** `billing/ServerRegister.kt` سطر 19 (الكود الكامل منقول في 2أ).
- **متى يُستدعى:** (1) عند الدخول (`GoogleAuthManager.registerServer` → `ensureRegistered`، سطر ~138)؛ (2) داخل `EntitlementsRepo.refresh()` قبل كل سؤال شبكة (شفاء ذاتي)؛ (3) `retryPendingRegister()` عند بدء التطبيق إن كان `pendingRegister`.
- **ماذا يُرسل:** `{accountId}` فقط — التعليق يؤكد "أي حقل زائد يرفضه السيرفر". **لا يُرسل اسم جهاز ولا device ID ولا entitlement** — فقط المعرف + توكن المصادقة في الـ header.
- **لو فشل:** يعيد `false`؛ في `refresh()` يُتجاهل الفشل ويُكمل لسؤال الاستحقاق؛ عند الدخول يُسجَّل `pendingRegister` لإعادة المحاولة لاحقًا.

### 2إ — `accountId`

- **كيف يُولَّد:** **Firebase UID** — يُضبط في `AccountStore.set(accountId, ...)` عند الدخول عبر Google/Firebase (`GoogleAuthManager`؛ التعليق: "accountId(uid)"). ليس UUID مولّدًا محليًا.
- **كيف يُخزَّن:** DataStore باسم `nota_account` (`AccountStore.kt`) — مفاتيح `account_id` + `email` + `id_token` (مختوم عبر `TokenVault`) + `last_sign_in` + `pending_register`.
- **بين الأجهزة:** **ثابت لنفس حساب Google** (UID من Firebase) — جهاز جديد بنفس الحساب = نفس `accountId` = نفس الاستحقاق السيرفري. حساب آخر = `accountId` مختلف = كاش القديم يُسقط.

---

## القسم 3 — آلة الحالة (State Machine)

| الحالة | الشرط (كيف تُكتشف؟) | ما يفتحه | ما يمنعه |
|---|---|---|---|
| **free** | `pro == false && !trialOn` (الحالة الافتراضية؛ `Entitlement.FREE`) | النص الأساسي، Drive/ملف `.nota`/سجل الاستعادة (غير مفحوصة — انظر قسم 4) | كل `isPro` (تثبيت/قفل/مفضلة/نسخ/تكرار/نقل لمجلد/ثيمات مدفوعة/تخصيص شريط/مجلدات/موقع/سحابة مقفلة) |
| **trial** | `trialOn == true` (محلي: `trial_start_ms > 0 && now < start + 7d`) | **كل شيء كـ Pro** (OR مع كل بوابة) + بانر المتبقي في Paywall | لا شيء (كل البوابات مفتوحة) |
| **pro** | `pro == true` من السيرفر (كاش أو حي) | كل ما سبق + بطاقة مفتاح السحابة + رفع الوسائط (`ncPro`) | لا شيء |
| **pro expired** | السيرفر يرد `pro:false` بعد انتهاء الاشتراك — يُكتشف خلال ≤24h (TTL الكاش) أو فورًا عبر `forceRefresh` اليدوي | يسقط إلى free (تدريجيًا حسب قدم الكاش) | نفس ممنوعات free بعد سقوط الكاش |
| **cached free** | `accountId == null` (بلا حساب) أو كاش قديم لحساب آخر | `Entitlement.FREE` دائمًا بلا شبكة | كل Pro |
| **offline** | أي استثناء شبكة في `refresh()` | آخر كاش معروف (قد يكون pro قديمًا حتى 24h+ — لا حد أعلى للقدم في مسار الخطأ) | لا شيء جديد — تجميد الحالة |
| **deleted account** | `signOut()` → `accounts.clear()` | الكاش يُسقط (`cached` تعيد FREE لأن `acct == null`)؛ الملاحظات المحلية تبقى | كل Pro فورًا |
| **refunded** | **غير منفّذ — لا يوجد في الكود.** لا RTDN ولا فحص إلغاء؛ يُكتشف فقط عندما يقلب السيرفر `pro` إلى false (خلال ≤24h) |
| **auth expired** | 401 مزدوج أو بلا جلسة Firebase | `authExpired = true` → واجهة تطلب إعادة الدخول (Paywall/Account) بدل السقوط الصامت | لا سقوط لـ FREE ظلمًا (تصميم مقصود — درس 27 سبتمبر 2026) |

**أسئلة إضافية:**
- **انتهاء trial (اليوم 8):** لا إشعار ولا حوار — `trialOn` تصبح `false` في إعادة التقييم الدقيقة التالية (`trialTick` كل 60 ثانية)، فتسقط كل البوابات silently إلى `pro`-فقط. لا كود تنبيه عند الانتهاء.
- **انتهاء Pro السيرفري:** التطبيق يعرف خلال ≤24 ساعة كحد أقصى (TTL الكاش) — أو فورًا بزر "تحديث الحالة" اليدوي (`forceRefresh` في Paywall/Account). لا polling تلقائي (انظر H24).
- **رفض OAuth أو حذف الحساب مع كاش Pro:** `signOut` يمسح `accountStore` → `cached` تعيد FREE فورًا. لكن **`entitlements_cache` DataStore لا يُمسح** — القيم القديمة تبقى على القرص لكن لا تُقرأ (النطاق مربوط بـ `K_ACCOUNT`). حساب جديد ≠ كاش قديم.
- **grace period:** **غير منفّذ — لا يوجد في الكود.** لا يوجد أي منطق مهلة بعد انتهاء الاشتراك.

---

## القسم 4 — سطح البوابة (Feature Gating Surface)

**المصدر الموحّد:** `App.kt` سطور 406–420 — `entitlementTop` (Flow واحد) + `trialOn` → `isPro = entitlementTop?.pro == true || trialOn`. كل الشاشات تستقبل `isPro` جاهزًا كمعامل — **لا شاشة تعيد السؤال بنفسها** (باستثناء `BackupScreen` التي تقرأ `premiumVm.entitlement` مرة ثانية لـ `ncPro` — انظر التناقض T4).

| # | الملف | السطر | الشرط | ما يحميه | UI فقط؟ أم يمنع التنفيذ؟ |
|---|---|---|---|---|---|
| 1 | `App.kt` | 420 | `entitlementTop?.pro == true \|\| trialOn` | تعريف `isPro` المركزي — كل ما يلي مشتق منه | تعريف |
| 2 | `App.kt` | 751 | `if (isPro)` | `effectiveSlots` (خانات الشريط السفلي الكاملة مقابل الافتراضية) | UI (تقييد خيارات) |
| 3 | `App.kt` | 1022 | `notaCloudVm.autoBackupIfDue(isPro)` | النسخ السحابي التلقائي — الوسائط تُرفق فقط إن `isPro` | **تنفيذ فعلي** (`allowMedia`) |
| 4 | `App.kt` | 1127–1130 | `if (isPro) vm.widgetTitles() else emptyList()` | عناوين الويدجت | تنفيذ (قائمة فارغة) |
| 5 | `App.kt` | 1210 | `it.pro \|\| hasFeature(..., LOCATION)` | `locAllowed` للمحرر (بدون trial — ثم يُجمع مع `isPro` في 1260) | منطق (الناتج مكافئ بفضل 1260) |
| 6 | `App.kt` | 1260/1262 | `locAllowed \|\| isPro` / `proAllowed = isPro` | الموقع المرفق + ميزات المحرر المدفوعة | UI + تعطيل |
| 7 | `App.kt` | 1383/1384 | `isPro` → `SettingsRoute` | ثيمات مدفوعة (`locked = !th.free && !isPro`)، تخصيص الشريط، التمزيق الآمن (`enabled = isPro`) | UI (تعطيل + `onProLocked`) |
| 8 | `App.kt` | 1395 | `locked = !isPro` | تخصيص شريط التنقل | UI |
| 9 | `App.kt` | 1420/1430 | `onLocked = { proLock() }` | شاشتا الوسوم والتذكيرات (مقفلتان بالكامل بلا `isPro`) | UI (الشاشة تُفتح لكن الإجراءات مقفلة — تحقق في `TagsScreen`/`RemindersScreen` عبر `locked`) |
| 10 | `App.kt` | 1434–1440 | `locked = !isPro` → `BackupScreen` | شاشة النسخ الاحتياطي كلها (بانر + تعطيل كل الأزرار) | UI فقط (انظر سؤال 1 أدناه) |
| 11 | `App.kt` | 1448 | `ProPaywall(... trialActive = trialOn ...)` | شاشة الترقية + بانر التجربة | UI |
| 12 | `App.kt` | 1452/1493/1530 | `showProLocks = !isPro` | أيقونات القفل في القوائم (أرشيف/مفضلة/رئيسية) | UI (أيقونة فقط) |
| 13 | `App.kt` | 1470–1474 وما يماثلها (×4 شاشات) | `if (isPro) { vm.togglePin/Lock/Favorite... } else proLock()` | تثبيت/قفل خاص/مفضلة/نسخ/تكرار من القائمة | **يمنع التنفيذ** (الفعل لا يُستدعى) |
| 14 | `App.kt` | 1545 | `if (premiumVm.canUseFolders()) ... else proLock()` | النقل إلى مجلد | **يمنع التنفيذ** |
| 15 | `App.kt` | 1521/1693 | `FoldersGate(locked = !premiumVm.canUseFolders())` | شاشة المجلدات (المحتوى معروض، الإجراءات معطلة) | UI |
| 16 | `BackupScreen.kt` | 133 | `ncPro = ncEntitlement?.pro == true` | بطاقة مفتاح السحابة (`if (ncPro) CloudKeyCard`) + `backupNow(ncPro)` | **تنفيذ فعلي للوسائط** |
| 17 | `BackupScreen.kt` | 609 | `ncvm.backupNow(ncPro)` | إرفاق الوسائط المشفرة في النسخة السحابية | **تنفيذ فعلي** |
| 18 | `AccountScreen.kt` | 74 | `pro = isPro(entitlement)` (الدالة الحرة — **بدون trial**) | شارة الباقة + زرّا الشراء (`enabled = ... && !pro`) + حزم التخزين (`enabled = ... && pro`) + نص `manage_play` | UI (انظر T5) |
| 19 | `NavDrawer.kt` | 110/154–182 | `isPro` | شارة الباقة + عناصر مقفلة في الدرج | UI |
| 20 | `NotaBottomBar.kt` | 69 | `locked = !isPro && key in PRO_ONLY_SLOTS` | خانات الشريط المدفوعة | UI |
| 21 | `PremiumViewModel` | `canUseFolders/canUseLocation/isPro` | `_trialActive \|\| ...` | البوابات المنطقية المعاد استخدامها | منطق |

**الأجوبة الدقيقة:**

1. **أين تُفحص البوابة في الإجراء نفسه؟** في موضعين فقط: (أ) إجراءات القائمة (`togglePin/Lock/Favorite`، نسخ، تكرار، نقل لمجلد) — `if (isPro)` يمنع استدعاء الـ ViewModel أصلًا؛ (ب) `backupNow(allowMedia)` — الوسائط لا تُرفق بلا `ncPro`. **عدا ذلك البوابة UI فقط (تعطيل أزرار):** `DriveCard` و`.nota` و`RestoreLogCard` تستقبل `locked` كمعامل شكلي يعطّل الأزرار (`enabled = !locked`) لكن دوال `CloudBackupViewModel.backupNow()` / `NotesViewModel` نفسها **بلا أي فحص داخلي** — من يستدعيها مباشرة يتجاوز البوابة. `restoreNow()` للسحابة **غير مفحوص إطلاقًا** (يعمل للكل — النص يُستعاد دائمًا).
2. **`locked` مقابل `pro`:** `locked` معامل UI يُمرَّر من `App.kt` (= `!isPro`) إلى `BackupScreen` وبطاقاتها — يعطّل الأزرار ويعرض بانر/شيت الباقات، ولا يمنع التنفيذ في طبقة البيانات. `pro`/`ncPro` قراءة مباشرة للاستحقاق تُستخدم في منطق تنفيذي (الوسائط، بطاقة المفتاح).
3. **`ncPro` مقابل `isPro`:** مصدران مختلفان — `isPro` (App.kt:420) = `pro || trialOn`؛ `ncPro` (BackupScreen.kt:133) = `pro` **فقط بلا trial**. أثناء التجربة المجانية: كل شيء مفتوح **إلا** بطاقة مفتاح السحابة ورفع الوسائط. (تناقض T4.)
4. **هل كل شاشة تقرأ `PremiumViewModel`؟** نعم — مصدر واحد (`premiumVm.entitlement` / `isPro()` / `canUseFolders()`) يُمرَّر كمعاملات. الاستثناء الوحيد: `BackupScreen` تعيد قراءة `premiumVm.entitlement` لـ `ncPro`، و`App.kt:1209` يعيد قراءة `premiumVm.entitlement` لـ `locAllowed`.
5. **مسار يفتح Pro بلا فحص:** نعم — (أ) النسخ السحابي النصي (`backupNow(false)` + `restoreNow()`) يعمل للكل بلا فحص؛ (ب) `DriveCard`/`.nota`/سجل الاستعادة — البوابة UI فقط؛ (ج) الدالة الحرة `canUse()` معرّفة ولا تُستخدم. مقصود أم عيب؟ **غير واضح في الكود** — لا تعليق يبرر فتح النص للكل.

---

## القسم 5 — مسار الشراء الحالي (Stub)

### 5أ — الرحلة الكاملة

`PaywallScreen` زر شهري/سنوي → `onUpgrade(productId)` → `ProPaywall` (App.kt:1674: `onUpgrade = { premiumVm.purchase(it) }`) → `PremiumViewModel.purchase` → `gateway.launchPurchase(productId)` → ثم `_lastResult` + `repo.refresh()` (غير force) عند النجاح. النهاية.

- **`purchase()` — منقولة حرفيًا** (انظر القسم 1ب — الدالة الأخيرة في الملف). النص:

```kotlin
fun purchase(productId: String) {
    if (_buying.value) return
    viewModelScope.launch {
        _buying.value = true
        val r = runCatching { gateway.launchPurchase(productId) }
            .getOrElse { BillingResult.Failed(it.message ?: "error") }
        _lastResult.value = r
        if (r is BillingResult.Success) {
            runCatching { repo.refresh() }.onSuccess { _entitlement.value = it }
        }
        _buying.value = false
    }
}
```

- **`PlayBillingGateway` (نكهة play) — حرفيًا (11 سطرًا):**

```kotlin
class PlayBillingGateway @Inject constructor() : BillingGateway {
    override suspend fun queryPurchases(): List<String> = emptyList()

    override suspend fun launchPurchase(productId: String): BillingResult =
        BillingResult.Unavailable
}
```

- **`FakeBilling` (نكهة mock) — حرفيًا (18 سطرًا):**

```kotlin
class FakeBilling @Inject constructor() : BillingGateway {
    override suspend fun queryPurchases(): List<String> = emptyList()

    override suspend fun launchPurchase(productId: String): BillingResult {
        delay(300) // محاكاة زمن الشبكة — بلا runBlocking
        return if (productId in BillingGateway.ALL_PRODUCTS) {
            BillingResult.Success(purchaseToken = "fake-$productId-${System.currentTimeMillis()}")
        } else {
            BillingResult.Failed("unknown product: $productId")
        }
    }
}
```

- معرفات المنتجات (`BillingGateway.companion`): `nota_pro_monthly`، `nota_pro_yearly`، `nota_lifetime`، `nota_storage_5g_monthly/yearly`، `nota_storage_10g_monthly/yearly` — **معرفات نصية فقط، بلا أي ربط BillingClient.**

### 5ب — النتائج

| النتيجة | ماذا يحدث في الكود |
|---|---|
| `Success` | `_lastResult = Success` + `repo.refresh()` (غير force) + `_entitlement` يُحدَّث إن نجح الـ refresh. لا UI يُظهر نجاحًا صريحًا — المستخدم يرى البوابات تُفتح (إن حدّث السيرفر فعلًا) |
| `Unavailable` | `_lastResult = Unavailable` — **لا UI يقرأها** (صمت تام للمستخدم في نكهة play) |
| `Failed` / `Cancelled` | `_lastResult` تُخزَّن — **لا UI يقرأها** (لا سناك بار ولا حوار خطأ) |
| استثناء من `launchPurchase` | يُحوَّل إلى `Failed` عبر `getOrElse` |

- **`_lastResult` في الواجهة:** **غير مقروءة في أي Composable** — `grep lastResult` لا يعيد إلا التعريف والتحديث في `PremiumViewModel`. (M4/C4 مؤكّد.)

### 5ج — `refresh()` بعد الشراء

- **مؤكَّد: غير force.** `purchase()` تستدعي `repo.refresh()` (الافتراضي `force = false`).
- قراءة جنبًا إلى جنب: إن كان الكاش طازجًا (< 24h — وهو الغالب لأن `refresh()` يُستدعى عند بدء الـ ViewModel)، يُعاد الكاش القديم (`pro:false`) ويُعرض FREE رغم الدفع الناجح — حتى انتهاء TTL أو ضغط "تحديث الحالة" اليدوي (`forceRefresh` في Paywall/Account). **النتائج العملية:** المستخدم يدفع → قد يرى FREE → يحتاج زر التحديث اليدوي أو الانتظار. (C3 مؤكّد — والتعليق فوق `forceRefresh` نفسه يقرّ بأن "بعد الدفع يحتاج المشتري هذا طبيعيًا" لكن `purchase()` لا تستدعيه.)
- ملاحظة: في نكهة mock لا أثر عملي (السيرفر الوهمي `FakeEntitlementsApi` يرد `pro:true` دائمًا)، وفي نكهة play الشراء `Unavailable` أصلًا فلا يصل لمسار النجاح.

### 5د — `queryPurchases`

- **غير مستدعاة في أي مكان.** `grep queryPurchases` يعيد التعريف في `BillingGateway` + تجاوزين (`FakeBilling` يعيد `[]`، `PlayBillingGateway` يعيد `[]`) — **صفر مواقع استدعاء.** لا استعادة مشتريات عند بدء التطبيق. (M: موجود لكن لا يعمل.)

---

## القسم 6 — مسار الشراء المقصود (عندما يُربط Play Billing)

فحص ما هو موجود وما ينقص — بلا تصميم:

1. **عميل Play Billing:** **غير منفّذ — لا يوجد في الكود.** لا تبعية `com.android.billingclient` (غير مذكورة في `build.gradle.kts` — تحقّق بالقراءة: لا أثر لها)، لا `BillingClient`، لا `PurchasesUpdatedListener`. الـ gateway واجهة معزولة تمامًا.
2. **إبلاغ السيرفر بعد الشراء:** **غير منفّذ.** لا يوجد `POST /v1/purchases` ولا أي endpoint لاستلام `purchaseToken` في الكود. `purchase()` عند النجاح تسأل `/v1/entitlements` فقط (قراءة، لا كتابة). ما يحتاجه التطبيق: endpoint + استدعاء يرسل `purchaseToken` + `productId` + `accountId` — كل ذلك غائب.
3. **التحقق من الإيصال:** **غير منفّذ.** لا كود يرسل `purchaseToken` للسيرفر، ولا كود سيرفر أصلًا في المستودع ("not in repository"). حتى `Success.purchaseToken` (الذي ينتجه `FakeBilling`) لا يُرسل لأي مكان — يُخزَّن في `_lastResult` غير المقروءة.
4. **RTDN:** **غير منفّذ — لا يوجد في الكود.** لا webhook ولا مستقبل إشعارات Play (تجديد/إلغاء/refund) — التطبيق يعتمد كليًا على polling الـ `pro` عبر `refresh()`.
5. **`manage_play` في شاشة الحساب:** **نص فقط.** `AccountScreen.kt` سطر ~162: `Text(stringResource(R.string.acc_manage_play))` — "Renewal and cancellation via Google Play subscriptions on your device" — بلا `clickable` ولا `Intent` لفتح Play Store. لا يعمل كزر.

---

## القسم 7 — التجربة (Trial)

1. **متى يبدأ `trial_start_ms`؟** عند ضغط "ابدأ التجربة" في `TrialOfferDialog` — `App.kt` سطر 461: `onStart = { settings.startTrial(); ... }`. العرض يظهر **أول استخدام فقط** (`if (!trialOfferedTop)` سطر 459 — أي قبل أي شراء وقبل/بغض النظر عن إنشاء الحساب). ليس أول فتح تلقائيًا (يتطلب قبولًا)، وليس مرتبطًا بحساب.
2. **ماذا يمنح؟** كل مزايا Pro (انظر 1د): `isPro()` + `canUseFolders()` + `canUseLocation()` + كل بوابات `App.kt` — **باستثناء** بطاقة مفتاح السحابة ورفع الوسائط (`ncPro` بلا trial — T4). عمليًا: المجلدات/الموقع/الثيمات/التذكير/الوسوم/التثبيت/القفل/المفضلة/النسخ/التكرار/تخصيص الشريط/التمزيق — كلها مفتوحة.
3. **كيف يظهر للمستخدم؟** (أ) حوار العرض أول مرة (`TrialOfferDialog`)؛ (ب) بانر "تجربتك المجانية نشطة + المتبقي (أيام/ساعات)" في `PaywallScreen` أثناء التجربة؛ (ج) سطر الخصم الدائم. لا عدّاد في الشاشة الرئيسية ولا شارة Pro أثناء التجربة في الدرج (شارة الدرج تقرأ `isPro(entitlement)` **بلا trial** — انظر T5).
4. **اليوم 7 (انتهاء):** سقوط صامت — لا إشعار ولا حوار ولا سناك بار في الكود. `trialOn=false` في التقييم التالي (خلال دقيقة) فتُقفل البوابات. **يحتاج جهازًا — التحقق من سلوك اليوم 7 الفعلي (H23) يتطلب انتظارًا حقيقيًا أو تغيير ساعة الجهاز.**
5. **`TRIAL_DISCOUNT_PCT = 50`:** نص فقط (انظر 1د) — يُعرض في `trial_discount_note` و`ob_sub_trial_note` و`trial_offer_msg`. لا تكامل مع Play offers/barcodes. (M4 مؤكّد.)

---

## القسم 8 — Cross-Device والاستحقاق

1. **`accountId` ثابت عبر الأجهزة؟** نعم — Firebase UID لنفس حساب Google (`AccountStore` + `GoogleAuthManager`). نفس الحساب = نفس المعرف = نفس الاستحقاق السيرفري.
2. **جهاز جديد + نفس الحساب:** يحصل على `pro` بعد أول `refresh()` ناجح (يُستدعى في `init` لـ `PremiumViewModel`). `ServerRegister.ensureRegistered` يضمن وجود الصف سيرفريًا أولًا. **يحتاج جهازًا (H21) — التحقق من التسجيل على جهاز جديد يتطلب جهازًا ثانيًا فعليًا.**
3. **جهاز جديد + حساب آخر:** الكاش القديم يُسقط (`cached` تعيد FREE عند اختلاف `K_ACCOUNT`؛ `refresh` تتجاهل الطزاجة). لكن ملف `entitlements_cache` القديم يبقى على القرص غير مقروء. **تغيير `accountId` أثناء جلسة (H22): يحتاج جهازًا — يتطلب تبديل حسابات فعليًا.**
4. **فشل `refresh()` على جهاز جديد (offline):** `accounts.currentAccountId()` موجود لكن لا كاش → كل الفروع تعيد `Entitlement(pro=false, ...)` من مخزن فارغ → **يظهر FREE** (لا "لا شيء" — الواجهة تعمل كمجاني). `entitlement` تصبح `FREE` لا `null` (في مسار `refresh()` العادي؛ أما `forceRefresh` عند الفشل فيُبقي آخر قيمة).
5. **`ServerRegister` ماذا يُرسل:** `{accountId}` فقط + توكن Firebase في الـ header. **لا اسم جهاز ولا device ID ولا طابع زمني ولا entitlement.** (انظر 2د.)

---

## القسم 9 — حالات الفشل (Failure Modes)

| # | الحالة | ماذا يحدث في الكود | الأثر على المستخدم |
|---|---|---|---|
| 1 | `refresh()` offline | أي `Exception` (غير Auth) → آخر كاش معروف من `entitlements_cache` (بلا حد لِقدمه في هذا المسار) | تجميد الحالة: Pro يبقى Pro مؤقتًا؛ Free يبقى Free |
| 2 | `refresh()` 401 مزدوج | `AuthExpiredException` تُرمى → `authExpired=true` | طلب إعادة الدخول (Paywall/Account) — لا سقوط صامت |
| 3 | `refresh()` 500 | `IOException("entitlements HTTP 500")` → مسار الكاش (كأوفلاين) | نفس الأوفلاين |
| 4 | `refresh()` timeout | `connectTimeout/readTimeout = 10s` → `IOException` → مسار الكاش | نفس الأوفلاين (انتظار حتى 10 ثوانٍ لكل محاولة) |
| 5 | JSON خاطئ | `Log.w` + `IOException("entitlements parse failed")` → مسار الكاش | نفس الأوفلاين؛ لا كراش |
| 6 | `accountId` null | `refresh()` تعيد `FREE` فورًا بلا شبكة | مجاني كامل |
| 7 | Refund من Play | **غير منفّذ — لا يوجد في الكود.** لا كشف استباقي؛ فقط عندما يقلب السيرفر `pro` (خلال ≤24h) | المستخدم يحتفظ بمزايا Pro حتى انتهاء الكاش/قلب السيرفر |
| 8 | Chargeback | **غير منفّذ — لا يوجد في الكود.** نفس مسار الـ refund | نفس الأثر |
| 9 | حذف الحساب مع كاش Pro | `signOut` → `clear()` → `cached` تعيد FREE؛ لكن `_entitlement` في الـ ViewModel تبقى حتى `refresh()` التالي | سقوط شبه فوري (الكاش مربوط بالحساب) |
| 10 | Server DB lost | **غير واضح في الكود** (لا كود سيرفر في المستودع). التطبيق: السيرفر سيرد `pro:false` أو خطأ → مسار الكاش/السقوط | يعتمد على سلوك السيرفر — خارج نطاق الكود المقروء |

---

## القسم 10 — ما ينقص لتحقيق "مدفوع حقيقي"

| # | البند | الحالة |
|---|---|---|
| 1 | عميل Play Billing (`billingclient`, `BillingClient`) | **غير منفّذ** — stub فقط (`Unavailable` في play، نجاح وهمي في mock) |
| 2 | إبلاغ السيرفر بعد الشراء (`POST /v1/purchases` أو ما يعادله) | **غير منفّذ** — لا endpoint ولا استدعاء في الكود |
| 3 | التحقق من الإيصال (سيرفر) | **غير منفّذ + يحتاج سيرفر** — "not in repository" |
| 4 | RTDN webhook | **غير منفّذ + يحتاج سيرفر** |
| 5 | `queryPurchases` عند بدء التطبيق | **غير منفّذ** — الدالة موجودة ولا تُستدعى إطلاقًا |
| 6 | grace period للتجديد | **غير منفّذ** — لا منطق في الكود |
| 7 | account hold | **غير منفّذ** — لا ذكر في الكود |
| 8 | Refund handling | **غير منفّذ** — يُكتشف عرضًا عبر polling الـ `pro` فقط |
| 9 | Multi-tier (Pro / Storage 5G / 10G) | **نصف موجود**: معرفات المنتجات السبعة موجودة في `BillingGateway`، وحزم التخزين لها أزرار في `AccountScreen` (مفعّلة فقط عند `pro`)، لكن **لا تمييز في `Entitlement`** (لا `tier` ولا سعات) ولا منطق سيرفر في الكود — التعليق يقول "السيرفر يجمع storage_* تلقائيًا" (ادعاء بلا كود مقابل) |
| 10 | Lifetime purchase | **غير منفّذ فعليًا**: `PRODUCT_LIFETIME` معرف نصي فقط — لا زر يشتريه في أي شاشة (`PaywallScreen` شهري/سنوي فقط؛ `AccountScreen` شهري/سنوي + حزم تخزين) ولا معالجة `expiresAt == null الدائم` مميزة |

---

## القسم 11 — تفاعل Entitlement مع النسخ الاحتياطي (لتصميم لاحق)

1. **عند downgrade (Pro → Free):** بطاقة المفتاح تختفي (`if (ncPro)` — `BackupScreen.kt` سطر 214) لكن المفتاح يبقى في `CloudKeyStore` محليًا (لا كود يمسحه عند السقوط — `grep` لا يظهر أي مسح مشروط بالاستحقاق). الوصول للمفتاح بعد الاختفاء: **غير واضح في الكود** — لا مسار بديل لعرض/تصدير المفتاح بلا `ncPro`. سؤال تصميم معلّق.
2. **نسخ سحابية بوسائط عند downgrade:** تبقى على السيرفر (لا كود حذف مشروط بالاستحقاق). قابلة للاستعادة؟ `restoreNow()` **غير مفحوص** — يعمل للكل ويجلب الـ blobs بما فيها الوسائط (`fetchMediaBlobs` يتطلب المفتاح المحلي فقط لا `pro`). أي Free يستعيد وسائط نسخة Pro **إن كان المفتاح على جهازه.**
3. **Free عنده نسخة سحابية بوسائط (من فترة Pro):** `restoreNow()` لا يتخطى الوسائط — يحاول جلبها (`mediaCount > 0` → `fetchMediaBlobs` إن وُجد المفتاح؛ وإلا `_detail = "media:N:nokey"` والنص يُستعاد). **لا تخطٍّ مشروط بـ `pro`.**
4. **Pro يستعيد نسخة Free (من فترة سابقة):** كل شيء يعمل — `restoreNow()` + `BackupCodec.decode` بلا أي فحص استحقاق. لا عائق في الكود.
5. **هل كل ميزات Backup تُفحص؟** لا: `DriveCard` وملف `.nota` و`RestoreLogCard` — البوابة `locked` **UI فقط** (تعطيل أزرار؛ دوال الـ ViewModels بلا فحص). `restoreNow` السحابي بلا فحص. المفحوص تنفيذيًا فقط: إرفاق الوسائط (`allowMedia = ncPro`) + إظهار بطاقة المفتاح (`if (ncPro)`). (M1 مؤكّد بمواضع دقيقة.)

---

## القسم 12 — قرارات منتج تنتظر محمد (أسئلة محددة)

```markdown
## قرار 1 — طبيعة Entitlement
- هل `pro` boolean فقط؟ أم متعدد الأبعاد (tier, storage, devices)؟
- [الحالي في الكود: `pro: Boolean` + `features: List<String>` + `expiresAt: Long?` — لا tier ولا storage ولا devices]

## قرار 2 — Trial
- هل trial محلي (7 أيام من أول قبول) أم Play trial (7 أيام من الشراء)؟
- هل يُمنح ميزات Pro كاملة أم فقط خصم 50%؟
- ماذا يحدث عند انتهاء trial إذا لم يشترِ؟ (الحالي: سقوط صامت بلا إشعار)
- [الحالي: محلي بالكامل، يمنح كل شيء، يُصفَّر بإعادة التثبيت، الخصم نص بلا تكامل]

## قرار 3 — عند downgrade
- هل النسخ السحابية تُمسح فورًا؟ أم تبقى قابلة للاستعادة؟
- هل يوجد grace period (7-30 يوم)؟ (الحالي: لا شيء — البوابة تسقط مع الكاش خلال ≤24h)
- كيف يصل المستخدم لمفتاح السحابة بعد اختفاء البطاقة؟

## قرار 4 — Multi-tier
- هل "Pro" طبقة واحدة؟ أم Pro + Storage 5G + Storage 10G؟
- كيف تُفرَّق على السيرفر؟ (حقل `tier`؟ `features` array؟)
- [الحالي: معرفات منتجات فقط + أزرار حزم في الحساب (Pro فقط) + تعليق "السيرفر يجمع تلقائيًا" بلا كود]

## قرار 5 — Lifetime purchase
- `nota_lifetime` معرف في الكود — هل يُفعَّل؟
- ما الفرق بينه وبين Pro شهري/سنوي على السيرفر؟
- [الحالي: لا زر يشتريه في أي شاشة، ولا معالجة مميزة]

## قرار 6 — الاستحقاق والبيانات
- إذا حُذف الحساب، ماذا يحدث للـ `entitlement` على السيرفر؟
- إذا انتهى Pro، هل تُمسح النسخ السحابية تلقائيًا أم لا؟
- [الحالي: لا كود سيرفر في المستودع — "not in repository" — ولا منطق حذف client-side مشروط بالاستحقاق]
```

---

## قسم إضافي — موجود لكن لا يعمل كما يوحي الاسم

| # | العنصر | الحالة المؤكدة في الكود |
|---|---|---|
| M1 | `locked` في `DriveCard`/`.nota`/`RestoreLogCard` | شكلي — `enabled = !locked` فقط؛ الدوال التنفيذية بلا فحص (مؤكّد من `BackupScreen.kt` + غياب أي فحص في `CloudBackupViewModel`/`NotesViewModel`) |
| M2 | `purchase()` بعد النجاح ينادي `refresh()` غير force | مؤكّد (`PremiumViewModel.kt` سطر 97) — قد يعيد كاش FREE طازج بعد دفع ناجح (C3) |
| M3 | `TRIAL_DISCOUNT_PCT=50` | نص بلا تكامل — يُعرض في 3 سلاسل نصية، لا يُطبَّق سعريًا (مؤكّد) |
| M4 | `_lastResult` تُخزَّن ولا تُعرض | مؤكّد — صفر قرّاء خارج `PremiumViewModel` |
| M5 | `queryPurchases` غير مستدعاة إطلاقًا | مؤكّد — تعريف + تجاوزان فقط، صفر استدعاء |
| M6 | `manage_play` نص فقط | مؤكّد — `Text` بلا `clickable`/`Intent` (`AccountScreen.kt` ~162) |
| M7 | `ServerRegister.ensureRegistered` — لا يُبلِّغ بالـ entitlement | مؤكّد — يرسل `{accountId}` فقط؛ لا entitlement ولا جهاز |
| M8 | الدالة الحرة `canUse(e, name)` في `Entitlements.kt` | معرّفة ولا تُستدعى في أي مكان — مسار ميت |
| M9 | `EntitlementsRepo.hasFeature(name)` | موجودة ولا تُستدعى في أي مكان (البوابات تستخدم `PremiumViewModel.canUseFolders`) |
| M10 | "السيرفر لا يرى المفتاح أبدًا" (C8 سابق) | يحتاج تدقيقًا: النص **مشفر** بمفتاح المستخدم قبل الرفع (`CloudMediaCrypto.encryptKey` في `backupNow`) — السيرفر يرى ظرفًا مشفرًا لا نصًا عاديًا. الادعاء صحيح **بمعنى المحتوى** لكن السيرفر يرى الـ blobs والـ sha256 والأحجام — "صفر معرفة" بمعنى المحتوى فقط. مؤكّد من `NotaCloudViewModel.backupNow` |
| M11 | زرّا حزم التخزين في `AccountScreen` | موجودان ومربوطان بـ `purchase(...)` لكن `enabled = ... && pro` — لا يُشترى إلا بوجود Pro أصلًا، ولا أثر للسعة في `Entitlement` |

---

## قسم إضافي — يحتاج جهازًا

| # | العنصر | ما يتطلبه التحقق |
|---|---|---|
| H1 | المدة الفعلية لمعرفة انتهاء Pro | سيرفر حي + اشتراك حقيقي + مراقبة حتى 24h (نظريًا ≤24h من TTL) |
| H2 | Pro على جهاز آخر قديم | جهازان + نفس الحساب + مقارنة الكاش |
| H3 | Pro offline | وضع طائرة + كاش Pro + مراقبة السلوك بعد انتهاء TTL |
| H4 | Refund فعلي | شراء حقيقي + استرداد من Play + مراقبة السقوط |
| H5 | OAuth/تجديد التوكن | إبطال جلسة Firebase فعليًا + مراقبة `AuthExpiredException` وواجهة إعادة الدخول |
| H20 | سلوك trial الـ 7 أيام فعليًا | انتظار 7 أيام حقيقية أو تغيير ساعة الجهاز + مراقبة البوابات |
| H21 | `ServerRegister` على جهاز جديد | جهاز ثانٍ + نفس الحساب + مراقبة التسجيل الإيدمبوتنت |
| H22 | تغيير `accountId` أثناء جلسة | تبديل حسابات Google فعلي + مراقبة سقوط الكاش |
| H23 | انتهاء trial بالضبط (اليوم 7) | توقيت دقيق + مراقبة غياب الإشعار والسقوط الصامت |
| H24 | `refresh()` كل 24 ساعة — تلقائي أم عند فتح الشاشة فقط؟ | الكود: `refresh()` تُستدعى في `init` للـ ViewModel + زر يدوي فقط — لا worker دوري. التأكد سلوكيًا يحتاج جهازًا (مراقبة هل يتحدث الاستحقاق دون فتح Paywall/Account) |

---

## قسم إضافي — تناقضات

| # | التناقض | المواضع | الوصف |
|---|---|---|---|
| C1 | `MAX_CLOUD_KEEP` — قيمتان (5 مقابل 7) | سابق | **لم يعد قائمًا**: القيمة الآن واحدة `MAX_CLOUD_KEEP = 7` (`SettingsStore.kt` سطر 80) والافتراضي 5 (`cloudKeep` سطر 156) — ثابت/افتراضي متمايزان بوضوح، لا تناقض حالي |
| C3 | `refresh()` غير force بعد شراء ناجح | `PremiumViewModel.purchase` سطر 97 | **قائم ومؤكّد** — التعليق فوق `forceRefresh` يقرّ بالحاجة لكن `purchase()` لا تستدعيه |
| C4 | `_lastResult` غير مستخدم | `PremiumViewModel` سطر 37–38، 95 | **قائم ومؤكّد** — تُخزَّن ولا تُقرأ |
| T1 | `repo.hasFeature` ضد `ViewModel.canUseLocation` | `EntitlementsRepo` سطر ~100 ضد `PremiumViewModel` | الأولى: `pro` وحده لا يكفي إلا لـ `folders`؛ الثانية: `pro` يكفي لكل شيء. منطقان مختلفان لنفس السؤال — لكن الأولى ميتة (لا تُستدعى) فالأثر صفري حاليًا |
| T2 | الدالة الحرة `canUse()` ضد بوابات الـ ViewModel | `Entitlements.kt` سطر 40 | `canUse` تعتبر `pro` كافيًا لأي ميزة؛ `canUseFolders` يضيف trial؛ `repo.hasFeature` يقيّد `pro` — ثلاثة تعريفات لـ "الصلاحية"، واحد فقط حي فعليًا |
| T3 | `ncPro` (بلا trial) ضد `isPro` (مع trial) | `BackupScreen.kt` سطر 133 ضد `App.kt` سطر 420 | أثناء التجربة: كل شيء مفتوح إلا بطاقة المفتاح والوسائط — استثناء غير معلن في أي تعليق |
| T4 | شارة الدرج/الحساب (بلا trial) ضد البوابات (مع trial) | `AccountScreen.kt` سطر 74 (`isPro(entitlement)` الحرة) + `NavDrawer.kt` سطر 110 | أثناء التجربة المجانية الشارة تقول "Free" بينما كل المزايا مفتوحة — المستخدم Pro فعليًا والشاشة تقول Free |
| T5 | `locAllowed` يعيد فحصًا بلا trial ثم يُجمع مع `isPro` | `App.kt` سطور 1209–1210 ثم 1260 | `locAllowed` وحده = false أثناء trial بلا pro، لكن `canUseLocation = locAllowed \|\| isPro` = true — النتيجة صحيحة لكن القراءة المزدوجة لـ `entitlement` (هنا + `premiumVm`) زائدة ومضللة |
| T6 | تعليق "السيرفر يجمع storage_* تلقائيًا" | `BillingGateway.kt` | ادعاء سيرفري بلا أي كود مقابل في المستودع ("not in repository") — غير قابل للتحقق |
| T7 | تعليق Paywall "يُفعَّل من عروض متجر Play" | `PaywallScreen.kt` | الخصم معروض كأنه قائم (`trial_discount_note` دائم الظهور) بينما التفعيل مستقبلي — النص يوحي بخصم حقيقي غير موجود |

---

## الجرد الرباعي

### (أ) غير منفّذ — لا يوجد في الكود (12 بندًا)

1. عميل Play Billing (`billingclient`/`BillingClient`/`PurchasesUpdatedListener`) — لا تبعية ولا كود
2. `POST /v1/purchases` أو أي إبلاغ سيرفري بعد الشراء
3. التحقق من الإيصال (`purchaseToken` لا يُرسل لأي مكان)
4. RTDN / webhook (تجديد/إلغاء/refund)
5. استدعاء `queryPurchases` (الدالة موجودة — الاستدعاء غائب)
6. grace period بعد انتهاء الاشتراك
7. account hold
8. معالجة refund/chargeback (غير عرض polling الـ `pro`)
9. تمييز multi-tier في `Entitlement` (لا `tier`/سعات — معرفات منتجات فقط)
10. تفعيل `nota_lifetime` (لا زر + لا معالجة)
11. زر/فعل `manage_play` (نص فقط)
12. كود السيرفر (`nota-api`) — **not in repository** بالكامل

### (ب) موجود لكن شكلي / لا يعمل كما يوحي الاسم (11 بندًا: M1–M11 أعلاه)

أبرزها: `locked` لبطاقات Drive/`.nota`/السجل (UI فقط)، `_lastResult` (تُخزَّن ولا تُقرأ)، `queryPurchases` (صفر استدعاء)، الخصم 50% (نص)، `manage_play` (نص)، `canUse`/`repo.hasFeature` (مسارات ميتة).

### (ج) يحتاج جهازًا (10 بنود: H1–H5، H20–H24 أعلاه)

أبرزها: زمن اكتشاف انتهاء Pro، الـ refund الفعلي، سلوك اليوم 7 للتجربة، هل الـ refresh دوري أم عند الفتح فقط.

### (د) تناقضات (8 بنود: C3، C4، T1–T5، T6، T7 أعلاه — C1 السابق مُغلق)

أبرزها: `purchase()` → `refresh()` غير force (C3)، شارة "Free" أثناء تجربة مفتوحة بالكامل (T4)، `ncPro` يستثني الـ trial بصمت (T3)، ثلاثة تعريفات متباينة للصلاحية (T2).

---

*انتهى الجرد — 20 أكتوبر 2026. المهارة المطبقة: `code-reviewer` (النطاق: billing/Premium/Paywall/البوابات — القراءة: ملفات فعلية كاملة — الحكم: جرد بلا توصيات). لا كود كُتب، لا commits، لا بناء.*
