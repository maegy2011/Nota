package com.nota.notes.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nota.notes.R
import com.nota.notes.autolockLabel
import com.nota.notes.data.SettingsStore

// صفحة الإعدادات الموحدة: الأساسية + المتقدمة مدمجة في شاشة واحدة (قرار 23 سبتمبر 2026)
// عن التطبيق انتقلت للقائمة الجانبية وفُك ارتباطها من هنا
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    theme: String, lang: String,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    autoLockMs: Long,
    screenshotsAllowed: Boolean,
    onTheme: (String) -> Unit, onLang: (String) -> Unit,
    // هوية الألوان العشر (قرار 26 سبتمبر 2026): 2 مجاني + 8 Pro — المقفل يُقاد للترقية
    appTheme: String = "warm", onAppTheme: (String) -> Unit = {},
    onBiometric: (Boolean) -> Unit, onAutoLock: (Long) -> Unit,
    onScreenshots: (Boolean) -> Unit,
    // المتقدمة المدمجة: لون افتراضي + خط + تواريخ + حفظ تلقائي + PIN + صحة التنبيهات
    defaultColor: Int, font: String, cardDates: Boolean, autosave: Boolean,
    onDefaultColor: (Int) -> Unit, onFont: (String) -> Unit, onCardDates: (Boolean) -> Unit, onAutosave: (Boolean) -> Unit,
    authVm: AuthViewModel,
    shredEnabled: Boolean,
    shredMethod: String,
    onShredEnabled: (Boolean) -> Unit,
    onShredMethod: (String) -> Unit,
    // بوابة Pro للإتلاف (قرار 23 سبتمبر 2026): المقفل يرى الصف ويُقاد للترقية — بلا تفعيل صامت
    isPro: Boolean = true,
    onProLocked: () -> Unit = {},
    // تخصيص شريط التنقل — Pro: المقفل يُقاد للترقية (قاعدة القفل المرئي 26 سبتمبر 2026)
    onCustomizeNav: () -> Unit = {},
    alarmHealth: AlarmHealth? = null,
    onOpenNotif: () -> Unit = {},
    onOpenExactAlarm: () -> Unit = {},
    onOpenBattery: () -> Unit = {},
    // الذكاء الاصطناعي BYOK (27 سبتمبر 2026): مجاني للجميع — المفتاح من المستخدم ويُخزَّن مشفرًا
    aiEndpoint: String = com.nota.notes.data.AiRewriteService.DEFAULT_ENDPOINT,
    aiModel: String = com.nota.notes.data.AiRewriteService.DEFAULT_MODEL,
    aiKeySet: Boolean = false,
    onAiEndpoint: (String) -> Unit = {},
    onAiModel: (String) -> Unit = {},
    onAiKey: (String) -> Unit = {},
    onAiTest: ((Boolean) -> Unit) -> Unit = {},
    // الترحيب والسرعة (27 سبتمبر 2026)
    username: String = "",
    welcomeOn: Boolean = true,
    quickOn: Boolean = false,
    onUsername: (String) -> Unit = {},
    onWelcome: (Boolean) -> Unit = {},
    onQuick: (Boolean) -> Unit = {},
    // التأثيرات والحركة (28 سبتمبر 2026): مفتاح واحد يحكم الاهتزاز اللمسي وأنيميشن القوائم
    effectsOn: Boolean = true,
    onEffects: (Boolean) -> Unit = {}
) {
    // تأكيد قبل تغيير اللغة (بأمر محمد 27 سبتمبر 2026): يعيد تحميل الواجهة كلها
    var pendingLang by remember { mutableStateOf<String?>(null) }
    pendingLang?.let { v ->
        ConfirmDialog(
            message = stringResource(R.string.confirm_lang_msg),
            onConfirm = { pendingLang = null; onLang(v) },
            onDismiss = { pendingLang = null }
        )
    }
    // خلفية الثيم صراحةً — خلفية النافذة ثابتة داكنة فلا تُترك شفافة
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val secScope = rememberCoroutineScope()
        val scrollState = rememberScrollState()
        val hasRec by authVm.hasRecoveryKey.collectAsState()
        val devCred by authVm.deviceCredEnabled.collectAsState()
        val bioReq = remember { BringIntoViewRequester() }
        val recReq = remember { BringIntoViewRequester() }
        val devReq = remember { BringIntoViewRequester() }
        val lockReq = remember { BringIntoViewRequester() }
        val shotsReq = remember { BringIntoViewRequester() }
        fun go(req: BringIntoViewRequester) = secScope.launch { runCatching { req.bringIntoView() } }
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
        // بطاقة حالة الأمان (Commit 4): ملخص + روابط — بلا أزرار إجراء
        val pinMin6 by authVm.pinMin6Ok.collectAsState()
        val lastOk by authVm.lastUnlockOk.collectAsState()
        val failsTotal by authVm.failTotal.collectAsState()
        SecStatusCard(
            unlockOk = biometricEnabled && biometricAvailable,
            unlockText = stringResource(R.string.sec_unlock_pin_unknown),
            recOn = hasRec,
            devOn = devCred,
            autoLockText = autolockLabel(LocalContext.current, autoLockMs),
            shotsBlocked = !screenshotsAllowed,
            onUnlock = { go(bioReq) },
            onRec = { go(recReq) },
            onDev = { go(devReq) },
            onLock = { go(lockReq) },
            onShots = { go(shotsReq) },
            lastOkText = if (lastOk > 0) secFmtDate(lastOk) else stringResource(R.string.sec_never),
            failsTotal = failsTotal
        )
        SectionCard(stringResource(R.string.theme_title), top = 0.dp) {
            ThemeOption("system", stringResource(R.string.theme_system), theme, onTheme)
            ThemeOption("light", stringResource(R.string.theme_light), theme, onTheme)
            ThemeOption("dark", stringResource(R.string.theme_dark), theme, onTheme)
        }
        SectionCard(stringResource(R.string.app_theme_title)) {
            AppThemeOptions(selected = appTheme, isPro = isPro, onPick = onAppTheme, onProLocked = onProLocked)
        }
        // اللغات السبع عشرة (27 سبتمبر 2026): علم يميز كل لغة — الاسم مترجم من ملف اللغة نفسها
        SectionCard(stringResource(R.string.lang_title)) {
            ThemeOption("system", stringResource(R.string.lang_system), lang, { pendingLang = it })
            LangOption("🇪🇬", "ar", stringResource(R.string.lang_ar), lang, { pendingLang = it })
            LangOption("🇬🇧", "en", stringResource(R.string.lang_en), lang, { pendingLang = it })
            LangOption("🇷🇺", "ru", stringResource(R.string.lang_ru), lang, { pendingLang = it })
            LangOption("🇮🇳", "hi", stringResource(R.string.lang_hi), lang, { pendingLang = it })
            LangOption("🇪🇸", "es", stringResource(R.string.lang_es), lang, { pendingLang = it })
            LangOption("🇮🇹", "it", stringResource(R.string.lang_it), lang, { pendingLang = it })
            LangOption("🇯🇵", "ja", stringResource(R.string.lang_ja), lang, { pendingLang = it })
            LangOption("🇨🇳", "zh", stringResource(R.string.lang_zh), lang, { pendingLang = it })
            LangOption("🇫🇷", "fr", stringResource(R.string.lang_fr), lang, { pendingLang = it })
            LangOption("🇹🇷", "tr", stringResource(R.string.lang_tr), lang, { pendingLang = it })
            LangOption("🇲🇾", "ms", stringResource(R.string.lang_ms), lang, { pendingLang = it })
            LangOption("🇩🇪", "de", stringResource(R.string.lang_de), lang, { pendingLang = it })
            LangOption("🇵🇰", "ur", stringResource(R.string.lang_ur), lang, { pendingLang = it })
            LangOption("🇳🇱", "nl", stringResource(R.string.lang_nl), lang, { pendingLang = it })
            LangOption("🇵🇱", "pl", stringResource(R.string.lang_pl), lang, { pendingLang = it })
            LangOption("🇵🇹", "pt", stringResource(R.string.lang_pt), lang, { pendingLang = it })
            LangOption("🇺🇦", "uk", stringResource(R.string.lang_uk), lang, { pendingLang = it })
        }
        SectionCard(stringResource(R.string.security_title)) {
            // بطاقة ترحيل المستخدمين الحاليين (Commit 2): بلا مفتاح استرداد — تظهر حتى مع بصمة مفعّلة
            val migVisible by authVm.migrationVisible.collectAsState()
            if (migVisible) {
                var showMigSetup by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            stringResource(R.string.mig_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            stringResource(R.string.mig_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        if (!showMigSetup) {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                TextButton(onClick = { showMigSetup = true; authVm.touch() }) {
                                    Text(stringResource(R.string.mig_setup))
                                }
                                TextButton(onClick = { authVm.snoozeMigration() }) {
                                    Text(stringResource(R.string.mig_later))
                                }
                            }
                        } else {
                            RecoveryKeySetup(
                                authVm = authVm,
                                onDone = { showMigSetup = false; authVm.touch() },
                                onBack = { showMigSetup = false }
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().bringIntoViewRequester(bioReq).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.bio_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (biometricAvailable) stringResource(R.string.bio_sub)
                        else stringResource(R.string.bio_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // حوار تأكيد التعطيل (Commit 4ج): التعطيل يُزيل مسار استرداد — إلغاء افتراضي
                var showDisableConfirm by remember { mutableStateOf(false) }
                Switch(
                    checked = biometricEnabled && biometricAvailable,
                    enabled = biometricAvailable,
                    onCheckedChange = { v ->
                        if (!v && biometricEnabled) showDisableConfirm = true
                        else onBiometric(v)
                    }
                )
                if (showDisableConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDisableConfirm = false },
                        title = { Text(stringResource(R.string.sec_disable_title)) },
                        text = { Text(stringResource(R.string.sec_disable_msg)) },
                        confirmButton = {
                            TextButton(onClick = { showDisableConfirm = false; onBiometric(false) }) {
                                Text(stringResource(R.string.sec_disable_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDisableConfirm = false }) { Text(stringResource(R.string.cancel)) }
                        }
                    )
                }
            }
            // تحذير تعطيل البصمة: الـ PIN يبقى الطريق الوحيد — يُعرض عند التفعيل فقط
            if (biometricEnabled && biometricAvailable) {
                Text(
                    stringResource(R.string.bio_disable_warn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            // فحص enrollment (Commit 4ب): مفعّلة لكن غير مسجّلة — تحذير بلا إلغاء تلقائي
            if (biometricEnabled && biometricAvailable && !authVm.auth.isBioEnrolled()) {
                Text(
                    stringResource(R.string.sec_enroll_warn),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
            Text(
                stringResource(R.string.autolock_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            AutoLockOptions(autoLockMs, onAutoLock)
            // توضيح سلوك تغيير المهلة (Commit 4د): تُطبق فورًا (LaunchedEffect في App يزامن AuthManager)
            Text(
                stringResource(R.string.autolock_applied),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.bringIntoViewRequester(lockReq).padding(top = 2.dp)
            )
            // إدارة مفتاح الاسترداد (Commit 2): الحالة + إعداد/إعادة إنشاء + تحقق بالعبارة الكاملة
            // انحراف موثق: التحقق بإدخال الـ 12 كلمة كاملة بدل عرض 3 كلمات — الكلمات لا تُخزَّن أبدًا فلا يمكن عرضها
            var showRecSetup by remember { mutableStateOf(false) }
            var showRecVerify by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().bringIntoViewRequester(recReq).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (hasRec) R.string.reckey_status_on else R.string.reckey_status_off),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showRecSetup = true; authVm.touch() }) {
                    Text(stringResource(if (hasRec) R.string.reckey_regen_btn else R.string.reckey_setup_btn))
                }
                if (hasRec) {
                    TextButton(onClick = { showRecVerify = true; authVm.touch() }) {
                        Text(stringResource(R.string.reckey_verify_btn))
                    }
                }
            }
            if (showRecSetup) {
                RecoveryKeySetup(
                    authVm = authVm,
                    onDone = { showRecSetup = false; authVm.touch() },
                    onBack = { showRecSetup = false }
                )
            }
            if (showRecVerify) {
                RecVerifyDialog(authVm = authVm, onDismiss = { showRecVerify = false })
            }
            // إعادة التعيين عبر قفل الجهاز (Commit 2): OFF افتراضيًا + حوار تحذير صريح عند التفعيل
            var showDevWarn by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().bringIntoViewRequester(devReq).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.devcred_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.devcred_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = devCred,
                    onCheckedChange = { v ->
                        if (v) showDevWarn = true
                        else { authVm.setDeviceCred(false); authVm.touch() }
                    }
                )
            }
            if (showDevWarn) {
                AlertDialog(
                    onDismissRequest = { showDevWarn = false },
                    title = { Text(stringResource(R.string.devcred_warn_title)) },
                    text = { Text(stringResource(R.string.devcred_warn_msg)) },
                    confirmButton = {
                        TextButton(onClick = { showDevWarn = false; authVm.setDeviceCred(true); authVm.touch() }) {
                            Text(stringResource(R.string.devcred_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDevWarn = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
            // الإتلاف الآمن قبل الحذف: اختياري (معطّل افتراضيًا) + اختيار الطريقة عند التفعيل — Pro فقط
            Row(
                Modifier.fillMaxWidth()
                    .then(if (isPro) Modifier else Modifier.clickable { onProLocked() })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.shred_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.shred_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isPro) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Switch(
                    checked = shredEnabled && isPro,
                    enabled = isPro,
                    onCheckedChange = onShredEnabled
                )
            }
            if (shredEnabled && isPro) {
                ShredMethodOptions(shredMethod, onShredMethod)
            }
            Row(
                Modifier.fillMaxWidth().bringIntoViewRequester(shotsReq).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.shots_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.shots_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // صياغة الحظر (27 سبتمبر 2026): السويتش يعكس الحماية — شغّال = محظور (الافتراضي)، والتخزين screenshotsAllowed معكوسًا
                Switch(checked = !screenshotsAllowed, onCheckedChange = { onScreenshots(!it) })
            }
        }
        // شريط التنقل السفلي — Pro: تخصيص الخانات + الترتيب + FAB (قرار 26 سبتمبر 2026)
        SectionCard(stringResource(R.string.navbar_customize_row)) {
            Row(
                Modifier.fillMaxWidth()
                    .then(if (isPro) Modifier.clickable { onCustomizeNav() } else Modifier.clickable { onProLocked() })
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.navbar_customize_row), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.navbar_customize_row_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!isPro) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
        // الذكاء الاصطناعي (27 سبتمبر 2026): خادم OpenAI-compatible + مفتاح المستخدم — مجاني للجميع
        SectionCard(stringResource(R.string.ai_title)) {
            Text(
                stringResource(R.string.ai_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = aiEndpoint,
                onValueChange = onAiEndpoint,
                label = { Text(stringResource(R.string.ai_endpoint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = aiModel,
                onValueChange = onAiModel,
                label = { Text(stringResource(R.string.ai_model)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            var aiKeyDraft by remember { mutableStateOf("") }
            if (aiKeySet) {
                // اختبار الاتصال: ping مصغر يتحقق من المفتاح والخادم والنموذج معًا
                var aiTest by remember { mutableStateOf<Boolean?>(null) }
                var aiTesting by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.ai_key_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onAiKey(""); aiKeyDraft = ""; aiTest = null }) {
                        Text(stringResource(R.string.ai_key_clear))
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val ts = aiTest
                    Text(
                        when {
                            aiTesting -> stringResource(R.string.ai_working)
                            ts == true -> stringResource(R.string.ai_test_ok)
                            ts == false -> stringResource(R.string.ai_test_fail)
                            else -> stringResource(R.string.ai_test_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (ts) {
                            true -> MaterialTheme.colorScheme.primary
                            false -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { aiTesting = true; aiTest = null; onAiTest { aiTesting = false; aiTest = it } },
                        enabled = !aiTesting
                    ) {
                        Text(stringResource(R.string.ai_test))
                    }
                }
            } else {
                OutlinedTextField(
                    value = aiKeyDraft,
                    onValueChange = { aiKeyDraft = it },
                    label = { Text(stringResource(R.string.ai_key)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Button(
                    onClick = { onAiKey(aiKeyDraft); aiKeyDraft = "" },
                    enabled = aiKeyDraft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.ai_key_save))
                }
            }
        }
        // الترحيب والوضع السريع (27 سبتمبر 2026): الاسم للتحية + تفعيل الرسالة + فتح سريع على ملاحظة جديدة
        SectionCard(stringResource(R.string.welcome_sec)) {
            var nameDraft by remember(username) { mutableStateOf(username) }
            OutlinedTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it; onUsername(it) },
                label = { Text(stringResource(R.string.username_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.welcome_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.welcome_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = welcomeOn, onCheckedChange = onWelcome)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.quickmode_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.quickmode_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = quickOn, onCheckedChange = onQuick)
            }
        }
        // المتقدمة المدمجة هنا (كانت شاشة منفصلة): لون افتراضي + خط + كتابة + PIN + صحة التنبيهات
        SectionCard(stringResource(R.string.defcolor_title)) {
            Text(
                stringResource(R.string.defcolor_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            ColorPickerRow(selected = defaultColor, onPick = onDefaultColor)
        }
        SectionCard(stringResource(R.string.font_title)) {
            FontOption(SettingsStore.FONT_SMALL, stringResource(R.string.font_small), font, onFont)
            FontOption(SettingsStore.FONT_NORMAL, stringResource(R.string.font_normal), font, onFont)
            FontOption(SettingsStore.FONT_LARGE, stringResource(R.string.font_large), font, onFont)
        }
        SectionCard(stringResource(R.string.write_title)) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.fx_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.fx_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = effectsOn, onCheckedChange = onEffects)
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.autosave_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.autosave_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autosave, onCheckedChange = onAutosave)
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.carddates_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.carddates_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = cardDates, onCheckedChange = onCardDates)
            }
        }
        SectionCard(stringResource(R.string.changepin_title)) {
            ChangePinBlock(authVm)
            // إجراء «إعادة ضبط PIN» مستقل — نفس شيت الاسترداد (مراجعة أكتوبر 2026 — Commit 1)
            var showReset by remember { mutableStateOf(false) }
            var resetNewPin by remember { mutableStateOf(false) }
            var resetErr by remember { mutableStateOf<String?>(null) }
            val resetCtx = LocalContext.current
            val resetAct = resetCtx as? androidx.fragment.app.FragmentActivity
            Row(
                Modifier.fillMaxWidth().clickable { showReset = true; resetErr = null; resetNewPin = false }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reset_pin_title), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.reset_pin_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val resetScope = androidx.compose.runtime.rememberCoroutineScope()
            RecoverySheet(
                visible = showReset,
                onDismiss = { showReset = false; resetNewPin = false; resetErr = null },
                canBio = authVm.canBiometric() && resetAct != null,
                bioEnrolled = authVm.auth.isBioEnrolled(),
                hasRecoveryKey = hasRec,
                deviceCredEnabled = devCred,
                onBio = {
                    resetAct?.let { a ->
                        authVm.promptBiometric(
                            a,
                            onOk = { resetNewPin = true; resetErr = null },
                            onErr = { resetErr = resetCtx.getString(R.string.recover_generic_fail) }
                        )
                    }
                },
                onRecoveryKey = { key ->
                    resetScope.launch {
                        when (val r = authVm.verifyRecoveryKey(key)) {
                            is AuthViewModel.RecoveryResult.Success -> { resetNewPin = true; resetErr = null }
                            is AuthViewModel.RecoveryResult.Wrong -> resetErr = resetCtx.getString(R.string.recover_generic_fail)
                            is AuthViewModel.RecoveryResult.Locked -> {
                                val s = ((r.untilMs - System.currentTimeMillis() + 999) / 1000).toInt()
                                resetErr = resetCtx.getString(R.string.pin_locked_fmt, s)
                            }
                        }
                    }
                },
                recoveryError = resetErr,
                onDeviceCred = {
                    resetAct?.let { a ->
                        authVm.promptDeviceCredential(
                            a,
                            onOk = { resetNewPin = true; resetErr = null },
                            onErr = { resetErr = if (it.isNotBlank()) it else resetCtx.getString(R.string.recover_generic_fail) }
                        )
                    }
                },
                onFactoryReset = { authVm.factoryReset {} },
                newPinContent = if (resetNewPin) {
                    {
                        Text(
                            stringResource(R.string.recover_newpin_title),
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        SetPinScreen(onDone = { np ->
                            authVm.changePin(np) { showReset = false; resetNewPin = false }
                        })
                    }
                } else null
            )
        }
        if (alarmHealth != null) {
            SectionCard(stringResource(R.string.health_title)) {
                Text(
                    stringResource(if (alarmHealth.ok()) R.string.health_ok_sub else R.string.health_bad_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alarmHealth.ok()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                HealthLine(ok = alarmHealth.notif, label = stringResource(R.string.health_notif), onFix = onOpenNotif)
                HealthLine(ok = alarmHealth.exact, label = stringResource(R.string.health_exact), onFix = onOpenExactAlarm)
                HealthLine(ok = alarmHealth.battery, label = stringResource(R.string.health_battery), onFix = onOpenBattery)
            }
        }
        }
    }
}

@Composable
private fun ShredMethodOptions(selected: String, onPick: (String) -> Unit) {
    val labels = mapOf(
        SettingsStore.SHRED_SINGLE to stringResource(R.string.shred_single),
        SettingsStore.SHRED_DOD to stringResource(R.string.shred_dod),
        SettingsStore.SHRED_SCHNEIER to stringResource(R.string.shred_schneier)
    )
    SettingsStore.SHRED_OPTIONS.forEach { v ->
        ThemeOption(v, labels[v] ?: v, selected) { onPick(v) }
    }
}

@Composable
private fun AutoLockOptions(selected: Long, onPick: (Long) -> Unit) {
    val labels = mapOf(
        15_000L to stringResource(R.string.autolock_15s),
        30_000L to stringResource(R.string.autolock_30s),
        60_000L to stringResource(R.string.autolock_1m),
        120_000L to stringResource(R.string.autolock_2m),
        300_000L to stringResource(R.string.autolock_5m),
        SettingsStore.AUTO_LOCK_NEVER to stringResource(R.string.autolock_never)
    )
    SettingsStore.AUTO_LOCK_OPTIONS.forEach { v ->
        ThemeOption(v.toString(), labels[v] ?: v.toString(), selected.toString()) { onPick(v) }
    }
}

// منتقي هوية الألوان: 3 نقاط معاينة + اسم + قفل Pro — النقاط من المخطط الفاتح (والدافئ من مخططيه الأصليين)
@Composable
private fun AppThemeOptions(selected: String, isPro: Boolean, onPick: (String) -> Unit, onProLocked: () -> Unit) {
    NotaThemes.forEach { th ->
        val locked = !th.free && !isPro
        val scheme = th.light ?: WarmLightScheme
        val dots = listOf(scheme.primary, scheme.secondary, scheme.tertiary)
        Row(
            Modifier.fillMaxWidth().clickable { if (locked) onProLocked() else onPick(th.id) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = (selected == th.id), onClick = { if (locked) onProLocked() else onPick(th.id) })
            Row(Modifier.padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                dots.forEach { c ->
                    Box(
                        Modifier.size(16.dp).clip(androidx.compose.foundation.shape.CircleShape)
                            .background(c)
                    )
                }
            }
            Text(stringResource(th.nameRes), modifier = Modifier.weight(1f))
            if (locked) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.pro_only),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
// خيار لغة بعلم مميز (27 سبتمبر 2026): العلم إيموجي ثابت مستقل عن اللغة + الاسم مترجمًا
private fun LangOption(flag: String, value: String, label: String, selected: String, onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = (selected == value), onClick = { onPick(value) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (selected == value), onClick = { onPick(value) })
        Text(flag, modifier = Modifier.padding(start = 8.dp))
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ThemeOption(value: String, label: String, selected: String, onPick: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = (selected == value), onClick = { onPick(value) })
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = (selected == value), onClick = { onPick(value) })
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

// بطاقة حالة الأمان (Commit 4أ): 5 مؤشرات بأيقونات هادئة (✓ أساسي، ⚠ ثالثي —
// لا أحمر صارخ) + روابط انتقال للأقسام — ملخص فقط بلا أزرار إجراء + سجل المحاولات.
@Composable
private fun SecStatusCard(
    unlockOk: Boolean,
    unlockText: String,
    recOn: Boolean,
    devOn: Boolean,
    autoLockText: String,
    shotsBlocked: Boolean,
    onUnlock: () -> Unit,
    onRec: () -> Unit,
    onDev: () -> Unit,
    onLock: () -> Unit,
    onShots: () -> Unit,
    lastOkText: String,
    failsTotal: Int
) {
    SectionCard(stringResource(R.string.sec_status_title), top = 0.dp) {
        SecRow(ok = unlockOk, label = stringResource(R.string.sec_row_unlock), value = unlockText, onGo = onUnlock)
        SecRow(
            ok = recOn,
            label = stringResource(R.string.sec_row_rec),
            value = stringResource(if (recOn) R.string.sec_on else R.string.sec_off),
            onGo = onRec
        )
        SecRow(
            ok = true,
            label = stringResource(R.string.sec_row_dev),
            value = stringResource(if (devOn) R.string.sec_dev_on else R.string.sec_dev_off),
            onGo = onDev
        )
        SecRow(ok = true, label = stringResource(R.string.sec_row_autolock), value = autoLockText, onGo = onLock)
        SecRow(
            ok = shotsBlocked,
            label = stringResource(R.string.sec_row_shots),
            value = stringResource(if (shotsBlocked) R.string.sec_shots_blocked else R.string.sec_shots_allowed),
            onGo = onShots
        )
        // سجل المحاولات (Commit 4هـ — اختياري ونُفّذ): عدد وتوقيت فقط — بلا تفاصيل محتوى
        Text(
            stringResource(R.string.sec_last_ok, lastOkText),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
        Text(
            stringResource(R.string.sec_fails, failsTotal),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecRow(ok: Boolean, label: String, value: String, onGo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (ok) "✓" else "⚠",
            style = MaterialTheme.typography.bodyLarge,
            color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onGo) { Text(stringResource(R.string.sec_go)) }
    }
}

// تنسيق التاريخ للسجل — محلي بلا مكتبات جديدة
private fun secFmtDate(ms: Long): String = runCatching {
    java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(ms))
}.getOrDefault("—")
