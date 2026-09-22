# mlam24-m1

_Keep this README up to date with the steps required to build and run the frontend and backend (including any scripts, config files, and environment variables). TAs ill follow these instructions._

## Requirements

Install the following before the frontend or backend setup steps:

- [git](https://git-scm.com/install/)


--- 

## Frontend Setup

### Requirements

- [Android Studio](https://developer.android.com/studio) (latest version)
- [Java 17](https://adoptium.net/temurin/releases/?version=17)
- [Android SDK](https://developer.android.com/studio#command-tools) with API level 35 (Android 15)

### Setup

1. **Open project**: Open the `frontend/` directory in Android Studio
2. **Sync Gradle**: Android Studio will automatically prompt you to sync the project. Click "Sync Now". You can also manually run `cd frontend && ./gradlew build` to trigger the sync and download the necessary dependencies.
3. **Configure Android SDK**: Ensure you have Android SDK 35 installed (`compileSdk`/`targetSdk` in `gradle/libs.versions.toml`).
4. **Set up emulator/device**:
   - Create a new AVD (Android Virtual Device) by selecting Pixel 9 as the device and a system image at API level 35+ with Google Play services (required for Google sign-in).
   - Alternatively, connect a physical Android device with Google Play services and a signed-in Google account.
5. **Setup app config**: Copy the example file, then fill in local values:
   ```bash
   cp frontend/local.properties.example frontend/local.properties
   ```
   Every key `app/build.gradle.kts` reads from `local.properties`:
   - `sdk.dir`: path to your Android SDK. Android Studio usually writes this the first time you open `frontend/`. On Mac it is often `sdk.dir=/Users/<username>/Library/Android/sdk`.
   - `API_BASE_URL`: backend URL baked into the APK as `BuildConfig.API_BASE_URL`. Use `http://10.0.2.2:3000` for the emulator (`10.0.2.2` is the host machine); use `https://<SERVER_HOST>` for a release build against the deployed server.
   - `GOOGLE_CLIENT_ID`: the **Web application** OAuth client ID (not an Android client ID) — Credential Manager's `GetSignInWithGoogleOption` uses this as `serverClientId`.
   - `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`: only required for `./gradlew assembleRelease`; debug builds work without them. Keep the `.jks` keystore file itself outside the repo.


### Build and Run

- **Debug build**: Click the green play button in the toolbar, to compile the code, package a debug APK, and install it on the connected device or running emulator. Alternatively, from the project root, run `./scripts/run-frontend.sh`.
- **Release build**: with the four `RELEASE_*` keys set in `local.properties`, run `cd frontend && ./gradlew assembleRelease`. The signed APK is written to `frontend/app/build/outputs/apk/release/app-release.apk`; rename it to `M1_mlam24-m1.apk` before submitting. Install with `adb install -r M1_mlam24-m1.apk`.


### Backend Configuration

Ensure the backend server is running and update the base URL in the app configuration if needed.

---
## Backend Setup

You can run the backend in one of two ways:
* Locally via Node.js 
* Via Docker Compose

Both ways use the same `backend/.env` file (see below).

### Environment configuration

From the project root:

```bash
cp backend/.env.example backend/.env
```

Variables actually read by the M1 server (`src/config/env.ts`):
- `PORT` (optional): defaults to `3000` if unset.
- `NODE_ENV` (optional): defaults to `development`.
- `STUDENT_FIRST_NAME` / `STUDENT_LAST_NAME` (optional): returned by `GET /api/name`; default to `Matthew` / `Lam`.

`MONGODB_URI`, `GOOGLE_CLIENT_ID`, and `JWT_SECRET` are in `.env.example` for later milestones but **not used in M1** — the server has no database or auth code yet.


### Option 1: Run locally

**Requirements:** 
- [Node.js](https://nodejs.org/en/download/) 22+
- [npm](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) 10+

**Setup:** 
1. Install dependencies:

   ```bash
   cd backend
   npm install
   ```

2. **Development** (TypeScript with auto-reload):

   ```bash
   npm run dev
   ```

3. **Production build** (optional):

   ```bash
   npm run build
   npm start
   ```

### Option 2: Run with Docker Compose

**Requirements:** 
- [Docker](https://docs.docker.com/desktop/setup/install) and [Docker Compose](https://docs.docker.com/desktop/setup/install) v2.24+
- [curl](https://curl.se/download.html)

**Setup**
1. **Start** (from the project root):

   ```bash
   ./scripts/run-backend.sh
   ```

   Or run Compose directly:

   ```bash
   docker compose up --build -d
   ```

2. **Stop**:

   ```bash
   docker compose down
   ```

## API Reference (M1)

| Method & path | Response |
|---|---|
| `GET /health` | `{ "status": "ok" }` |
| `GET /api/server-ip` | `{ "ip": "203.0.113.5" }` — EC2 public IP (IMDSv2, falls back to api.ipify.org) |
| `GET /api/server-time` | `{ "time": "21:04:09 GMT+00:00" }` |
| `GET /api/name` | `{ "firstName": "...", "lastName": "..." }` |
| `GET /api/client-ip` | `{ "ip": "..." }` — echoes the caller's IP |
| `GET /ws/pixels` (WebSocket) | Pass-through relay of the upstream pixel stream (`{"x":int,"y":int,"color":"#RRGGBB"}` text frames over a 16x16 grid) |

## Deploying to EC2

1. SSH into the instance and install Node 22, git, nginx, certbot, and pm2.
2. Clone the repo, then `cd backend && cp .env.example .env` and fill in real values.
3. `npm ci && npm run build`.
4. `pm2 start dist/index.js --name m1-backend && pm2 save && pm2 startup` (follow the printed command to enable pm2 on boot).
5. Copy `backend/deploy/nginx-m1.conf` to `/etc/nginx/sites-available/m1`, replace `<SERVER_HOST>` with the real domain, symlink it into `sites-enabled`, then `sudo certbot --nginx -d <SERVER_HOST>` to add TLS.
6. Verify with `curl https://<SERVER_HOST>/api/server-time`.
7. To deploy an update later: `git pull && npm ci && npm run build && pm2 restart m1-backend`.

## Additional Setup

_Please specify any other additional setup steps non-specific to either frontend nor backend_