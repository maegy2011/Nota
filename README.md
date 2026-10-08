# Nota — نوتا

ملاحظات عربية offline مشفرة — نص فقط + بحث + أرشيف + قفل بيومتريك/PIN + نسخة احتياطية مشفرة بكلمة مرور.

- المواصفة: `docs/SPEC-V1.md`
- التقنية: Kotlin + Compose + Room + SQLCipher + DataStore + Hilt + Biometric
- minSdk 26 — targetSdk 35 — R8 من أول release

## الهيكل

- `app/` — كود التطبيق (المواصفة: package `com.nota.notes`)
- `docs/` — المواصفة وملفات التوثيق
- `keys/` — مفتاح التوقيع (`nota-release.keystore` — نفس المفتاح لكل التحديثات)
- `releases/` — نسخ APK النهائية
- `scripts/` — سكريبتات مساعدة (`build-safe.sh` للبناء المقيد)

## البناء

عبر طابور البناء المشترك (بناء واحد في المرة):

```bash
cd /home/administrator/.openclaw/workspace && ./build.sh <أمر البناء>
```

النشر: `https://mohamedadel.cfd/nota/Nota-v1.0.apk` + `Nota-latest.apk` — nginx `/nota/` ← `/var/www/nota/downloads/`
