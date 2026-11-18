# Ciclo

Offline menstrual-cycle tracker for Android (Kotlin + Jetpack Compose). No network permission; all data stays in one JSON file in app-private storage.

- Build: GitHub Actions (`.github/workflows/build.yml`) runs unit tests and produces a signed `ciclo.apk` artifact and a signed `ciclo.aab` (App Bundle, for Google Play). Tags `v*` also publish a Release.
- Signing key: kept outside the repo (`~/.keys/ciclo/`), loaded in CI from the `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` secrets. Losing it means future APKs can't update an installed copy.
- Predictions: `app/src/main/java/pe/favio/ciclo/domain/Cycle.kt` (tests in `app/src/test`).
- Health content and citations: `app/src/main/java/pe/favio/ciclo/content/PhaseContent.kt`.

## License

GPL-3.0-or-later. See `LICENSE`.

## Distribution

- Store texts live in `fastlane/metadata/android/` (the layout F-Droid, IzzyOnDroid and fastlane read) and in `docs/store/listing_es.md` with the answers for the Play Console forms.
- `docs/` is the public page and the privacy policy for GitHub Pages. The policy page is generated from the in-app text: `python3 scripts/privacy_page.py`.
