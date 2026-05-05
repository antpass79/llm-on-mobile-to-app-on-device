# llm-on-mobile-to-app-on-device
Use the power of LLM on a smartphone to control a disconnected app.

---

## Running the apps

### Web app

```bash
cd web-app
npm install       # first time only
npm run dev
```

The Vite dev server starts at **http://localhost:5173**.  
A WebSocket relay server starts automatically alongside it.

**WebSocket configuration** — `web-app/vite-ws-plugin.ts`:

| Constant | Default | Description |
|---|---|---|
| `WS_PORT` | `8080` | Port the WebSocket server listens on |

The server binds to all interfaces. On startup it prints both the `localhost` and LAN addresses, e.g.:

```
⚡ WebSocket server:  ws://localhost:8080
   (LAN clients use)  ws://192.168.x.x:8080
```

---

### Mobile app (Android)

#### Prerequisites
- Android Studio with the **pixel34** (or any API 28+) AVD, or a physical device
- `JAVA_HOME` pointing to the Android Studio JBR (e.g. `C:\Program Files\Android\Android Studio\jbr`)

#### Configuration

Copy `mobile-app/local.properties.example` to `mobile-app/local.properties` and fill in the values:

```properties
sdk.dir=C:\Users\<user>\AppData\Local\Android\Sdk

# WebSocket endpoint — must be reachable from the device
# Use 10.0.2.2 for the Android emulator (maps to host loopback)
WS_URL=ws://10.0.2.2:8080

# Google Gemini API key
GEMINI_KEY=<your-gemini-api-key>
```

##### Getting a Gemini API key

1. Go to **[aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)** (free Google account required)
2. Click **Create API key** and select or create a project
3. Copy the generated key (`AIza...`)
4. Paste it as `GEMINI_KEY=AIza...` in `local.properties`

The key is injected at build time via `BuildConfig.GEMINI_KEY` (see `mobile-app/app/build.gradle.kts`) and never leaves the device — it is only used to call the Gemini REST API directly from the app.

| Scenario | `WS_URL` value |
|---|---|
| Android emulator → host machine | `ws://10.0.2.2:8080` |
| Physical device on same Wi-Fi | `ws://<host-LAN-IP>:8080` |

#### Build and run

```bash
cd mobile-app

# Install on connected device / running emulator
./gradlew installDebug

# Then launch
adb shell monkey -p com.esaote.imageparams 1
```
