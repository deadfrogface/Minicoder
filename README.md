# Minicode

Offline Android AI mini codewriter. Edits one file at a time, outputs full file content only. No explanations, no markdown, no internet.

## Requirements

- Android 8.0 (API 26) or higher
- ~100 MB free storage for app + model
- Target device: e.g. Samsung Galaxy S20 FE (Snapdragon 865, 6–8 GB RAM)

## Model

Place the GGUF model in the app assets before building:

- **Recommended:** DeepSeek-Coder 1.3B GGUF Q4 (~65–70 MB)
- **Path:** `app/src/main/assets/deepseek-coder-1.3b-q4.gguf`

You can obtain the model from [Hugging Face](https://huggingface.co/) or similar. The app copies it to internal storage on first run.

## Build

- Open the project in **Android Studio** (it will sync Gradle and create the wrapper if needed).
- Build locally (no CI, no GitHub dependency).
- Build → Build Bundle(s) / APK(s) → Build APK(s).

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
