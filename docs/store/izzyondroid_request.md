# Inclusion request for IzzyOnDroid (to file at https://gitlab.com/IzzyOnDroid/repo/-/issues, template "Request for Packaging")

Title: Ciclo (pe.favio.ciclo): offline menstrual cycle tracker

- Source code: https://github.com/lotuslatte/ciclo-app
- Licence: GPL-3.0-or-later
- APK: attached to GitHub releases as `ciclo.apk`, signed with the developer key, built by GitHub Actions from the tagged commit
- Size: about 1.7 MB
- Permissions: POST_NOTIFICATIONS only. The manifest removes INTERNET and the CI fails if the APK requests it.
- No proprietary libraries, no trackers, no ads, no network access. Dependencies: AndroidX (Compose, WorkManager), kotlinx-serialization.
- Metadata: `fastlane/metadata/android/` (es-PE, es-419)
- Language: Spanish

Description: Ciclo records periods and symptoms and estimates the next dates from the person's own cycles. Everything stays in one file in app-private storage; backups and a PDF summary leave the device only when exported.

Before filing: a release tagged after the GPL licence commit must exist (v1.4), because the request points at a release APK. F-Droid proper can follow later; it builds from source and will need the build to work without the signing secrets, which the Gradle file already allows (it falls back to the debug key).
