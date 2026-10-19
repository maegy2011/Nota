package com.nota.notes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("nota_settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val K_THEME = stringPreferencesKey("theme") // system | light | dark — الافتراضي system
    private val K_APP_THEME = stringPreferencesKey("app_theme") // هوية الألوان: warm افتراضيًا (قرار 26 سبتمبر 2026)
    private val K_LANG = stringPreferencesKey("lang")   // system | ar | en
    private val K_BIO = booleanPreferencesKey("biometric_enabled") // فتح بالبصمة
    private val K_AUTOLOCK = longPreferencesKey("auto_lock_ms")    // مهلة القفل التلقائي
    private val K_SORT = stringPreferencesKey("sort_order")        // ترتيب الملاحظات
    private val K_DEF_COLOR = intPreferencesKey("default_color")   // لون الملاحظات الجديدة (0 = افتراضي)
    private val K_FONT = stringPreferencesKey("editor_font")       // حجم خط المحرر small|normal|large
    private val K_CARD_DATES = booleanPreferencesKey("card_dates") // إظهار التواريخ في البطاقات
    private val K_ONBOARD = booleanPreferencesKey("onboarding_done") // شاشات التعريف عُرضت مرة واحدة
    private val K_PERMS = booleanPreferencesKey("perms_reviewed") // مراجعة الصلاحيات عند أول تثبيت عُرضت مرة واحدة
    private val K_SHOTS = booleanPreferencesKey("screenshots_allowed") // السماح بلقطات الشاشة — الافتراضي ممنوع
    private val K_AUTOSAVE = booleanPreferencesKey("autosave_enabled") // الحفظ التلقائي للمحرر — مفعّل افتراضيًا
    private val K_FX = booleanPreferencesKey("effects_enabled") // الاهتزاز اللمسي + حركات القوائم — مفعّل افتراضيًا (28 سبتمبر 2026)
    private val K_SHEET_RECENT = stringPreferencesKey("sheet_recent") // آخر مفاتيح إضافات شيت المحرر — مفصولة بفاصلة (28 سبتمبر 2026)
    private val K_TPL_CUSTOM = stringPreferencesKey("tpl_custom") // قوالب المستخدم: اسم\u001Fمتن\u001E ... (28 سبتمبر 2026)
    private val K_DEF_FOLDER = longPreferencesKey("default_folder_id") // المجلد الافتراضي للملاحظات الجديدة — null = الرئيسية
    private val K_CLOUD_LAST = longPreferencesKey("cloud_backup_last") // آخر نسخة سحابية ناجحة (ms) — 0 = لا شيء بعد
    private val K_CLOUD_HASH = stringPreferencesKey("cloud_backup_hash") // بصمة آخر JSON مرفوع — تخطي الرفع المكرر بلا تغيير (27 سبتمبر 2026)
    private val K_CLOUD_KEEP = intPreferencesKey("cloud_backup_keep") // عدد نسخ Drive المحتفظ بها 1..7 — الأقدم يُحذف عند التجاوز (27 سبتمبر 2026)
    private val K_CLOUD_AUTO = booleanPreferencesKey("cloud_backup_auto") // النسخ السحابي التلقائي اليومي — معطّل افتراضيًا
    private val K_CLOUD_FREQ = stringPreferencesKey("cloud_backup_freq") // جدولة النسخ: off|daily|weekly — معطّلة افتراضيًا (27 سبتمبر 2026)
    private val K_BATT_PROMPT = booleanPreferencesKey("battery_prompt_shown") // عرض طلب استثناء البطارية مرة واحدة فقط
    private val K_USERNAME = stringPreferencesKey("username") // اسم المستخدم لرسالة الترحيب — فارغ = تحية عامة
    private val K_WELCOME = booleanPreferencesKey("welcome_enabled") // رسالة الترحيب عند فتح التطبيق — مفعّلة افتراضيًا
    private val K_QUICK_MODE = booleanPreferencesKey("quick_mode") // الوضع السريع: بعد القفل → ملاحظة جديدة فورًا — معطّل افتراضيًا
    private val K_TRIAL_OFFERED = booleanPreferencesKey("trial_offered") // عُرضت التجربة أول استخدام — مرة واحدة فقط
    private val K_TRIAL_START = longPreferencesKey("trial_start_ms") // بداية التجربة — 0 = لم تبدأ
    private val K_NAV_SLOTS = stringPreferencesKey("nav_slots") // خانات شريط التنقل مفصولة بفاصلة — 5 كحد أقصى
    private val K_FAB = booleanPreferencesKey("fab_visible") // إظهار زر الإضافة العائم — Pro فقط (يُجبر عند إزالة الإضافة من الشريط)
    private val K_AI_ENDPOINT = stringPreferencesKey("ai_endpoint") // خادم الذكاء الاصطناعي المتوافق مع OpenAI — مفتاح المستخدم نفسه
    private val K_AI_MODEL = stringPreferencesKey("ai_model") // اسم النموذج على الخادم
    private val K_AI_KEY = stringPreferencesKey("ai_key_sealed") // مفتاح API مشفر بـ TokenVault — لا يُخزن مكشوفًا أبدًا
    private val K_SHRED = booleanPreferencesKey("shred_enabled") // الإتلاف الآمن قبل الحذف — معطّل افتراضيًا (اختياري)
    private val K_SHRED_METHOD = stringPreferencesKey("shred_method") // طريقة الإتلاف single|dod|schneier

    companion object {
        const val AUTO_LOCK_NEVER = -1L
        val AUTO_LOCK_OPTIONS = listOf(15_000L, 30_000L, 60_000L, 120_000L, 300_000L, AUTO_LOCK_NEVER)
        // خيارات ترتيب الملاحظات — الافتراضي الأحدث تعديلًا أولًا
        const val SORT_UPDATED_DESC = "updated_desc"
        const val SORT_UPDATED_ASC = "updated_asc"
        const val SORT_CREATED_DESC = "created_desc"
        const val SORT_CREATED_ASC = "created_asc"
        const val SORT_TITLE_ASC = "title_asc"
        val SORT_OPTIONS = listOf(SORT_UPDATED_DESC, SORT_UPDATED_ASC, SORT_CREATED_DESC, SORT_CREATED_ASC, SORT_TITLE_ASC)
        const val FONT_SMALL = "small"
        const val FONT_NORMAL = "normal"
        const val FONT_LARGE = "large"
        // تكرار النسخ السحابي المجدول (27 سبتمبر 2026)
        const val FREQ_OFF = "off"
        const val FREQ_DAILY = "daily"
        const val FREQ_WEEKLY = "weekly"
        const val MAX_CLOUD_KEEP = 7 // حد نسخ Drive الأقصى — يختار المستخدم 1..7
        const val DEFAULT_CLOUD_KEEP = 5
        // مستويات الإتلاف الآمن (الأفضل ممارسةً على التخزين الحديث — Gutmann35 مستبعد عمدًا: خرافة على الفلاش)
        const val SHRED_SINGLE = "single"   // تمريرة عشوائية واحدة — كافية عمليًا (NIST 800-88 Clear)
        const val SHRED_DOD = "dod"         // ثلاث تمريرات DoD 5220.22-M (00 ثم FF ثم عشوائي)
        const val SHRED_SCHNEIER = "schneier" // سبع تمريرات Schneier (00 ثم FF ثم 5 عشوائية)
        val SHRED_OPTIONS = listOf(SHRED_SINGLE, SHRED_DOD, SHRED_SCHNEIER)
        // شريط التنقل السفلي (قرار 26 سبتمبر 2026 — حُدّث 27 سبتمبر 2026): 5 خانات — المجاني ثابتة، والمدفوع يخصصها
        // رسائل النفس تحل محل زر الإضافة في الافتراضي — الإضافة الجديدة عبر زر FAB العائم
        // التجربة المجانية (27 سبتمبر 2026 — بأمر محمد): 7 أيام Pro + خصم 50% — تُعرض أول استخدام فقط
        const val TRIAL_DAYS = 7
        const val TRIAL_DISCOUNT_PCT = 50
        fun trialDurationMs(): Long = TRIAL_DAYS * 24L * 3600L * 1000L
        fun isTrialActive(startMs: Long, nowMs: Long = System.currentTimeMillis()): Boolean =
            startMs > 0L && nowMs < startMs + trialDurationMs()
        const val NAV_ADD = "add"
        const val NAV_SELF = "self"
        val DEFAULT_NAV_SLOTS = listOf("list", "favorites", NAV_SELF, "reminders", "settings")
        // المرشحة للتخصيص — تُستبعد الحساب (في ترويسة الدرج) والخانات غير اليومية تُترك للدرج
        val NAV_ELIGIBLE = listOf("list", "favorites", NAV_ADD, NAV_SELF, "reminders", "settings", "archive", "folders", "categories", "tags", "backup", "about")
    }

    val theme: Flow<String> = ctx.settingsStore.data.map { it[K_THEME] ?: "system" }
    val appTheme: Flow<String> = ctx.settingsStore.data.map { it[K_APP_THEME] ?: "warm" }
    val lang: Flow<String> = ctx.settingsStore.data.map { it[K_LANG] ?: "system" }
    val biometricEnabled: Flow<Boolean> = ctx.settingsStore.data.map { it[K_BIO] ?: false }
    val autoLockMs: Flow<Long> = ctx.settingsStore.data.map { coerceAutoLock(it[K_AUTOLOCK] ?: 30_000L) }
    val sortOrder: Flow<String> = ctx.settingsStore.data.map { it[K_SORT] ?: SORT_UPDATED_DESC }
    val defaultColor: Flow<Int> = ctx.settingsStore.data.map { it[K_DEF_COLOR] ?: 0 }
    val editorFont: Flow<String> = ctx.settingsStore.data.map { it[K_FONT] ?: FONT_NORMAL }
    val cardDates: Flow<Boolean> = ctx.settingsStore.data.map { it[K_CARD_DATES] ?: true }
    val onboardingDone: Flow<Boolean> = ctx.settingsStore.data.map { it[K_ONBOARD] ?: false }
    val permsReviewed: Flow<Boolean> = ctx.settingsStore.data.map { it[K_PERMS] ?: false }
    val screenshotsAllowed: Flow<Boolean> = ctx.settingsStore.data.map { it[K_SHOTS] ?: false }
    val shredEnabled: Flow<Boolean> = ctx.settingsStore.data.map { it[K_SHRED] ?: false }
    val shredMethod: Flow<String> = ctx.settingsStore.data.map { it[K_SHRED_METHOD] ?: SHRED_SINGLE }
    val autosave: Flow<Boolean> = ctx.settingsStore.data.map { it[K_AUTOSAVE] ?: true }
    // التأثيرات والحركة: يحكم الاهتزاز اللمسي وأنيميشن القوائم معًا — إطفاؤه = صمت وحركة فورية
    val effects: Flow<Boolean> = ctx.settingsStore.data.map { it[K_FX] ?: true }
    // "مستخدمة مؤخرًا" في شيت الإضافة: 6 مفاتيح فقط — الفاسد (عنصر محذوف) يُتجاهل عند القراءة
    val sheetRecent: Flow<List<String>> = ctx.settingsStore.data.map { s ->
        s[K_SHEET_RECENT]?.split(',')?.map { it.trim() }?.filter { it.isNotBlank() }?.take(6) ?: emptyList()
    }
    // قوالب المستخدم (28 سبتمبر 2026): فواصل unicode نادرة تفصل الاسم/المتن/المدخلات
    val tplCustom: Flow<List<Pair<String, String>>> = ctx.settingsStore.data.map { s ->
        s[K_TPL_CUSTOM]?.split('\u001D')?.mapNotNull { e ->
            val p = e.split('\u001F')
            if (p.size == 2 && p[0].isNotBlank()) p[0] to p[1] else null
        }?.take(20) ?: emptyList()
    }
    val batteryPromptShown: Flow<Boolean> = ctx.settingsStore.data.map { it[K_BATT_PROMPT] ?: false } // عُرض طلب البطارية؟
    // المجلد الافتراضي — null تعني الحفظ في القائمة الرئيسية
    val defaultFolderId: Flow<Long?> = ctx.settingsStore.data.map { it[K_DEF_FOLDER] }
    // النسخ السحابي: آخر نجاح + مفتاح التلقائي اليومي
    val cloudLast: Flow<Long> = ctx.settingsStore.data.map { it[K_CLOUD_LAST] ?: 0L }
    val cloudAuto: Flow<Boolean> = ctx.settingsStore.data.map { it[K_CLOUD_AUTO] ?: false }
    val cloudFreq: Flow<String> = ctx.settingsStore.data.map { it[K_CLOUD_FREQ] ?: FREQ_OFF }
    suspend fun setCloudLast(v: Long) { ctx.settingsStore.edit { it[K_CLOUD_LAST] = v } }
    val cloudHash: Flow<String?> = ctx.settingsStore.data.map { it[K_CLOUD_HASH] }
    suspend fun setCloudHash(v: String) { ctx.settingsStore.edit { it[K_CLOUD_HASH] = v } }
    val cloudKeep: Flow<Int> = ctx.settingsStore.data.map { (it[K_CLOUD_KEEP] ?: 5).coerceIn(1, MAX_CLOUD_KEEP) }
    suspend fun setCloudKeep(v: Int) { ctx.settingsStore.edit { it[K_CLOUD_KEEP] = v.coerceIn(1, MAX_CLOUD_KEEP) } }
    private val K_CLOUD_COUNT = intPreferencesKey("cloud_backup_count") // عدد ملاحظات آخر رفع — عتبة الانكماش (27 سبتمبر 2026)
    val cloudCount: Flow<Int> = ctx.settingsStore.data.map { it[K_CLOUD_COUNT] ?: 0 }
    suspend fun setCloudCount(v: Int) { ctx.settingsStore.edit { it[K_CLOUD_COUNT] = v } }
    // كلمة مرور Drive الثابتة — مشفرة بمفتاح الجهاز (iv.cipher) — null = غير مضبوطة (27 سبتمبر 2026)
    private val K_DRIVE_PW = stringPreferencesKey("drive_pw_enc")
    val drivePwEnc: Flow<String?> = ctx.settingsStore.data.map { it[K_DRIVE_PW] }
    suspend fun setDrivePwEnc(v: String?) { ctx.settingsStore.edit { if (v == null) it.remove(K_DRIVE_PW) else it[K_DRIVE_PW] = v } }
    // موعد تأكيد كلمة المرور القادم — عشوائي 3..7 أيام بعد كل نجاح، وغدًا عند التأجيل (27 سبتمبر 2026)
    private val K_DRIVE_PW_NEXT = longPreferencesKey("drive_pw_next")
    val drivePwNext: Flow<Long> = ctx.settingsStore.data.map { it[K_DRIVE_PW_NEXT] ?: 0L }
    suspend fun setDrivePwNext(v: Long) { ctx.settingsStore.edit { it[K_DRIVE_PW_NEXT] = v } }
    // سجل عمليات الاستعادة (27 سبتمبر 2026): JSON بحد 20 — كل دمج (وقته/مصدره/عدده/ids المضافة) + علامة التراجع
    private val K_RESTORE_LOG = stringPreferencesKey("restore_log")
    val restoreLog: Flow<List<RestoreRecord>> = ctx.settingsStore.data.map { prefs ->
        runCatching {
            val arr = org.json.JSONArray(prefs[K_RESTORE_LOG] ?: "[]")
            List(arr.length()) { i -> RestoreRecord.fromJson(arr.getJSONObject(i)) }
        }.getOrDefault(emptyList())
    }
    suspend fun logRestore(rec: RestoreRecord) {
        ctx.settingsStore.edit { prefs ->
            val arr = runCatching { org.json.JSONArray(prefs[K_RESTORE_LOG] ?: "[]") }.getOrDefault(org.json.JSONArray())
            val merged = org.json.JSONArray().put(rec.toJson())
            for (i in 0 until minOf(arr.length(), 19)) merged.put(arr.get(i))
            prefs[K_RESTORE_LOG] = merged.toString()
        }
    }
    suspend fun markRestoreUndone(id: Long) {
        ctx.settingsStore.edit { prefs ->
            val arr = runCatching { org.json.JSONArray(prefs[K_RESTORE_LOG] ?: "[]") }.getOrDefault(org.json.JSONArray())
            val out = org.json.JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                if (o.optLong("id") == id) o.put("undone", true)
                out.put(o)
            }
            prefs[K_RESTORE_LOG] = out.toString()
        }
    }
    suspend fun setCloudAuto(v: Boolean) { ctx.settingsStore.edit { it[K_CLOUD_AUTO] = v } }
    suspend fun setCloudFreq(v: String) { ctx.settingsStore.edit { it[K_CLOUD_FREQ] = v } }

    suspend fun setTheme(v: String) { ctx.settingsStore.edit { it[K_THEME] = v } }
    suspend fun setAppTheme(v: String) { ctx.settingsStore.edit { it[K_APP_THEME] = v } }
    suspend fun setLang(v: String) { ctx.settingsStore.edit { it[K_LANG] = v } }
    suspend fun setBiometricEnabled(v: Boolean) { ctx.settingsStore.edit { it[K_BIO] = v } }
    suspend fun setAutoLockMs(v: Long) { ctx.settingsStore.edit { it[K_AUTOLOCK] = coerceAutoLock(v) } }

    private fun coerceAutoLock(v: Long): Long = if (v in AUTO_LOCK_OPTIONS) v else 30_000L
    suspend fun setSortOrder(v: String) { ctx.settingsStore.edit { it[K_SORT] = v } }
    suspend fun setDefaultColor(v: Int) { ctx.settingsStore.edit { it[K_DEF_COLOR] = v } }
    suspend fun setEditorFont(v: String) { ctx.settingsStore.edit { it[K_FONT] = v } }
    suspend fun setCardDates(v: Boolean) { ctx.settingsStore.edit { it[K_CARD_DATES] = v } }
    suspend fun setOnboardingDone() { ctx.settingsStore.edit { it[K_ONBOARD] = true } }
    suspend fun setPermsReviewed() { ctx.settingsStore.edit { it[K_PERMS] = true } }
    suspend fun setScreenshotsAllowed(v: Boolean) { ctx.settingsStore.edit { it[K_SHOTS] = v } }
    suspend fun setShredEnabled(v: Boolean) { ctx.settingsStore.edit { it[K_SHRED] = v } }
    suspend fun setShredMethod(v: String) { ctx.settingsStore.edit { it[K_SHRED_METHOD] = v } }
    suspend fun setAutosave(v: Boolean) { ctx.settingsStore.edit { it[K_AUTOSAVE] = v } }
    suspend fun setEffects(v: Boolean) { ctx.settingsStore.edit { it[K_FX] = v } }
    suspend fun setSheetRecent(v: List<String>) { ctx.settingsStore.edit { it[K_SHEET_RECENT] = v.take(6).joinToString(",") } }
    suspend fun setTplCustom(v: List<Pair<String, String>>) {
        ctx.settingsStore.edit {
            it[K_TPL_CUSTOM] = v.take(20)
                .map { (n, b) -> "$n\u001F${b.take(8000)}" }
                .joinToString("\u001D")
        }
    }
    suspend fun setBatteryPromptShown() { ctx.settingsStore.edit { it[K_BATT_PROMPT] = true } }
    suspend fun setDefaultFolderId(v: Long?) { ctx.settingsStore.edit { if (v == null) it.remove(K_DEF_FOLDER) else it[K_DEF_FOLDER] = v } }
    // شريط التنقل: تُحفظ 5 مفاتيح مميزة بلا تكرار — الفاسد يُستبدل بالافتراضي
    val navSlots: Flow<List<String>> = ctx.settingsStore.data.map { prefs ->
        // ترحيل 27 سبتمبر 2026: زر الإضافة القديم يُستبدل برسائل النفس في المخزن أيضًا
        val raw = (prefs[K_NAV_SLOTS] ?: DEFAULT_NAV_SLOTS.joinToString(",")).split(",").map { it.trim() }.map { if (it == NAV_ADD) NAV_SELF else it }.filter { it in NAV_ELIGIBLE }.distinct()
        if (raw.isEmpty()) DEFAULT_NAV_SLOTS else (raw + DEFAULT_NAV_SLOTS).distinct().take(5)
    }
    // الذكاء الاصطناعي (27 سبتمبر 2026): BYOK — الإعدادات حرة، والمفتاح يُختم خارج المخزن
    val aiEndpoint: Flow<String> = ctx.settingsStore.data.map { it[K_AI_ENDPOINT] ?: com.nota.notes.data.AiRewriteService.DEFAULT_ENDPOINT }
    val aiModel: Flow<String> = ctx.settingsStore.data.map { it[K_AI_MODEL] ?: com.nota.notes.data.AiRewriteService.DEFAULT_MODEL }
    val aiKeySealed: Flow<String?> = ctx.settingsStore.data.map { it[K_AI_KEY] }
    // P0-1 (أكتوبر 2026): https فقط — أي http story يُرفض باستثناء قبل الحفظ.
    // الروابط غير المشفرة تُعرّض مفتاح API ونصوص الملاحظات للاعتراض على الشبكة.
    suspend fun setAiEndpoint(v: String) {
        val e = v.trim().trimEnd('/')
        require(e.startsWith("https://")) { "يجب أن يبدأ الرابط بـ https:// — الروابط غير المشفرة تُعرّض مفتاحك ونصوص ملاحظاتك للاعتراض." }
        ctx.settingsStore.edit { it[K_AI_ENDPOINT] = e }
    }
    // P0-3 (أكتوبر 2026): مسح كل الإعدادات — إعادة ضبط المصنع الصادقة.
    suspend fun clearAll() { ctx.settingsStore.edit { it.clear() } }
    suspend fun setAiModel(v: String) { ctx.settingsStore.edit { it[K_AI_MODEL] = v.trim() } }
    suspend fun setAiKeySealed(v: String?) { ctx.settingsStore.edit { if (v == null) it.remove(K_AI_KEY) else it[K_AI_KEY] = v } }
    // الترحيب والسرعة (27 سبتمبر 2026): الاسم للتحية + تفعيل الرسالة + الوضع السريع بعد القفل
    val username: Flow<String> = ctx.settingsStore.data.map { it[K_USERNAME] ?: "" }
    val welcomeEnabled: Flow<Boolean> = ctx.settingsStore.data.map { it[K_WELCOME] ?: true }
    val quickMode: Flow<Boolean> = ctx.settingsStore.data.map { it[K_QUICK_MODE] ?: false }
    suspend fun setUsername(v: String) { ctx.settingsStore.edit { it[K_USERNAME] = v.trim().take(40) } }
    suspend fun setWelcomeEnabled(v: Boolean) { ctx.settingsStore.edit { it[K_WELCOME] = v } }
    suspend fun setQuickMode(v: Boolean) { ctx.settingsStore.edit { it[K_QUICK_MODE] = v } }
    // التجربة المجانية: تُعرض مرة واحدة أول استخدام — والقبول يفتح كل مزايا Pro حتى انتهاء المدة
    val trialOffered: Flow<Boolean> = ctx.settingsStore.data.map { it[K_TRIAL_OFFERED] ?: false }
    val trialStart: Flow<Long> = ctx.settingsStore.data.map { it[K_TRIAL_START] ?: 0L }
    suspend fun dismissTrialOffer() { ctx.settingsStore.edit { it[K_TRIAL_OFFERED] = true } }
    suspend fun startTrial() { ctx.settingsStore.edit { it[K_TRIAL_START] = System.currentTimeMillis(); it[K_TRIAL_OFFERED] = true } }
    val fabVisible: Flow<Boolean> = ctx.settingsStore.data.map { it[K_FAB] ?: true }
    suspend fun setNavSlots(v: List<String>) {
        val clean = (v.filter { it in NAV_ELIGIBLE }.distinct() + DEFAULT_NAV_SLOTS).distinct().take(5)
        ctx.settingsStore.edit { it[K_NAV_SLOTS] = clean.joinToString(",") }
    }
    suspend fun setFabVisible(v: Boolean) { ctx.settingsStore.edit { it[K_FAB] = v } }
    suspend fun currentLang(): String = lang.first()
}

// سجل عملية استعادة واحدة — المصدر file|drive|ncloud — ids المضافة للتراجع بالنقل للسلة
data class RestoreRecord(
    val id: Long = System.currentTimeMillis(),
    val at: Long = System.currentTimeMillis(),
    val src: String = "file",
    val added: Int = 0,
    val skipped: Int = 0,
    val addedIds: List<Long> = emptyList(),
    val undone: Boolean = false
) {
    fun toJson(): org.json.JSONObject = org.json.JSONObject()
        .put("id", id).put("at", at).put("src", src)
        .put("added", added).put("skipped", skipped).put("undone", undone)
        .put("ids", org.json.JSONArray(addedIds.take(2000)))
    companion object {
        fun fromJson(o: org.json.JSONObject): RestoreRecord = RestoreRecord(
            id = o.optLong("id"), at = o.optLong("at"), src = o.optString("src", "file"),
            added = o.optInt("added"), skipped = o.optInt("skipped"),
            addedIds = o.optJSONArray("ids")?.let { a -> List(a.length()) { a.optLong(it) } } ?: emptyList(),
            undone = o.optBoolean("undone", false)
        )
    }
}
