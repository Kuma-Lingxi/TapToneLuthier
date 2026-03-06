# TapTone Luthier (Android) — Tap Tone Frequency Analyzer

A mobile tool for luthiers: record a tap on a guitar top/brace, estimate the dominant resonance frequency, and track changes over iterative carving.

This repository is a portfolio/demo project focused on **React Native (Android)** + a small **Kotlin native module** to capture **PCM 16-bit mono** audio from the phone microphone, then run **FFT-based** frequency estimation in JavaScript.

---

## What this app does (MVP)

- Record a short tap sample (1–2 seconds) using the phone microphone
- Automatically detect the tap segment (simple energy threshold)
- Estimate the dominant frequency (FFT + peak detection)
- Display:
  - dominant frequency (Hz)
  - confidence score (peak-to-total energy ratio)
- Save measurements with notes (wood, body size, brace stage) and compare history

---

## Why this project

Brace tuning is often done by tapping and listening, which is subjective and hard to reproduce.  
This app provides a quick, repeatable way to **quantify** tap responses and **track trends** over time.

---

## Tech stack

- **React Native (Android)**
- **Kotlin Native Module** (Android `AudioRecord`) for raw audio capture
- **JavaScript DSP**:
  - tap detection (RMS/energy threshold)
  - Hann window
  - FFT
  - peak picking + confidence estimation
- Storage: local persistence for measurement history (MVP)

---

## Architecture (high level)

1. **Record PCM** audio (mono, 16-bit) from microphone via Kotlin module  
2. Convert PCM to float samples in JS  
3. Detect tap region and slice a short window  
4. Apply windowing and FFT  
5. Extract dominant frequency + confidence  
6. Render results and store measurement metadata

---

## Getting started (Android)

### Prerequisites

- Node.js (LTS)
- Android Studio + Android SDK
- JDK 17 (or the version required by the RN template you use)
- A physical Android phone (recommended for microphone testing)

### Install & run

```bash
cd app
npm install
npx react-native run-android
```
Tip: If you use Yarn:
```bash
yarn && npx react-native run-android
```

---

## Usage
1. Open the app on your Android device
2. Tap Record
3. Tap the guitar top/brace once (a short, clean tap)
4. The app displays the dominant frequency (Hz) and confidence
5. Add notes (optional) and Save
6. Repeat after carving to compare changes over time

---

## Notes on accuracy & limitations
 - Results depend on room noise, phone microphone characteristics, and tap consistency.
 - The goal is repeatability and trend tracking, not lab-grade calibration.
 - For best results:
    - record in a quiet room
    - keep distance and tap force consistent
    - avoid handling noise (touching the phone during recording)

---

## Roadmap
### Phase 1 — Phone microphone MVP (current)
- [ ] Native PCM capture module (AudioRecord)
- [ ] Tap segment detection
- [ ] FFT + dominant frequency estimation
- [ ] Measurement history + comparison

### Phase 2 — TapHammer (ESP32) integration (next)
- [ ] BLE input source: receive frequency results from an ESP32 hammer with IMU
- [ ] Unified measurement pipeline (mic or BLE)
- [ ] Improved stability indicators

---

## Contributing
This is a personal demo/portfolio project. Suggestions and pull requests are welcome.

---

## License
MIT License — see [LICENSE](./LICENSE).

