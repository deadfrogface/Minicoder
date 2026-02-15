# Minicode

Offline Android AI mini codewriter. Edits one file at a time, outputs full file content only. No explanations, no markdown, no internet.

## Requirements

- Android 8.0 (API 26) or higher
- ~100 MB free storage for app + model
- Target device: e.g. Samsung Galaxy S20 FE (Snapdragon 865, 6–8 GB RAM)

## Model

- **Only one model file** is supported in assets: **DeepSeek-Coder 1.3B GGUF Q4**.
- **Filename:** `deepseek-coder-1.3b-q4.gguf`
- **Path:** `app/src/main/assets/deepseek-coder-1.3b-q4.gguf`
- **Target size:** ~65–70 MB (use Q4_0 or smallest Q4 variant; note: public 1.3B Q4 GGUF are typically 650–800 MB; for APK &lt;120 MB the CI build does **not** bundle the model—add it manually for full offline use).

Obtain the model from [TheBloke/deepseek-coder-1.3b-instruct-GGUF](https://huggingface.co/TheBloke/deepseek-coder-1.3b-instruct-GGUF) or similar. The app copies it from assets to internal storage on first run.

## Build

**GitHub Action:** On every push to `main`, the workflow builds a **release** APK (no model bundled, to keep size under 120 MB) and uploads it as artifact **Minicode-APK**. Download from Actions → latest run → Artifacts. Add `deepseek-coder-1.3b-q4.gguf` to the app’s internal storage or assets for full offline use.

**Local:** Open the project in Android Studio. Optionally place `deepseek-coder-1.3b-q4.gguf` in `app/src/main/assets/` for a full build. Build → Build Bundle(s) / APK(s) → Build APK(s). APK is arm64-only, stripped, minified.

No internet permission is required; the app works fully offline.

## Usage

1. **Select project folder** – Grant access to a folder containing your files.
2. **Pick a file** from the list.
3. Enter an **instruction** (e.g. “Add a function that validates the input”).
4. Tap **GENERATE** – The full updated file is produced (no partial edits).
5. Review **Original** vs **Generated** (diff highlight).
6. **APPLY CHANGES** – Backs up the file first, then writes the new content.
7. **COPY** – One tap copies the generated file to the clipboard.
8. **RESTORE BACKUP** – Restore a previous backup for the current file.

## Limits

- Max input: 2000 characters
- Max output: 900 tokens
- Max generation time: 12 seconds
- File size: max 15,000 characters or 600 lines (otherwise “File too large”)

## License

See [LICENSE](LICENSE).
