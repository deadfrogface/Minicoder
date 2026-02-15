# Minicode

Offline Android AI mini codewriter. Edits one file at a time, outputs full file content only. No explanations, no markdown, no internet.

## Requirements

- Android 8.0 (API 26) or higher
- ~100 MB free storage for app + model
- Target device: e.g. Samsung Galaxy S20 FE (Snapdragon 865, 6–8 GB RAM)

## Model

- **Recommended model:** **SmolLM2-360M-Instruct Q4_0** (229 MB). See [MODEL_CHOICE.md](MODEL_CHOICE.md) for the selection rationale.
- **Only one model file** is supported; the app expects this **filename:** `deepseek-coder-1.3b-q4.gguf`
- **Path (optional bundle):** `app/src/main/assets/deepseek-coder-1.3b-q4.gguf`
- **Target size:** 229 MB (SmolLM2-360M-Instruct Q4_0). For APK &lt;120 MB the CI build does **not** bundle the model—add it manually for full offline use.

Download **SmolLM2-360M-Instruct.Q4_0.gguf** from [QuantFactory/SmolLM2-360M-Instruct-GGUF](https://huggingface.co/QuantFactory/SmolLM2-360M-Instruct-GGUF), rename it to `deepseek-coder-1.3b-q4.gguf`, then place it in assets or copy to app storage. The app copies from assets to internal storage on first run if present.

## Build

**GitHub Action:** On every push to `main`, the workflow clones llama.cpp, builds the app (arm64 release, signed), and uploads the **finished APK** as artifact **Minicode-APK** (no model bundled; size under 120 MB). Download from Actions → latest run → Artifacts. Add the model file as `deepseek-coder-1.3b-q4.gguf` (recommended: SmolLM2-360M-Instruct Q4_0, 229 MB) to the app’s internal storage or assets for full offline use.

**Local:** Open the project in Android Studio. Optionally place the model as `deepseek-coder-1.3b-q4.gguf` in `app/src/main/assets/` for a full build. Build → Build Bundle(s) / APK(s) → Build APK(s). APK is arm64-only, stripped, minified.

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
