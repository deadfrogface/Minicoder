# Modell-Entscheidung für Minicode

## Ergebnis der Recherche

**Kein code-spezialisiertes GGUF-Modell unter 250 MB gefunden.**  
Code Llama 1.3B/3B, Phi-3 Mini (Code), CodeAlpaca und DeepSeek-Coder 1.3B liegen in Q4 alle im Bereich 600 MB–2+ GB.  
Die einzigen GGUF-Modelle, die **unter 250 MB** liegen, **llama.cpp-kompatibel** sind und **für Code nutzbar** sind (durch Training auf The Stack / Instruktionsfolge), sind die **SmolLM2-360M**-Varianten.

---

## Gewähltes Modell

**SmolLM2-360M-Instruct, Quantisierung Q4_0**

| Eigenschaft        | Wert |
|--------------------|------|
| **Modellname**     | SmolLM2-360M-Instruct (GGUF Q4_0) |
| **Erwartete Dateigröße** | **229 MB** |
| **Quelle**         | [QuantFactory/SmolLM2-360M-Instruct-GGUF](https://huggingface.co/QuantFactory/SmolLM2-360M-Instruct-GGUF) – Datei `SmolLM2-360M-Instruct.Q4_0.gguf` |
| **Parameter**      | 360M |
| **Quantisierung**  | Q4_0 (4-bit) |

---

## Warum dieses Modell?

- **APK-Größe:** 229 MB ist das kleinste realistische Q4-Modell mit Code-/Instruktionsfähigkeit. Alle genannten Code-Modelle (Code Llama, Phi-3, CodeAlpaca, DeepSeek 1.3B) sind in Q4 deutlich größer (600 MB+).
- **Mobile Performance:** 360M Parameter, Q4 → geringer Speicherbedarf und schnellere Inferenz auf Snapdragon 865 (6–8 GB RAM).
- **Determinismus:** Mit `temperature=0` (oder äquivalent in llama.cpp) ist die Ausgabe deterministisch; Standard für Minicode.
- **Code-Qualität:** SmolLM2 wurde u. a. auf The Stack (Code) und Instruktionsdaten trainiert; die **Instruct**-Variante folgt Anweisungen besser als die Base-Variante und eignet sich für „Instruction → vollständige Datei“.
- **Stabilität:** Kleines Modell, begrenzter Kontext (z. B. 2000 Zeichen + 900 Tokens) → gut beherrschbar, weniger OOM-Risiko.

**Alternativen unter 250 MB:**  
- **Q4_1** (249 MB): etwas bessere Qualität, minimal größer.  
- **Q3_K_M** (235 MB): oft etwas bessere Qualität als Q4_0, geringere Dateigröße.  
- **Q2_K** (219 MB): kleinste Variante, Qualitätsverlust; nur wenn jede MB zählt.

---

## Erwartete Kennzahlen

| Kennzahl | Wert |
|----------|------|
| **Modell-Dateigröße** | **229 MB** (Q4_0) |
| **Erwartete APK-Größe (ohne Modell)** | ~80–120 MB (wie aktuell im CI) |
| **Erwartete APK-Größe (mit gebündeltem Modell)** | ~310–350 MB (überschreitet Ziel 200 MB) |
| **Empfehlung** | Modell **nicht** in die APK packen; Nutzer legt die Datei manuell ab (Assets oder App-Speicher). So bleibt die APK unter 120 MB und das Ziel „unter 200 MB“ ist erfüllt (APK < 200 MB). |
| **RAM-Nutzung auf dem Gerät** | Ca. **300–500 MB** (Modell ~180 MB + Kontext/KV-Cache); unkritisch für 6–8 GB RAM. |

---

## Integration in Minicode

Die App lädt weiterhin **eine** Modelldatei mit dem festen Namen **`deepseek-coder-1.3b-q4.gguf`** (keine App-Änderung).  
**Vorgehen:**  
1. `SmolLM2-360M-Instruct.Q4_0.gguf` von Hugging Face herunterladen.  
2. Datei in **`deepseek-coder-1.3b-q4.gguf`** umbenennen.  
3. In `app/src/main/assets/` legen (lokaler Voll-Build) oder nach Installation in den App-internen Speicher kopieren (wie in README beschrieben).

---

## Kurzfassung

- **Beste Wahl unter den harten Grenzen (<250 MB, Q4, Code/Instruktion, llama.cpp):**  
  **SmolLM2-360M-Instruct Q4_0**, **229 MB**.  
- **Erwartete finale APK-Größe:** Ohne Modell **~80–120 MB**; mit gebündeltem Modell **~310 MB** (daher Modell extern bereitstellen).  
- **RAM:** ~300–500 MB auf dem Gerät.
