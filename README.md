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

---

## Download

<p align="center">
  <a href="https://github.com/maegy2011/Nota/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-blue?style=for-the-badge&logo=android" alt="Download APK"/>
  </a>
</p>

**Direct:** [Nota-latest.apk](https://github.com/maegy2011/Nota/releases/latest)

---

## Features

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
| Secure shred (permanent delete) | 🔒 | ✅ |
| Home screen widget | 🔒 | ✅ |
| PDF export | 🔒 | ✅ |

---

## Screenshots

<p align="center">
  <em>Screenshots coming soon</em>
</p>

---

## Security

- **SQLCipher** — AES-256 encrypted local database
- **Biometric auth** — fingerprint / face unlock
- **PIN fallback** — secure PIN with hashing
- **No data leaves your device** — fully offline by default
- **Encrypted cloud backup** — optional, password-protected
- **Secure shred** — overwrite and permanently delete sensitive notes
- **Root detection** — warns on rooted devices

---

## Tech Stack

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

---

## Building

```bash
git clone https://github.com/maegy2011/Nota.git
cd Nota
./gradlew assembleDebug
```

**Requirements:** JDK 17+, Android SDK 35

---

## Contributing

Contributions are welcome! Please read our [contributing guidelines](CONTRIBUTING.md) first.

---

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  <sub>Built with ❤️ by <a href="https://github.com/maegy2011">maegy2011</a></sub>
</p>
