# MD Convert

An Android app that converts Markdown files to beautifully styled HTML.

## Features

- **Open Markdown files** from your device's file manager
- **Paste from clipboard** to quickly convert markdown text
- **Share menu integration** — share `.md` files or text from any app
- **Auto-copy HTML** to clipboard (optional)
- **Deferred viewing** — convert now, view later (optional)
- **GFM support** — tables, task lists, strikethrough, autolinks

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
app/src/main/java/com/mdconvert/app/
├── MainActivity.kt       # Main UI, file handling, clipboard, intents
└── MarkdownConverter.kt   # Markdown-to-HTML conversion logic
```

### Key Classes

- **`MarkdownConverter`** — Stateless converter that parses markdown using CommonMark with extensions (tables, strikethrough, autolinks, task lists) and wraps the output in a styled HTML template.
- **`MainActivity`** — Handles the UI lifecycle, file picker, clipboard paste, share intent reception, settings (auto-copy, deferred viewing), and displays converted HTML in a WebView.

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

The app stores two settings in SharedPreferences (`md_convert_prefs`):

| Setting           | Key              | Default | Description                              |
|-------------------|------------------|---------|------------------------------------------|
| Auto-copy HTML    | `auto_copy_html` | `false` | Copy converted HTML to clipboard automatically |
| Don't auto-show   | `no_auto_show`   | `false` | Defer showing the result until the eye button is tapped |

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
