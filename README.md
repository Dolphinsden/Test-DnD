# The Ashen Lantern of Hollowbrook — D&D 5E Couch Co-op (Java 17 + LibGDX)

A couch co-op, text-based **Dungeons & Dragons 5th Edition (5E)** desktop RPG built purely in **Java 17 + LibGDX** (LWJGL3 + Scene2D + FreeType).

## Key Features
- **Couch Co-op Hot-Seat (1–6 Players):** Quick-start roster of 6 curated Level 1 5E heroes (Fighter, Wizard, Rogue, Cleric, Ranger, Paladin across Human, Elf, Dwarf, Halfling) with customizable Character Names, Player Names, and Backstories.
- **Exploration vs. Strict 5E Combat:**
  - **Out of Combat:** Purely narrative free-form text prompts from any selected party member.
  - **In Combat:** Strict official D&D 5E **Initiative order** with action economy (`Attack`, `Cast Spell`, `Dodge`, `Dash`, `Disengage`, `Help`, `Hide`, `Second Wind`, `Lay on Hands`, Death Saving Throws, Conditions, Short/Long Rests, XP & Leveling).
- **Hybrid Local Dungeon Master:**
  - Defaults to **Ollama** (`http://localhost:11434`, e.g., `llama3.2`) with one-click Settings presets for **LM Studio** (`http://localhost:1234`) and any **OpenAI-compatible** local server.
  - Seamlessly falls back to a built-in deterministic **Offline Rule-Based DM** whenever no local LLM is running — playable immediately with zero external setup!
- **Interactive Digital + Physical Dice:**
  - Roll digitally in-app (or press `Space`) with full transparent 5E math (`1d20 + mod`, Advantage/Disadvantage, Halfling Lucky, Critical Hit 2x dice), **OR** type the number you rolled on real physical dice at the table!
  - Includes an **Auto-Roll** toggle in DM Settings.
- **Flagship 4-Act Campaign — *The Ashen Lantern of Hollowbrook*:**
  - 10 interconnected locations across Hollowbrook, The Whispering Mire, The Sunken Reliquary, and The Ashen Sanctum, with NPCs, skill checks, traps, locked vaults, and 4 balanced 5E encounters culminating in **Malakor, the Cinder Warden**.
- **Dark Fantasy Grimoire Dashboard:**
  - Three-pane Obsidian, Parchment & Antique Gold interface with a live **Couch Font-Size Toggle** (`Normal` / `Large` / `TV`).
- **Local Save / Load & Adventure Chronicle Export:**
  - Auto-saves after every turn to `~/.dnd5e-coop/saves/autosave.json`.
  - Supports multiple named local JSON save slots (preserving mid-combat state, initiative, pending rolls, inventory, spells, and full log).
  - One-click **Export Story** saves your adventure log as a readable Markdown Chronicle under `~/.dnd5e-coop/chronicles/`.

---

## Running the Game

### Option 1: Windows `.exe` Launcher (Requires Java 17+)
1. Ensure **Java 17 or newer** is installed on Windows (e.g., [Eclipse Temurin JDK 17](https://adoptium.net/temurin/releases/?version=17)).
2. Double-click:
   ```
   desktop/build/launch4j/AshenLanternDnD5E.exe
   ```

### Option 2: Runnable Fat `.jar` (Windows / Linux / macOS)
```bash
java -jar desktop/build/libs/AshenLanternDnD5E.jar
```

### Option 3: Run from Source via Gradle
```bash
./gradlew :desktop:run
```

---

## Connecting a Local LLM (Optional)
The game works out of the box using the built-in Offline DM. To enhance free-form storytelling with a local LLM:
1. **Ollama (Default):** Start Ollama locally (`ollama serve`) and pull a model (e.g., `ollama pull llama3.2`).
2. **LM Studio:** Start LM Studio's Local Server on port `1234`, open **Local Dungeon Master Settings** in-game, click **LM Studio**, and click **Test Connection & Detect Models**.

---

## Rebuilding & Packaging

```bash
# Run full JUnit 5 test suite (compiled with --release 17)
./gradlew test

# Build runnable fat JAR
./gradlew :desktop:shadowJar

# Build standalone Windows .exe launcher via Launch4j
./gradlew :desktop:createExe

# Optional (run on Windows with JDK 17+ to bundle an embedded JRE):
gradlew.bat :desktop:jpackageWindows
```
