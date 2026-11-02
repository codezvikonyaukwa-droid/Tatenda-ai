# CHATENDER AI Desktop Workspace

A desktop-first workspace, separate from the native Android app.

## Run on Windows 10

1. Download the repository branch `chatender/android-core-v1` from GitHub and extract it.
2. Install Python 3.8 or newer if it is not already installed. During setup, enable **Add Python to PATH**.
3. Open the extracted repository folder in File Explorer.
4. Open the `desktop` folder.
5. Click the address bar, type `cmd` and press Enter.
6. Run:

   ```bat
   py server.py
   ```

   If `py` is not recognized, try `python server.py`.
7. Enter your OpenRouter API key in the terminal prompt. The characters are hidden while typing. The key is held only in the running Python process and is not saved to a file or browser storage.
8. Open `http://127.0.0.1:8765` in Chrome or Edge.
9. To stop the local bridge, return to the terminal and press `Ctrl+C`.

Keep the terminal open while using the desktop interface. The bridge listens only on `127.0.0.1` (this computer), not on your Wi-Fi network.

## Current capabilities

- Desktop workspace with navigation and system overview.
- Responsive layout for smaller screens.
- General, coding, diagnostics and memory role selection.
- Per-role OpenRouter model preferences.
- Live AI chat through a local Python bridge.
- Provider key entered in a hidden terminal prompt, not in browser code or browser storage.
- Browser-local preferences for model names and a non-secret voice profile ID.

## Security and limits

- Do not paste API keys into the chat box, HTML, JavaScript, GitHub, or screenshots.
- The bridge sends requests to OpenRouter over HTTPS. Provider usage limits and any charges are controlled by the provider and selected model.
- The bridge does not run shell commands, execute generated code, inspect private files, or control Windows. Those capabilities require separate, explicit, permission-controlled modules.
- This is a local desktop app preview, not an installer. It requires Python and an internet connection for cloud AI.
- The voice ID is stored as a preference only. Cloud voice synthesis is not connected.

The Android app remains the separate native Kotlin application under `app/`.
