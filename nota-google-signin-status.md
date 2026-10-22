# Nota Google Sign-In — Status Report
Generated: 2026-10-22 (investigation)

## 1. Failing endpoint + status code

- **Primary failing endpoint (Google sign-in registration):**
  - `POST https://mohamedadel.cfd/nota-api/v1/users/register`
  - Returns **401 UNAUTHENTICATED** (dominant) in production logs.
  - Body contract (server `registerSchema`): `{"accountId": "<firebaseUid>"}` (strict; unknown fields rejected).
  - Auth: `Authorization: Bearer <Firebase ID token>` (RS256, verified server-side against Google certs).
  - Binding check: verified token `sub` must equal `accountId`, else **403 FORBIDDEN** ("Token does not own this account.").
- **Secondary failing endpoint (post sign-in entitlements):**
  - `GET https://mohamedadel.cfd/nota-api/v1/entitlements?accountId=<uid>`
  - Also 401 in logs; 200 only after a valid/fresh token.
- **Cloud endpoints also 401** (`/v1/cloud/quota`, `/v1/cloud/pointers/backup_latest`, `/v1/cloud/blobs/*`) — same root cause (invalid/expired Firebase token or unconfigured project id).
- Nginx has **no `/nota-api` location** in `/etc/nginx/sites-enabled/barakat`; traffic reaches `nota-server` on `127.0.0.1:3111` (cloudflared → nginx :8080 → …). The API path is served by the node process directly when invoked (logs show `/users/register` etc. on port 3111). Note: the deployed nginx config does not proxy `/nota-api`, so external `/nota-api/*` requests currently fall through to the SPA `location /` (try_files → index.html). This is a routing gap to verify alongside the auth failure.

## 2. Client request body (Android)

Source: `projects/nota/app/src/main/java/com/nota/notes/billing/ServerRegister.kt`

```json
POST https://mohamedadel.cfd/nota-api/v1/users/register
Content-Type: application/json
Authorization: Bearer <Firebase ID token>

{"accountId": "<firebaseUid>"}
```

- `accountId` is the Firebase UID (`authResult.user.uid`), obtained in `GoogleAuthManager.completeSignIn` after `FirebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))`.
- Token is a **fresh Firebase ID token** fetched best-effort via `user.getIdToken(false)` and cached in `AccountStore`; `IdTokenProvider` refreshes on 401 (`getIdToken(true)`).
- `ServerRegister.ensureRegistered` retries once on 401 (token refresh) before giving up; failure marks `pendingRegister=true` and keeps local sign-in (fail-open locally, server registration deferred).
- Other clients using the same base (`NotaCloudApi`, `HttpEntitlementsApi`): `CLOUD_BASE`/`ENTITLEMENTS_BASE = "https://mohamedadel.cfd/nota-api"`.

## 3. Server logs around `auth_reject`

Source: `~/.pm2/logs/nota-entitlements-*.log` (rotated). Total `auth_reject` entries: 41.

Counts by reason:
- `invalid_token`: 40
- `missing_bearer`: 1 (on `/entitlements`)

Counts by detail:
- `FIREBASE_PROJECT_ID not configured` (truncated): 24
- `expired`: 15
- `Unexpected token '�', "��-…` (binary/non-JWT body): 1

Counts by path:
- `/users/register`: 14
- `/entitlements`: 11
- `/cloud/quota`: 8
- `/cloud/pointers/backup_latest`: 8

Representative log lines:
```json
{"level":"warn","msg":"auth_reject","reason":"invalid_token","detail":"FIREBASE_PROJECT_ID not configur","path":"/users/register"}
{"level":"warn","msg":"auth_reject","reason":"invalid_token","detail":"expired","path":"/users/register"}
{"level":"warn","msg":"auth_reject","reason":"missing_bearer","path":"/entitlements"}
{"level":"warn","msg":"auth_reject","reason":"invalid_token","detail":"Unexpected token '�', \"��-\" is n","path":"/entitlements"}
```

Access-log sample (status codes) from `nota-entitlements-out__2026-09-28`:
```
POST /users/register  401  (repeated)
POST /users/register  200  (after token refresh / valid token)
GET  /entitlements    401 → 200  (same session, after refresh)
```

Root causes evidenced:
1. **Unconfigured `FIREBASE_PROJECT_ID`** on an earlier server instance (dotenv injected only 1 var instead of 5 on 2026-09-28) → `PROJECT_ID === ""` → `verifyIdToken` throws "FIREBASE_PROJECT_ID not configured" → all tokens rejected with 401. Fixed later (dotenv injects 5 vars), but stale clients still hit 401.
2. **Expired Firebase ID tokens** (default 1 h lifetime) sent by clients whose `IdTokenProvider` did not refresh before the call, or where `getIdToken(false)` returned a stale cached token.
3. **Malformed/binary token** sent once (client bug or proxy corruption) → JSON parse failure in `verifyIdToken`.
4. **Missing bearer** on one `/entitlements` call (client sent no Authorization header).

## 4. Firebase project / client IDs

From `projects/nota/app/google-services.json` and server `.env` / `ecosystem.config.js`:

- **Firebase project ID:** `nota-6d145` (matches server `FIREBASE_PROJECT_ID=nota-6d145`).
- **Project number:** `650187681542`.
- **Storage bucket:** `nota-6d145.firebasestorage.app`.
- **Android mobilesdk app id:** `1:650187681542:android:4760cc5694b4ed1486b887`.
- **Android (type 1) OAuth client id:** `650187681542-75dfl66nu58ugdnqs5fssae1kjji8is0.apps.googleusercontent.com` (package `com.nota.notes` / `com.nota.notes.mock`, cert hash `1a1635854b16a940e230e833e73556559d8e7aca`).
- **Web (type 3) OAuth client id** (used as `default_web_client_id` for `requestIdToken`): `650187681542-2reih0ulb09k47hof7iqs2b3v8bc8ifm.apps.googleusercontent.com`.
- **Firebase API key:** `AIzaSyD8f1uCckY14kKTi6ffS-ufkDL5484MFX0`.
- **Server issuer/audience expected:** `https://securetoken.google.com/nota-6d145` / audience `nota-6d145`.

Project/client IDs match between client and server (no mismatch detected). The Firebase UID issued by Google is used as both `accountId` and `google_sub`.

## 5. DB `users` row state

Database: `nota_entitlements` (Postgres, `localhost:5432`), table `users`.

Schema: `id, account_id (unique), created_at, google_sub (nullable), trial_used, trial_started_at`.

Rows (1 total):
| id | account_id | created_at | google_sub | trial_used | trial_started_at |
|----|------------|------------|------------|------------|------------------|
| 2 | NG9kSGXZdgQVaoB2xIP9KGksEO22 | 2026-09-20 21:31:00+03 | NG9kSGXZdgQVaoB2xIP9KGksEO22 | false | (null) |

- Only the **beta trial account** is present; its `google_sub` equals its own `account_id` (self-seeded, not a real Google UID).
- **No real Google user has ever successfully registered** (`/users/register` never returned 201 for a Google UID; no row with a distinct Firebase UID exists).
- `entitlements`: 1 row (trial account, features `{folders}`, expires 2027-09-20).
- `cloud_files`: 2 rows; `cloud_pointers`: 1 row (trial account only).

## 6. Conclusion / next steps

- Google sign-in client flow (GoogleSignIn → Firebase credential → UID) is structurally correct and IDs match.
- The server-side verification path (`requireAuth` → `verifyIdToken`) is the blocker: tokens rejected due to (a) historically missing `FIREBASE_PROJECT_ID`, (b) expired tokens not refreshed by clients, (c) occasional malformed tokens.
- Server is currently **stopped** (no process on port 3111; pm2 app not running); last start 2026-10-09. Restart required before any client can register.
- Recommended fixes:
  1. Ensure `FIREBASE_PROJECT_ID=nota-6d145` is present in the runtime env (ecosystem.config.js already sets it; verify `.env` has all 5 vars) before starting.
  2. Make `IdTokenProvider.current()` proactively refresh when token is near expiry (decode `exp`, refresh if < 5 min), and have `ServerRegister`/`HttpEntitlementsApi`/`NotaCloudApi` always call `refresh()` on 401 (already partially done).
  3. Add `/nota-api` → `127.0.0.1:3111` proxy in nginx (or update client base URL) so external clients actually reach nota-server; currently `/nota-api/*` falls through to the SPA.
  4. Re-test registration end-to-end once server is up: expect `POST /v1/users/register` → 201 with `{"accountId": "<uid>", "created": true}` and a new `users` row with `google_sub = <firebase uid>`.

No APK build or deploy performed (per standing rule). Report only.
