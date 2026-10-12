# Ciclo

Offline menstrual-cycle tracker for Android (Kotlin + Jetpack Compose). No network permission; all data stays in one JSON file in app-private storage.

- Build: GitHub Actions (`.github/workflows/build.yml`) runs unit tests and produces a signed `ciclo.apk` artifact. Tags `v*` also publish a Release.
- Signing key: kept outside the repo (`~/.keys/ciclo/`), loaded in CI from the `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` secrets. Losing it means future APKs can't update an installed copy.
- Predictions: `app/src/main/java/pe/favio/ciclo/domain/Cycle.kt` (tests in `app/src/test`).
- Health content and citations: `app/src/main/java/pe/favio/ciclo/content/PhaseContent.kt`.
