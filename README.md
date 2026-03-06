# TapTone Luthier (Android)

[🇫🇷 Français](#-Analyseur-de-fréquence) | [🇬🇧 English](#-Tap-Tone-Frequency-Analyzer)


# TapTone Luthier (Android) — Analyseur de fréquence “tap tone”

Un outil mobile pour les luthiers : enregistrer un tap sur une table d’harmonie / un barrage, estimer la fréquence de résonance dominante et suivre les changements au fil des ajustements (rabotage/ponçage).

Ce dépôt est un projet démo/portfolio basé sur **React Native (Android)** + un petit **module natif Kotlin** pour capturer l’audio du micro en **PCM mono 16 bits**, puis estimer la fréquence via une approche **FFT** en JavaScript.

---

## Ce que fait l’application (MVP)

- Enregistrer un court échantillon de tap (1–2 secondes) avec le micro du téléphone
- Détecter automatiquement le segment du tap (seuil d’énergie simple)
- Estimer la fréquence dominante (FFT + détection de pic)
- Afficher :
  - la fréquence dominante (Hz)
  - un score de confiance (ratio énergie du pic / énergie totale)
- Sauvegarder des mesures avec des notes (bois, gabarit, étape du barrage) et comparer l’historique

---

## Pourquoi ce projet

Le réglage des barrages se fait souvent en tapant et en écoutant, ce qui est subjectif et difficile à reproduire.  
Cette application propose une méthode simple et répétable pour **quantifier** la réponse au tap et **suivre les tendances** dans le temps.

---

## Pile technologique

- **React Native (Android)**
- **Module natif Kotlin** (Android `AudioRecord`) pour la capture audio brute
- **DSP en JavaScript** :
  - détection du tap (RMS / seuil d’énergie)
  - fenêtre de Hann
  - FFT
  - détection de pic + estimation de confiance
- Stockage : persistance locale de l’historique des mesures (MVP)

---

## Architecture (vue d’ensemble)

1. **Capturer l’audio PCM** (mono, 16 bits) via le module Kotlin  
2. Convertir le PCM en échantillons float côté JS  
3. Détecter la zone du tap et découper une courte fenêtre  
4. Appliquer la fenêtre + FFT  
5. Extraire la fréquence dominante + confiance  
6. Afficher le résultat et stocker les métadonnées de mesure

---

## Démarrage (Android)

### Prérequis

- Node.js (LTS)
- Android Studio + Android SDK
- JDK 17 (ou la version requise par le template RN utilisé)
- Un téléphone Android physique (recommandé pour tester le micro)

### Installation & exécution

```bash
cd app
npm install
npx react-native run-android
```
Astuce : si vous utilisez Yarn :
```
yarn && npx react-native run-android
```

---

## Utilisation
1. Ouvrir l’application sur un appareil Android
2. Appuyer sur Record
3. Taper une fois la table/barrage (tap court et net)
4. L’application affiche la fréquence dominante (Hz) et la confiance
5. Ajouter des notes (optionnel) puis Save
6. Répéter après ajustement pour comparer l’évolution

---

## Précision & limites
- Les résultats dépendent du bruit ambiant, du micro du téléphone et de la régularité du tap.
- L’objectif est la répétabilité et le suivi de tendances, pas une calibration de laboratoire.
- Pour de meilleurs résultats :
    - enregistrer dans une pièce calme
    - garder une distance et une force de tap constantes
    - éviter les bruits de manipulation (ne pas toucher le téléphone pendant l’enregistrement)

---

## Feuille de route
### Phase 1 — MVP micro du téléphone (actuel)
- [ ] Module natif de capture PCM (AudioRecord)
- [ ] Détection du segment de tap
- [ ] FFT + estimation de la fréquence dominante
- [ ] Historique des mesures + comparaison

### Phase 2 — Intégration TapHammer (ESP32) (prochaine)
- [ ] Source d’entrée BLE : recevoir des fréquences depuis un marteau ESP32 avec IMU
- [ ] Pipeline de mesure unifié (micro ou BLE)
- [ ] Indicateurs de stabilité améliorés

---

## Contribuer
Projet personnel démo/portfolio. Les suggestions et pull requests sont les bienvenues.

---

## Licence
MIT License — voir [LICENSE](./LICENSE).


---

---


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

