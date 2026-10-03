# RuneRuin — agent map

NeoForge mod `runeruin` for Minecraft 26.2.0 + NeoForge 26.2.0.59 (official Mojang mappings): a stacked-cave dimension `runeruin:runeruin_dimension` (`/execute in runeruin:runeruin_dimension …`). `RuneRuinMod` registers the DeferredRegisters; datapack registries come from `DatagenMain`. Helpers: `RR.id` / `RR.resourceKey` / `RR.tagKey`.

Commands below are for Windows PowerShell. On Linux/macOS run the `scripts/*.ps1` with `pwsh` (PowerShell 7), e.g. `pwsh ./scripts/start-task.ps1 feature giant_goblet`, and Gradle as `./gradlew`.

## Task protocol

Several agents work in this repository at once. Every change, documentation included, goes in its own task branch and worktree; never edit files in the shared main checkout.

1. Run `git branch --show-current` and `git status --short --branch`.
2. Create the task worktree: `feature` or `bug`, a short snake_case name from the request, no `add_` prefix.

   ```powershell
   .\scripts\start-task.ps1 feature giant_goblet
   ```

   It creates `..\RuneRuin-giant-goblet` on `feature/giant_goblet` from the current `HEAD`, links the shared Minecraft sources and runs `runData`. If the name is taken, choose another; never take over another agent's branch or worktree. In Claude Code, then call `EnterWorktree` with `path` set to the new worktree (not `name`, not the built-in worktree option).
3. Work only inside the task worktree: edits, searches, Gradle, file links in replies. Do not copy files or uncommitted changes between worktrees; never switch to a branch used by another worktree.
4. Follow-ups after a commit stay in the same branch and worktree. Start a new branch only for an unrelated feature or bug.
5. Before the final report, merge `main` into the task branch, resolve conflicts and repeat the checks.
6. Merge into `main`, cherry-pick or remove worktrees only when the user asks. Merge from the main checkout, then remove the worktree only with `.\scripts\finish-task.ps1 -Path ../RuneRuin-<name>`: it unlinks `.mc-sources` first, whereas `git worktree remove --force` deletes the shared sources cache through the junction.

Git hooks in `.githooks` (enabled by the setup script) reject commits to `main` other than merges, cherry-picks and reverts, branch names other than `feature/<snake_case>` / `bug/<snake_case>`, a detached `HEAD`, and attribution lines. Never use `--no-verify`.

## Questions, designs, reports

- A question ("can we…?", "why…?", "how would you…?") gets an answer, not code changes. Edit only after an explicit go-ahead.
- Propose a design (2–3 options and a recommendation) and wait before implementing a new block or mechanic, new blockstate properties, tiling textures, anything that changes existing worlds (see Worldgen) or work touching more than ~5 files.
- Answer numbered follow-up requests point by point.

**Done** means every applicable check passed in the task worktree:

1. `gradlew.bat compileJava`
2. `gradlew.bat runData` — after any change to datagen, blocks/items, models, tags, loot or worldgen bootstrap
3. `gradlew.bat runGameTestServer` — mod and datapack load, GameTests in `preview/RRGameTests`
4. shapes: `runPreview` of the matching job (`headless-preview` skill); models, textures, translations: `python scripts/lint_assets.py` after `runData`, and renders from `scripts/render_model.py` / `scripts/render_texture_tile.py`
5. `gradlew.bat build`

Lighting, render layers, interaction and biome placement need the game: list them under "Check in game".

**Final report**, also for partial work:

```
## Done
1. <requested item> — done | partial | not done — <one line>
## Verified
<item> — compile / datagen / GameTest / preview render / needs the game
## Check in game (up to 3 steps)
1. <command or coordinates> — <what should be visible>
## Decisions I made myself
- <decision> — <why>; <risk>
## Branch
<branch>, <worktree path>, output of `git status --short --branch`
```

End with single-line PowerShell commands using absolute paths to the task worktree, without `cd`: `runData` first if it is needed, then `& 'C:\path\to\worktree\gradlew.bat' -p 'C:\path\to\worktree' runClient`. Copy "Decisions I made myself" into the commit message body so it outlives the chat.

## Commits and code

- Commit subject in the imperative, no prefix: `Make floating moss sink under creatures that stand still`. No attribution lines (`Co-Authored-By`, "Generated with …").
- Reduce code rather than grow it: fix existing code instead of adding more, change an algorithm instead of adding another condition, comment only important points. Keep the surrounding style; packing code onto one line is not less code.
- Imports one per line, sorted, no wildcards: the shared registry files then merge cleanly.

## Gradle, datagen and the game

Gradle needs any installed JDK; the wrapper downloads Java 25.

| Task | Purpose |
|------|---------|
| `runClient` | game with the mod; game dir `~/.runeruin/game`, shared by all checkouts |
| `runServer` | dedicated server (`--nogui`) |
| `runGameTestServer` | run GameTests, exit non-zero on failure |
| `runPreview` | headless shape export into `exports/` (`headless-preview` skill) |
| `runData` | datagen into `src/generated/resources` |
| `build` | mod jar |
| `extractMcSources` | Minecraft + NeoForge sources into `.mc-sources/`, textures and models into `.vanilla-textures/` |

- Never `clean`, `--rerun-tasks`, `--refresh-dependencies`, `--offline` or `--stop`; never kill Java or Gradle; never start, restart or wait for the game. A Claude Code hook blocks these.
- `src/generated` is not in git. Never delete it or `src/generated/resources/.cache`, never hand-write `src/generated/**`: change the Java bootstrap and run `runData` yourself.
- A running game locks `build/` of the checkout it was started from. If Gradle fails on a locked file, tell the user the game is locking `build/`, ask them to close it, and give the single-line absolute `runData` and `runClient` commands for the task worktree.
- `build.gradle` pins NeoForm Runtime to `%USERPROFILE%\.gradle\caches\neoformruntime`. If `downloadAssets` starts downloading thousands of files, stop: the pin failed.
- Every run except `runClient` (server, datagen, GameTests, previews) uses its own `<checkout>/run`, never the game directory.

## Minecraft and NeoForge sources

Do not use Minecraft or NeoForge APIs from memory. Search `.mc-sources/` (`net/minecraft`, `com/mojang`, `net/neoforged`, `VERSION.txt`), a junction to the shared cache `~/.runeruin/mc-sources/<version>`. Never edit it or copy it into `src/`.

- No `.mc-sources/VERSION.txt`: run `.\scripts\setup-codex-worktree.ps1`. If it fails or the file is still missing, find the cause in the script's output, report it to the user and stop the task.
- `.ignore` un-ignores `.mc-sources` for ripgrep; if Grep still skips it, use `rg --no-ignore-vcs`. Do not search `~/.gradle`.
- Vanilla textures and models: `.vanilla-textures/assets/minecraft/{textures,models}/`.

## Skills

Step-by-step procedures live in skills; load the matching one before starting such work. Skills are in `.agents/skills/`; Claude Code reads stubs in `.claude/skills/`, keep their `description` in sync.

| Skill | Use for |
|-------|---------|
| `block-and-item` | adding, renaming or removing a block or item |
| `worldgen-feature` | decoration features: plants, rocks, vines, mushrooms, spikes |
| `worldgen-biome` | adding, reweighting, moving or changing biomes |
| `worldgen-structure` | multi-piece structures such as Giant Goblet and Baobab |
| `headless-preview` | checking feature and structure shapes without the game; new preview jobs |
| `region-export-compare` | `/rrexport` dumps and terrain replay (`world_region`, was → expected) |
| `minecraft-pixel-texture-generation` | creating, recoloring, quantizing and validating textures |
| `minecraft-model-texture-analysis` | model geometry and texture UVs |

## Packages (`src/main/java/ioann/uwu/runeruin/`)

| Path | Role |
|------|------|
| `blocks/`, `items/`, `entities/` | `RRBlocks` (also registers BlockItems), `RRItems`, `RREntityTypes`; teleport item `RuneOfSpaceItem` |
| `creativetab/` | creative tab; its item list is manual |
| `datagen/` | models, blockstates, tags, loot, recipes, worldgen bootstrap (`DatagenMain`) |
| `client/` | renderers, models, tints, clouds (`RuneRuinClient`) |
| `portal/`, `loottables/`, `mixin/` | dimension portal, `RRLootTables`, vanilla tweaks |
| `region/` | `/rrpos1` `/rrpos2` `/rrexport` `/rrclear` |
| `preview/` | headless previews and GameTests |
| `dimension/` | worldgen registries: `RRFeatures`, `RRConfiguredFeatures`, `RRPlacedFeatures`, `RRBiomes`, `RRBiomeSource`, `RRChunkGenerator`, `RRDimension`, `RRStructure*`, `RRPlacementModifierTypes` |
| `dimension/biomes/<layer>/` | one class per biome |
| `dimension/features/` | `Feature<?>` implementations |
| `dimension/placements/` | placement modifiers |
| `dimension/chunkgenerator/` | terrain fill, `RRTerrainSurfaces`, `HangingTerrainGenerator` |
| `dimension/structures/` | structures and pieces |
| `dimension/noise/` | noise for terrain and biome choice |
| `dimension/runes/` | rune patterns on pillars |

Translations are hand-written in both `assets/runeruin/lang/en_us.json` and `ru_ru.json`.

## Worldgen

Layers bottom → top (`Const`): Void `0…50` → arcane plate → Lost caves `LOST_CAVES_Y` 56 … `LOST_CAVES_CEILING_Y` 131 → plate → Deep caves 137 … 212 → plate → Blooming caves 218 … 293 → Top layer from `TOP_LAYER_Y` 294 to the build limit 512.

Two independent systems:

1. **Terrain**: `RRChunkGenerator.fillFromNoise` → `TopLayerAndBloomingCavesGen`, `DeepCavesGen` (deep caves ceiling), `DeepCavesAndLostCavesGen`, `VoidGen`; plates, pillars and runes in `ArcaneStructureGen`; surface blocks per biome in `RRTerrainSurfaces`. Biomes do not carve the caves.
2. **Decoration**: the vanilla feature pipeline per biome. `RRBiomeSource.getNoiseBiome` picks a biome by Y and noise from the lists in `RRBiomeSource.newDefault` (also used by `RRDimension.bootstrapStem`); weights are repeated entries.

These change existing worlds, so design them first: adding, removing or reordering a biome list entry (shifts every border in the layer), renaming noise seed strings, changing `RRTerrainSurfaces`, changing a structure algorithm (seams in half-generated structures).

Procedures for features, biomes and structures: the `worldgen-*` skills.
