# Mobile App (Android)

Android app to control image parameters over WebSocket and through voice + LLM.

## Features

- Connects to the same WebSocket protocol used by the web app:
  - Message shape: `{ type: "update" | "sync", payload: { gain?, depth?, zoom? } }`
- Manual parameter control with sliders for gain/depth/zoom
- Microphone capture using Android speech recognition
- LLM command parsing (Azure OpenAI) from voice transcript to parameter updates
- Centralized runtime configuration via `BuildConfig` through `AppConfig`

## Configuration

1. Copy `local.properties.example` to `local.properties`.
2. Set the following values:
   - `WS_URL`
   - `LLM_URL`
   - `LLM_KEY`
   - `LLM_MODEL`
3. Keep `local.properties` out of source control (already ignored).

## Run

- Open `mobile-app` with Android Studio.
- Let Gradle sync.
- Run on emulator/device (Android 9+, API 28+).

## Notes

- If you run the WebSocket server on your development machine, `10.0.2.2` maps host localhost from Android emulator.
- The app requires microphone permission for voice commands.
- LLM output is constrained to JSON parameters and then sent through the same websocket update pipeline.
