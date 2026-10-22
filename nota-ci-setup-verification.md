# Nota CI Setup — تقرير التحقق (nota-ci-setup-verification.md)

التاريخ: 22 أكتوبر 2026 — الفرع المحلي: `release-ci` — المنهج: مهارة `coding` (التزام بالقيود حرفيًا).
الممنوعات المحترمة: لا push إطلاقًا، لا assembleRelease محليًا، لا minify=false، لا تعديل proguard، لا secrets في الكود، لا تعديل في gradle.properties beyond سطر jvmargs + تعليق.

## القرار

- `assembleRelease` فشل محليًا بسبب OOM في R8 (الجهاز 3.8GB RAM؛ وR8 يحتاج ≥4GB).
- القرار: البناء عبر GitHub Actions (runner بذاكرة 7GB + swap) — لا كود تطبيق جديد، فقط إعدادات CI + أوامر git محلية.
- لا push مباشر إلى master — العمل على فرع جديد `release-ci`؛ الـ merge إلى master قرار محمد بعد نجاح CI.

## المهمة 0 — فحص المستودع والبيئة (نواتج حرفية)

### 0-1 — حالة git (وقت الفحص، على master قبل إنشاء الفرع)

```
On branch master
Your branch is up to date with 'origin/master'.

Changes not staged for commit:
	modified:   nota-full-report-0-6.md

Untracked files:
	nota-closure-verification.md
	nota-device-test-final.md
	nota-fixes-verification.md
	nota-verification.md

no changes added to commit (use "git add" and/or "git commit -a")
```

```
26ece16 docs: add nota-full-report-0-6 status report
80688b8 docs: phase 6 status report
bb204f5 fix: correct ServerRegister import package in SettingsViewModel
c74f2ac feat(export): optional password encryption for GDPR zip
7a3ab43 feat(cloud): resumable chunked upload with silent full-upload fallback
```

```
origin	https://github.com/maegy2011/Nota.git (fetch)
origin	https://github.com/maegy2011/Nota.git (push)
```

```
master
```

- توجد uncommitted changes (ملف معدل + 4 تقارير غير متتبعة) — ليست من هذه المهمة، تُركت كما هي ولم تُلتزم.
- الـ remote URL هو `https://github.com/maegy2011/Nota.git` (بصيغة https وليس ssh — وظيفيًا نفس المستودع `maegy2011/Nota`).
- الفرع وقت الفحص كان `master`. ✅

### 0-2 — فحص build.gradle.kts

```
25:    signingConfigs {
26:        create("release") {
38:                ?: rootProject.file(System.getProperty("user.home") + "/.config/nota/nota-release.keystore").absolutePath
49:        release {
50:            signingConfig = signingConfigs.getByName("release")
59:            isShrinkResources = true
```

(ملاحظة: السلسلة `isMinifyEnabled` لم تطابق grep بسبب اختلاف الاسم — الملف يحتوي `isMinifyEnabled = true` سطر 58، تحقق يدويًا من قراءة الملف الكاملة.)

- يوجد `signingConfig` للـ release يقرأ keystore خارج المستودع (`~/.config/nota/nota-release.keystore` أو `NOTA_KEYSTORE_PATH`) وكلمات السر من env/ملف — لا debug signing في الكود، والبناء يفشل برسالة واضحة إن غابت الأسرار.
- `isMinifyEnabled = true` و `isShrinkResources = true` — لم يُعطَّلا. ✅
- `applicationId = "com.nota.notes"` (النكهات: `mock` بلاحقة `.mock`، و`play` للإنتاج).

### 0-3 — فحص gradle.properties (قبل التعديل)

```
7:org.gradle.jvmargs=-Xmx2500m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

- القيمة السابقة: `-Xmx2500m` — غير كافية لـ R8 (نفس OOM المحلي). ✅ شخصّت الحاجة.

### 0-4 — فحص .github/workflows/

```
total 12
drwxr-xr-x 2 administrator administrator 4096 Oct 19 22:56 .
drwxr-xr-x 3 administrator administrator 4096 Oct 19 22:56 ..
-rw-r--r-- 1 administrator administrator  304 Oct 19 22:56 test.yml
```

محتوى `test.yml` الموجود (من المرحلة P2):

```yaml
name: test
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew test
```

## المهمة 1 — تحديث gradle.properties ✅

- غُيّر فقط سطر `org.gradle.jvmargs` إلى:
  `org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8`
- مع تعليق فوقه: سبب R8 على GitHub runners + تنبيه أن البناء المحلي (3.8GB) يستخدم override.
- لم تُمس أي قيمة أخرى في الملف (diff: سطران فقط — تعليق + قيمة).
- Commit محلي (على `release-ci`):
  - `6f336f1` — `build(ci): raise gradle heap to 6g for R8 on GitHub runners`

## المهمة 2 — إنشاء build-release.yml ✅

- الملف: `.github/workflows/build-release.yml` (97 سطرًا، YAML صالح — فُحص بـ yaml.safe_load).
- يحتوي: `workflow_dispatch` بمدخل `variant` (mock/play) + `push tags nota-v*` ✅ | JDK 17 temurin ✅ | `gradle/actions/setup-gradle@v3` ✅ | تحرير مساحة ✅ | swapfile 10GB ✅ | `GRADLE_OPTS` بـ 6g ✅ | tests (`./gradlew test`) ثم assemble variant واحد (`assembleMockRelease` أو `assemblePlayRelease` حسب المدخل؛ الـ push بالـ tags يبني mock) ✅ | فحص presence الأسرار الثلاثة دون طباعة قيم ✅ | استعادة الـ keystore من `NOTA_KEYSTORE_BASE64` إلى المسار الافتراضي الذي يتوقعه `app/build.gradle.kts` ✅ | رفع الـ APKs عبر `upload-artifact@v4` (retention 7 أيام) ✅.
- لا secrets في الكود — GitHub Secrets فقط (`NOTA_KEYSTORE_BASE64` + `NOTA_KEYSTORE_PASSWORD` + `NOTA_KEY_PASSWORD`).
- Commit محلي (على `release-ci`):
  - `5a81135` — `ci: add build-release workflow with R8 OOM mitigation`

## المهمة 3 — signing/keystore ⏸️ لم تُنفذ

- لم تُنفذ — debug signing مؤقتًا (حسب التعليمات: المهمة اختيارية ولا تنفذ).
- الواقع الحالي: `app/build.gradle.kts` يتطلب keystore حقيقيًا (يفشل برسالة واضحة بدونه) — أي أن أول تشغيل CI بدون أسرار سيكون أحمر عند خطوة الـ signing، وهذا مقصود ومؤقت حتى يضيف محمد الـ Secrets الثلاثة. قرار بديل debug signing يبقى لمحمد وحده.

## المهمة 4 — الفرع المحلي ✅ (بلا push)

```
* release-ci (من master عند 26ece16)
  5a81135 ci: add build-release workflow with R8 OOM mitigation
  6f336f1 build(ci): raise gradle heap to 6g for R8 on GitHub runners
```

- `git log origin/master..HEAD` = الـ commitان أعلاه فقط — لم يُنفذ أي `git push` إطلاقًا. الرفع والـ merge قرار محمد وحده.

## ما ينتظر محمد

1. إضافة 3 أسرار في GitHub (Settings → Secrets → Actions): `NOTA_KEYSTORE_BASE64` (ملف الـ keystore مُشفر base64) + `NOTA_KEYSTORE_PASSWORD` + `NOTA_KEY_PASSWORD` (نفس القيم المحلية في `~/.config/nota/signing.env` — تُنسخ يدويًا، لا تُلتزم أبدًا).
2. مراجعة الفرع ثم الدفع والـ merge بنفسه:
   `git push -u origin release-ci` (من جهازه/قراره) → تشغيل الـ workflow يدويًا (variant: mock أولًا) → بعد الأخضر: الـ merge إلى master.
3. ملاحظة التكلفة: إن كان المستودع private فـ GitHub Actions يمنح 2000 دقيقة/شهر مجانًا — بناء R8 واحد (~15-25 دقيقة) × عدد الإصدارات الشهري يكفي بسهولة؛ المستودع public فالبناء مجاني بلا حدود عملية.
4. أول تشغيل مقترح: `workflow_dispatch` بـ `mock` (التحقق من R8 + الـ keeps قبل نسخة play).

## ما لم يُنفذ (بأمر المهمة)

- المهمة 3 (signing/keystore): لم تُنفذ — debug signing مؤقتًا/قرار محمد.
- لم يُشغَّل أي بناء محلي (`assembleRelease` ممنوع) — التحقق هنا استاتيكي (YAML صالح + grep للعناصر المطلوبة).
- ملفات التقارير غير المرتبطة (4 ملفات + تعديل `nota-full-report-0-6.md`) تُركت uncommitted كما وُجدت — ليست ضمن هذه المهمة.

## مخاطر

1. أول CI أحمر متوقع إن غابت الـ Secrets — مقصود ومشخّص (خطوة الفحص تطبع MISSING بوضوح).
2. `gradle.properties` الجديد (6g) ثقيل على الجهاز المحلي (3.8GB + حد systemd) — أي بناء release محلي مستقبلًا يجب أن يستخدم `-Dorg.gradle.jvmargs` override بقيمة صغيرة، وإلا ظهر swapping شديد.
3. R8 مع Hilt 2.56.2 + `isMinifyEnabled=true` قد يكسر الإقلاع كما حدث مع Hilt 2.52 (موثق في `app/build.gradle.kts`) — التحقق على جهاز حقيقي إلزامي قبل أي نشر، وأي كسر يُعالج بإضافة `-keep` لا بإطفاء minify.
4. الـ keystore يُستعاد من secret في كل تشغيل — التأكد من `NOTA_KEYSTORE_BASE64` سليم (base64 لملف `.keystore` بلا أسطر ملتفة) وإلا فشل الـ decode.

---

## الحالة: PUSHED + WORKFLOW DISPATCHED

الفرع `release-ci` مدفوع إلى origin:

- `6f336f1` build(ci): raise gradle heap to 6g for R8 on GitHub runners
- `5a81135` ci: add build-release workflow with R8 OOM mitigation
- `8a9b4ac` fix(ci): gate keystore step on env not secrets (422 on dispatch)

### إصلاح عطل التشغيل اليدوي (22 أكتوبر)

- العرض: `gh workflow run ... -f variant=play` رفض بـ HTTP 422 مرتين:
  1. `Unexpected inputs provided: ["build_variant"]` — اسم المُدخل الصحيح `variant`.
  2. `failed to parse workflow (Line 68): Unrecognized named-value: 'secrets'` — سياق `secrets` ممنوع في `steps[*].if` (GitHub يرفض الملف كله عند الـ parse، وهذا نفسه سبب فشل run الـ push رقم `39750162417`).
- الإصلاح: نسخ السر إلى `env.HAS_RELEASE_KEYSTORE` على مستوى الـ job، وبوابة الخطوة أصبحت `if: ${{ env.HAS_RELEASE_KEYSTORE != '' }}` — `env` مسموح في `steps[*].if`.
- بعد الإصلاح: التشغيل اليدوي قُبِل — run رقم `39750785162` (workflow_dispatch, variant=play) بحالة `in_progress`.
- المتابعة: مراقبة الـ run حتى النهاية (~20-40 دقيقة) ثم تنزيل الـ APK من Artifacts.
