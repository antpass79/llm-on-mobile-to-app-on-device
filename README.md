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

# Azure OpenAI credentials
LLM_URL=https://<your-resource>.openai.azure.com
LLM_KEY=<your-azure-openai-key>
LLM_MODEL=<deployment-name>
```

`WS_URL` is injected at build time via `BuildConfig.WS_URL` (see `mobile-app/app/build.gradle.kts`).  
The default fallback when the property is absent is `ws://10.0.2.2:8080`.

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
