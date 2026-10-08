# Nota — نوتا

<p align="center">
  <img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.png" width="120" alt="Nota Logo"/>
</p>

<p align="center">
  <strong>Arabic Offline Encrypted Notes</strong><br/>
  Private. Secure. Beautiful.
</p>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/github/v/release/maegy2011/Nota?include_prerleases&style=flat-square" alt="Release"/>
  </a>
  <a href="https://github.com/maegy2011/Nota/stargazers">
    <img src="https://img.shields.io/github/stars/maegy2011/Nota?style=flat-square" alt="Stars"/>
  </a>
  <a href="https://github.com/maegy2011/Nota/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/maegy2011/Nota?style=flat-square" alt="License"/>
  </a>
</p>

<p align="center">
  <strong>Language:</strong>
  <a href="#english">English</a> ·
  <a href="#العربية">العربية</a> ·
  <a href="#русский">Русский</a> ·
  <a href="#हिन्दी">हिन्दी</a> ·
  <a href="#italiano">Italiano</a> ·
  <a href="#日本語">日本語</a> ·
  <a href="#中文">中文</a> ·
  <a href="#français">Français</a> ·
  <a href="#español">Español</a> ·
  <a href="#türkçe">Türkçe</a> ·
  <a href="#bahasa-melayu">Bahasa Melayu</a> ·
  <a href="#deutsch">Deutsch</a> ·
  <a href="#اردو">اردو</a> ·
  <a href="#polski">Polski</a> ·
  <a href="#українська">Українська</a> ·
  <a href="#português">Português</a> ·
  <a href="#nederlands">Nederlands</a>
</p>

---

## English

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-blue?style=for-the-badge&logo=android" alt="Download APK"/>
  </a>
</p>

**Nota** is a beautiful, fully offline encrypted notes app built for Arabic speakers — and everyone who values privacy.

### Features

| Feature | Free | Pro |
|---|---|---|
| Unlimited encrypted notes | ✅ | ✅ |
| Full-text search | ✅ | ✅ |
| Biometric / PIN lock | ✅ | ✅ |
| Markdown editor | ✅ | ✅ |
| Folders & tags | 🔒 | ✅ |
| Reminders | 🔒 | ✅ |
| Cloud backup | 🔒 | ✅ |
| Custom themes (10 Pro themes) | 🔒 | ✅ |
| AI rewrite | 🔒 | ✅ |
| Secure shred | 🔒 | ✅ |
| Home screen widget | 🔒 | ✅ |
| PDF export | 🔒 | ✅ |

### Security

- **SQLCipher** — AES-256 encrypted local database
- **Biometric auth** — fingerprint / face unlock
- **PIN fallback** — secure PIN with hashing
- **No data leaves your device** — fully offline by default
- **Encrypted cloud backup** — optional, password-protected
- **Secure shred** — overwrite and permanently delete sensitive notes
- **Root detection** — warns on rooted devices

### Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Database | Room + SQLCipher |
| DI | Hilt |
| Auth | Google Sign-In + Firebase |
| Billing | Google Play Billing |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

### Building

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Requirements:** JDK 17+, Android SDK 35

---

<details>
<summary><b>العربية</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/تحميل-أحدث%20APK-blue?style=for-the-badge&logo=android" alt="تحميل"/>
  </a>
</p>

**نوتا** — تطبيق ملاحظات عربي مشفّر يعمل بدون إنترنت، صُمم بعناية لجمهور عربي ولكل من يقدّر خصوصيته.

### المميزات

| الميزة | مجاني | احترافي |
|---|---|---|
| ملاحظات مشفّرة غير محدودة | ✅ | ✅ |
| بحث نصي شامل | ✅ | ✅ |
| قفل بيومتريك / رمز PIN | ✅ | ✅ |
| محرر Markdown | ✅ | ✅ |
| مجلدات ووسوم | 🔒 | ✅ |
| تذكيرات | 🔒 | ✅ |
| نسخ احتياطي سحابي | 🔒 | ✅ |
| ثيمات مخصصة (10 ثيمات) | 🔒 | ✅ |
| إعادة صياغة بالذكاء الاصطناعي | 🔒 | ✅ |
| حذف آمن نهائي | 🔒 | ✅ |
| ودجت الشاشة الرئيسية | 🔒 | ✅ |
| تصدير PDF | 🔒 | ✅ |

### الأمان

- **SQLCipher** — قاعدة بيانات محلية مشفّرة AES-256
- **مصادقة بيومترية** — بصمة / وجه
- **قفل PIN** — رمز آمن مع تجزئة
- **لا تغادر بياناتك جهازك** — يعمل بدون إنترنت بالكامل
- **نسخ احتياطي سحابي مشفّر** — اختياري ومحمي بكلمة مرور
- **حذف آمن** — الكتابة فوق البيانات وحذفها نهائيًا
- **كشف الروت** — تحذير على الأجهزة المكسورة الحماية

### البناء

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**المتطلبات:** JDK 17+، Android SDK 35

</details>

<details>
<summary><b>Русский</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Скачать-Последний%20APK-blue?style=for-the-badge&logo=android" alt="Скачать"/>
  </a>
</p>

**Nota** — красивое приложение для заметок с шифрованием, полностью работающее офлайн.

### Возможности

| Возможность | Бесплатно | Pro |
|---|---|---|
| Неограниченные зашифрованные заметки | ✅ | ✅ |
| Полнотекстовый поиск | ✅ | ✅ |
| Биометрия / PIN-код | ✅ | ✅ |
| Markdown-редактор | ✅ | ✅ |
| Папки и теги | 🔒 | ✅ |
| Напоминания | 🔒 | ✅ |
| Облачное резервное копирование | 🔒 | ✅ |
| Темы (10 Pro-тем) | 🔒 | ✅ |
| AI-переписывание | 🔒 | ✅ |
| Безопасное удаление | 🔒 | ✅ |
| Виджет на главном экране | 🔒 | ✅ |
| Экспорт в PDF | 🔒 | ✅ |

### Безопасность

- **SQLCipher** — локальная база данных с шифрованием AES-256
- **Биометрическая аутентификация** — отпечаток / лицо
- **PIN-код** — безопасный хэш
- **Данные не покидают устройство** — полностью офлайн
- **Шифрованное облачное резервное копирование** — опционально
- **Безопасное удаление** — перезапись и полное удаление
- **Обнаружение root** — предупреждение

### Сборка

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Требования:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>हिन्दी</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/डाउनलोड-नवीनतम%20APK-blue?style=for-the-badge&logo=android" alt="डाउनलोड"/>
  </a>
</p>

**Nota** — एक सुंदर, पूरी तरह ऑफ़लाइन एन्क्रिप्टेड नोट्स ऐप।

### विशेषताएँ

| विशेषता | मुफ़्त | Pro |
|---|---|---|
| असीमित एन्क्रिप्टेड नोट्स | ✅ | ✅ |
| पूर्ण-पाठ खोज | ✅ | ✅ |
| बायोमेट्रिक / PIN लॉक | ✅ | ✅ |
| Markdown एडिटर | ✅ | ✅ |
| फ़ोल्डर और टैग | 🔒 | ✅ |
| रिमाइंडर | 🔒 | ✅ |
| क्लाउड बैकअप | 🔒 | ✅ |
| कस्टम थीम (10 Pro थीम) | 🔒 | ✅ |
| AI रीराइट | 🔒 | ✅ |
| सुरक्षित श्रेड | 🔒 | ✅ |
| होम स्क्रीन विजेट | 🔒 | ✅ |
| PDF एक्सपोर्ट | 🔒 | ✅ |

### सुरक्षा

- **SQLCipher** — AES-256 एन्क्रिप्टेड लोकल डेटाबेस
- **बायोमेट्रिक ऑथेंटिकेशन** — फ़िंगरप्रिंट / फ़ेस
- **PIN फ़ॉलबैक** — सुरक्षित हैशिंग
- **डेटा डिवाइस से बाहर नहीं जाता** — पूरी तरह ऑफ़लाइन
- **एन्क्रिप्टेड क्लाउड बैकअप** — वैकल्पिक
- **सुरक्षित श्रेड** — ओवरराइट और स्थायी रूप से हटाएँ
- **रूट डिटेक्शन** — रूटेड डिवाइस पर चेतावनी

### निर्माण

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**आवश्यकताएँ:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Italiano</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Scarica-Ultima%20APK-blue?style=for-the-badge&logo=android" alt="Scarica"/>
  </a>
</p>

**Nota** — una bellissima app di note crittografate completamente offline.

### Funzionalità

| Funzionalità | Gratis | Pro |
|---|---|---|
| Note crittografate illimitate | ✅ | ✅ |
| Ricerca full-text | ✅ | ✅ |
| Blocco biometrico / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Cartelle e tag | 🔒 | ✅ |
| Promemoria | 🔒 | ✅ |
| Backup cloud | 🔒 | ✅ |
| Temi personalizzati (10 Pro) | 🔒 | ✅ |
| Riscrittura AI | 🔒 | ✅ |
| Distruzione sicura | 🔒 | ✅ |
| Widget home screen | 🔒 | ✅ |
| Esportazione PDF | 🔒 | ✅ |

### Sicurezza

- **SQLCipher** — database locale crittografato AES-256
- **Autenticazione biometrica** — impronta / volto
- **PIN fallback** — hashing sicuro
- **I dati non lasciano il dispositivo** — completamente offline
- **Backup cloud crittografato** — opzionale
- **Distruzione sicura** — sovrascrittura e cancellazione definitiva
- **Rilevamento root** — avviso su dispositivi rootati

### Compilazione

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Requisiti:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>日本語</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/ダウンロード-最新%20APK-blue?style=for-the-badge&logo=android" alt="ダウンロード"/>
  </a>
</p>

**Nota** — 美しい、完全オフラインの暗号化メモアプリ。

### 機能

| 機能 | 無料 | Pro |
|---|---|---|
| 無制限の暗号化メモ | ✅ | ✅ |
| 全文検索 | ✅ | ✅ |
| 生体認証 / PINロック | ✅ | ✅ |
| Markdownエディタ | ✅ | ✅ |
| フォルダとタグ | 🔒 | ✅ |
| リマインダー | 🔒 | ✅ |
| クラウドバックアップ | 🔒 | ✅ |
| カスタムテーマ（10 Proテーマ） | 🔒 | ✅ |
| AI書き換え | 🔒 | ✅ |
| 安全な完全削除 | 🔒 | ✅ |
| ホーム画面ウィジェット | 🔒 | ✅ |
| PDFエクスポート | 🔒 | ✅ |

### セキュリティ

- **SQLCipher** — AES-256暗号化ローカルデータベース
- **生体認証** — 指紋 / 顔
- **PINフォールバック** — 安全なハッシュ化
- **データはデバイスの外に出ない** — 完全オフライン
- **暗号化クラウドバックアップ** — オプション
- **安全な完全削除** — 上書きと恒久的削除
- **ルート検出** — ルート化デバイスで警告

### ビルド

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**要件:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>中文</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/下载-最新%20APK-blue?style=for-the-badge&logo=android" alt="下载"/>
  </a>
</p>

**Nota** — 精美的完全离线加密笔记应用。

### 功能

| 功能 | 免费 | 专业版 |
|---|---|---|
| 无限加密笔记 | ✅ | ✅ |
| 全文搜索 | ✅ | ✅ |
| 生物识别 / PIN 锁 | ✅ | ✅ |
| Markdown 编辑器 | ✅ | ✅ |
| 文件夹和标签 | 🔒 | ✅ |
| 提醒 | 🔒 | ✅ |
| 云备份 | 🔒 | ✅ |
| 自定义主题（10 个专业主题） | 🔒 | ✅ |
| AI 重写 | 🔒 | ✅ |
| 安全粉碎 | 🔒 | ✅ |
| 主屏幕小部件 | 🔒 | ✅ |
| PDF 导出 | 🔒 | ✅ |

### 安全

- **SQLCipher** — AES-256 加密本地数据库
- **生物识别认证** — 指纹 / 面部
- **PIN 备用** — 安全哈希
- **数据不离开设备** — 完全离线
- **加密云备份** — 可选
- **安全粉碎** — 覆盖并永久删除
- **Root 检测** — 在已 root 设备上警告

### 构建

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**要求：** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Français</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Télécharger-Dernière%20APK-blue?style=for-the-badge&logo=android" alt="Télécharger"/>
  </a>
</p>

**Nota** — une belle application de notes chiffrées entièrement hors ligne.

### Fonctionnalités

| Fonctionnalité | Gratuit | Pro |
|---|---|---|
| Notes chiffrées illimitées | ✅ | ✅ |
| Recherche plein texte | ✅ | ✅ |
| Verrou biométrique / PIN | ✅ | ✅ |
| Éditeur Markdown | ✅ | ✅ |
| Dossiers et tags | 🔒 | ✅ |
| Rappels | 🔒 | ✅ |
| Sauvegarde cloud | 🔒 | ✅ |
| Thèmes personnalisés (10 Pro) | 🔒 | ✅ |
| Réécriture IA | 🔒 | ✅ |
| Destruction sécurisée | 🔒 | ✅ |
| Widget écran d'accueil | 🔒 | ✅ |
| Export PDF | 🔒 | ✅ |

### Sécurité

- **SQLCipher** — base de données locale chiffrée AES-256
- **Authentification biométrique** — empreinte / visage
- **PIN de secours** — hachage sécurisé
- **Les données ne quittent pas l'appareil** — entièrement hors ligne
- **Sauvegarde cloud chiffrée** — optionnelle
- **Destruction sécurisée** — écrasement et suppression définitive
- **Détection root** — avertissement sur les appareils rootés

### Compilation

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Prérequis :** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Español</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Descargar-Última%20APK-blue?style=for-the-badge&logo=android" alt="Descargar"/>
  </a>
</p>

**Nota** — una hermosa aplicación de notas cifradas completamente sin conexión.

### Características

| Característica | Gratis | Pro |
|---|---|---|
| Notas cifradas ilimitadas | ✅ | ✅ |
| Búsqueda de texto completo | ✅ | ✅ |
| Bloqueo biométrico / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Carpetas y etiquetas | 🔒 | ✅ |
| Recordatorios | 🔒 | ✅ |
| Copia de seguridad en la nube | 🔒 | ✅ |
| Temas personalizados (10 Pro) | 🔒 | ✅ |
| Reescritura con IA | 🔒 | ✅ |
| Destrucción segura | 🔒 | ✅ |
| Widget de pantalla de inicio | 🔒 | ✅ |
| Exportar a PDF | 🔒 | ✅ |

### Seguridad

- **SQLCipher** — base de datos local cifrada AES-256
- **Autenticación biométrica** — huella / rostro
- **PIN alternativo** — hash seguro
- **Los datos no salen del dispositivo** — completamente sin conexión
- **Copia de seguridad en la nube cifrada** — opcional
- **Destrucción segura** — sobrescritura y eliminación permanente
- **Detección de root** — advertencia en dispositivos con root

### Compilación

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Requisitos:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Türkçe</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/İndir-Son%20APK-blue?style=for-the-badge&logo=android" alt="İndir"/>
  </a>
</p>

**Nota** — tamamen çevrimdışı, şifrelenmiş not uygulaması.

### Özellikler

| Özellik | Ücretsiz | Pro |
|---|---|---|
| Sınırsız şifrelenmiş not | ✅ | ✅ |
| Tam metin arama | ✅ | ✅ |
| Biyometrik / PIN kilidi | ✅ | ✅ |
| Markdown editörü | ✅ | ✅ |
| Klasörler ve etiketler | 🔒 | ✅ |
| Hatırlatıcılar | 🔒 | ✅ |
| Bulut yedekleme | 🔒 | ✅ |
| Özel temalar (10 Pro teması) | 🔒 | ✅ |
| AI yeniden yazma | 🔒 | ✅ |
| Güvenli silme | 🔒 | ✅ |
| Ana ekran widget'ı | 🔒 | ✅ |
| PDF dışa aktarma | 🔒 | ✅ |

### Güvenlik

- **SQLCipher** — AES-256 şifreli yerel veritabanı
- **Biyometrik kimlik doğrulama** — parmak izi / yüz
- **PIN yedekleme** — güvenli hash
- **Veriler cihazdan ayrılmaz** — tamamen çevrimdışı
- **Şifreli bulut yedekleme** — isteğe bağlı
- **Güvenli silme** — üzerine yazma ve kalıcı silme
- **Root tespiti** — root'lu cihazlarda uyarı

### Derleme

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Gereksinimler:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Bahasa Melayu</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Muat%20Turun-APK%20Terkini-blue?style=for-the-badge&logo=android" alt="Muat Turun"/>
  </a>
</p>

**Nota** — aplikasi nota sulit sepenuhnya luar talian yang cantik.

| Ciri | Percuma | Pro |
|---|---|---|
| Nota sulit tanpa had | ✅ | ✅ |
| Carian teks penuh | ✅ | ✅ |
| Kunci biometrik / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Folder dan tag | 🔒 | ✅ |
| Peringatan | 🔒 | ✅ |
| Sandaran awan | 🔒 | ✅ |
| Tema tersuai (10 tema Pro) | 🔒 | ✅ |
| Tulis semula AI | 🔒 | ✅ |
| Musnah selamat | 🔒 | ✅ |
| Widget skrin utama | 🔒 | ✅ |
| Eksport PDF | 🔒 | ✅ |

### Keselamatan

- **SQLCipher** — pangkalan data tempatan disulitkan AES-256
- **Pengesahan biometrik** — cap jari / wajah
- **PIN sandaran** — hash selamat
- **Data tidak meninggalkan peranti** — sepenuhnya luar talian
- **Sandaran awan disulitkan** — pilihan
- **Musnah selamat** — tulis ganti dan padam kekal
- **Pengesanan root** — amaran pada peranti root

### Binaan

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Keperluan:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Deutsch</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Herunterladen-Neueste%20APK-blue?style=for-the-badge&logo=android" alt="Herunterladen"/>
  </a>
</p>

**Nota** — eine wunderschöne, vollständig offline verschlüsselte Notizen-App.

### Funktionen

| Funktion | Kostenlos | Pro |
|---|---|---|
| Unbegrenzte verschlüsselte Notizen | ✅ | ✅ |
| Volltextsuche | ✅ | ✅ |
| Biometrische / PIN-Sperre | ✅ | ✅ |
| Markdown-Editor | ✅ | ✅ |
| Ordner und Tags | 🔒 | ✅ |
| Erinnerungen | 🔒 | ✅ |
| Cloud-Backup | 🔒 | ✅ |
| Eigene Themen (10 Pro-Themen) | 🔒 | ✅ |
| AI-Umschreibung | 🔒 | ✅ |
| Sicheres Vernichten | 🔒 | ✅ |
| Homescreen-Widget | 🔒 | ✅ |
| PDF-Export | 🔒 | ✅ |

### Sicherheit

- **SQLCipher** — AES-256 verschlüsselte lokale Datenbank
- **Biometrische Authentifizierung** — Fingerabdruck / Gesicht
- **PIN-Fallback** — sicheres Hashing
- **Daten verlassen das Gerät nicht** — vollständig offline
- **Verschlüsseltes Cloud-Backup** — optional
- **Sicheres Vernichten** — Überschreiben und endgültiges Löschen
- **Root-Erkennung** — Warnung auf gerooteten Geräten

### Kompilierung

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Anforderungen:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>اردو</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/ڈاؤن%20لوڈ-تازہ%20ترین%20APK-blue?style=for-the-badge&logo=android" alt="ڈاؤن لوڈ"/>
  </a>
</p>

**Nota** — ایک خوبصورت، مکمل طور پر آف لائن انکرپٹڈ نوٹس ایپ۔

### خصوصیات

| خصوصیت | مفت | Pro |
|---|---|---|
| لامحدود انکرپٹڈ نوٹس | ✅ | ✅ |
| مکمل متن کی تلاش | ✅ | ✅ |
| بایو میٹرک / PIN لاک | ✅ | ✅ |
| Markdown ایڈیٹر | ✅ | ✅ |
| فولڈر اور ٹیگ | 🔒 | ✅ |
| ریمائنڈر | 🔒 | ✅ |
| کلاؤڈ بیک اپ | 🔒 | ✅ |
| حسب ضرورت تھیم (10 Pro تھیم) | 🔒 | ✅ |
| AI دوبارہ تحریر | 🔒 | ✅ |
| محفوظ شیڈ | 🔒 | ✅ |
| ہوم اسکرین ویجیٹ | 🔒 | ✅ |
| PDF ایکسپورٹ | 🔒 | ✅ |

### سیکیورٹی

- **SQLCipher** — AES-256 انکرپٹڈ لوکل ڈیٹابیس
- **بایو میٹرک تصدیق** — فنگر پرنٹ / چہرہ
- **PIN بیک اپ** — محفوظ ہیشنگ
- **ڈیٹا ڈیوائس سے باہر نہیں جاتا** — مکمل طور پر آف لائن
- **انکرپٹڈ کلاؤڈ بیک اپ** — اختیاری
- **محفوظ شیڈ** — اوور رائٹ اور مستقل حذف
- **روٹ کی پہچان** — روٹ شدہ ڈیوائسز پر وارننگ

### تعمیر

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**ضروریات:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Polski</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Pobierz-Najnowszy%20APK-blue?style=for-the-badge&logo=android" alt="Pobierz"/>
  </a>
</p>

**Nota** — piękna, w pełni offline aplikacja do notatek z szyfrowaniem.

### Funkcje

| Funkcja | Darmowe | Pro |
|---|---|---|
| Nieograniczone szyfrowane notatki | ✅ | ✅ |
| Wyszukiwanie pełnotekstowe | ✅ | ✅ |
| Blokada biometryczna / PIN | ✅ | ✅ |
| Edytor Markdown | ✅ | ✅ |
| Foldery i tagi | 🔒 | ✅ |
| Przypomnienia | 🔒 | ✅ |
| Kopia zapasowa w chmurze | 🔒 | ✅ |
| Motywy niestandardowe (10 motywów Pro) | 🔒 | ✅ |
| Przepisywanie AI | 🔒 | ✅ |
| Bezpieczne niszczenie | 🔒 | ✅ |
| Widget ekranu głównego | 🔒 | ✅ |
| Eksport PDF | 🔒 | ✅ |

### Bezpieczeństwo

- **SQLCipher** — lokalna baza danych szyfrowana AES-256
- **Uwierzytelnianie biometryczne** — odcisk palca / twarz
- **PIN zapasowy** — bezpieczne hashowanie
- **Dane nie opuszczają urządzenia** — w pełni offline
- **Szyfrowana kopia zapasowa w chmurze** — opcjonalna
- **Bezpieczne niszczenie** — nadpisanie i trwałe usunięcie
- **Wykrywanie roota** — ostrzeżenie na urządzeniach z rootem

### Kompilacja

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Wymagania:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Українська</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Завантажити-Останній%20APK-blue?style=for-the-badge&logo=android" alt="Завантажити"/>
  </a>
</p>

**Nota** — красива, повністю офлайн зашифрована нотатки.

### Функції

| Функція | Безкоштовно | Pro |
|---|---|---|
| Необмежені зашифровані нотатки | ✅ | ✅ |
| Повнотекстовий пошук | ✅ | ✅ |
| Біометричний / PIN-код | ✅ | ✅ |
| Markdown-редактор | ✅ | ✅ |
| Папки та теги | 🔒 | ✅ |
| Нагадування | 🔒 | ✅ |
| Хмарне резервне копіювання | 🔒 | ✅ |
| Теми (10 Pro-тем) | 🔒 | ✅ |
| AI-переписування | 🔒 | ✅ |
| Безпечне знищення | 🔒 | ✅ |
| Віджет головного екрана | 🔒 | ✅ |
| Експорт PDF | 🔒 | ✅ |

### Безпека

- **SQLCipher** — локальна база даних з шифруванням AES-256
- **Біометрична автентифікація** — відбиток пальця / обличчя
- **PIN-код** — безпечне хешування
- **Дані не покидають пристрій** — повністю офлайн
- **Шифроване хмарне резервне копіювання** — опціонально
- **Безпечне знищення** — перезапис та остаточне видалення
- **Виявлення root** — попередження на пристроях з root

### Збірка

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Вимоги:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Português</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Baixar-Última%20APK-blue?style=for-the-badge&logo=android" alt="Baixar"/>
  </a>
</p>

**Nota** — um belo aplicativo de notas criptografadas totalmente offline.

### Recursos

| Recurso | Grátis | Pro |
|---|---|---|
| Notas criptografadas ilimitadas | ✅ | ✅ |
| Pesquisa de texto completo | ✅ | ✅ |
| Bloqueio biométrico / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Pastas e tags | 🔒 | ✅ |
| Lembretes | 🔒 | ✅ |
| Backup na nuvem | 🔒 | ✅ |
| Temas personalizados (10 Pro) | 🔒 | ✅ |
| Reescrita com IA | 🔒 | ✅ |
| Destruição segura | 🔒 | ✅ |
| Widget da tela inicial | 🔒 | ✅ |
| Exportar PDF | 🔒 | ✅ |

### Segurança

- **SQLCipher** — banco de dados local criptografado AES-256
- **Autenticação biométrica** — impressão digital / rosto
- **PIN alternativo** — hash seguro
- **Os dados não saem do dispositivo** — totalmente offline
- **Backup na nuvem criptografado** — opcional
- **Destruição segura** — sobrescrever e excluir permanentemente
- **Detecção de root** — aviso em dispositivos com root

### Compilação

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Requisitos:** JDK 17+, Android SDK 35

</details>

<details>
<summary><b>Nederlands</b></summary>

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Downloaden-Laatste%20APK-blue?style=for-the-badge&logo=android" alt="Downloaden"/>
  </a>
</p>

**Nota** — een prachtige, volledig offline versleutelde notitie-app.

### Functies

| Functie | Gratis | Pro |
|---|---|---|
| Onbeperkte versleutelde notities | ✅ | ✅ |
| Volledige tekst zoeken | ✅ | ✅ |
| Biometrische / PIN-vergrendeling | ✅ | ✅ |
| Markdown-editor | ✅ | ✅ |
| Mappen en tags | 🔒 | ✅ |
| Herinneringen | 🔒 | ✅ |
| Cloud-back-up | 🔒 | ✅ |
| Aangepaste thema's (10 Pro-thema's) | 🔒 | ✅ |
| AI-herschrijving | 🔒 | ✅ |
| Veilig vernietigen | 🔒 | ✅ |
| Startscherm-widget | 🔒 | ✅ |
| PDF-export | 🔒 | ✅ |

### Beveiliging

- **SQLCipher** — AES-256 versleutelde lokale database
- **Biometrische authenticatie** — vingerafdruk / gezicht
- **PIN-fallback** — veilige hashing
- **Gegevens verlaten het apparaat niet** — volledig offline
- **Versleutelde cloud-back-up** — optioneel
- **Veilig vernietigen** — overschrijven en permanent verwijderen
- **Root-detectie** — waarschuwing op gerootte apparaten

### Compilatie

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Vereisten:** JDK 17+, Android SDK 35

</details>

---

## Contributing

Contributions are welcome! Please read our [contributing guidelines](CONTRIBUTING.md) first.

## License

This project is licensed under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for details.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://github.com/maegy2011">maegy2011</a></sub>
</p>
