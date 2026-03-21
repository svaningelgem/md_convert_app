# MD Convert

An Android app that converts Markdown files to beautifully styled HTML.

## Features

- **Open Markdown files** from your device's file manager
- **Paste from clipboard** to quickly convert markdown text
- **Toggle view** — switch between rendered HTML and raw markdown with the eye button
- **Copy HTML** — one-tap copy of converted HTML to clipboard
- **Share menu integration** — share `.md` files or text from any app
- **Auto-copy HTML** on conversion (optional setting)
- **Auto-restore** — reopens your last file on startup; shows nothing if the file was removed
- **Welcome guide** — bundled markdown file on first launch to explore features immediately
- **GFM support** — tables, task lists, strikethrough, autolinks

### FAB Layout

The app has four floating action buttons arranged in two groups:

| Group    | Button    | Icon      | Action                           |
|----------|-----------|-----------|----------------------------------|
| Bottom   | Open      | +         | Open a markdown file             |
| Bottom   | Paste     | Clipboard | Paste markdown from clipboard    |
| *(gap)*  |           |           |                                  |
| Top      | Toggle    | Eye       | Switch between HTML and raw view |
| Top      | Copy      | Copy      | Copy HTML to clipboard           |

### Supported Markdown Elements

| Element          | Syntax                     |
|------------------|----------------------------|
| Headings         | `# H1` through `###### H6`|
| Bold             | `**bold**`                 |
| Italic           | `*italic*`                 |
| Strikethrough    | `~~deleted~~`              |
| Links            | `[text](url)`              |
| Images           | `![alt](url)`              |
| Code (inline)    | `` `code` ``               |
| Code blocks      | ` ```language ... ``` `    |
| Blockquotes      | `> quote`                  |
| Ordered lists    | `1. item`                  |
| Unordered lists  | `- item`                   |
| Tables           | GFM pipe tables            |
| Task lists       | `- [x] done`               |
| Horizontal rules | `---`                      |
| Autolinks        | Raw URLs auto-linked       |

## Architecture

Single-activity Android app built with:

- **Kotlin** — primary language
- **CommonMark** — markdown parsing and HTML rendering (with GFM extensions)
- **Material Design 3** — UI components and theming
- **WebView** — HTML display

### Project Structure

```
app/src/main/
├── assets/
│   └── welcome.md              # Bundled welcome markdown shown on first launch
├── java/com/mdconvert/app/
│   ├── MainActivity.kt         # UI, file handling, clipboard, intents, state persistence
│   └── MarkdownConverter.kt    # Markdown-to-HTML conversion logic
└── res/
    ├── drawable/                # Vector icons (add, paste, visibility, copy, code)
    ├── layout/activity_main.xml # CoordinatorLayout with WebView and 4 FABs
    ├── menu/menu_main.xml       # Settings menu (auto-copy toggle)
    └── values/                  # Colors, strings, themes
```

### Key Classes

- **`MarkdownConverter`** — Stateless converter that parses markdown using CommonMark with extensions (tables, strikethrough, autolinks, task lists) and wraps the output in a styled HTML template.
- **`MainActivity`** — Handles the UI lifecycle, file picker, clipboard paste/copy, share intent reception, raw/HTML view toggle, state persistence across launches, and displays content in a WebView.

### Startup Flow

1. **First launch** — loads `welcome.md` from assets so users can immediately explore features
2. **Subsequent launches** — restores the last loaded file by re-reading from its URI
3. **File removed** — if the previously loaded file is no longer accessible, shows a blank screen and clears persisted state
4. **Pasted/shared content** — restored from saved markdown text (no URI needed)

## Building

### Prerequisites

- JDK 17+
- Android SDK (API 35)

### Build

```bash
./gradlew assembleDebug
```

The debug APK is automatically copied to the project root as `md-convert.apk`.

### Run Tests

```bash
./gradlew testDebugUnitTest
```

Tests use JUnit 4 and Robolectric for full coverage of both the converter logic and Activity behavior.

## CI/CD

A GitHub Actions workflow runs on every pull request:

- Runs all unit tests
- Builds the debug APK
- Uploads the APK and test results as artifacts

See [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml).

## Configuration

The app stores settings and state in SharedPreferences (`md_convert_prefs`):

| Key              | Type    | Default | Description                              |
|------------------|---------|---------|------------------------------------------|
| `auto_copy_html` | Boolean | `false` | Auto-copy HTML to clipboard on conversion |
| `last_source_uri`| String  | `null`  | URI of last loaded file (for auto-restore) |
| `last_markdown`  | String  | `null`  | Raw markdown of last conversion           |
| `last_subtitle`  | String  | `null`  | Display name of last conversion           |

## Sample File

A [`sample.md`](sample.md) file is included in the repository for testing the app.

## Tech Stack

| Component     | Version  |
|---------------|----------|
| Kotlin        | 2.0.21   |
| Gradle        | 8.9      |
| AGP           | 8.7.3    |
| Min SDK       | 26 (Android 8) |
| Target SDK    | 35 (Android 15) |
| CommonMark    | 0.24.0   |
| Robolectric   | 4.14.1   |
