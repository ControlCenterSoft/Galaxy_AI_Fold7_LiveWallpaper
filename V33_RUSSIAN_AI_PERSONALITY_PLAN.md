# v33.0 — Russian AI Personality Layer

## Goal
Make Russian the default AI communication language on Galaxy Fold7 while preserving the full v32 portrait, AIDI, memory, local fallback, privacy and Fold-continuity stack.

## Implemented
- On-device AI communication locale is fixed to `ru-RU` by default.
- Ambient AI voice uses Android TextToSpeech with Russian locale; no microphone or speech-recognition permission is introduced.
- All built-in emotion reactions (`calm`, `focused`, `thinking`, `happy`, `sleep`) have Russian phrases.
- AIDI scene requests advertise `language=ru`, `locale=ru-RU`, `culture=russian`, and `response_style=friendly` in a bounded communication contract.
- Phone configuration/status text is localized to Russian so the selected AI language is visible to the user.
- v32 Expression Presence, v31 dimensional portrait, v30 personalization, v29 ambient voice controls, v28 styles, AIDI Memory/Qdrant compatibility, LocalFallbackAI and secure hybrid routing remain intact.

## Privacy and safety
- No `CAMERA`, `RECORD_AUDIO`, or fine-location permission.
- TTS is output-only and runs through the Android speech service.
- No raw audio, camera frames, location coordinates, account data or user name is transmitted to AIDI.
- AIDI receives only the locale/culture/style identifiers plus the already bounded non-sensitive state contract.

## Release gate
- JVM test validates locale and Russian phrase coverage for all supported emotions.
- CI verifies v33 metadata, Russian TTS wiring, AIDI locale payload, inherited gateway Python syntax and privacy permissions.
- APK signature and SHA-256 are verified before artifact upload.
