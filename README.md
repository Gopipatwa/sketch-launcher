# ✏️ Sketch Launcher

A minimalist Android home-screen launcher that looks like it was doodled in a notebook.
Everything — borders, the clock, the battery, the calendar, the icons — is drawn with
wobbly, hand-inked lines in a handwriting font.

## Features

- **7 paper themes** — Notebook (ruled), Blueprint (grid), Chalkboard, Midnight Ink, Kraft Paper (dots), Pastel Doodle, Graph Paper
- **Sketchiness control** — Calm / Normal / Wild line wobble, plus optional *boiling lines* animation
- **Built-in sketch widgets** — clock (digital, analog or *"quarter past ten"* words), date, battery, month calendar, sticky note
- **Real Android widgets** — add any app's widget, resize it, remove it
- **Minimal home** — your pinned apps as a clean text list (icons optional: colour, ink, or hand-lettered)
- **Fast app drawer** — type to search, Enter launches the top hit, A–Z headers, work-profile apps marked
- **Gestures** — swipe up for apps, swipe down for notifications, long-press home for the menu
- **App menu** — long-press any app to pin/unpin, reorder, hide, open app info or uninstall
- No internet permission, no tracking, no ads

## Install

1. Open the **Actions** tab of this repo → latest **Build APK** run → download the `SketchLauncher-…` artifact.
   (Tagged versions, e.g. `v1.0.0`, also appear under **Releases**.)
2. Unzip and install `SketchLauncher-<version>.apk` on your phone (allow "install unknown apps").
3. Press Home and choose **Sketch Launcher**, or open it and tap *Make Sketch Launcher my home screen* in settings.

The `-debug.apk` installs side by side with the release build (different package id), which is handy for testing.

## Building

Every push builds through `.github/workflows/build-apk.yml`. To build locally you need JDK 17 and the Android SDK:

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

### Signing with your own key (optional, recommended)

Without a key, release APKs are signed with the CI's debug key, which changes between runs —
so you'd need to uninstall before updating. To keep a stable signature, create a keystore once:

```bash
keytool -genkeypair -v -keystore release.jks -alias sketch -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks   # copy the output
```

and add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `SIGNING_KEYSTORE_BASE64` | base64 output from above |
| `SIGNING_STORE_PASSWORD` | keystore password |
| `SIGNING_KEY_ALIAS` | `sketch` (or your alias) |
| `SIGNING_KEY_PASSWORD` | key password |

### Releasing

```bash
git tag v1.0.0 && git push origin v1.0.0
```

## Project layout

```
app/src/main/java/com/sketch/launcher/
├── MainActivity.kt          # HOME activity, screen switching, theme
├── LauncherViewModel.kt     # app list, settings, favorites
├── data/                    # app loading (LauncherApps), preferences
├── sketch/                  # hand-drawn rendering: Rough lines, themes, components, glyphs
├── ui/                      # home, drawer, settings, widget picker, built-in widgets
└── widgets/                 # AppWidgetHost integration
```

## Credits

Fonts: [Patrick Hand](https://fonts.google.com/specimen/Patrick+Hand) and [Caveat](https://fonts.google.com/specimen/Caveat),
both under the SIL Open Font License (see `licenses/`).
