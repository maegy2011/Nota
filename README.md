# Nota — نوتا

<p align="center">
  <img src="app/src/main/res/mipmap-anydpi-v26/ic_launcher.png" width="120" alt="Nota Logo"/>
</p>

<p align="center">
  <strong>Military-Grade Encrypted Notes</strong><br/>
  Your thoughts. Your device. Your rules.
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
  <a href="https://github.com/maegy2011/Nota#english">English</a> ·
  <a href="https://github.com/maegy2011/Nota#العربية">العربية</a> ·
  <a href="https://github.com/maegy2011/Nota#русский">Русский</a> ·
  <a href="https://github.com/maegy2011/Nota#हिन्दी">हिन्दी</a> ·
  <a href="https://github.com/maegy2011/Nota#italiano">Italiano</a> ·
  <a href="https://github.com/maegy2011/Nota#日本語">日本語</a> ·
  <a href="https://github.com/maegy2011/Nota#中文">中文</a> ·
  <a href="https://github.com/maegy2011/Nota#français">Français</a> ·
  <a href="https://github.com/maegy2011/Nota#español">Español</a> ·
  <a href="https://github.com/maegy2011/Nota#türkçe">Türkçe</a> ·
  <a href="https://github.com/maegy2011/Nota#bahasa-melayu">Bahasa Melayu</a> ·
  <a href="https://github.com/maegy2011/Nota#deutsch">Deutsch</a> ·
  <a href="https://github.com/maegy2011/Nota#اردو">اردو</a> ·
  <a href="https://github.com/maegy2011/Nota#polski">Polski</a> ·
  <a href="https://github.com/maegy2011/Nota#українська">Українська</a> ·
  <a href="https://github.com/maegy2011/Nota#português">Português</a> ·
  <a href="https://github.com/maegy2011/Nota#nederlands">Nederlands</a>
</p>

---

## English

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-blue?style=for-the-badge&logo=android" alt="Download APK"/>
  </a>
</p>

**Nota** is a privacy-first encrypted notes app. Every note is encrypted on-device before it ever touches storage — we can't read your notes, no one can.

### Security First

| Layer | Protection |
|---|---|
| **Database** | SQLCipher — AES-256 full-database encryption |
| **Key Storage** | Android Keystore — hardware-backed when available |
| **Authentication** | Biometric (fingerprint / face) + secure PIN fallback |
| **Network** | Zero telemetry. Zero analytics. Zero trackers. |
| **Offline** | Core app works 100% offline — no account required |
| **Cloud Backup** | Optional, end-to-end encrypted with your passphrase |
| **Secure Shred** | Overwrite + crypto-shred — deleted notes are unrecoverable |
| **Root Detection** | Warns and logs on rooted/jailbroken devices |
| **Memory** | Sensitive data cleared from RAM on background |
| **Screenshots** | FLAG_SECURE — blocks screenshots in recents |

### Features

| Feature | Free | Pro |
|---|---|---|
| Unlimited encrypted notes | ✅ | ✅ |
| Full-text search | ✅ | ✅ |
| Biometric / PIN lock | ✅ | ✅ |
| Markdown editor | ✅ | ✅ |
| Folders & tags | 🔒 | ✅ |
| Reminders | 🔒 | ✅ |
| Encrypted cloud backup | 🔒 | ✅ |
| Custom themes (10 Pro themes) | 🔒 | ✅ |
| AI rewrite | 🔒 | ✅ |
| Secure shred | 🔒 | ✅ |
| Home screen widget | 🔒 | ✅ |
| PDF export | 🔒 | ✅ |

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

**نوتا** — تطبيق ملاحظات مشفّر بأولوية الخصوصية. كل ملاحظة تُشفّر على جهازك قبل أن تُخزّن — لا نستطيع قراءة ملاحظاتك، ولا أحد يستطيع.

### الأمان أولًا

| الطبقة | الحماية |
|---|---|
| **قاعدة البيانات** | SQLCipher — تشفير AES-256 كامل لقاعدة البيانات |
| **تخزين المفاتيح** | Android Keystore — مدعوم بالعتاد عند توفره |
| **المصادقة** | بيومترية (بصمة / وجه) + رمز PIN آمن |
| **الشبكة** | صفر تتبع. صفر تحليلات. صفر متتبعين. |
| **بدون إنترنت** | التطبيق الأساسي يعمل 100% بدون إنترنت — بدون حساب |
| **النسخ الاحتياطي السحابي** | اختياري، مشفّر من الطرف إلى الطرف بكلمة مرورك |
| **الحذف الآمن** | كتابة فوق البيانات + تشفير — الملاحظات المحذوفة غير قابلة للاسترداد |
| **كشف الروت** | تحذير وتسجيل على الأجهزة المكسورة الحماية |
| **الذاكرة** | مسح البيانات الحساسة من الذاكرة عند الخلفية |
| **لقطات الشاشة** | FLAG_SECURE — يمنع لقطات الشاشة |

### المميزات

| الميزة | مجاني | احترافي |
|---|---|---|
| ملاحظات مشفّرة غير محدودة | ✅ | ✅ |
| بحث نصي شامل | ✅ | ✅ |
| قفل بيومتريك / رمز PIN | ✅ | ✅ |
| محرر Markdown | ✅ | ✅ |
| مجلدات ووسوم | 🔒 | ✅ |
| تذكيرات | 🔒 | ✅ |
| نسخ احتياطي سحابي مشفّر | 🔒 | ✅ |
| ثيمات مخصصة (10 ثيمات) | 🔒 | ✅ |
| إعادة صياغة بالذكاء الاصطناعي | 🔒 | ✅ |
| حذف آمن نهائي | 🔒 | ✅ |
| ودجت الشاشة الرئيسية | 🔒 | ✅ |
| تصدير PDF | 🔒 | ✅ |

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

**Nota** — приложение для заметок с шифрованием и приоритетом приватности. Каждая заметка шифруется на устройстве до сохранения — мы не можем прочитать ваши заметки, никто не может.

### Безопасность прежде всего

| Слой | Защита |
|---|---|
| **База данных** | SQLCipher — полное шифрование AES-256 |
| **Хранение ключей** | Android Keystore — аппаратное при наличии |
| **Аутентификация** | Биометрия (отпечаток / лицо) + PIN-код |
| **Сеть** | Ноль телеметрии. Ноль аналитики. Ноль трекеров. |
| **Офлайн** | Ядро работает 100% офлайн — без аккаунта |
| **Облачное резервное копирование** | Опционально, сквозное шифрование с вашей фразой |
| **Безопасное удаление** | Перезапись + крипто-шреддинг — удалённое невосстановимо |
| **Обнаружение root** | Предупреждение на рутированных устройствах |
| **Память** | Очистка чувствительных данных из RAM |
| **Скриншоты** | FLAG_SECURE — блокирует скриншоты |

### Возможности

| Возможность | Бесплатно | Pro |
|---|---|---|
| Неограниченные зашифрованные заметки | ✅ | ✅ |
| Полнотекстовый поиск | ✅ | ✅ |
| Биометрия / PIN-код | ✅ | ✅ |
| Markdown-редактор | ✅ | ✅ |
| Папки и теги | 🔒 | ✅ |
| Напоминания | 🔒 | ✅ |
| Шифрованное облачное резервное копирование | 🔒 | ✅ |
| Темы (10 Pro-тем) | 🔒 | ✅ |
| AI-переписывание | 🔒 | ✅ |
| Безопасное удаление | 🔒 | ✅ |
| Виджет на главном экране | 🔒 | ✅ |
| Экспорт в PDF | 🔒 | ✅ |

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

**Nota** — गोपनीयता-प्रथम एन्क्रिप्टेड नोट्स ऐप। हर नोट स्टोरेज में जाने से पहले डिवाइस पर एन्क्रिप्ट होता है — हम आपके नोट्स नहीं पढ़ सकते, कोई नहीं पढ़ सकता।

### सुरक्षा पहले

| परत | सुरक्षा |
|---|---|
| **डेटाबेस** | SQLCipher — AES-256 पूर्ण डेटाबेस एन्क्रिप्शन |
| **कुंजी स्टोरेज** | Android Keystore — हार्डवेयर-समर्थित जब उपलब्ध |
| **प्रमाणीकरण** | बायोमेट्रिक (फ़िंगरप्रिंट / चेहरा) + सुरक्षित PIN |
| **नेटवर्क** | शून्य टेलीमेट्री। शून्य एनालिटिक्स। शून्य ट्रैकर। |
| **ऑफ़लाइन** | मूल ऐप 100% ऑफ़लाइन काम करता है — बिना खाते |
| **क्लाउड बैकअप** | वैकल्पिक, एंड-टू-एंड एन्क्रिप्टेड आपके पासफ़्रेज़ के साथ |
| **सुरक्षित श्रेड** | ओवरराइट + क्रिप्टो-श्रेड — हटाए गए नोट्स अप्राप्य |
| **रूट डिटेक्शन** | रूटेड डिवाइस पर चेतावनी |
| **मेमोरी** | बैकग्राउंड पर संवेदनशील डेटा साफ़ |
| **स्क्रीनशॉट** | FLAG_SECURE — स्क्रीनशॉट ब्लॉक |

### विशेषताएँ

| विशेषता | मुफ़्त | Pro |
|---|---|---|
| असीमित एन्क्रिप्टेड नोट्स | ✅ | ✅ |
| पूर्ण-पाठ खोज | ✅ | ✅ |
| बायोमेट्रिक / PIN लॉक | ✅ | ✅ |
| Markdown एडिटर | ✅ | ✅ |
| फ़ोल्डर और टैग | 🔒 | ✅ |
| रिमाइंडर | 🔒 | ✅ |
| एन्क्रिप्टेड क्लाउड बैकअप | 🔒 | ✅ |
| कस्टम थीम (10 Pro थीम) | 🔒 | ✅ |
| AI रीराइट | 🔒 | ✅ |
| सुरक्षित श्रेड | 🔒 | ✅ |
| होम स्क्रीन विजेट | 🔒 | ✅ |
| PDF एक्सपोर्ट | 🔒 | ✅ |

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

**Nota** — app di note crittografate con priorità alla privacy. Ogni nota viene crittografata sul dispositivo prima dell'archiviazione — non possiamo leggere le tue note, nessuno può.

### Sicurezza prima di tutto

| Livello | Protezione |
|---|---|
| **Database** | SQLCipher — crittografia completa AES-256 |
| **Archivio chiavi** | Android Keystore — supportato hardware quando disponibile |
| **Autenticazione** | Biometrico (impronta / volto) + PIN sicuro |
| **Rete** | Zero telemetria. Zero analytics. Zero tracker. |
| **Offline** | Il core funziona 100% offline — senza account |
| **Backup cloud** | Opzionale, crittografato end-to-end con la tua passphrase |
| **Distruzione sicura** | Sovrascrittura + crypto-shred — le note eliminate sono irrecuperabili |
| **Rilevamento root** | Avviso su dispositivi rootati |
| **Memoria** | Cancellazione dati sensibili dalla RAM |
| **Screenshot** | FLAG_SECURE — blocca screenshot |

### Funzionalità

| Funzionalità | Gratis | Pro |
|---|---|---|
| Note crittografate illimitate | ✅ | ✅ |
| Ricerca full-text | ✅ | ✅ |
| Blocco biometrico / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Cartelle e tag | 🔒 | ✅ |
| Promemoria | 🔒 | ✅ |
| Backup cloud crittografato | 🔒 | ✅ |
| Temi personalizzati (10 Pro) | 🔒 | ✅ |
| Riscrittura AI | 🔒 | ✅ |
| Distruzione sicura | 🔒 | ✅ |
| Widget home screen | 🔒 | ✅ |
| Esportazione PDF | 🔒 | ✅ |

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

**Nota** — プライバシー最優先の暗号化メモアプリ。すべてのメモは保存前にデバイス上で暗号化されます — 私たちも誰もあなたのメモを読めません。

### セキュリティファースト

| レイヤー | 保護 |
|---|---|
| **データベース** | SQLCipher — AES-256完全データベース暗号化 |
| **キーストア** | Android Keystore — ハードウェアバック（利用可能な場合） |
| **認証** | 生体認証（指紋 / 顔）+ 安全なPIN |
| **ネットワーク** | ゼロテレメトリ。ゼロアナリティクス。ゼロトラッカー。 |
| **オフライン** | コアアプリは100%オフラインで動作 — アカウント不要 |
| **クラウドバックアップ** | オプション、エンドツーエンド暗号化（パスフレーズ） |
| **安全な完全削除** | 上書き + 暗号シュレッド — 削除したメモは復元不可 |
| **ルート検出** | ルート化デバイスで警告 |
| **メモリ** | バックグラウンド時に機密データをRAMから消去 |
| **スクリーンショット** | FLAG_SECURE — スクリーンショットをブロック |

### 機能

| 機能 | 無料 | Pro |
|---|---|---|
| 無制限の暗号化メモ | ✅ | ✅ |
| 全文検索 | ✅ | ✅ |
| 生体認証 / PINロック | ✅ | ✅ |
| Markdownエディタ | ✅ | ✅ |
| フォルダとタグ | 🔒 | ✅ |
| リマインダー | 🔒 | ✅ |
| 暗号化クラウドバックアップ | 🔒 | ✅ |
| カスタムテーマ（10 Proテーマ） | 🔒 | ✅ |
| AI書き換え | 🔒 | ✅ |
| 安全な完全削除 | 🔒 | ✅ |
| ホーム画面ウィジェット | 🔒 | ✅ |
| PDFエクスポート | 🔒 | ✅ |

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

**Nota** — 隐私优先的加密笔记应用。每条笔记在存储前都在设备上加密 — 我们无法读取您的笔记，任何人都无法读取。

### 安全第一

| 层级 | 保护 |
|---|---|
| **数据库** | SQLCipher — AES-256 全数据库加密 |
| **密钥存储** | Android Keystore — 硬件支持（可用时） |
| **认证** | 生物识别（指纹 / 面部）+ 安全 PIN |
| **网络** | 零遥测。零分析。零追踪器。 |
| **离线** | 核心应用 100% 离线运行 — 无需账户 |
| **云备份** | 可选，端到端加密，使用您的密码短语 |
| **安全粉碎** | 覆盖 + 加密粉碎 — 删除的笔记不可恢复 |
| **Root 检测** | 在已 root 设备上发出警告 |
| **内存** | 后台时清除 RAM 中的敏感数据 |
| **截图** | FLAG_SECURE — 阻止截图 |

### 功能

| 功能 | 免费 | 专业版 |
|---|---|---|
| 无限加密笔记 | ✅ | ✅ |
| 全文搜索 | ✅ | ✅ |
| 生物识别 / PIN 锁 | ✅ | ✅ |
| Markdown 编辑器 | ✅ | ✅ |
| 文件夹和标签 | 🔒 | ✅ |
| 提醒 | 🔒 | ✅ |
| 加密云备份 | 🔒 | ✅ |
| 自定义主题（10 个专业主题） | 🔒 | ✅ |
| AI 重写 | 🔒 | ✅ |
| 安全粉碎 | 🔒 | ✅ |
| 主屏幕小部件 | 🔒 | ✅ |
| PDF 导出 | 🔒 | ✅ |

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

**Nota** — application de notes chiffrées axée sur la confidentialité. Chaque note est chiffrée sur l'appareil avant stockage — nous ne pouvons pas lire vos notes, personne ne le peut.

### Sécurité d'abord

| Couche | Protection |
|---|---|
| **Base de données** | SQLCipher — chiffrement complet AES-256 |
| **Stockage des clés** | Android Keystore — matériel si disponible |
| **Authentification** | Biométrique (empreinte / visage) + PIN sécurisé |
| **Réseau** | Zéro télémétrie. Zéro analytics. Zéro trackers. |
| **Hors ligne** | Le cœur fonctionne 100% hors ligne — sans compte |
| **Sauvegarde cloud** | Optionnelle, chiffrée de bout en bout avec votre phrase secrète |
| **Destruction sécurisée** | Écrasement + crypto-shred — les notes supprimées sont irrécupérables |
| **Détection root** | Avertissement sur les appareils rootés |
| **Mémoire** | Effacement des données sensibles de la RAM |
| **Captures d'écran** | FLAG_SECURE — bloque les captures |

### Fonctionnalités

| Fonctionnalité | Gratuit | Pro |
|---|---|---|
| Notes chiffrées illimitées | ✅ | ✅ |
| Recherche plein texte | ✅ | ✅ |
| Verrou biométrique / PIN | ✅ | ✅ |
| Éditeur Markdown | ✅ | ✅ |
| Dossiers et tags | 🔒 | ✅ |
| Rappels | 🔒 | ✅ |
| Sauvegarde cloud chiffrée | 🔒 | ✅ |
| Thèmes personnalisés (10 Pro) | 🔒 | ✅ |
| Réécriture IA | 🔒 | ✅ |
| Destruction sécurisée | 🔒 | ✅ |
| Widget écran d'accueil | 🔒 | ✅ |
| Export PDF | 🔒 | ✅ |

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

**Nota** — aplicación de notas cifradas con prioridad a la privacidad. Cada nota se cifra en el dispositivo antes del almacenamiento — no podemos leer tus notas, nadie puede.

### Seguridad primero

| Capa | Protección |
|---|---|
| **Base de datos** | SQLCipher — cifrado completo AES-256 |
| **Almacén de claves** | Android Keystore — respaldo hardware si disponible |
| **Autenticación** | Biométrico (huella / rostro) + PIN seguro |
| **Red** | Cero telemetría. Cero analíticas. Cero rastreadores. |
| **Sin conexión** | El núcleo funciona 100% sin conexión — sin cuenta |
| **Copia de seguridad en la nube** | Opcional, cifrado de extremo a extremo con tu frase |
| **Destrucción segura** | Sobrescritura + crypto-shred — las notas eliminadas son irrecuperables |
| **Detección de root** | Advertencia en dispositivos con root |
| **Memoria** | Borrado de datos sensibles de la RAM |
| **Capturas de pantalla** | FLAG_SECURE — bloquea capturas |

### Características

| Característica | Gratis | Pro |
|---|---|---|
| Notas cifradas ilimitadas | ✅ | ✅ |
| Búsqueda de texto completo | ✅ | ✅ |
| Bloqueo biométrico / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Carpetas y etiquetas | 🔒 | ✅ |
| Recordatorios | 🔒 | ✅ |
| Copia de seguridad cifrada | 🔒 | ✅ |
| Temas personalizados (10 Pro) | 🔒 | ✅ |
| Reescritura con IA | 🔒 | ✅ |
| Destrucción segura | 🔒 | ✅ |
| Widget de pantalla de inicio | 🔒 | ✅ |
| Exportar a PDF | 🔒 | ✅ |

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

**Nota** — gizlilik öncelikli şifrelenmiş not uygulaması. Her not, saklanmadan önce cihazda şifrelenir — notlarınızı okuyamayız, kimse okuyamaz.

### Güvenlik Önce

| Katman | Koruma |
|---|---|
| **Veritabanı** | SQLCipher — AES-256 tam veritabanı şifreleme |
| **Anahtar Deposu** | Android Keystore — donanım destekli (mevcutsa) |
| **Kimlik Doğrulama** | Biyometrik (parmak izi / yüz) + güvenli PIN |
| **Ağ** | Sıfır telemetri. Sıfır analitik. Sıfır izleyici. |
| **Çevrimdışı** | Çekirdek uygulama %100 çevrimdışı çalışır — hesap gerektirmez |
| **Bulut Yedekleme** | İsteğe bağlı, uçtan uca şifreli (ifrenizle) |
| **Güvenli Silme** | Üzerine yazma + kripto-shred — silinen notlar kurtarılamaz |
| **Root Tespiti** | Root'lu cihazlarda uyarı |
| **Bellek** | Arka planda hassas veriler RAM'den temizlenir |
| **Ekran Görüntüsü** | FLAG_SECURE — ekran görüntüsünü engeller |

### Özellikler

| Özellik | Ücretsiz | Pro |
|---|---|---|
| Sınırsız şifrelenmiş not | ✅ | ✅ |
| Tam metin arama | ✅ | ✅ |
| Biyometrik / PIN kilidi | ✅ | ✅ |
| Markdown editörü | ✅ | ✅ |
| Klasörler ve etiketler | 🔒 | ✅ |
| Hatırlatıcılar | 🔒 | ✅ |
| Şifreli bulut yedekleme | 🔒 | ✅ |
| Özel temalar (10 Pro teması) | 🔒 | ✅ |
| AI yeniden yazma | 🔒 | ✅ |
| Güvenli silme | 🔒 | ✅ |
| Ana ekran widget'ı | 🔒 | ✅ |
| PDF dışa aktarma | 🔒 | ✅ |

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

**Nota** — aplikasi nota sulit yang mengutamakan privasi. Setiap nota disulitkan pada peranti sebelum disimpan — kami tidak boleh membaca nota anda, sesiapa pun tidak boleh.

### Keselamatan Dahulu

| Lapisan | Perlindungan |
|---|---|
| **Pangkalan Data** | SQLCipher — penyulitan penuh AES-256 |
| **Storan Kunci** | Android Keystore — sokongan perkakasan |
| **Pengesahan** | Biometrik (cap jari / wajah) + PIN selamat |
| **Rangkaian** | Sifar telemetri. Sifar analitik. Sifar penjejak. |
| **Luar Talian** | Aplikasi teras berfungsi 100% luar talian — tanpa akaun |
| **Sandaran Awan** | Pilihan, penyulitan hujung-ke-hujung dengan frasa laluan anda |
| **Musnah Selamat** | Tulis ganti + krypto-shred — nota yang dipadam tidak boleh dipulihkan |
| **Pengesanan Root** | Amaran pada peranti root |
| **Memori** | Padam data sensitif dari RAM |
| **Tangkapan Skrin** | FLAG_SECURE — sekat tangkapan skrin |

### Ciri

| Ciri | Percuma | Pro |
|---|---|---|
| Nota sulit tanpa had | ✅ | ✅ |
| Carian teks penuh | ✅ | ✅ |
| Kunci biometrik / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Folder dan tag | 🔒 | ✅ |
| Peringatan | 🔒 | ✅ |
| Sandaran awan disulitkan | 🔒 | ✅ |
| Tema tersuai (10 tema Pro) | 🔒 | ✅ |
| Tulis semula AI | 🔒 | ✅ |
| Musnah selamat | 🔒 | ✅ |
| Widget skrin utama | 🔒 | ✅ |
| Eksport PDF | 🔒 | ✅ |

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

**Nota** — eine datenschutzorientierte verschlüsselte Notizen-App. Jede Notiz wird auf dem Gerät verschlüsselt, bevor sie gespeichert wird — wir können Ihre Notizen nicht lesen, niemand kann.

### Sicherheit zuerst

| Schicht | Schutz |
|---|---|
| **Datenbank** | SQLCipher — AES-256-Vollverschlüsselung |
| **Schlüsselspeicher** | Android Keystore — hardwaregestützt |
| **Authentifizierung** | Biometrisch (Fingerabdruck / Gesicht) + sicheres PIN |
| **Netzwerk** | Null Telemetrie. Null Analytics. Null Tracker. |
| **Offline** | Kern funktioniert 100% offline — kein Konto |
| **Cloud-Backup** | Optional, Ende-zu-Ende verschlüsselt mit Ihrer Passphrase |
| **Sicheres Vernichten** | Überschreiben + Crypto-Shred — gelöschte Notizen unrettbar |
| **Root-Erkennung** | Warnung auf gerooteten Geräten |
| **Speicher** | Sensible Daten werden aus RAM gelöscht |
| **Screenshots** | FLAG_SECURE — blockiert Screenshots |

### Funktionen

| Funktion | Kostenlos | Pro |
|---|---|---|
| Unbegrenzte verschlüsselte Notizen | ✅ | ✅ |
| Volltextsuche | ✅ | ✅ |
| Biometrische / PIN-Sperre | ✅ | ✅ |
| Markdown-Editor | ✅ | ✅ |
| Ordner und Tags | 🔒 | ✅ |
| Erinnerungen | 🔒 | ✅ |
| Verschlüsseltes Cloud-Backup | 🔒 | ✅ |
| Eigene Themen (10 Pro-Themen) | 🔒 | ✅ |
| AI-Umschreibung | 🔒 | ✅ |
| Sicheres Vernichten | 🔒 | ✅ |
| Homescreen-Widget | 🔒 | ✅ |
| PDF-Export | 🔒 | ✅ |

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

**Nota** — رازداری کو ترجیح دینے والا انکرپٹڈ نوٹس ایپ۔ ہر نوٹ اسٹوریج میں جانے سے پہلے ڈیوائس پر انکرپٹ ہوتا ہے — ہم آپ کے نوٹس نہیں پڑھ سکتے، کوئی نہیں پڑھ سکتا۔

### سیکیورٹی پہلے

| پرت | تحفظ |
|---|---|
| **ڈیٹابیس** | SQLCipher — AES-256 مکمل انکرپشن |
| **کلید اسٹوریج** | Android Keystore — ہارڈویئر سپورٹڈ |
| **تصدیق** | بایو میٹرک (فنگر پرنٹ / چہرہ) + محفوظ PIN |
| **نیٹ ورک** | صفر ٹیلی میٹری۔ صفر تجزیات۔ صفر ٹریکر۔ |
| **آف لائن** | بنیادی ایپ 100% آف لائن کام کرتی ہے — بغیر اکاؤنٹ |
| **کلاؤڈ بیک اپ** | اختیاری، اینڈ ٹو اینڈ انکرپٹڈ آپ کے پاس فریز کے ساتھ |
| **محفوظ شیڈ** | اوور رائٹ + کرپٹو شیڈ — حذف شدہ نوٹس ناقابل بحالی |
| **روٹ کی پہچان** | روٹ شدہ ڈیوائسز پر وارننگ |
| **میموری** | حساس ڈیٹا RAM سے صاف |
| **اسکرین شاٹ** | FLAG_SECURE — اسکرین شاٹ روکتا ہے |

### خصوصیات

| خصوصیت | مفت | Pro |
|---|---|---|
| لامحدود انکرپٹڈ نوٹس | ✅ | ✅ |
| مکمل متن کی تلاش | ✅ | ✅ |
| بایو میٹرک / PIN لاک | ✅ | ✅ |
| Markdown ایڈیٹر | ✅ | ✅ |
| فولڈر اور ٹیگ | 🔒 | ✅ |
| ریمائنڈر | 🔒 | ✅ |
| انکرپٹڈ کلاؤڈ بیک اپ | 🔒 | ✅ |
| حسب ضرورت تھیم (10 Pro تھیم) | 🔒 | ✅ |
| AI دوبارہ تحریر | 🔒 | ✅ |
| محفوظ شیڈ | 🔒 | ✅ |
| ہوم اسکرین ویجیٹ | 🔒 | ✅ |
| PDF ایکسپورٹ | 🔒 | ✅ |

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

**Nota** — aplikacja do notatek z szyfrowaniem i priorytetem prywatności. Każda notatka jest szyfrowana na urządzeniu przed zapisem — nie możemy przeczytać twoich notatek, nikt nie może.

### Bezpieczeństwo przede wszystkim

| Warstwa | Ochrona |
|---|---|
| **Baza danych** | SQLCipher — pełne szyfrowanie AES-256 |
| **Magazyn kluczy** | Android Keystore — sprzętowe wsparcie |
| **Uwierzytelnianie** | Biometryczne (odcisk / twarz) + bezpieczny PIN |
| **Sieć** | Zero telemetrii. Zero analityki. Zero trackerów. |
| **Offline** | Rdzeń działa 100% offline — bez konta |
| **Kopia zapasowa w chmurze** | Opcjonalna, szyfrowana end-to-end z twoją frazą |
| **Bezpieczne niszczenie** | Nadpisanie + crypto-shred — usunięte notatki nieodwracalne |
| **Wykrywanie roota** | Ostrzeżenie na urządzeniach z rootem |
| **Pamięć** | Czyszczenie wrażliwych danych z RAM |
| **Zrzuty ekranu** | FLAG_SECURE — blokuje zrzuty |

### Funkcje

| Funkcja | Darmowe | Pro |
|---|---|---|
| Nieograniczone szyfrowane notatki | ✅ | ✅ |
| Wyszukiwanie pełnotekstowe | ✅ | ✅ |
| Blokada biometryczna / PIN | ✅ | ✅ |
| Edytor Markdown | ✅ | ✅ |
| Foldery i tagi | 🔒 | ✅ |
| Przypomnienia | 🔒 | ✅ |
| Szyfrowana kopia zapasowa | 🔒 | ✅ |
| Motywy niestandardowe (10 Pro) | 🔒 | ✅ |
| Przepisywanie AI | 🔒 | ✅ |
| Bezpieczne niszczenie | 🔒 | ✅ |
| Widget ekranu głównego | 🔒 | ✅ |
| Eksport PDF | 🔒 | ✅ |

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

**Nota** — зашифрована нотатки з пріоритетом конфіденційності. Кожна нотатка шифрується на пристрої перед збереженням — ми не можемо прочитати ваші нотатки, ніхто не може.

### Безпека перш за все

| Шар | Захист |
|---|---|
| **База даних** | SQLCipher — повне шифрування AES-256 |
| **Сховище ключів** | Android Keystore — апаратна підтримка |
| **Автентифікація** | Біометричний (відбиток / обличчя) + безпечний PIN |
| **Мережа** | Нуль телеметрії. Нуль аналітики. Нуль трекерів. |
| **Офлайн** | Ядро працює 100% офлайн — без облікового запису |
| **Хмарне резервне копіювання** | Опціонально, наскрізне шифрування з вашою фразою |
| **Безпечне знищення** | Перезапис + крипто-шред — видалене невідновне |
| **Виявлення root** | Попередження на пристроях з root |
| **Пам'ять** | Очистка чутливих даних з RAM |
| **Скріншоти** | FLAG_SECURE — блокує скріншоти |

### Функції

| Функція | Безкоштовно | Pro |
|---|---|---|
| Необмежені зашифровані нотатки | ✅ | ✅ |
| Повнотекстовий пошук | ✅ | ✅ |
| Біометричний / PIN-код | ✅ | ✅ |
| Markdown-редактор | ✅ | ✅ |
| Папки та теги | 🔒 | ✅ |
| Нагадування | 🔒 | ✅ |
| Шифроване резервне копіювання | 🔒 | ✅ |
| Теми (10 Pro-тем) | 🔒 | ✅ |
| AI-переписування | 🔒 | ✅ |
| Безпечне знищення | 🔒 | ✅ |
| Віджет головного екрана | 🔒 | ✅ |
| Експорт PDF | 🔒 | ✅ |

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

**Nota** — aplicativo de notas criptografadas com prioridade à privacidade. Cada nota é criptografada no dispositivo antes do armazenamento — não podemos ler suas notas, ninguém pode.

### Segurança primeiro

| Camada | Proteção |
|---|---|
| **Banco de dados** | SQLCipher — criptografia completa AES-256 |
| **Armazenamento de chaves** | Android Keystore — suporte hardware |
| **Autenticação** | Biométrico (impressão digital / rosto) + PIN seguro |
| **Rede** | Zero telemetria. Zero análises. Zero rastreadores. |
| **Offline** | O núcleo funciona 100% offline — sem conta |
| **Backup na nuvem** | Opcional, criptografado de ponta a ponta com sua frase |
| **Destruição segura** | Sobrescrever + crypto-shred — notas excluídas irrecuperáveis |
| **Detecção de root** | Aviso em dispositivos com root |
| **Memória** | Limpar dados sensíveis da RAM |
| **Capturas de tela** | FLAG_SECURE — bloqueia capturas |

### Recursos

| Recurso | Grátis | Pro |
|---|---|---|
| Notas criptografadas ilimitadas | ✅ | ✅ |
| Pesquisa de texto completo | ✅ | ✅ |
| Bloqueio biométrico / PIN | ✅ | ✅ |
| Editor Markdown | ✅ | ✅ |
| Pastas e tags | 🔒 | ✅ |
| Lembretes | 🔒 | ✅ |
| Backup criptografado | 🔒 | ✅ |
| Temas personalizados (10 Pro) | 🔒 | ✅ |
| Reescrita com IA | 🔒 | ✅ |
| Destruição segura | 🔒 | ✅ |
| Widget da tela inicial | 🔒 | ✅ |
| Exportar PDF | 🔒 | ✅ |

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

**Nota** — een privacy-first versleutelde notitie-app. Elke notitie wordt op het apparaat versleuteld voordat deze wordt opgeslagen — wij kunnen je notities niet lezen, niemand kan dat.

### Veiligheid eerst

| Laag | Bescherming |
|---|---|
| **Database** | SQLCipher — AES-256 volledige versleuteling |
| **Sleutelopslag** | Android Keystore — hardware-ondersteund |
| **Authenticatie** | Biometrisch (vingerafdruk / gezicht) + veilige PIN |
| **Netwerk** | Nul telemetrie. Nul analytics. Nul trackers. |
| **Offline** | Kern werkt 100% offline — geen account |
| **Cloud-back-up** | Optioneel, end-to-end versleuteld met je wachtwoordzin |
| **Veilig vernietigen** | Overschrijven + crypto-shred — verwijderde notities onherstelbaar |
| **Root-detectie** | Waarschuwing op gerootte apparaten |
| **Geheugen** | Gevoelige gegevens uit RAM wissen |
| **Schermafbeeldingen** | FLAG_SECURE — blokkeert schermafbeeldingen |

### Functies

| Functie | Gratis | Pro |
|---|---|---|
| Onbeperkte versleutelde notities | ✅ | ✅ |
| Volledige tekst zoeken | ✅ | ✅ |
| Biometrische / PIN-vergrendeling | ✅ | ✅ |
| Markdown-editor | ✅ | ✅ |
| Mappen en tags | 🔒 | ✅ |
| Herinneringen | 🔒 | ✅ |
| Versleutelde cloud-back-up | 🔒 | ✅ |
| Aangepaste thema's (10 Pro) | 🔒 | ✅ |
| AI-herschrijving | 🔒 | ✅ |
| Veilig vernietigen | 🔒 | ✅ |
| Startscherm-widget | 🔒 | ✅ |
| PDF-export | 🔒 | ✅ |

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
