# Nota — Comprehensive Code Review Report

**Date:** 2026-11-06
**Scope:** Full source tree (`app/src/main`, security, data, UI, billing, reminder, geo)
**Reviewer:** code-reviewer skill (with android-ux/dev/testing/retrofit/debugging/data-layer/gradle-logic/source-search skills loaded)

---

## 1. Summary

Nota is a mature, security-conscious offline-first notes app: SQLCipher DB keyed by Keystore-wrapped passphrase, per-file media keys, biometric-bound challenge cipher, PBKDF2 PIN, account-scoped queries, soft-delete trash, encrypted backups, Drive sync, and a Pro gating layer. Overall architecture is sound and the codebase shows strong defensive habits (mutexes, runCatching boundaries, account scoping). The findings below are mostly robustness/performance/consistency gaps rather than correctness bugs.

**Verdict:** Request Changes — 1 critical, 5 major, several minor.

---

## 2. Critical issues (must fix before merge)

### C1 — Recovery salt not backed up (lockout risk)
**File:** `security/PassphraseStore.kt` → `getOrCreateRecSalt()`
**Problem:** Every other secret (DB passphrase, PIN salt) is persisted to BOTH `secureStore` and `secureStoreBak` for self-healing. The recovery salt is persisted only to `secureStore`. If the primary DataStore file is lost/corrupted, the recovery hash (PBKDF2) can no longer be verified → user permanently locked out of recovery even with the correct 12-word phrase.
**Fix:**
```kotlin
ctx.secureStore.edit { it[K_REC_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP) }
ctx.secureStoreBak.edit { it[K_REC_SALT] = Base64.encodeToString(salt, Base64.NO_WRAP) }  // add
```
And read with fallback like `readBackupPassphrase`.

---

## 3. Major issues (should fix)

### M1 — Per-file media key sidecar parse fragility
**File:** `security/PerFileMedia.kt` → `parseKeyFile()`
**Problem:** `String(keyBytes).split("\n")` requires exactly 3 lines. A trailing newline (common from editors/OS writes) yields 4 elements → `lines.size != 3` → returns null → file becomes permanently unreadable (crypto-shred-like data loss) even though key is intact.
**Fix:** `val lines = String(keyBytes, Charsets.UTF_8).trimEnd('\n').split("\n")` (and similarly in `decryptWithKey`).

### M2 — Search is O(n) full-table scan
**File:** `data/NoteDao.kt` (all `search*` queries)
**Problem:** `LIKE '%' || :q || '%'` cannot use any index → full scan of notes + correlated subquery for tags on every keystroke (debounced 200ms but still). On large note sets this janks the list.
**Fix:** Add an FTS4 virtual table (`notes_fts`) populated via triggers, or at minimum add indexes on `title`/`body` won't help with leading wildcard. Recommended: Room FTS4 entity + `@Fts4` table; query `MATCH`. Also consider limiting result count.

### M3 — VACUUM called per-note in batch deletes
**File:** `data/NotesRepository.kt` → `hardDelete()`, `deleteManyPermanently()`, `emptyTrash()`
**Problem:** `hardDelete` runs `VACUUM` after every single note; `deleteManyPermanently` calls `hardDelete` for each id then VACUUM again. Emptying 50 trashed notes = 51 VACUUMs on the encrypted DB → seconds of I/O on main-thread-adjacent path.
**Fix:** Remove VACUUM from `hardDelete`; keep a single VACUUM at the end of `deleteManyPermanently`/`emptyTrash`/`purgeTrash`.

### M4 — Biometric status/authenticator mismatch
**File:** `security/AuthManager.kt` → `biometricStatus()` vs `canBiometric()`
**Problem:** `canBiometric()` checks `BIOMETRIC_STRONG` only; `biometricStatus()` checks `STRONG or WEAK`. So a device with only weak biometric enrolled reports `BIOMETRIC_SUCCESS` from status but `BIOMETRIC_ERROR_NO_HARDWARE/...` from canBiometric — inconsistent UI gating.
**Fix:** Use the same authenticator set in both (`BIOMETRIC_STRONG`), or document the intentional difference.

### M5 — Drive upload verification doubles bandwidth
**File:** `data/DriveBackup.kt` → `upload()` (verify step)
**Problem:** After upload, downloads the whole file back to compare SHA-256. For 100MB backups this is a second full transfer. Acceptable for integrity but costly on mobile data.
**Fix (optional):** Use Drive's `md5Checksum`/`sha256Checksum` field in the upload response metadata, or skip verification in favor of Drive's own integrity. Keep current behavior as default if offline verification is required.

---

## 4. Minor issues (nice to have)

- **m1** `AuthManager.deviceCredential` calls `ctx.getString(ctx.resources.getIdentifier("bio_sub", ...))` — reflection-style resource lookup; fragile under R8 (resource names may be shrunk). Use `R.string.bio_sub` directly.
- **m2** `PassphraseStore.getOrCreatePinSalt` persists only to primary store (no backup) — same class of issue as C1 but lower impact (PIN reset possible via recovery). Consider backing up.
- **m3** `PerFileMedia.writeNew` writes key file BEFORE data file. If process dies between, orphan key file remains (harmless). If data write fails, key file orphan. Acceptable.
- **m4** `NotesRepository.createTag` inserts a tag with no note link; `deleteOrphans()` (called from `setTags`) would delete it. Standalone tags may vanish after any note save. Decide intended lifecycle.
- **m5** `SecureDelete.random` is a single shared `SecureRandom` — thread-safe, fine. `maskText` uses it; okay.
- **m6** `AiRewriteService.complete` does not close `errorStream` on the 200 path? It disconnects; stream may linger. Minor leak. Close in finally.
- **m7** `SettingsStore.dumpAll` includes `ai_key_sealed` (filtered by `sanitizeSettings` before export — verified used). Good.
- **m8** `KeyManager.getBioChallengeCipher` uses `PURPOSE_ENCRYPT` only — cannot decrypt; used only to build CryptoObject. Correct as documented.
- **m9** `DbHolder.get()` catch block calls `quarantine()` then reopens with same passphrase logic; if reopen fails, exception propagates. Caller (ViewModel) must handle. Verify callers wrap in runCatching — most do.

---

## 5. Security review (OWASP baseline)

| Check | Status |
|---|---|
| SQL injection | ✅ Room parameterized queries throughout |
| Hardcoded secrets | ✅ None in source; signing via env/file |
| Token storage | ✅ TokenVault AES-256-GCM with Keystore key |
| DB encryption | ✅ SQLCipher with Keystore-derived passphrase |
| Biometric binding | ✅ CryptoObject + invalidation on enrollment |
| Account scoping | ✅ ownerAccountId filters on every query |
| Screenshot privacy | ✅ FLAG_SECURE default, user opt-in |
| Clipboard clear on lock | ✅ best-effort |
| HTTPS enforcement | ✅ AiRewriteService + SettingsStore.setAiEndpoint require https |
| Backup encryption | ✅ per-file + cloud wrap keys |
| Sensitive keys in GDPR export | ✅ filtered via sanitizeSettings |
| PIN brute-force protection | ✅ escalating lockout persisted in DataStore |
| R8 keeps | ⚠️ documented risk (Hilt injector) — verify on device |

---

## 6. Performance review

- N+1: `backupNotes()` calls `tagDao.tagsOf(n.id)` per note inside `map` → N+1 tag queries. For large note sets (backup), batch tags into a single `allMappings()` join. **Major perf.**
- `sortNotes` filters pinned/rest with two passes + sort — O(n log n), fine.
- `tagsMap` Flow groups rows in memory — fine for hundreds.
- `minuteTick` scoped to cards with reminders (recent commit) — good.
- Compose recomposition: `minuteTick` was a global tick; now scoped. Good.
- `VACUUM` per delete — see M3.

---

## 7. Test coverage

- Unit tests exist for NotesViewModel gating, PinHash, RecoveryKey, AuthManager, SettingsStore, BackupCodec, etc.
- Missing: integration tests for DbHolder open/quarantine, PerFileMedia round-trip (JVM-testable per comments), DriveBackup mutex.
- KMP N/A (Android-only).

---

## 8. Positive feedback

- Excellent account-scoping discipline (`ownerAccountId IS NULL OR = :acct`) everywhere.
- Defensive `runCatching` at boundaries with clear fallbacks.
- Crypto-shred design (per-file keys + sidecar) is well-reasoned and documented.
- `flushPendingSave` hook to avoid losing last 300ms on lock — thoughtful.
- `isAutoLockExpired` extracted as pure function for JVM testing — good testability.
- Migration strategy (17 migrations, all additive) preserves data.

---

## 9. Recommended priority order

1. C1 (recovery salt backup) — 5 min fix, high impact.
2. M3 (VACUUM batching) — perf, easy.
3. M1 (trailing newline) — data-loss prevention.
4. M4 (biometric consistency) — security correctness.
5. N+1 in backupNotes — perf on large data.
6. M2 (FTS) — larger effort, schedule for v1.27.
