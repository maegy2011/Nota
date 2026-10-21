package com.nota.notes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.LayoutDirection
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import com.nota.notes.data.NoteBgFiles
import com.nota.notes.data.NotePdfExporter
import com.nota.notes.ui.AccountViewModel
import com.nota.notes.ui.AccountScreen
import com.nota.notes.ui.DrawerKeys
import com.nota.notes.ui.NotaBottomBar
import com.nota.notes.ui.NavBarCustomizeScreen
import com.nota.notes.ui.NotaDrawerContent
import com.nota.notes.ui.PermissionsReviewScreen
import com.nota.notes.ui.AmiriFamily
import com.nota.notes.ui.AuthViewModel
import com.nota.notes.ui.DmSerifFamily
import com.nota.notes.ui.TajawalFamily
import com.nota.notes.ui.LockScreen
import com.nota.notes.ui.LuxurySplashScreen
import com.nota.notes.ui.LocalNotaDark
import com.nota.notes.ui.NotaTheme
import com.nota.notes.ui.NoteEditorScreen
import com.nota.notes.ui.LockChallenge
import com.nota.notes.ui.MarkdownFormat
import com.nota.notes.ui.NotesListScreen
import com.nota.notes.ui.TrashScreen
import com.nota.notes.ui.NotesViewModel
import com.nota.notes.ui.FoldersScreen
import com.nota.notes.ui.FoldersViewModel
import com.nota.notes.ui.Haptics
import com.nota.notes.ui.OnboardingScreen
import com.nota.notes.ui.PaywallScreen
import com.nota.notes.ui.PremiumViewModel
import com.nota.notes.ui.rememberGoogleSignInLauncher
import com.nota.notes.ui.SetPinScreen
import com.nota.notes.ui.BackupScreen
import com.nota.notes.ui.CategoriesScreen
import com.nota.notes.ui.CloudBackupViewModel
import com.nota.notes.ui.NotaCloudViewModel
import com.nota.notes.ui.RemindersScreen
import com.nota.notes.ui.TagsScreen
import com.nota.notes.ui.SelfChatScreen
import com.nota.notes.ui.SettingsScreen
import com.nota.notes.ui.TrialOfferDialog
import com.nota.notes.ui.WelcomeDialog
import com.nota.notes.ui.SettingsViewModel

private enum class Screen { LIST, ARCHIVE, TRASH, FAVORITES, FOLDERS, CATEGORIES, SETTINGS, ADVANCED, ABOUT, TAGS_MGR, REMINDERS, CLOUD, PAYWALL, ACCOUNT, NAVBAR, SELF }

// مدة الحفظ التلقائي بالميللي ثانية (27 سبتمبر 2026): ثابت واحد يحكم الكود والنص معًا
const val AUTOSAVE_DELAY_MS = 300L

// تسمية مهلة القفل التلقائي للرسائل (Commit 3ز): تُطابق نصوص الإعدادات نفسها
fun autolockLabel(ctx: android.content.Context, ms: Long): String = when (ms) {
    15_000L -> ctx.getString(R.string.autolock_15s)
    30_000L -> ctx.getString(R.string.autolock_30s)
    60_000L -> ctx.getString(R.string.autolock_1m)
    120_000L -> ctx.getString(R.string.autolock_2m)
    300_000L -> ctx.getString(R.string.autolock_5m)
    else -> ctx.getString(R.string.autolock_30s)
}

@Composable
fun App(
    newNoteSignal: Int = 0,
    openNoteId: Long = -1L,
    vm: NotesViewModel = hiltViewModel(),
    settings: SettingsViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    premiumVm: PremiumViewModel = hiltViewModel(),
    accountVm: AccountViewModel = hiltViewModel()
) {
    val themeMode by settings.theme.collectAsStateWithLifecycle()
    val appThemeId by settings.appTheme.collectAsStateWithLifecycle()
    val lang by settings.lang.collectAsStateWithLifecycle()
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme() // الافتراضي: تلقائي يتبع النظام
    }
    // العربية RTL افتراضياً — الإنجليزية الصريحة فقط LTR
    val dir = if (lang == "en") LayoutDirection.Ltr else LayoutDirection.Rtl

    // خط العناوين يتبع اللغة: أميري للعربية وDM Serif للإنجليزية — والنصوص دائمًا تجوال
    val headFam = if (lang == "en") DmSerifFamily else AmiriFamily
    CompositionLocalProvider(LocalLayoutDirection provides dir, LocalNotaDark provides dark) {
        NotaTheme(dark = dark, head = headFam, body = TajawalFamily, arabic = (lang != "en"), themeId = appThemeId) {
            val loaded by authVm.loaded.collectAsStateWithLifecycle()
            val pinHash by authVm.pinHash.collectAsStateWithLifecycle()
            val onboarded by settings.onboardingDone.collectAsStateWithLifecycle(initialValue = true)
            val permsReviewed by settings.permsReviewed.collectAsStateWithLifecycle(initialValue = true)
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            // بوابة شاشة البداية 2026: التهيئة الحقيقية (loaded) + انتهاء الحركة (350ms) — بلا تأخير مصطنع
            var splashAnimDone by remember { mutableStateOf(false) }
            // مؤشر التهيئة الطويلة (مهمة 3): يظهر فقط إن تجاوزت التهيئة 1000ms — لا يغيّر البوابة ولا ينقل قسرًا
            var showSplashSpinner by remember { mutableStateOf(false) }
            LaunchedEffect(loaded) {
                if (!loaded) {
                    kotlinx.coroutines.delay(1000)
                    showSplashSpinner = true
                }
            }
            // دمج Splash مع القفل (Commit 3و): مقفل من جلسة سابقة → شاشة القفل مباشرة بلا انتقال مزدوج
            val skipSplash = loaded && pinHash != null
            // تحرير شاشة النظام مع اكتمال البوابة (مهمة 1: مربوط بـ loaded الحقيقي لا مؤقت)
            // عند تخطي Compose Splash (مستخدم PIN حالي) لا تُستدعى onAnimationDone أبدًا —
            // فتُحرَّر البوابة فور الجاهزية وإلا علقت شاشة النظام للأبد (ANR بعد البصمة)
            LaunchedEffect(loaded, splashAnimDone, skipSplash) {
                if (loaded && (splashAnimDone || skipSplash)) com.nota.notes.ui.SplashGate.keepOnScreen = false
            }
            when {
                !loaded || (!splashAnimDone && !skipSplash) -> LuxurySplashScreen(
                    onAnimationDone = { splashAnimDone = true },
                    showLoading = showSplashSpinner && !loaded
                )
                // شاشات التعريف عند أول تشغيل فقط — قبل إنشاء PIN
                !onboarded -> OnboardingScreen(onDone = {
                    scope.launch { settings.setOnboardingDone() }
                })
                // مراجعة الصلاحيات عند أول تثبيت فقط — بعد التعريف وقبل إنشاء PIN (27 سبتمبر 2026)
                !permsReviewed -> PermissionsReviewScreen(onDone = {
                    scope.launch { settings.setPermsReviewed() }
                })
                pinHash == null -> SetPinScreen(
                    onDone = { pin -> authVm.setupPin(pin) {} },
                    authVm = authVm
                )
                else -> AuthedApp(vm, settings, authVm, premiumVm, accountVm, newNoteSignal = newNoteSignal, openNoteId = openNoteId)
            }
        }
    }
}

@Composable
private fun LoadingGate() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AuthedApp(
    vm: NotesViewModel,
    settings: SettingsViewModel,
    authVm: AuthViewModel,
    premiumVm: PremiumViewModel,
    accountVm: AccountViewModel,
    foldersVm: FoldersViewModel = hiltViewModel(),
    cloudVm: CloudBackupViewModel = hiltViewModel(),
    // P1-A: نفس نسخة SelfChatScreen (بلا NavHost فالمالك هو الـ Activity) — للمسح/إعادة التحميل عند القفل/الفتح
    selfVm: com.nota.notes.ui.SelfChatViewModel = hiltViewModel(),
    newNoteSignal: Int = 0,
    openNoteId: Long = -1L
) {
    // دخول مباشر مرة واحدة بعد إنشاء PIN، ومقفل دائماً بعد أي إعادة إنشاء (تغيير لغة/تدوير)
    var unlocked by remember { mutableStateOf(authVm.consumeFreshSetup()) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var bioError by remember { mutableStateOf<String?>(null) }
    var lockUntil by remember { mutableStateOf(0L) }
    var prelock by remember { mutableStateOf(false) }
    var successFlash by remember { mutableStateOf(false) }
    // التنبيه الاستباقي يختفي بعد 3 ثوانٍ (Commit 3ج)
    LaunchedEffect(prelock) {
        if (prelock) {
            kotlinx.coroutines.delay(3000)
            prelock = false
        }
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val bioEnabled by settings.biometricEnabled.collectAsStateWithLifecycle()
    val autoLock by settings.autoLockMs.collectAsStateWithLifecycle()
    LaunchedEffect(autoLock) { authVm.auth.autoLockMs = autoLock }
    LaunchedEffect(unlocked) { if (unlocked) { authVm.touch(); pinError = null; bioError = null } }

    // القفل التلقائي حسب المهلة: عند العودة من الخلفية نفحص انقضاء المهلة.
    // وعند الذهاب للخلفية نمسح الحافظة — نص منسوخ من ملاحظة لا يبقى متاحًا لتطبيقات أخرى.
    val lifecycleOwner = LocalLifecycleOwner.current
    val appCtx = LocalContext.current.applicationContext
    DisposableEffect(lifecycleOwner) {
        // إطفاء شاشة الهاتف = قفل فوري — بلا انتظار مهلة القفل التلقائي
        val screenOff = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, i: android.content.Intent?) {
                // إطفاء الشاشة = نسيان كل فتح خاص — المقفلة تتطلب تحديًا من جديد
                foldersVm.relockAll()
                // P1-A fix 4: تفريغ المسودة أولًا (متزامن — يكتمل قبل إغلاق DB) ثم المسح/القفل.
                scope.launch {
                    runCatching { vm.flushPendingSave() }
                    // P1-A: مسح الذاكرة عند القفل — نصوص القوائم + إغلاق DB + الحافظة
                    vm.clearSensitiveData()
                    selfVm.clearSensitiveData()
                    authVm.lockAndWipe()
                    unlocked = false
                }
            }
        }
        appCtx.registerReceiver(
            screenOff,
            android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_OFF)
        )
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_PAUSE -> authVm.touch()
                Lifecycle.Event.ON_STOP -> {
                    // الذهاب للخلفية = نسيان كل فتح خاص (قفل تلقائي للعناصر المقفلة)
                    foldersVm.relockAll()
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        try {
                            (appCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as? android.content.ClipboardManager)?.clearPrimaryClip()
                        } catch (e: Exception) { /* المسح أفضل جهد — لا يكسر شيئًا */ }
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    // رسالة القفل التلقائي (Commit 3ز): إشعار لثانية عند العودة بعد انقضاء المهلة
                    if (unlocked && !authVm.auth.checkAutoLock()) {
                        // P1-A fix 4: تفريغ المسودة أولًا (متزامن — يكتمل قبل إغلاق DB) ثم المسح/القفل.
                        scope.launch {
                            runCatching { vm.flushPendingSave() }
                            // P1-A: مسح الذاكرة عند القفل التلقائي — كمسار إطفاء الشاشة
                            vm.clearSensitiveData()
                            selfVm.clearSensitiveData()
                            authVm.lockAndWipe()
                            unlocked = false
                        }
                        runCatching {
                            val label = autolockLabel(appCtx, autoLock)
                            android.widget.Toast.makeText(
                                appCtx,
                                appCtx.getString(R.string.autolock_msg_fmt, label),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            try { appCtx.unregisterReceiver(screenOff) } catch (_: Exception) { /* مسجل مسبقًا — تجاهل */ }
        }
    }

    // الترحيب والسرعة (27 سبتمبر 2026): يُجمع فوق الإرجاع المبكر ليثبت عبر دورات القفل
    val userName by settings.username.collectAsStateWithLifecycle()
    val welcomeOn by settings.welcomeEnabled.collectAsStateWithLifecycle()
    val quickOn by settings.quickMode.collectAsStateWithLifecycle()
    // التأثيرات والحركة (28 سبتمبر 2026): مفتاح عام واحد يحكم الاهتزاز اللمسي وأنيميشن القوائم
    val effectsOn by settings.effectsOn.collectAsStateWithLifecycle()
    // شيت الإضافة: آخر الإضافات المستخدمة (28 سبتمبر 2026) — بتتخزّن في الإعدادات فتبقى بين الجلسات
    val sheetRecent by settings.sheetRecent.collectAsStateWithLifecycle()
    // قوالب المستخدم (28 سبتمبر 2026): تُحفظ في الإعدادات فتظهر في أي ملاحظة
    val tplCust by settings.tplCustom.collectAsStateWithLifecycle()
    LaunchedEffect(effectsOn) { com.nota.notes.ui.Haptics.enabled = effectsOn }
    // لغة واجهة مساعد الكتابة (28 سبتمبر 2026): تُجمع هنا لأن onAiRun بلا lang في نطاقه
    val aiLang by settings.lang.collectAsStateWithLifecycle()
    // الترحيب مرة واحدة لكل فتح تطبيق (27 سبتمبر 2026): فوق الإرجاع المبكر ليثبت عبر دورات القفل
    var welcomedSession by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(false) }
    LaunchedEffect(unlocked) {
        if (unlocked && !welcomedSession) {
            welcomedSession = true
            if (welcomeOn) showWelcome = true
        }
    }
    if (!unlocked) {
        val wrong = stringResource(R.string.pin_wrong)
        val ctx = LocalContext.current
        val activity = ctx as? FragmentActivity
        val available = bioEnabled && authVm.canBiometric() && activity != null
        var autoFired by remember { mutableStateOf(false) }
        // البصمة مقفلة مؤقتًا من النظام — تُخفى خليتها ويبقى PIN؛ تُعاد المحاولة بعد 30 ثانية
        var bioLockedOut by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(bioLockedOut) {
            if (bioLockedOut) {
                kotlinx.coroutines.delay(30_000)
                bioLockedOut = false
            }
        }
        val rootNeverDismissed by authVm.rootWarnDismissed.collectAsStateWithLifecycle()
        val hasRecKey by authVm.hasRecoveryKey.collectAsStateWithLifecycle()
        val devCredOn by authVm.deviceCredEnabled.collectAsStateWithLifecycle()
        // محاولة بصمة تلقائية أول ظهور لشاشة القفل إن كانت مفعّلة ومتاحة
        LaunchedEffect(available) {
            if (available && !autoFired && activity != null) {
                autoFired = true
                authVm.promptBiometric(activity, onOk = { unlocked = true; authVm.biometricOk(); authVm.recordUnlockOk(); vm.onUnlocked(); selfVm.reload() }, onErr = { if (it.isNotBlank()) bioError = it }, onLockout = { bioLockedOut = true })
            }
        }
        // زر رجوع الهاتف على شاشة القفل: يخرج من التطبيق (السلوك الافتراضي) — لا BackHandler يعترضه
        LockScreen(
            showBiometric = available,
            onBiometric = {
                if (activity != null) {
                    authVm.promptBiometric(activity,
                        onOk = { unlocked = true; authVm.biometricOk(); authVm.recordUnlockOk(); vm.onUnlocked(); selfVm.reload() },
                        onErr = { if (it.isNotBlank()) bioError = it },
                        onLockout = { bioLockedOut = true })
                }
            },
            bioLockedOut = bioLockedOut,
            rootNeverDismissed = rootNeverDismissed,
            onRootNeverShow = { authVm.dismissRootWarn() },
            authVm = authVm,
            hasRecoveryKey = hasRecKey,
            deviceCredEnabled = devCredOn,
            onRecovered = { unlocked = true; authVm.recordUnlockOk(); vm.onUnlocked(); selfVm.reload() },
            bioEnrolled = authVm.auth.isBioEnrolled(),
            prelockWarn = prelock,
            successFlash = successFlash,
            onPin = { pin ->
                if (pin.isEmpty()) return@LockScreen
                scope.launch {
                    when (val r = authVm.verify(pin)) {
                        is com.nota.notes.ui.AuthViewModel.PinResult.Success -> {
                            // تغذية راجعة للنجاح (Commit 3هـ): وميض 300ms + اهتزاز تأكيد قبل الانتقال — بلا صوت
                            Haptics.confirm(appCtx)
                            successFlash = true; pinError = null; prelock = false; lockUntil = 0L
                            kotlinx.coroutines.delay(300)
                            unlocked = true; successFlash = false; vm.onUnlocked(); selfVm.reload()
                            authVm.upgradePinIfLegacy(pin) // ترقية صامتة من SHA-256 إلى PBKDF2 بعد أول فتح ناجح
                        }
                        is com.nota.notes.ui.AuthViewModel.PinResult.Wrong -> {
                            // تغذية راجعة للفشل (Commit 3د): اهتزاز مميز + اهتزاز بصري في اللوحة
                            Haptics.reject(appCtx)
                            if (r.fails == 2) { prelock = true; pinError = null }
                            else { prelock = false; pinError = wrong }
                        }
                        is com.nota.notes.ui.AuthViewModel.PinResult.Locked -> {
                            lockUntil = r.untilMs; pinError = null; prelock = false
                        }
                        // حدة الـ when: أي حالة مستقبلية تُعامل كرفض ظاهر — لا فتح صامت أبدًا
                        else -> pinError = wrong
                    }
                }
            },
            pinError = pinError,
            bioError = bioError,
            lockUntilMs = lockUntil,
            onLockExpired = { lockUntil = 0L }
        )
        return
    }
    // زر + في الويدجت: يفتح محررًا جديدًا بعد القفل — يتجاوز أي شاشة مفتوحة
    // كاش الويدجت: بعد فتح القفل ومع كل تغيّر في الملاحظات — عناوين فقط بلا فك تشفير من الـ provider
    var screen by remember { mutableStateOf(Screen.LIST) }
    // بوابة PRO: pro من السيرفر يفتح كل مزايا ما بعد إغلاق المجانية (قرار 20 سبتمبر 2026) — غيره يُحوَّل للـ paywall مع تنبيه
    // التجربة المجانية (27 سبتمبر 2026): تفتح كل شيء كـ Pro حتى انتهاء المدة — تُعاد كل دقيقة لالتقاط الانتهاء
    val trialStartTop by settings.trialStart.collectAsStateWithLifecycle()
    val trialOfferedTop by settings.trialOffered.collectAsStateWithLifecycle()
    var trialTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            trialTick++
        }
    }
    val trialNow = trialTick.let { System.currentTimeMillis() }
    val trialOn = com.nota.notes.data.SettingsStore.isTrialActive(trialStartTop, trialNow)
    val trialLeftMs = (trialStartTop + com.nota.notes.data.SettingsStore.trialDurationMs() - trialNow).coerceAtLeast(0L)
    LaunchedEffect(trialOn) { premiumVm.setTrialActive(trialOn) }
    // المرحلة 1ب-1: المصدر الوحيد لتعريف isPro — premiumVm.isPro() (سيرفر pro أو تجربة نشطة)
    val isPro = premiumVm.isPro()
    // أفاتار الرئيسية: يُحدَّث مع تبدّل الحساب — صورته أو حرفه أو عام
    val acctId by accountVm.accountId.collectAsStateWithLifecycle()
    val acctMail by accountVm.email.collectAsStateWithLifecycle()
    // الحجر الصحي للقاعدة: حوار استعادة بدل التعطل الصامت — Drive أو ملف محلي من المتقدمة
    val dbHealth: com.nota.notes.ui.DbHealthViewModel = hiltViewModel()
    val dbCorrupt by dbHealth.corruptionFound.collectAsStateWithLifecycle()
    if (dbCorrupt) {
        AlertDialog(
            onDismissRequest = { dbHealth.dismiss() },
            title = { Text(stringResource(R.string.db_corrupt_title)) },
            text = { Text(stringResource(R.string.db_corrupt_text)) },
            confirmButton = {
                TextButton(onClick = { dbHealth.dismiss(); screen = Screen.CLOUD; authVm.touch() }) {
                    Text(stringResource(R.string.db_corrupt_drive))
                }
            },
            dismissButton = {
                TextButton(onClick = { dbHealth.dismiss() }) {
                    Text(stringResource(R.string.db_corrupt_later))
                }
            }
        )
    }
    fun proLock() {
        android.widget.Toast.makeText(appCtx, R.string.pro_only, android.widget.Toast.LENGTH_SHORT).show()
        screen = Screen.PAYWALL
        authVm.touch()
    }
    // حوار الترحيب (27 سبتمبر 2026): الحالة تُدار فوق الإرجاع المبكر — هنا العرض فقط
    if (showWelcome) {
        WelcomeDialog(
            username = userName,
            onSaveName = { settings.setUsername(it); authVm.touch() },
            onEnter = { showWelcome = false; authVm.touch() },
            onHideForever = { settings.setWelcomeEnabled(false); showWelcome = false; authVm.touch() }
        )
    }
    // عرض التجربة أول استخدام فقط (27 سبتمبر 2026): قبول يفتح Pro لمدته — ورفض لا يعيد العرض أبدًا
    if (!trialOfferedTop) {
        TrialOfferDialog(
            onStart = { settings.startTrial(); authVm.touch() },
            onDecline = { settings.dismissTrialOffer(); authVm.touch() }
        )
    }
    // درج التنقل: مفتاح الوجهة الحالية + انتقال يغلق الدرج — الأقفال تُحترم في المسارات نفسها
    val selectedKey = when (screen) {
        Screen.ARCHIVE -> DrawerKeys.ARCHIVE
        Screen.TRASH -> DrawerKeys.TRASH
        Screen.FOLDERS -> DrawerKeys.FOLDERS
        Screen.CATEGORIES -> DrawerKeys.CATEGORIES
        Screen.TAGS_MGR -> DrawerKeys.TAGS
        Screen.FAVORITES -> DrawerKeys.FAVORITES
        Screen.REMINDERS -> DrawerKeys.REMINDERS
        Screen.CLOUD -> DrawerKeys.BACKUP
        Screen.SETTINGS -> DrawerKeys.SETTINGS
        Screen.ABOUT -> DrawerKeys.ABOUT
        Screen.ACCOUNT -> DrawerKeys.ACCOUNT
        Screen.SELF -> DrawerKeys.SELF
        else -> DrawerKeys.LIST
    }
    // بقاء المحرر عند الدوران: المعرف والعنوان والنص والألوان والموقع والوسوم تُحفظ — فلا ضياع ولا تكرار
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    // نص الملاحظة كـ TextFieldValue حتى يدعم البحث الداخلي تحريك المؤشر والتمرير للنتائج
    var body by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    // الملاحظة المحفوظة تُفتح عرضًا فقط — الجديدة تُفتح تحريرًا مباشرة
    var readOnly by rememberSaveable { mutableStateOf(true) }
    var editingCreated by rememberSaveable { mutableStateOf(0L) }
    var editingUpdated by rememberSaveable { mutableStateOf(0L) }
    var editingColor by rememberSaveable { mutableIntStateOf(0) }
    // حوار تأكيد الحفظ في المجلد الافتراضي للملاحظة الجديدة
    var confirmDefault by remember { mutableStateOf(false) }
    val query by vm.query.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val sortOrder by settings.sortOrder.collectAsStateWithLifecycle()
    val colorFilter by vm.colorFilter.collectAsStateWithLifecycle()
    val includeArchive by vm.includeArchive.collectAsStateWithLifecycle()
    val defColor by settings.defaultColor.collectAsStateWithLifecycle()
    val editorFont by settings.editorFont.collectAsStateWithLifecycle()
    val cardDates by settings.cardDates.collectAsStateWithLifecycle()
    val archiveQuery by vm.archiveQuery.collectAsStateWithLifecycle()
    val archived by vm.archived.collectAsStateWithLifecycle()
    val trashed by vm.trashed.collectAsStateWithLifecycle()
    val favoriteQuery by vm.favoriteQuery.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val favoritesArchived by vm.favoritesArchived.collectAsStateWithLifecycle()
    // الوسوم: كل الأسماء لصف الفلترة + خريطة العرض أسفل البطاقة + الفلتر + وسوم المحرر
    val allTags by vm.allTags.collectAsStateWithLifecycle()
    val tagsMap by vm.tagsMap.collectAsStateWithLifecycle()
    // شاشات الإدارة: عدّادات الوسوم + كل ذات الموعد — تُجمَع هنا لتُمرر للشاشات
    val tagCounts by vm.tagCounts.collectAsStateWithLifecycle()
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val tagFilter by vm.tagFilter.collectAsStateWithLifecycle()
    val allCategories by vm.categories.collectAsStateWithLifecycle()
    val categoryCounts by vm.categoryCounts.collectAsStateWithLifecycle()
    val categoryFilter by vm.categoryFilter.collectAsStateWithLifecycle()
    var editingTags by rememberSaveable { mutableStateOf(listOf<String>()) }
    // التصنيف: تسمية واحدة حرة — يُحمَّل مع الفتح ويُحفظ فوريًا للمحفوظة ومع أول حفظ للجديدة (ك الوسوم)
    var editingCategory by rememberSaveable { mutableStateOf("") }
    // التذكير: قاعدة واحدة (موعد + تكرار بسيط/مخصص + مسبق) — تُحفظ مع الحفظ التلقائي واليدوي
    // التذكير يُحفظ عند الدوران بحافظ مخصص (حقول بدائية فقط) — فلا يضيع الموعد المضبوط
    val reminderSaver = remember {
        mapSaver(
            save = { r: com.nota.notes.data.ReminderSpec ->
                mapOf("at" to r.at, "repeat" to r.repeat, "before" to r.beforeMin,
                    "interval" to r.interval, "days" to r.days, "month" to r.monthMode,
                    "end" to r.end, "endValue" to r.endValue)
            },
            restore = { m ->
                runCatching {
                    com.nota.notes.data.ReminderSpec(
                        at = m["at"] as? Long, repeat = m["repeat"] as? String ?: "none",
                        beforeMin = (m["before"] as? Long) ?: 0L, interval = (m["interval"] as? Int) ?: 1,
                        days = m["days"] as? String ?: "",
                        monthMode = m["month"] as? String ?: com.nota.notes.data.ReminderSpec.MONTH_SAME,
                        end = m["end"] as? String ?: com.nota.notes.data.ReminderSpec.END_FOREVER,
                        endValue = (m["endValue"] as? Long) ?: 0L
                    )
                }.getOrDefault(com.nota.notes.data.ReminderSpec())
            }
        )
    }
    var editingReminder by rememberSaveable(stateSaver = reminderSaver) { mutableStateOf(com.nota.notes.data.ReminderSpec()) }
    // الموقع المرفق: إحداثيات اختيارية — تُحفظ مع الحفظ التلقائي واليدوي (ميزة مدفوعة)
    var editingLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var editingLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    // تذكير الموقع (27 سبتمبر 2026): إحداثيات السياج + نصف القطر + الاسم — null = بلا سياج
    var editingGeoLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var editingGeoLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var editingGeoRadius by rememberSaveable { mutableStateOf(200f) }
    var editingGeoName by rememberSaveable { mutableStateOf("") }
    // المجلدات للشارة والنقل في القائمة الرئيسية
    val folders by foldersVm.folders.collectAsStateWithLifecycle(initialValue = emptyList())
    // المجلد الافتراضي — يُحل لاسم من القائمة (المحذوف يُعامل كلا شيء)
    val defaultFolderId by foldersVm.defaultFolderId.collectAsStateWithLifecycle()
    val defaultFolder = folders.find { it.id == defaultFolderId }
    // هل الملاحظة المفتوحة حاليًا من الأرشيف؟ يحدد زر أرشفة/استعادة ووجهة الرجوع
    var openedFromArchive by rememberSaveable { mutableStateOf(false) }
    // الحفظ التلقائي: مفعّل افتراضيًا من الإعدادات المتقدمة — صامت كل 1.5 ثانية بعد آخر تغيير
    val autosaveEnabled by settings.autosave.collectAsStateWithLifecycle()
    // خط الأساس للمقارنة — يمنع حفظًا زائفًا يرفع updatedAt عند مجرد الفتح
    var savedTitle by remember { mutableStateOf("") }
    var savedBody by remember { mutableStateOf("") }
    var savedColor by remember { mutableIntStateOf(0) }
    // صورة الخلفية قيد التحرير + خط أساسها — المؤقتة المختارة تُمسح فور استبدالها قبل الحفظ
    var editingBackground by rememberSaveable { mutableStateOf<String?>(null) }
    var savedBg by remember { mutableStateOf<String?>(null) }
    // لقطة الحفظ: الموقع جزء منها حتى يلتقط الحفظ التلقائي تغييره وحده
    var savedLat by remember { mutableStateOf<Double?>(null) }
    var savedLng by remember { mutableStateOf<Double?>(null) }
    // التصنيف جزء من لقطة الحفظ — تغييره وحده (ملاحظة جديدة) يجب أن يوقظ الحفظ التلقائي
    var savedCategory by remember { mutableStateOf("") }
    fun markSaved(t: String, b: String, c: Int, bg: String? = null, lat: Double? = null, lng: Double? = null, cat: String = "") {
        savedTitle = t; savedBody = b; savedColor = c; savedBg = bg; savedLat = lat; savedLng = lng; savedCategory = cat
    }
    fun setBgTemp(next: String?) {
        val cur = editingBackground
        if (cur != null && cur != savedBg && cur != next) {
            val f = NoteBgFiles.file(appCtx, cur)
            runCatching { f.delete() }
        }
        editingBackground = next
    }
    // اختيار صورة خلفية: نسخ داخلي مصغّر فوري — بلا إذن تخزين ولا رابط خارجي
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = NoteBgFiles.copyPicked(appCtx, uri)
            if (name != null) { setBgTemp(name); authVm.touch() }
        }
    }
    // التذكير: حالة المحرر + إذن الإشعار (أندرويد 13+) قبل أول جدولة
    var pendingReminderAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    // رفض إذن الإشعارات = تعذّر إنشاء التنبيه — حوار توجيه للإعدادات (27 سبتمبر 2026)
    var notifDeniedAsk by remember { mutableStateOf(false) }
    var geoDeniedAsk by remember { mutableStateOf(false) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pendingReminderAction?.invoke()
        else if (pendingReminderAction != null) notifDeniedAsk = true
        pendingReminderAction = null
    }
    // إذن الموقع للسياج الجغرافي: الرفض = تعذّر التفعيل — حوار توجيه للإعدادات (27 سبتمبر 2026)
    var pendingGeoAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val locPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        val ok = res[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            res[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) pendingGeoAction?.invoke()
        else if (pendingGeoAction != null) geoDeniedAsk = true
        pendingGeoAction = null
    }
    fun withLocationPerm(action: () -> Unit) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(appCtx, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(appCtx, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) action()
        else {
            pendingGeoAction = action
            locPerm.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    fun openAppSettings() {
        runCatching {
            appCtx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:com.nota.notes"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        authVm.touch()
    }
    fun withNotifPerm(action: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT < 33 ||
            androidx.core.content.ContextCompat.checkSelfPermission(appCtx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED) action()
        else { pendingReminderAction = action; notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
    }
    // ضمان الانطلاق: إشعار → منبه دقيق → استثناء بطارية — كل خطوة تشرح نفسها مرة واحدة ثم تُكمل
    var exactAlarmAsk by remember { mutableStateOf<(() -> Unit)?>(null) }
    var batteryAsk by remember { mutableStateOf<(() -> Unit)?>(null) }
    val batteryShown by settings.batteryPromptShown.collectAsStateWithLifecycle()
    fun openExactAlarmSettings() {
        runCatching {
            appCtx.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(android.net.Uri.parse("package:com.nota.notes"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        authVm.touch()
    }
    fun openBatterySettings() {
        runCatching {
            appCtx.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(android.net.Uri.parse("package:com.nota.notes"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        authVm.touch()
    }
    fun withBatteryFree(action: () -> Unit) {
        val pm = appCtx.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        if (pm == null || pm.isIgnoringBatteryOptimizations("com.nota.notes")) action()
        else if (batteryShown) action() // رفضها مرة = لا إلحاح — تُطلب من المتقدمة عند الحاجة
        else { settings.setBatteryPromptShown(); batteryAsk = action }
    }
    fun withExactAlarm(action: () -> Unit) {
        val mgr = appCtx.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
        if (android.os.Build.VERSION.SDK_INT < 31 || mgr == null || mgr.canScheduleExactAlarms())
            withBatteryFree(action)
        else exactAlarmAsk = { withBatteryFree(action) }
    }
    fun ensureReminderReady(action: () -> Unit) = withNotifPerm { withExactAlarm(action) }
    // حفظ التذكير للملاحظة المحفوظة — الجديدة تُحفظ مع أول حفظ (يدوي/تلقائي)
    fun persistReminder(id: Long, spec: com.nota.notes.data.ReminderSpec) {
        if (spec.at == null) vm.clearReminder(id)
        else ensureReminderReady { vm.setReminder(id, spec) }
    }
    // السياج: الحفظ يُسجِّل النظامي — والمسح يلغيه (27 سبتمبر 2026)
    fun persistGeofence(id: Long, lat: Double?, lng: Double?, radius: Float, name: String) {
        if (lat == null || lng == null) vm.clearGeofence(id)
        else ensureReminderReady { withLocationPerm { vm.setGeofence(id, lat, lng, radius, name) } }
    }
    // التنقل من أي شاشة (مضمنها المحرر): حفظ المسودة المتسخة أولًا ثم انتقال — بعد كل التبعيات (قرار 26 سبتمبر 2026)
    fun goKey(key: String) {
        val target = when (key) {
            DrawerKeys.ARCHIVE -> Screen.ARCHIVE
            DrawerKeys.TRASH -> Screen.TRASH
            DrawerKeys.FOLDERS -> Screen.FOLDERS
            DrawerKeys.CATEGORIES -> Screen.CATEGORIES
            DrawerKeys.TAGS -> Screen.TAGS_MGR
            DrawerKeys.FAVORITES -> Screen.FAVORITES
            DrawerKeys.REMINDERS -> Screen.REMINDERS
            DrawerKeys.BACKUP -> Screen.CLOUD
            DrawerKeys.SETTINGS -> Screen.SETTINGS
            DrawerKeys.ABOUT -> Screen.ABOUT
            DrawerKeys.ACCOUNT -> Screen.ACCOUNT
            DrawerKeys.SELF -> Screen.SELF
            else -> Screen.LIST
        }
        // من المحرر: حفظ تلقائي للمسودة ثم انتقال — فلا نقرة ميتة ولا ضياع (قرار 26 سبتمبر 2026)
        // الفارغة تُغلق بلا حفظ، والعرض يُغلق بلا كتابة — والمتسخة تُحفظ أولًا ثم يُنتقل
        val id = editing
        if (id != null && !readOnly && autosaveEnabled && (title.isNotBlank() || body.text.isNotBlank())) {
            scope.launch {
                runCatching {
                    val nid = vm.saveNow(if (id == 0L) null else id, title, body.text, editingColor,
                        if (id == 0L) defaultFolder?.id else null,
                        editingBackground, editingLatitude, editingLongitude, editingCategory)
                    if (editingTags.isNotEmpty()) vm.saveTags(nid, editingTags)
                    if (editingReminder.at != null) persistReminder(nid, editingReminder)
                    if (editingGeoLat != null && editingGeoLng != null) persistGeofence(nid, editingGeoLat, editingGeoLng, editingGeoRadius, editingGeoName)
                    markSaved(title, body.text, editingColor, editingBackground, editingLatitude, editingLongitude, editingCategory)
                }
                editing = null
                screen = target
                authVm.touch()
            }
        } else {
            if (id != null) editing = null
            screen = target
            authVm.touch()
        }
    }
    // حفظ هيكلي للجديدة عند أول محتوى (مهمة/صورة) — حتى الفارغة نصًا تُحفظ كحاوية
    fun ensureSavedId(onId: (Long) -> Unit) {
        val id = editing
        if (id != null && id != 0L) { onId(id); return }
        scope.launch {
            val newId = vm.saveNow(null, title, body.text, editingColor, defaultFolder?.id, editingBackground, editingLatitude, editingLongitude, editingCategory)
            if (newId > 0) {
                editing = newId
                val now = System.currentTimeMillis()
                if (editingCreated == 0L) editingCreated = now
                editingUpdated = now
                markSaved(title, body.text, editingColor, editingBackground, editingLatitude, editingLongitude, editingCategory)
                if (editingTags.isNotEmpty()) vm.saveTags(newId, editingTags)
                if (editingReminder.at != null) persistReminder(newId, editingReminder)
                    if (editingGeoLat != null && editingGeoLng != null) persistGeofence(newId, editingGeoLat, editingGeoLng, editingGeoRadius, editingGeoName)
                onId(newId)
            }
        }
    }
    // محتوى الملاحظة (مهام + صور): يُحمَّل مع الفتح — والإضافة للجديدة تحفظ هيكلًا أولًا
    var editingTasks by remember { mutableStateOf<List<com.nota.notes.data.ChecklistItem>>(emptyList()) }
    var editingAttachments by remember { mutableStateOf<List<com.nota.notes.data.NoteAttachment>>(emptyList()) }
    // ملاحظة جديدة: تُفتح من FAB أو زر الإضافة في الشريط السفلي (قرار 26 سبتمبر 2026)
    val startNewNote: () -> Unit = { title = ""; body = TextFieldValue(""); readOnly = false; openedFromArchive = false; editingColor = defColor; editingCreated = 0L; editingUpdated = 0L; editingBackground = null; editingTags = emptyList(); editingCategory = ""; editingReminder = com.nota.notes.data.ReminderSpec(); editingTasks = emptyList(); editingAttachments = emptyList(); editingLatitude = null; editingLongitude = null; editingGeoLat = null; editingGeoLng = null; editingGeoRadius = 200f; editingGeoName = ""; markSaved("", "", defColor, null, null, null); editing = 0L; authVm.touch() }
    // شريط التنقل السفلي: الخانات + FAB — المجاني ثابت وظاهر، والمدفوع يخصص (قرار 26 سبتمبر 2026)
    val navSlots by settings.navSlots.collectAsStateWithLifecycle()
    val fabPref by settings.fabVisible.collectAsStateWithLifecycle()
    // الذكاء الاصطناعي BYOK للمحرر (27 سبتمبر 2026): مجاني للجميع — المفتاح يُقرأ لحظة الاستدعاء فقط
    val aiEpMain by settings.aiEndpoint.collectAsStateWithLifecycle()
    val aiMdMain by settings.aiModel.collectAsStateWithLifecycle()
    val aiKsMain by settings.aiKeySet.collectAsStateWithLifecycle()
    // انتهاء Pro يعيد الشريط للافتراضي فورًا — التخصيص المحفوظ يبقى لعودة الاشتراك
    val effectiveSlots = if (isPro) navSlots else com.nota.notes.data.SettingsStore.DEFAULT_NAV_SLOTS
    val barHasAdd = effectiveSlots.contains(com.nota.notes.data.SettingsStore.NAV_ADD)
    fun barAdd(fromList: Boolean) { if (!fromList) { screen = Screen.LIST }; startNewNote() }
    // الوضع السريع (27 سبتمبر 2026): بعد فتح القفل → ملاحظة جديدة فورًا — مرة لكل فتح (editing يُبطل التكرار)
    LaunchedEffect(unlocked, quickOn) {
        if (unlocked && quickOn && editing == null && screen == Screen.LIST && newNoteSignal == 0) {
            startNewNote()
        }
    }
    fun refreshContent(id: Long) {
        scope.launch {
            editingTasks = vm.loadTasks(id)
            editingAttachments = vm.loadAttachments(id)
            authVm.touch()
        }
    }
    // بعد الدوران: المعرف يُستعاد لكن القوائم (مهام/صور) تُعاد من القاعدة — بلا ازدواج تحميل عند الفتح العادي
    LaunchedEffect(editing) {
        val id = editing
        if (id != null && id != 0L && unlocked && editingTasks.isEmpty() && editingAttachments.isEmpty()) {
            refreshContent(id)
        }
    }
    // directName = ملف الكاميرا الداخلي أصلًا (يُختم تشفيرًا فور الالتقاط) — والمعرض يُنسخ مشفرًا أولًا
    fun attachImageUri(uri: android.net.Uri, directName: String? = null) {
        scope.launch {
            if (directName != null) com.nota.notes.data.NoteMediaFiles.sealExisting(appCtx, directName)
            val name = directName ?: com.nota.notes.data.NoteMediaFiles.copyImage(appCtx, uri)
            if (name == null) return@launch
            ensureSavedId { id ->
                scope.launch {
                    runCatching { vm.addAttachmentNow(id, name) }
                    refreshContent(id)
                }
            }
        }
    }
    // الكاميرا: ملف مؤقت داخلي + FileProvider — الإلغاء يمسحه فورًا بلا أثر
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var cameraName by remember { mutableStateOf<String?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val u = cameraUri; val n = cameraName; cameraUri = null; cameraName = null
        if (ok && u != null && n != null) attachImageUri(u, n)
        else if (n != null) com.nota.notes.data.NoteMediaFiles.delete(appCtx, n)
    }
    fun launchCamera() {
        val name = "cam_${System.currentTimeMillis()}.jpg"
        val f = com.nota.notes.data.NoteMediaFiles.file(appCtx, name)
        if (!runCatching { f.createNewFile(); true }.getOrDefault(false)) return
        cameraName = name
        cameraUri = runCatching {
            androidx.core.content.FileProvider.getUriForFile(appCtx, "com.nota.notes.files", f)
        }.getOrNull() ?: run { cameraName = null; return }
        // إصلاح تعطل: جهاز بلا كاميرا كان يكسر التطبيق — رسالة بدل السقوط
        runCatching { cameraUri?.let { takePicture.launch(it) } }.onFailure {
            cameraUri = null
            com.nota.notes.data.NoteMediaFiles.delete(appCtx, cameraName)
            cameraName = null
            android.widget.Toast.makeText(appCtx, R.string.content_no_viewer, android.widget.Toast.LENGTH_SHORT).show()
        }
        authVm.touch()
    }
    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) attachImageUri(uri)
    }
    // ملف عام (قائمة + 27 سبتمبر 2026): نسخ مشفر + قيد KIND_FILE — الفشل toast فقط
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = com.nota.notes.data.NoteMediaFiles.copyFile(appCtx, uri)
            if (name == null) {
                android.widget.Toast.makeText(appCtx, R.string.content_file_too_big, android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            ensureSavedId { id ->
                scope.launch {
                    runCatching { vm.addAttachmentNow(id, name, com.nota.notes.data.NoteAttachment.KIND_FILE) }
                    refreshContent(id)
                }
            }
        }
        authVm.touch()
    }
    // نسخ الملاحظة للحافظة: العنوان + النص — فعل صريح من المستخدم (أندرويد 13+ يعرض تأكيد النظام تلقائيًا)
    // ملحوظة الخصوصية: التطبيق يمسح الحافظة عند الذهاب للخلفية — المنسوخ للصق الفوري فقط
    fun copyToClipboard(t: String, b: String) {
        val text = if (t.isNotBlank() && b.isNotBlank()) "$t\n$b" else t.ifBlank { b }
        if (text.isBlank()) return
        runCatching {
            val cm = appCtx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("nota", text))
            android.widget.Toast.makeText(appCtx, R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
        }
        authVm.touch()
    }
    // تصدير الملاحظة الحالية PDF: بناء أوفلاين في الكاش + مشاركة عبر FileProvider — الفشل toast فقط
    val appName = stringResource(R.string.app_name)
    fun exportCurrentPdf() {
        val t = title
        val b = body.text
        scope.launch {
            val file = NotePdfExporter.export(appCtx, appName, t, b, editingCreated, editingLatitude, editingLongitude, editingTasks)
            if (file == null) {
                android.widget.Toast.makeText(appCtx, R.string.export_failed, android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(appCtx, "com.nota.notes.files", file)
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(send, appCtx.getString(R.string.export_pdf))
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { appCtx.startActivity(chooser) }
            authVm.touch()
        }
    }
    var pendingLockedOpen by remember { mutableStateOf<Long?>(null) }
    // فتح ملاحظة: المقفلة (أو في مجلد مقفل) تتطلب تحديًا أولًا
    fun openNoteDirect(id: Long, fromArchive: Boolean) {
        val pool = if (fromArchive) archived else notes
        // إصلاح: معرف ميت (ويدجت قديم/رابط محذوف) كان يفتح محررًا فارغًا مربكًا — تجاهل صامت
        if (pool.none { it.id == id }) return
        pool.find { it.id == id }?.let {
            title = it.title; body = TextFieldValue(it.body); readOnly = true
            editingCreated = it.createdAt; editingUpdated = it.updatedAt
            editingColor = it.color
            editingBackground = it.background
            markSaved(it.title, it.body, it.color, it.background, it.latitude, it.longitude, it.category.orEmpty())
            // التذكير يُحمَّل مع الفتح من القائمة — بلا استعلام إضافي
            editingReminder = com.nota.notes.data.ReminderSpec(at = it.reminderAt, repeat = it.reminderRepeat,
                beforeMin = it.reminderBeforeMin, interval = it.repeatInterval, days = it.repeatDays,
                monthMode = it.repeatMonthMode, end = it.repeatEnd, endValue = it.repeatEndValue)
            // الموقع يُحمَّل مع الفتح من القائمة — بلا استعلام إضافي
            editingLatitude = it.latitude
            editingLongitude = it.longitude
            editingGeoLat = it.geoLat
            editingGeoLng = it.geoLng
            editingGeoRadius = it.geoRadiusM
            editingGeoName = it.geoName
            // وسوم الملاحظة تُحمَّل مع الفتح — الفشل يعني بلا وسوم لا كسر الفتح
            scope.launch { editingTags = vm.loadTags(it.id) }
            // التصنيف يُحمَّل مع الفتح من الكيان مباشرة — بلا استعلام إضافي
            editingCategory = it.category.orEmpty()
            // المحتوى (مهام/صور) يُحمَّل مع الفتح — الفشل يعني فارغًا لا كسر الفتح
            refreshContent(it.id)
        }
        openedFromArchive = fromArchive || (pool.find { it.id == id }?.archivedAt != null)
        editing = id
        authVm.touch()
    }
    fun requestOpen(id: Long, fromArchive: Boolean) {
        // التحدي فقط للملاحظة المقفولة صراحة من المستخدم — قفل المجلد يحمي شاشته الخاصة فقط (قرار 26 سبتمبر 2026)
        val pool = if (fromArchive) archived else notes
        val n = pool.find { it.id == id }
        if (n != null && n.locked) pendingLockedOpen = id
        else openNoteDirect(id, fromArchive)
    }
    // تكرار ملاحظة: نسخة بعنوان يوضح أنها نسخة ثم تُفتح مباشرة (المقفلة تتحدى عبر requestOpen)
    fun duplicateAndOpen(n: com.nota.notes.data.Note) {
        scope.launch {
            val base = n.title.ifBlank { appCtx.getString(R.string.untitled) }
            val newId = vm.duplicateNow(n.id, appCtx.getString(R.string.copy_title_fmt, base))
            if (newId > 0) requestOpen(newId, false)
            else authVm.touch()
        }
    }
    // الدفعة د: مشاركة ملف نصي (Markdown/HTML) من الكاش عبر FileProvider — الفشل toast فقط
    fun shareTextFile(name: String, mime: String, content: String, chooserTitle: String) {
        scope.launch {
            val file = runCatching {
                val dir = java.io.File(appCtx.cacheDir, "shared_tmp").apply { if (!exists()) mkdirs() }
                dir.listFiles()?.forEach { runCatching { it.delete() } }
                java.io.File(dir, name).apply { writeText(content) }
            }.getOrNull()
            if (file == null) {
                android.widget.Toast.makeText(appCtx, R.string.export_failed, android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(appCtx, "com.nota.notes.files", file)
            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = mime
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = android.content.Intent.createChooser(send, chooserTitle)
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { appCtx.startActivity(chooser) }
            authVm.touch()
        }
    }
    fun shareCurrentMarkdown() {
        val safe = title.trim().take(24).replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { appName }
        val md = (if (title.isNotBlank()) "# $title\n\n" else "") + body.text
        if (md.isBlank()) return
        shareTextFile("$safe-${System.currentTimeMillis()}.md", "text/markdown", md, appCtx.getString(R.string.share_markdown))
    }
    fun shareCurrentHtml() {
        val safe = title.trim().take(24).replace(Regex("[\\\\/:*?\"<>|]"), "_").ifBlank { appName }
        if (title.isBlank() && body.text.isBlank()) return
        val html = com.nota.notes.ui.MarkdownFormat.markdownToHtml(title, body.text)
        shareTextFile("$safe-${System.currentTimeMillis()}.html", "text/html", html, appCtx.getString(R.string.share_html))
    }
    // الدفعة د: فتح رابط [[عنوان]] — يحفظ الحالية إن اتسخت ثم يفتح (المقفلة تتحدى كالمعتاد)
    fun openNoteByTitle(t: String) {
        // العنوان المكرر: النشطة أولًا ثم الأحدث — بدل الأولى عشوائيًا (تدقيق الدفعة د)
        val target = (notes + archived).filter { it.title == t }
            .sortedWith(compareBy({ it.archivedAt != null }, { -it.updatedAt }))
            .firstOrNull()
        if (target == null) {
            android.widget.Toast.makeText(appCtx, R.string.link_dead, android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val cur = editing
        val dirty = cur != null && (title != savedTitle || body.text != savedBody || editingColor != savedColor ||
            editingBackground != savedBg || editingCategory != savedCategory ||
            editingLatitude != savedLat || editingLongitude != savedLng)
        if (!dirty || (title.isBlank() && body.text.isBlank())) {
            requestOpen(target.id, target.archivedAt != null)
            authVm.touch()
            return
        }
        scope.launch {
            runCatching {
                val nid = vm.saveNow(if (cur == 0L) null else cur, title, body.text, editingColor, null,
                    editingBackground, editingLatitude, editingLongitude, editingCategory)
                if (editingTags.isNotEmpty()) vm.saveTags(nid, editingTags)
                if (editingReminder.at != null) persistReminder(nid, editingReminder)
                    if (editingGeoLat != null && editingGeoLng != null) persistGeofence(nid, editingGeoLat, editingGeoLng, editingGeoRadius, editingGeoName)
            }
            requestOpen(target.id, target.archivedAt != null)
            authVm.touch()
        }
    }
    // الدفعة د: قلب مهمة نصية من المعاينة في وضع العرض — حفظ فوري كمهام Checklist
    fun flipTaskSave(newBody: String) {
        body = TextFieldValue(newBody, body.selection)
        val id = editing
        if (id != null && id != 0L) {
            scope.launch {
                runCatching {
                    val now = System.currentTimeMillis()
                    vm.saveNow(id, title, newBody, editingColor, null, editingBackground, editingLatitude, editingLongitude, editingCategory)
                    editingUpdated = now
                    markSaved(title, newBody, editingColor, editingBackground, editingLatitude, editingLongitude, editingCategory)
                }
                authVm.touch()
            }
        } else authVm.touch()
    }
    // زر + في الويدجت: يفتح محررًا جديدًا بعد القفل — يتجاوز أي شاشة مفتوحة
    LaunchedEffect(newNoteSignal, unlocked) {
        if (newNoteSignal > 0 && unlocked) {
            title = ""; body = TextFieldValue(""); readOnly = false
            openedFromArchive = false; editingColor = defColor; editingCreated = 0L; editingUpdated = 0L
            editingBackground = null
            editingLatitude = null; editingLongitude = null
            editingGeoLat = null; editingGeoLng = null; editingGeoRadius = 200f; editingGeoName = ""; editingGeoLat = null; editingGeoLng = null; editingGeoRadius = 200f; editingGeoName = ""
            editingTasks = emptyList(); editingAttachments = emptyList()
            markSaved("", "", defColor, null)
            editing = 0L; screen = Screen.LIST; authVm.touch()
        }
    }
    // النسخ السحابي التلقائي: بعد كل فتح قفل — ينسخ إن مرّ يوم كامل (صامت تمامًا عند أي فشل)
    val cloudVm: CloudBackupViewModel = hiltViewModel()
    LaunchedEffect(unlocked) { if (unlocked) cloudVm.autoBackupIfDue() }
    // تطهير السلة: المنتهية 30 يومًا تُحذف نهائيًا بعد كل فتح قفل (27 سبتمبر 2026)
    LaunchedEffect(unlocked) { if (unlocked) vm.purgeTrash() }
    // سحابة نوتا التلقائية: نفس القاعدة — للمشتركين تشمل الوسائط بمفتاحهم (بلا حوار، المفتاح على الجهاز)
    val notaCloudVm: NotaCloudViewModel = hiltViewModel()
    LaunchedEffect(unlocked, isPro) { if (unlocked) notaCloudVm.autoBackupIfDue(isPro) }
    // الربط العميق من إشعار التذكير: يفتح الملاحظة بعد القفل — لمرة واحدة لكل معرف (والمقفلة تتحدى كالمعتاد)
    var consumedDeepLink by remember { mutableStateOf(-1L) }
    LaunchedEffect(openNoteId, unlocked) {
        if (openNoteId > 0 && unlocked && consumedDeepLink != openNoteId) {
            consumedDeepLink = openNoteId
            editing = null
            val fromArchive = archived.any { it.id == openNoteId }
            screen = if (fromArchive) Screen.ARCHIVE else Screen.LIST
            requestOpen(openNoteId, fromArchive)
            authVm.touch()
        }
    }
    // تفسير المنبه الدقيق: بدونه التنبيه قد يتأخر — القبول يفتح الإعداد والرفض يُكمل بالتقريبي
    exactAlarmAsk?.let { cont ->
        AlertDialog(
            onDismissRequest = { exactAlarmAsk = null },
            title = { Text(stringResource(R.string.exact_alarm_title)) },
            text = { Text(stringResource(R.string.exact_alarm_msg)) },
            confirmButton = { TextButton(onClick = { exactAlarmAsk = null; openExactAlarmSettings(); cont() }) { Text(stringResource(R.string.exact_alarm_allow)) } },
            dismissButton = { TextButton(onClick = { exactAlarmAsk = null; cont() }) { Text(stringResource(R.string.exact_alarm_later)) } }
        )
    }
    // تفسير استثناء البطارية: بدونه النظام قد يقتل المنبه في السكون — مرة واحدة فقط
    batteryAsk?.let { cont ->
        AlertDialog(
            onDismissRequest = { batteryAsk = null; cont() },
            title = { Text(stringResource(R.string.battery_title)) },
            text = { Text(stringResource(R.string.battery_msg)) },
            confirmButton = { TextButton(onClick = { batteryAsk = null; openBatterySettings(); cont() }) { Text(stringResource(R.string.battery_allow)) } },
            dismissButton = { TextButton(onClick = { batteryAsk = null; cont() }) { Text(stringResource(R.string.battery_later)) } }
        )
    }
    // تعذّر إنشاء التنبيه (بلا إذن إشعارات) أو السياج (بلا موقع): توجيه للإعدادات أو إلغاء (27 سبتمبر 2026)
    if (notifDeniedAsk) {
        AlertDialog(
            onDismissRequest = { notifDeniedAsk = false },
            title = { Text(stringResource(R.string.remind_no_notif_title)) },
            text = { Text(stringResource(R.string.remind_no_notif_msg)) },
            confirmButton = { TextButton(onClick = { notifDeniedAsk = false; openAppSettings() }) { Text(stringResource(R.string.perm_open_app_settings)) } },
            dismissButton = { TextButton(onClick = { notifDeniedAsk = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (geoDeniedAsk) {
        AlertDialog(
            onDismissRequest = { geoDeniedAsk = false },
            title = { Text(stringResource(R.string.geo_no_loc_title)) },
            text = { Text(stringResource(R.string.geo_no_loc_msg)) },
            confirmButton = { TextButton(onClick = { geoDeniedAsk = false; openAppSettings() }) { Text(stringResource(R.string.perm_open_app_settings)) } },
            dismissButton = { TextButton(onClick = { geoDeniedAsk = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    // تأكيد كلمة مرور Drive (27 سبتمبر 2026): عشوائي كل 3..7 أيام بعد فتح القفل — نجاح يجدول التالي، وتأجيل لغد
    var pwConfirmDue by remember { mutableStateOf(false) }
    var pwConfirmText by remember { mutableStateOf("") }
    var pwConfirmErr by remember { mutableStateOf(false) }
    LaunchedEffect(unlocked) {
        if (unlocked && !pwConfirmDue) pwConfirmDue = cloudVm.confirmDueNow()
    }
    if (pwConfirmDue && unlocked) {
        AlertDialog(
            onDismissRequest = { pwConfirmDue = false; pwConfirmText = ""; pwConfirmErr = false; cloudVm.snoozePwConfirm() },
            title = { Text(stringResource(R.string.cloud_confirm_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.cloud_confirm_sub))
                    OutlinedTextField(
                        value = pwConfirmText, onValueChange = { pwConfirmText = it; pwConfirmErr = false },
                        label = { Text(stringResource(R.string.cloud_confirm_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pwConfirmErr,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    if (pwConfirmErr) {
                        Text(
                            stringResource(R.string.cloud_confirm_wrong),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (cloudVm.checkDrivePassword(pwConfirmText)) {
                            cloudVm.markPwConfirmed(); pwConfirmDue = false; pwConfirmText = ""; pwConfirmErr = false
                        } else pwConfirmErr = true
                    }
                }) { Text(stringResource(R.string.cloud_confirm_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pwConfirmDue = false; pwConfirmText = ""; pwConfirmErr = false; cloudVm.snoozePwConfirm() }) {
                    Text(stringResource(R.string.cloud_confirm_later))
                }
            }
        )
    }
    // إعادة جدولة المنبهات مع كل فتح قفل — تحسّبًا لمنبه أُلغي (تحديث نظام/مسح بيانات الساعة)
    LaunchedEffect(unlocked) {
        if (unlocked) runCatching { vm.rescheduleReminders() }
    }
    // كاش الويدجت: للمشتركين فقط — غير المشترك يُدفع له فارغ فيعرض حالة القفل بدل العناوين
    LaunchedEffect(unlocked, notes, isPro) {
        if (!unlocked) return@LaunchedEffect
        runCatching {
            val raw = if (isPro) vm.widgetTitles() else emptyList()
            val display = raw.map { t -> if (t.isBlank()) appCtx.getString(com.nota.notes.R.string.untitled) else t }
            com.nota.notes.widget.WidgetUpdater.push(appCtx, display)
            val mgr = appCtx.getSystemService(android.content.Context.APPWIDGET_SERVICE) as? android.appwidget.AppWidgetManager
            val comp = android.content.ComponentName(appCtx, com.nota.notes.widget.NotaWidgetProvider::class.java)
            (mgr?.getAppWidgetIds(comp) ?: intArrayOf()).forEach { id ->
                // دفع التحديث عبر broadcast صريح — الـ provider بلا updatePeriod
                appCtx.sendBroadcast(android.content.Intent(appCtx, com.nota.notes.widget.NotaWidgetProvider::class.java).setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id)))
            }
        }
    }

    // P1-A fix 4: تسجيل حافظ المسودة — كل recomposition يُحدِّثه فيحمل أحدث حالة دائمًا.
    // يُستدعى من مساري القفل (إطفاء الشاشة/تلقائي) قبل المسح — حفظ واحد متزامن بلا انتظار الـ debounce.
    // النطاق: المحتوى فقط (saveNow) — الوسوم/التذكير/السياج تُستكمل بعد الفتح (الحالة تبقى في rememberSaveable).
    androidx.compose.runtime.SideEffect {
        val id = editing
        vm.setPendingDraftSaver(
            if (id != null && !readOnly && autosaveEnabled && (title.isNotBlank() || body.text.isNotBlank()) &&
                (title != savedTitle || body.text != savedBody || editingColor != savedColor || editingBackground != savedBg ||
                    editingLatitude != savedLat || editingLongitude != savedLng || editingCategory != savedCategory)
            ) {
                {
                    val target = if (id == 0L) null else id
                    val folder = if (id == 0L) defaultFolder?.id else null
                    val newId = vm.saveNow(target, title, body.text, editingColor, folder, editingBackground, editingLatitude, editingLongitude, editingCategory)
                    val now = System.currentTimeMillis()
                    if (id == 0L && newId > 0) { editing = newId; editingCreated = now; editingUpdated = now }
                    else editingUpdated = now
                    markSaved(title, body.text, editingColor, editingBackground, editingLatitude, editingLongitude, editingCategory)
                    authVm.touch()
                }
            } else null
        )
    }

    // الحفظ التلقائي: بعد AUTOSAVE_DELAY_MS من آخر تغيير — الجديدة تُحفظ في المجلد الافتراضي صامتًا
    // (بلا حوار: الحوار للحفظ اليدوي فقط)، ثم تُتابع بنفس الجلسة بلا تكرار
    LaunchedEffect(editing, readOnly, autosaveEnabled, title, body.text, editingColor, editingBackground, editingLatitude, editingLongitude, editingCategory) {
        val id = editing
        if (!autosaveEnabled || id == null || readOnly) return@LaunchedEffect
        if (title.isBlank() && body.text.isBlank()) return@LaunchedEffect
        if (title == savedTitle && body.text == savedBody && editingColor == savedColor && editingBackground == savedBg && editingLatitude == savedLat && editingLongitude == savedLng && editingCategory == savedCategory) return@LaunchedEffect
        kotlinx.coroutines.delay(AUTOSAVE_DELAY_MS)
        if (title == savedTitle && body.text == savedBody && editingColor == savedColor && editingBackground == savedBg && editingLatitude == savedLat && editingLongitude == savedLng && editingCategory == savedCategory) return@LaunchedEffect
        runCatching {
            val target = if (id == 0L) null else id
            val folder = if (id == 0L) defaultFolder?.id else null
            val newId = vm.saveNow(target, title, body.text, editingColor, folder, editingBackground, editingLatitude, editingLongitude, editingCategory)
            // وسوم الجديدة المعلقة تُحفظ مع أول حفظ تلقائي — والمحفوظة حُفظت وسومها لحظة تغييرها
            if (editingTags.isNotEmpty()) vm.saveTags(if (id == 0L) newId else id, editingTags)
            if (editingReminder.at != null) persistReminder(if (id == 0L) newId else id, editingReminder)
            val now = System.currentTimeMillis()
            if (id == 0L && newId > 0) { editing = newId; editingCreated = now; editingUpdated = now }
            else editingUpdated = now
            markSaved(title, body.text, editingColor, editingBackground, editingLatitude, editingLongitude, editingCategory)
            authVm.touch()
        }
    }
    // أزرار تنقل الهاتف: رجوع من المحرر → قائمته، ومن الأرشيف/الإعدادات → الرئيسية
    BackHandler(enabled = editing != null) { editing = null; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.SETTINGS) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.NAVBAR) { screen = Screen.SETTINGS; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.ARCHIVE) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.TRASH) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.FAVORITES) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.FOLDERS) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.ABOUT) { screen = Screen.LIST; authVm.touch() }
    // الشاشات المستقلة: رجوعها للرئيسية (والمفضلة-إدارة للمفضلة) — لا علاقة لها بالإعدادات
    BackHandler(enabled = editing == null && screen == Screen.TAGS_MGR) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.CATEGORIES) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.REMINDERS) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.CLOUD) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.PAYWALL) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.ACCOUNT) { screen = Screen.LIST; authVm.touch() }
    BackHandler(enabled = editing == null && screen == Screen.SELF) { screen = Screen.LIST; authVm.touch() }

    when {
        editing != null -> {
            // المرحلة 1ب-3: البوابة الموحدة — الموقع المرفق مدفوع، ويُفتح بـ Pro (سيرفر أو تجربة)
            val locAllowed = premiumVm.isPro()
            // المحرر: الشريط السفلي ثابت أيضًا (قرار 26 سبتمبر 2026) — بلا زر ➕ فيه حتى لا يمسح المسودة
            DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots.filter { it != com.nota.notes.data.SettingsStore.NAV_ADD }, onBarAdd = {}) { _ -> NoteEditorScreen(
            title = title, body = body,
            createdAt = editingCreated, updatedAt = editingUpdated,
            color = editingColor, onColor = { editingColor = it },
            readOnly = readOnly, onEdit = { readOnly = false; authVm.touch() },
            isArchived = openedFromArchive,
            font = editorFont,
            autosave = autosaveEnabled,
            effects = effectsOn,
            recentSheet = sheetRecent,
            onRecentSheet = settings::setSheetRecent,
            customTpls = tplCust,
            onSaveTpl = { nm, body -> settings.setTplCustom(tplCust + (nm to body)) },
            onDelTpl = { i -> settings.setTplCustom(tplCust.filterIndexed { idx, _ -> idx != i }) },
            background = editingBackground,
            onPickBg = { bgPicker.launch("image/*"); authVm.touch() },
            onRemoveBg = { setBgTemp(null); authVm.touch() },
            onExportPdf = ::exportCurrentPdf,
            onCopy = { copyToClipboard(title, body.text) },
            // الدفعة د: مشاركة Markdown/HTML + فتح [[ ]] + قلب مهام المعاينة
            onShareMarkdown = { shareCurrentMarkdown() },
            onShareHtml = { shareCurrentHtml() },
            onOpenNoteLink = { t -> openNoteByTitle(t) },
            onPreviewTaskFlip = { nb -> flipTaskSave(nb) },
            // المحتوى: يُحمَّل مع الفتح ويُحفظ فوريًا (مهام/صور) — والجديدة تُهيكَل عند أول إضافة
            tasks = editingTasks,
            onAddTask = { t -> ensureSavedId { id -> vm.addTask(id, t) { refreshContent(id) } } },
            onToggleTask = { item -> val id = editing; if (id != null && id != 0L) vm.toggleTask(item) { refreshContent(id) } },
            onRenameTask = { item, t -> val id = editing; if (id != null && id != 0L) vm.renameTask(item, t) { refreshContent(id) } },
            onDeleteTask = { tid -> val id = editing; if (id != null && id != 0L) vm.deleteTask(tid) { refreshContent(id) } },
            attachments = editingAttachments,
            onTakePhoto = { launchCamera() },
            onPickImage = { imgPicker.launch("image/*"); authVm.touch() },
            onPickFile = { filePicker.launch("*/*"); authVm.touch() },
            onDeleteAttachment = { att -> val id = editing; if (id != null && id != 0L) vm.deleteAttachment(att) { refreshContent(id) } },
            onSaveAudio = { f -> ensureSavedId { id -> scope.launch { runCatching { com.nota.notes.data.NoteMediaFiles.sealExisting(appCtx, f) }; runCatching { vm.addAttachmentNow(id, f, com.nota.notes.data.NoteAttachment.KIND_AUDIO) }; refreshContent(id) } } },
            onSaveDrawing = { f -> ensureSavedId { id -> scope.launch { runCatching { vm.addAttachmentNow(id, f, com.nota.notes.data.NoteAttachment.KIND_DRAWING) }; refreshContent(id) } } },
            // التذكير: التغيير يُحفظ فورًا للمحفوظة — والجديدة مع أول حفظ
            reminder = editingReminder,
            onReminderChange = { spec ->
                editingReminder = spec
                val id = editing
                if (id != null && id != 0L) persistReminder(id, spec)
                authVm.touch()
            },
            // الموقع: التغيير يُحفظ مع الحفظ التلقائي — وغير المشترك تنبهه مدفوعة فقط
            latitude = editingLatitude,
            longitude = editingLongitude,
            canUseLocation = locAllowed || isPro,
            onLocationLocked = { android.widget.Toast.makeText(appCtx, R.string.location_pro_only, android.widget.Toast.LENGTH_SHORT).show(); authVm.touch() },
            proAllowed = isPro,
            onProLocked = { proLock() },
            // الذكاء الاصطناعي BYOK (27 سبتمبر 2026): مجاني — المفتاح يُفك لحظة الاستدعاء ولا يُحفظ مكشوفًا
            aiReady = aiKsMain,
            onAiSetup = { screen = Screen.SETTINGS; authVm.touch() },
            onAiRun = { mode, text, cb ->
                scope.launch {
                    val key = settings.aiKeyPlain()
                    if (key.isNullOrBlank()) { cb(Result.failure(Exception("nokey"))); return@launch }
                    val sys = when (mode) {
                        "format" -> com.nota.notes.data.AiRewriteService.systemFormat(aiLang)
                        "summarize" -> com.nota.notes.data.AiRewriteService.systemSummarize(aiLang)
                        else -> com.nota.notes.data.AiRewriteService.systemRewrite(aiLang)
                    }
                    cb(com.nota.notes.data.AiRewriteService.complete(aiEpMain, key, aiMdMain, sys, text))
                }
            },
            onLocationChange = { lat, lng ->
                editingLatitude = lat; editingLongitude = lng
                authVm.touch()
            },
            // السياج: التغيير يُحفظ فورًا للمحفوظة — والجديدة مع أول حفظ (27 سبتمبر 2026)
            geoLat = editingGeoLat,
            geoLng = editingGeoLng,
            geoRadiusM = editingGeoRadius,
            geoName = editingGeoName,
            onGeofenceChange = { lat, lng, radius, name ->
                editingGeoLat = lat; editingGeoLng = lng
                editingGeoRadius = radius; editingGeoName = name
                val id = editing
                if (id != null && id != 0L && lat != null && lng != null)
                    persistGeofence(id, lat, lng, radius, name)
                authVm.touch()
            },
            onGeofenceClear = {
                editingGeoLat = null; editingGeoLng = null; editingGeoName = ""
                val id = editing
                if (id != null && id != 0L) vm.clearGeofence(id)
                authVm.touch()
            },
            // الوسوم تُحفظ فور تغييرها للملاحظة المحفوظة — والجديدة تُحفظ مع أول حفظ
            tags = editingTags,
            onTagsChange = {
                editingTags = it
                val id = editing
                if (id != null && id != 0L) vm.saveTags(id, it)
                authVm.touch()
            },
            // التصنيف يُحفظ فور تغييره للمحفوظة — والجديدة مع أول حفظ (مجاني — بلا بوابة)
            category = editingCategory,
            onCategoryChange = {
                editingCategory = it
                val id = editing
                if (id != null && id != 0L) vm.setCategory(id, it)
                authVm.touch()
            },
            existingCategories = allCategories,
            // الدفعة ج: إكمال الوسوم + روابط الملاحظات — بلا الملاحظة الحالية نفسها
            existingTags = allTags,
            allNotes = notes.map { MarkdownFormat.NoteLink(it.id, it.title) }.filter { it.id != editing },
            onArchive = {
                editing?.let { if (openedFromArchive) vm.unarchive(it) else vm.archive(it) }
                editing = null; authVm.touch()
            },
            onTitle = { title = it }, onBody = { body = it },
            onSave = {
                // الملاحظة الجديدة مع مجلد افتراضي: تأكيد أولًا — الرفض يحفظ في الرئيسية
                if (editing == 0L && defaultFolder != null) confirmDefault = true
                else {
                    val id = editing
                    scope.launch {
                        // saveNow يرجع المعرف — لتُحفظ وسوم الجديدة وتذكيرها معها في نفس العملية
                        val savedId = vm.saveNow(if (id == 0L) null else id, title, body.text, editingColor, null, editingBackground, editingLatitude, editingLongitude, editingCategory)
                        if (editingTags.isNotEmpty()) vm.saveTags(savedId, editingTags)
                        if (editingReminder.at != null) persistReminder(savedId, editingReminder)
                    if (editingGeoLat != null && editingGeoLng != null) persistGeofence(savedId, editingGeoLat, editingGeoLng, editingGeoRadius, editingGeoName)
                    }
                    editing = null; authVm.touch()
                }
            },
            onBack = { editing = null; authVm.touch() }
            )
            // تأكيد الحفظ في المجلد الافتراضي — الرفض يحفظ في القائمة الرئيسية
            if (confirmDefault && defaultFolder != null) {
                AlertDialog(
                    onDismissRequest = { confirmDefault = false },
                    title = { Text(stringResource(R.string.save_to_default_title)) },
                    text = { Text(stringResource(R.string.save_to_default_msg, defaultFolder.name)) },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                val savedId = vm.saveNow(null, title, body.text, editingColor, defaultFolder.id, editingBackground, editingLatitude, editingLongitude, editingCategory)
                                if (editingTags.isNotEmpty()) vm.saveTags(savedId, editingTags)
                                if (editingReminder.at != null) persistReminder(savedId, editingReminder)
                    if (editingGeoLat != null && editingGeoLng != null) persistGeofence(savedId, editingGeoLat, editingGeoLng, editingGeoRadius, editingGeoName)
                            }
                            confirmDefault = false; editing = null; authVm.touch()
                        }) {
                            Text(stringResource(R.string.save_here))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            scope.launch {
                                val savedId = vm.saveNow(null, title, body.text, editingColor, null, editingBackground, editingLatitude, editingLongitude, editingCategory)
                                if (editingTags.isNotEmpty()) vm.saveTags(savedId, editingTags)
                                if (editingReminder.at != null) persistReminder(savedId, editingReminder)
                    if (editingGeoLat != null && editingGeoLng != null) persistGeofence(savedId, editingGeoLat, editingGeoLng, editingGeoRadius, editingGeoName)
                            }
                            confirmDefault = false; editing = null; authVm.touch()
                        }) {
                            Text(stringResource(R.string.save_main_list))
                        }
                    }
                )
            }
            }
        }
        screen == Screen.SETTINGS -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.settings_cd)) { _ -> SettingsRoute(
            settings, authVm,
            onBack = { screen = Screen.LIST; authVm.touch() },
            isPro = isPro,
            onProLocked = { proLock() },
            onCustomizeNav = { screen = Screen.NAVBAR; authVm.touch() }
        ) }
        // تخصيص شريط التنقل — Pro فقط (قرار 26 سبتمبر 2026): المقفل يُقاد للترقية
        screen == Screen.NAVBAR && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.navbar_customize_row)) { _ -> NavBarCustomizeScreen(
            slots = navSlots,
            fabVisible = fabPref,
            onSlots = { settings.setNavSlots(it); authVm.touch() },
            onFab = { settings.setFabVisible(it); authVm.touch() },
            onReset = { settings.setNavSlots(com.nota.notes.data.SettingsStore.DEFAULT_NAV_SLOTS); settings.setFabVisible(true); authVm.touch() },
            onBack = { screen = Screen.SETTINGS; authVm.touch() },
            locked = !isPro,
            onLocked = { proLock() }
        ) }
        screen == Screen.ABOUT && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.about_title)) { _ -> AboutRoute(onBack = { screen = Screen.LIST; authVm.touch() }) }
        screen == Screen.ACCOUNT && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.account_title)) { _ -> AccountRoute(
            accountVm, premiumVm,
            onBack = { screen = Screen.LIST; authVm.touch() }
        ) }
        // شاشة التصنيفات: تسمية حرة للملاحظة + فلترة — مجانية للجميع (مستقلة عن المجلدات والوسوم المدفوعة)
        screen == Screen.CATEGORIES && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.cat_title)) { _ -> CategoriesScreen(
            counts = categoryCounts,
            onShowNotes = { cat -> vm.setCategoryFilter(cat); screen = Screen.LIST; authVm.touch() },
            onRename = { old, newName -> vm.renameCategory(old, newName); authVm.touch() },
            onDelete = { name -> vm.clearCategory(name); authVm.touch() },
            onBack = { screen = Screen.LIST; authVm.touch() },
            notes = notes,
            onCreate = { name, id -> vm.setCategory(id, name); authVm.touch() }
        ) }
        screen == Screen.TAGS_MGR && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.mgr_tags)) { _ -> TagsScreen(
            counts = tagCounts,
            onShowNotes = { tag -> vm.setTagFilter(tag); screen = Screen.LIST; authVm.touch() },
            onRename = { old, newName -> vm.renameTag(old, newName); authVm.touch() },
            onDelete = { name -> vm.deleteTag(name); authVm.touch() },
            onBack = { screen = Screen.LIST; authVm.touch() },
            onCreate = { vm.createTag(it); authVm.touch() },
            locked = !isPro,
            onLocked = { proLock() }
        ) }
        screen == Screen.REMINDERS && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.mgr_reminders)) { _ -> RemindersScreen(
            notes = reminders,
            onOpen = { id -> requestOpen(id, archived.any { it.id == id }); authVm.touch() },
            onCancel = { id -> vm.clearReminder(id); authVm.touch() },
            candidates = notes.filter { it.reminderAt == null && it.archivedAt == null },
            onSetReminder = { id, spec -> persistReminder(id, spec); authVm.touch() },
            onBack = { screen = Screen.LIST; authVm.touch() },
            locked = !isPro,
            onLocked = { proLock() }
        ) }
        // النسخ الاحتياطي الموحد: Drive + سحابة نوتا + المفتاح + ملف .nota — من القائمة الجانبية فقط — مدفوعة
        screen == Screen.CLOUD && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.backup_screen_title)) { _ -> BackupScreen(
            onBack = { screen = Screen.LIST; authVm.touch() },
            onSignInNeeded = { screen = Screen.SETTINGS; authVm.touch() },
            notesVm = vm,
            authVm = authVm,
            locked = !isPro,
            onLocked = { proLock() }
        ) }
        // رسائل النفس (27 سبتمبر 2026): محادثة المستخدم مع نفسه — مجانية للجميع بلا بوابة
        screen == Screen.SELF && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.self_chat_title)) { _ -> SelfChatScreen(
            vm = androidx.hilt.navigation.compose.hiltViewModel<com.nota.notes.ui.SelfChatViewModel>(),
            onBack = { screen = Screen.LIST; authVm.touch() }
        ) }
        // شاشة الترقية الموحدة: تُفتح من أي زر مدفوعة مقفول (قفل/تثبيت/مفضلة/وسوم/تذكير/…) — والرجوع للرئيسية
        screen == Screen.PAYWALL && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.paywall_title)) { _ -> ProPaywall(premiumVm, accountVm, onBack = { screen = Screen.LIST; authVm.touch() }, trialActive = trialOn, trialLeftMs = trialLeftMs) }
        screen == Screen.ARCHIVE && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }) { open -> NotesListScreen(
            notes = archived, query = archiveQuery, onQuery = { vm.archiveQuery.value = it },
            deleting = vm.deleting.collectAsStateWithLifecycle().value,
            showProLocks = !isPro,
            onOpen = { id -> requestOpen(id, true) },
            onAdd = {},
            onDelete = { vm.delete(it); authVm.touch() },
            onNav = open,
            accountId = acctId,
            accountLabel = acctMail ?: acctId,
            onAccount = { screen = Screen.ACCOUNT; authVm.touch() },
            sortOrder = sortOrder,
            onSortChange = { settings.setSortOrder(it); authVm.touch() },
            colorFilter = colorFilter,
            onColorFilter = { vm.setColorFilter(it); authVm.touch() },
            isArchive = true,
            onBack = { screen = Screen.LIST; authVm.touch() },
            onUnarchive = { vm.unarchive(it); authVm.touch() },
            onUnarchiveMany = { vm.unarchiveMany(it); authVm.touch() },
            onDeleteMany = { vm.deleteMany(it); authVm.touch() },
            showDates = cardDates,
            onTogglePin = { if (isPro) { vm.togglePin(it); authVm.touch() } else proLock() },
            onToggleLock = { if (isPro) { vm.toggleLock(it); authVm.touch() } else proLock() },
            onToggleFavorite = { if (isPro) { vm.toggleFavorite(it); authVm.touch() } else proLock() },
            onCopyNote = { if (isPro) copyToClipboard(it.title, it.body) else proLock() },
            onDuplicateNote = { if (isPro) duplicateAndOpen(it) else proLock() },
            tagsByNote = tagsMap
        ) }
        // السلة (27 سبتمبر 2026): استعادة/حذف نهائي/إفراغ + بحث — بلا فتح (المحذوفة تُستعاد أولًا)
        screen == Screen.TRASH && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }) { open -> TrashScreen(
            notes = trashed, query = vm.trashQuery.collectAsStateWithLifecycle().value,
            onQuery = { vm.trashQuery.value = it },
            deleting = vm.deleting.collectAsStateWithLifecycle().value,
            onBack = { screen = Screen.LIST; authVm.touch() },
            onRestore = { vm.restoreOne(it); authVm.touch() },
            onRestoreMany = { vm.restoreMany(it); authVm.touch() },
            onDeleteForever = { vm.deletePermanently(it); authVm.touch() },
            onDeleteManyForever = { vm.deleteManyPermanently(it); authVm.touch() },
            onMenu = open,
            onEmptyTrash = { vm.emptyTrash(); authVm.touch() },
        ) }
        screen == Screen.FAVORITES && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }) { open -> NotesListScreen(
            notes = favorites, query = favoriteQuery, onQuery = { vm.favoriteQuery.value = it },
            deleting = vm.deleting.collectAsStateWithLifecycle().value,
            showProLocks = !isPro,
            favCandidates = notes.filter { !it.favorite && it.archivedAt == null },
            onOpen = { id -> requestOpen(id, false) },
            onAdd = {},
            onDelete = { vm.delete(it); authVm.touch() },
            onNav = open,
            accountId = acctId,
            accountLabel = acctMail ?: acctId,
            onAccount = { screen = Screen.ACCOUNT; authVm.touch() },
            sortOrder = sortOrder,
            onSortChange = { settings.setSortOrder(it); authVm.touch() },
            colorFilter = colorFilter,
            onColorFilter = { vm.setColorFilter(it); authVm.touch() },
            isFavorite = true,
            archivedTabNotes = favoritesArchived,
            onBack = { screen = Screen.LIST; authVm.touch() },
            onArchive = { vm.archive(it); authVm.touch() },
            onArchiveMany = { vm.archiveMany(it); authVm.touch() },
            onDeleteMany = { vm.deleteMany(it); authVm.touch() },
            onUnfavoriteMany = { vm.unfavoriteMany(it); authVm.touch() },
            showDates = cardDates,
            onTogglePin = { if (isPro) { vm.togglePin(it); authVm.touch() } else proLock() },
            onToggleLock = { if (isPro) { vm.toggleLock(it); authVm.touch() } else proLock() },
            onToggleFavorite = { if (isPro) { vm.toggleFavorite(it); authVm.touch() } else proLock() },
            onCopyNote = { if (isPro) copyToClipboard(it.title, it.body) else proLock() },
            onDuplicateNote = { if (isPro) duplicateAndOpen(it) else proLock() },
            tagsByNote = tagsMap
        ) }
        screen == Screen.FOLDERS && editing == null -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(false) }, title = appCtx.getString(R.string.folders_title)) { _ -> FoldersGate(
            premiumVm = premiumVm,
            accountVm = accountVm,
            onBack = { screen = Screen.LIST; authVm.touch() },
            onLocked = { proLock() }
        ) }
        else -> DrawerShell(selectedKey, acctId, acctMail ?: acctId, isPro, premiumVm.canUseFolders(), ::goKey, barSlots = effectiveSlots, onBarAdd = { barAdd(true) }) { open -> NotesListScreen(
            notes = notes, query = query, onQuery = { vm.query.value = it },
            deleting = vm.deleting.collectAsStateWithLifecycle().value,
            showProLocks = !isPro,
            onOpen = { id -> requestOpen(id, false) },
            onAdd = startNewNote,
            showFab = !isPro || fabPref || !barHasAdd,
            onDelete = { vm.delete(it); authVm.touch() },
            onNav = open,
            sortOrder = sortOrder,
            onSortChange = { settings.setSortOrder(it); authVm.touch() },
            colorFilter = colorFilter,
            onColorFilter = { vm.setColorFilter(it); authVm.touch() },
            showAccount = true,
            accountId = acctId,
            accountLabel = acctMail ?: acctId,
            onAccount = { screen = Screen.ACCOUNT; authVm.touch() },
            folders = folders,
            onMoveToFolder = { id, fid -> if (premiumVm.canUseFolders()) { foldersVm.moveNote(id, fid); authVm.touch() } else proLock() },
            onArchive = { vm.archive(it); authVm.touch() },
            onArchiveMany = { vm.archiveMany(it); authVm.touch() },
            onUnarchiveMany = { vm.unarchiveMany(it); authVm.touch() },
            onDeleteMany = { vm.deleteMany(it); authVm.touch() },
            includeArchive = includeArchive,
            onIncludeArchive = { vm.includeArchive.value = it; authVm.touch() },
            showDates = cardDates,
            onTogglePin = { if (isPro) { vm.togglePin(it); authVm.touch() } else proLock() },
            onToggleLock = { if (isPro) { vm.toggleLock(it); authVm.touch() } else proLock() },
            onToggleFavorite = { if (isPro) { vm.toggleFavorite(it); authVm.touch() } else proLock() },
            onCopyNote = { if (isPro) copyToClipboard(it.title, it.body) else proLock() },
            onDuplicateNote = { if (isPro) duplicateAndOpen(it) else proLock() },
            tagsByNote = tagsMap,
            // الفلترة بالوسم في الرئيسية فقط — مع خريطة العرض أسفل البطاقة
            allTags = allTags,
            tagFilter = tagFilter,
            onTagFilter = { vm.setTagFilter(it); authVm.touch() },
            // الفلترة بالتصنيف في الرئيسية — نقرة تفعّل وثانية تلغي (ك الوسم)
            allCategories = allCategories,
            categoryFilter = categoryFilter,
            onCategoryFilter = { vm.setCategoryFilter(it); authVm.touch() }
        ) }
    }
    // تحدي فتح عنصر مقفل: PIN أو بصمة — النجاح يفتح مباشرة، الرفض يلغي بصمت
    val pendingId = pendingLockedOpen
    if (pendingId != null && editing == null) {
        val pool = notes + archived
        val pn = pool.find { it.id == pendingId }
        LockChallenge(
            title = pn?.title?.ifBlank { null } ?: appCtx.getString(R.string.untitled),
            authVm = authVm,
            onUnlocked = { pendingLockedOpen = null; openNoteDirect(pendingId, archived.any { it.id == pendingId }) },
            onDismiss = { pendingLockedOpen = null }
        )
    }
}

// غلاف درج التنقل + الشريط السفلي الثابت: يُلفّ كل شاشات المستوى الأول (قرار 26 سبتمبر 2026) — الشريط ثابت في أي شاشة
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawerShell(
    selectedKey: String,
    accountId: String?,
    accountLabel: String?,
    isPro: Boolean,
    canFolders: Boolean,
    onNavigate: (String) -> Unit,
    // الشريط السفلي (قرار 26 سبتمبر 2026): القائمة الفارغة تخفيه — يُمرَّر في شاشات القوائم فقط
    barSlots: List<String> = emptyList(),
    onBarAdd: () -> Unit = {},
    // الهيدر المثبت (27 سبتمبر 2026): عنوان + زر القائمة في كل الشاشات الثانوية —
    // null = الشاشة تملك هيدرها الخاص (القوائم بالبحث/السلة/المحرر)
    title: String? = null,
    content: @Composable (() -> Unit) -> Unit
) {
    var drawerOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    BackHandler(enabled = drawerOpen) { drawerOpen = false }
    Scaffold(
        // الهيدر فوق الدرج دائمًا — لا يغطيه أبدًا
        topBar = {
            if (title != null) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { drawerOpen = true }) {
                            Icon(Icons.Filled.Menu, contentDescription = LocalContext.current.getString(R.string.menu_cd))
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (barSlots.isNotEmpty()) NotaBottomBar(
                slots = barSlots,
                selectedKey = selectedKey,
                isPro = isPro,
                onNavigate = onNavigate,
                onAdd = onBarAdd
            )
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            content({ drawerOpen = true })
            // الدرج المدمج (27 سبتمبر 2026): داخل جسم الشاشة فقط — لا يغطي الهيدر ولا الشريط السفلي،
            // كل عناصره قابلة للتمرير وظاهرة كاملة، والإغلاق بالخلفية المعتمة أو زر الرجوع أو اختيار وجهة
            if (drawerOpen) {
                Box(
                    Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                        .clickable { drawerOpen = false }
                )
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.width(300.dp).fillMaxHeight().align(Alignment.CenterStart)
                ) {
                    NotaDrawerContent(
                        selectedKey = selectedKey,
                        accountId = accountId,
                        accountLabel = accountLabel,
                        isPro = isPro,
                        canFolders = canFolders,
                        onNavigate = { key -> drawerOpen = false; onNavigate(key) }
                    )
                }
            }
        }
    }
}

// شاشة الترقية الموحدة لكل بوابات PRO (مجلدات/وسوم/تذكير/مفضلة/سحابة/…): دخول جوجل + تحديث الحالة + شراء
@Composable
private fun ProPaywall(premiumVm: PremiumViewModel, accountVm: AccountViewModel, onBack: () -> Unit, trialActive: Boolean = false, trialLeftMs: Long = 0L) {
    val entitlement by premiumVm.entitlement.collectAsStateWithLifecycle()
    val buying by premiumVm.buying.collectAsStateWithLifecycle()
    if (entitlement == null) {
        LoadingGate()
        return
    }
    // المرحلة 0 — المدخل المقفول يعرض الدخول بحساب جوجل قبل الترقية
    val launcher = rememberGoogleSignInLauncher(accountVm)
    val signedInId by accountVm.accountId.collectAsStateWithLifecycle()
    val busy by accountVm.signingIn.collectAsStateWithLifecycle()
    val refreshingState by premiumVm.refreshing.collectAsStateWithLifecycle()
    // M4 (المرحلة 0أ-3): نتيجة آخر شراء تُعرض للمستخدم — النجاح يُحدِّث الحالة تلقائيًا
    val lastResult by premiumVm.lastResult.collectAsStateWithLifecycle()
    // 5ب: التفعيل المؤجل (شراء ناجح بلا إبلاغ سيرفر) — تنبيه واحد ثم استهلاك
    val purchaseDeferred by premiumVm.purchaseDeferred.collectAsStateWithLifecycle()
    PaywallScreen(
        buying = buying,
        onUpgrade = { premiumVm.purchase(it) },
        onBack = onBack,
        signedIn = (signedInId != null),
        signingIn = busy,
        onGoogleSignIn = { launcher.launch(accountVm.signInIntent()) },
        refreshing = refreshingState,
        onRefreshStatus = { premiumVm.forceRefresh() },
        trialActive = trialActive,
        trialLeftMs = trialLeftMs,
        lastResult = lastResult,
        onPurchaseSuccess = { premiumVm.forceRefresh() },
        onResultConsumed = { premiumVm.clearLastResult() },
        purchaseDeferred = purchaseDeferred,
        deferredMsg = stringResource(R.string.purchase_deferred_fmt),
        onDeferredConsumed = { premiumVm.clearPurchaseDeferred() }
    )
}

// بوابة مدخل المجلدات (القفل المرئي 27 سبتمبر 2026): المحتوى معروض دائمًا —
// بلا صلاحية folders تُعطَّل الإجراءات مع أقفال بدل شاشة الترقية الكاملة
@Composable
private fun FoldersGate(premiumVm: PremiumViewModel, accountVm: AccountViewModel, onBack: () -> Unit, onLocked: () -> Unit = {}) {
    val entitlement by premiumVm.entitlement.collectAsStateWithLifecycle()
    if (entitlement == null) {
        LoadingGate()
        return
    }
    FoldersScreen(onBack = onBack, locked = !premiumVm.canUseFolders(), onLocked = onLocked)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AboutRoute(onBack: () -> Unit) {
    // الهيدر المثبت من DrawerShell — بلا شريط داخلي (27 سبتمبر 2026)
    com.nota.notes.ui.AboutScreen()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AccountRoute(accountVm: AccountViewModel, premiumVm: PremiumViewModel, onBack: () -> Unit) {
    // الهيدر المثبت من DrawerShell — بلا شريط داخلي (27 سبتمبر 2026)
    AccountScreen(accountVm = accountVm, premiumVm = premiumVm, onBack = onBack)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsRoute(settings: SettingsViewModel, authVm: AuthViewModel, onBack: () -> Unit, isPro: Boolean = true, onProLocked: () -> Unit = {}, onCustomizeNav: () -> Unit = {}) {
    val t by settings.theme.collectAsStateWithLifecycle()
    val l by settings.lang.collectAsStateWithLifecycle()
    val at by settings.appTheme.collectAsStateWithLifecycle()
    val bio by settings.biometricEnabled.collectAsStateWithLifecycle()
    val lock by settings.autoLockMs.collectAsStateWithLifecycle()
    val shots by settings.screenshotsAllowed.collectAsStateWithLifecycle()
    val shred by settings.shredEnabled.collectAsStateWithLifecycle()
    val shredM by settings.shredMethod.collectAsStateWithLifecycle()
    // المتقدمة المدمجة: حالتها هنا بعد دمج الشاشتين (قرار 23 سبتمبر 2026)
    val dc by settings.defaultColor.collectAsStateWithLifecycle()
    val f by settings.editorFont.collectAsStateWithLifecycle()
    val cd by settings.cardDates.collectAsStateWithLifecycle()
    val asv by settings.autosave.collectAsStateWithLifecycle()
    // الذكاء الاصطناعي BYOK (27 سبتمبر 2026): مجاني للجميع
    val aiEp by settings.aiEndpoint.collectAsStateWithLifecycle()
    val aiMd by settings.aiModel.collectAsStateWithLifecycle()
    val aiKs by settings.aiKeySet.collectAsStateWithLifecycle()
    // الترحيب والسرعة (27 سبتمبر 2026)
    val userName by settings.username.collectAsStateWithLifecycle()
    val welcomeOn by settings.welcomeEnabled.collectAsStateWithLifecycle()
    val quickOn by settings.quickMode.collectAsStateWithLifecycle()
    val effectsOn by settings.effectsOn.collectAsStateWithLifecycle()
    // الخصوصية GDPR (P3-B): opt-in + عدّاء التصدير عبر جسر Hilt
    val crashOpt by settings.crashOptIn.collectAsStateWithLifecycle()
    val analyticsOpt by settings.analyticsOptIn.collectAsStateWithLifecycle()
    val gdprVm: com.nota.notes.ui.GdprViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    // دقة التنبيهات: تُقرأ مع كل عرض + تُحدَّث بعد كل إصلاح (إعادة القراءة عند العودة من الإعداد)
    val routeCtx = LocalContext.current
    var healthTick by remember { mutableStateOf(0) }
    val health = remember(healthTick) { com.nota.notes.ui.readAlarmHealth(routeCtx.applicationContext) }
    fun refreshHealth() { healthTick++ }
    fun openNotifSettings() {
        runCatching {
            routeCtx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, "com.nota.notes"))
        }
        authVm.touch()
    }
    fun openExactAlarmSettings() {
        runCatching {
            routeCtx.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(android.net.Uri.parse("package:com.nota.notes"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        authVm.touch()
    }
    fun openBatterySettings() {
        runCatching {
            routeCtx.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(android.net.Uri.parse("package:com.nota.notes"))
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        authVm.touch()
    }
    // الإعدادات تتبع مظهر التطبيق (تلقائي افتراضيًا في كل الأجزاء)
    // الهيدر المثبت من DrawerShell — بلا شريط داخلي (27 سبتمبر 2026)
    SettingsScreen(
                theme = t, lang = l,
                appTheme = at, onAppTheme = settings::setAppTheme,
                biometricEnabled = bio,
                biometricAvailable = authVm.canBiometric(),
                autoLockMs = lock,
                onTheme = settings::setTheme, onLang = settings::setLang,
                onBiometric = settings::setBiometricEnabled,
                onAutoLock = settings::setAutoLockMs,
                screenshotsAllowed = shots,
                onScreenshots = settings::setScreenshotsAllowed,
                shredEnabled = shred,
                shredMethod = shredM,
                onShredEnabled = settings::setShredEnabled,
                onShredMethod = settings::setShredMethod,
                isPro = isPro,
                onProLocked = onProLocked,
                onCustomizeNav = onCustomizeNav,
                defaultColor = dc, font = f, cardDates = cd, autosave = asv,
                onDefaultColor = settings::setDefaultColor,
                onFont = settings::setEditorFont,
                onCardDates = settings::setCardDates,
                onAutosave = settings::setAutosave,
                authVm = authVm,
                alarmHealth = health,
                onOpenNotif = { openNotifSettings(); refreshHealth() },
                onOpenExactAlarm = { openExactAlarmSettings(); refreshHealth() },
                onOpenBattery = { openBatterySettings(); refreshHealth() },
                aiEndpoint = aiEp, aiModel = aiMd, aiKeySet = aiKs,
                onAiEndpoint = settings::setAiEndpoint,
                onAiModel = settings::setAiModel,
                onAiKey = settings::setAiKey,
                onAiTest = settings::testAi,
                username = userName, welcomeOn = welcomeOn, quickOn = quickOn,
                onUsername = settings::setUsername,
                onWelcome = settings::setWelcomeEnabled,
                onQuick = settings::setQuickMode,
                effectsOn = effectsOn,
                onEffects = settings::setEffects,
                crashOpt = crashOpt,
                analyticsOpt = analyticsOpt,
                onCrashOpt = settings::setCrashOptIn,
                onAnalyticsOpt = settings::setAnalyticsOptIn,
                exportRunner = gdprVm.runner
        )
}
