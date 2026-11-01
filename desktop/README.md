# CHATENDER AI Desktop Workspace

This folder contains a standalone, responsive desktop-first interface preview. It is separate from the native Android app.

## Run on Windows

1. Download or clone this repository branch: `chatender/android-core-v1`.
2. Open the repository folder.
3. Open `desktop/index.html` in Chrome, Edge, or Firefox.

No Node.js installation or build step is required for this UI preview.

## Current capabilities

- Desktop workspace with navigation and system overview.
- Responsive layout for smaller screens.
- General, coding, diagnostics and memory role selection.
- Local preview replies for basic interface testing.
- Browser-local preferences for model names and a non-secret voice profile ID.

## Not connected yet

This is a front-end preview, not a finished desktop AI agent. It does not yet connect to OpenRouter, run code, inspect files, or execute operating-system commands. A secure local backend is required before provider keys or powerful tools are connected. Do not place API keys in HTML, JavaScript, browser storage, or Git.

The Android app remains the separate native Kotlin application under `app/`.
