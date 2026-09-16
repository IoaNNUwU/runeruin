# RuneRuin — agent map

## Development branches

For every substantial change, create a separate branch and do all work for the feature or bug in that branch. Never merge branches except when explicitly asked.

The following exceptions do not require creating a branch: editing `README` files, `AGENTS.md`, `.json` files, or only changing text inside a string literal in a code file.

- New functionality: use `feature/<name>`, for example `feature/giant_goblet`. Do not include `add_` in the name; use just the feature name.
- Bug fixes: use `bug/<name>`, for example `bug/giant_goblet_spawns_in_wrong_biome`.

At the beginning of every coding task, before editing files, run `git branch --show-current`.
If it returns an empty string, Codex is in a newly created detached worktree and must create
an aptly named task branch before continuing:

- New functionality: `git switch -c feature/<short-kebab-case-name>`
- Bug fixes: `git switch -c bug/<short-kebab-case-name>`

Choose the short name from the user's request, then verify the result with
`git status --short --branch`. Never continue coding while `HEAD` is detached.

## Code size and quality

When writing code, look for ways to reduce the amount of code rather than increase it:

- Add comments only for important points.
- Fix existing code instead of endlessly adding more.
- Preserve style and quality; putting all code on one line does not reduce code.
- Look for opportunities to change an existing algorithm instead of adding another condition. That is what reducing code means.

## Parallel agent worktrees

Codex provides each parallel task with its own worktree. Work only in the current assigned checkout; never create or manage another worktree from inside the task.

Codex-managed worktrees may initially use a detached `HEAD`. In a newly created detached
worktree, the branch-initialization procedure above is the required exception that allows
one `git switch -c` command. Do not switch to another existing branch, check out a branch
that may be used by another worktree, or create another worktree from inside the task.

Keep changes isolated to the current task. Do not manually copy files or uncommitted changes between worktrees. Tracked files are available automatically; ignored local files are available only when explicitly provided by the environment (for example through `.worktreeinclude`).

Before finishing, report the current worktree state and leave branch creation, handoff, merge, cherry-pick, or other cross-worktree operations to the user/Codex orchestration. Never merge branches unless explicitly asked.

NeoForge mod (`runeruin`), MC 26.2. Custom stacked-cave dimension. Entry: `RuneRuinMod` → registers DeferredRegisters; datapack registries come from `DatagenMain`.

Helpers: `RR.id` / `RR.resourceKey` / `RR.tagKey`. Dimension command: `/execute in runeruin:runeruin_dimension …`

## Run

Gradle needs some JDK installed to start; the wrapper then downloads **Java 25** for this project.

```powershell
.\gradlew.bat runClient
```

- `runClient` — launch the game with the mod
- `runServer` — dedicated server (`--nogui`)
- `runGameTestServer` — run GameTests, then exit
- `runPreview` — headless structure/feature dump into `exports/` (no client)
- `runData` — datagen into `src/generated/resources`
- `build` — compile and package the mod jar
- `extractMcSources` — explode Minecraft + NeoForge Java into `.mc-sources/` for Agents to index minecraft sources (also runs on IDE Gradle sync)

# Datagen cache and game lock

Run datagen with `.\gradlew runData` from the repo root (never `clean` / `--rerun-tasks` / `--refresh-dependencies` / `--offline`).

A running `runClient` / `runServer` locks `build/`. Datagen cannot run in parallel.

**Do not** kill Java/Gradle, `gradlew --stop`, or start/restart the client. Do not wait for the game to close.

If `runData` fails because the client is still running (file-in-use / lock / unable to delete): tell the user Minecraft is locking `build/`, ask them to close the game, and have them run datagen and the client themselves. Give these exact commands:

```
.\gradlew.bat runData
.\gradlew.bat runClient
```

Do not:

- `gradlew clean` / `clean runData`
- `--rerun-tasks`, `--refresh-dependencies`, `--offline`
- delete `src/generated/` or `src/generated/resources/.cache`
- hand-write `src/generated/**` JSON to avoid running datagen

`build.gradle` pins NeoForm Runtime to `%USERPROFILE%\.gradle\caches\neoformruntime` so a Cursor sandbox (temp `GRADLE_USER_HOME`) still reuses Minecraft assets and decompiles.

Abort if logs show `cursor-sandbox-cache` **and** `downloadAssets` counting thousands of files — that is a cache miss. Fix the pin or rerun so NFRT uses the user Gradle cache; do not wait out a full asset download.

## Vanilla / NeoForge sources

This mod compiles against **Minecraft 26.2.0 + NeoForge 26.2.0.59** with **official Mojang mappings**. Do not use APIs from memory.

Exploded sources (after `./gradlew extractMcSources` or IDE Gradle sync) live in **`.mc-sources/`**. If there is no `.mc-sources/`, use `extractMcSources` gradle task:

- `VERSION.txt` — pinned versions
- `net/minecraft/`, `com/mojang/` — decompiled vanilla with NeoForge patches
- `net/neoforged/` — NeoForge API

**Always search `.mc-sources` for vanilla/NeoForge types** (official mappings, this version only). `.ignore` un-ignores that folder for ripgrep; do not search `~/.gradle` (multiple Minecraft versions, mostly jars). If Grep still skips it, use Shell `rg` or `rg --no-ignore-vcs`. Do not treat `.mc-sources` as mod source — never edit it, never add it to `src/`.

## Packages

| Path | Role |
|------|------|
| `blocks/`, `items/`, `creativetab/` | Content registries |
| `datagen/` | Models, tags, recipes, loot + **worldgen bootstrap** |
| `dimension/` | Almost all worldgen |
| `dimension/biomes/<layer>/` | Biome definitions (one class per biome) |
| `dimension/features/` | Custom `Feature<?>` implementations |
| `dimension/chunkgenerator/` | Terrain fill (floors/ceilings/plates) |
| `dimension/structures/` | Structure + piece classes |
| `dimension/noise/` | Noise used by chunkgen + biome picking |
| `client/` | Clouds / renderers |
| `mixin/` | Vanilla tweaks |

Generated JSON lands under `src/generated/` (and mirrors in `bin/`); **edit Java bootstrap, then run datagen** — don’t hand-edit generated worldgen unless intentional.

## Vertical layers (`Const`)

Bottom → top (Y ≈): Void `0…50` → Lost caves → Deep caves → Blooming caves → Top layer (build limit 512).

Key Y constants: `LOST_CAVES_Y`, `LOST_CAVES_CEILING_Y`, `DEEP_CAVES_Y`, `DEEP_CAVES_CEILING_Y`, `BLOOMING_CAVES_Y`, `BLOOMING_CAVES_CEILING_Y`, `TOP_LAYER_Y`. Arcane stone plates sit between layers (`ArcaneStructureGen`).

## How generation splits

Two independent systems:

1. **Terrain shape** — `RRChunkGenerator.fillFromNoise` → `chunkgenerator/*Gen` (noise floors/ceilings) + `ArcaneStructureGen` (plates/pillars/runes). Biomes do **not** carve the stacked caves.
2. **Decoration** — vanilla feature pipeline after terrain: biome → placed features.

Biome pick by Y (+ noise within layer): `RRBiomeSource.getNoiseBiome`. Layer biome lists wired in `RRBiomeSource.newDefault` (also used by `RRDimension.bootstrapStem`).

```
RRDimension (stem + type)
    └─ RRChunkGenerator + RRBiomeSource
         ├─ terrain: TopLayerAndBloomingCavesGen / DeepCaves* / LostCaves* / VoidGen / ArcaneStructureGen
         └─ biomes: RRBiomes → biomes/<layer>/*.java → RRPlacedFeatures
```

## Feature pipeline (add decoration here)

```
Feature class          → register type     → configure (blocks/params) → place (count/height/scan) → attach to biome
dimension/features/X   RRFeatures          RRConfiguredFeatures          RRPlacedFeatures           biomes/... + RRBiomes
```

Order matters in datagen (`DatagenMain.DATAPACK_REGISTRY_BUILDER`): configured → placed → structures → biomes → dimension.

| Step | File | What to do |
|------|------|------------|
| 1 | `features/FooFeature.java` | Implement `Feature` + config |
| 2 | `RRFeatures` | `REGISTRY.register(...)` (runtime) |
| 3 | `RRConfiguredFeatures` | Key + `bootstrap` entry |
| 4 | `RRPlacedFeatures` | Key + placement; heights often use `Const.*` |
| 5 | `biomes/<layer>/Bar.java` | `generation.addFeature(..., RRPlacedFeatures.FOO)` |
| 6 | `RRBiomes` | Key + `ctx.register` if new biome |

Custom placement mods: `placements/` + `RRPlacementModifierTypes`.

Vanilla features OK in configured/placed (e.g. `Feature.BLOCK_COLUMN`, `CavePlacements.*`).

## Biomes (add biome here)

1. Class under `dimension/biomes/<layer>/` with `bootstrap(placedFeatures, carvers)` → builds `Biome` (mobs, effects, `addFeature`).
2. Key + register in `RRBiomes`.
3. Put holder into the right list in `RRBiomeSource.newDefault` or it never spawns.
4. Tags (structures etc.): `RRBiomeTags` + `DatagenBiomeTagProvider`.

Layer folders match vertical bands: `toplayer`, `bloomingcaves` / `bloomingcavesceiling`, `deepcaves` / `deepcavesceiling`, `lostcaves` / `lostcavesceiling`.

## Structures (separate from Features)

For large/jigsaw-style pieces (e.g. Giant Goblet):

- Type: `RRStructureTypes` + `structures/*Structure.java` / `*Piece.java` + `RRStructurePieceTypes`
- Datapack: `RRStructures` + `RRStructureSets`
- Biome filter via tag (`RRBiomeTags.HAS_GIANT_GOBLET`)

Giant Goblet is a structure (`GiantGobletStructure` / `GiantGobletPiece`), not a feature.

## Datagen

After changing Java bootstrap (`datagen/*`, worldgen registries, block models/tags/loot), the agent must run datagen itself. Do not skip it and do not ask the user to type `gradlew` unless datagen cannot run.

`runClient` / `runServer` lock `build/` — datagen cannot run in parallel. **Never** kill Java/Gradle, never `gradlew --stop`, never start or restart the game.

If `runData` fails because the client is still running (file-in-use / unable to delete / lock timeout): tell the user Minecraft is locking `build/`, ask them to close the game, and have them run datagen and the client themselves. Give these exact commands:

```
.\gradlew.bat runData
.\gradlew.bat runClient
```

Do not wait for the game to close. Do not hand-write `src/generated/**`.

Reuse caches: never `clean`, `--rerun-tasks`, `--refresh-dependencies`, or `--offline`. Do not delete `src/generated/` (Minecraft’s incremental cache is `src/generated/resources/.cache`).

NFRT (Minecraft assets + decompile) is pinned to `%USERPROFILE%\.gradle\caches\neoformruntime` in `build.gradle` so Cursor sandbox cannot force a full redownload. If `downloadAssets` still starts thousands of downloads, stop — the pin failed.

## Content outside worldgen

- Blocks/items: `RRBlocks`, `RRItems` (+ lang under `resources/assets/runeruin/lang/`)
- Models/loot/recipes/tags: `datagen/*`
- Teleport item: `items/RuneOfSpaceItem`
- Region export commands: `region/RegionCommands` → `exports/`; each `/rrexport` JSON includes the dimension seed as `worldSeed`
- Headless preview: `preview/PreviewJobs` → `exports/preview_*.txt` (same `runeruin.region/1` format)

## Headless structure / feature preview

Generates a **named job** into an in-memory world (no terrain, no client) and writes `exports/` in the same format as `/rrexport`. The job is chosen at launch — do not hardcode Giant Goblet when testing something else.

```
.\gradlew.bat runPreview -Ppreview=list
.\gradlew.bat runPreview -Ppreview=giant_goblet
.\gradlew.bat runPreview -Ppreview=giant_goblet -Pseed=42 -Pheight=75 -Pradius=40
.\gradlew.bat runPreview -Ppreview=boulder -Pseed=3 -Pradius=10
.\gradlew.bat runPreview -Ppreview=monolith -Pradius=6
```

Default job is `giant_goblet` if `-Ppreview` is omitted. Extra params: `-Pheight` `-Pradius` `-Pseed` `-PpreviewName=…`, or any `-Parg.<key>=<value>` (becomes `runeruin.preview.<key>`).

Outputs (name defaults to `preview_<job>`):

- `exports/preview_<job>.json` — full volume
- `exports/preview_<job>_yz.txt` — midplane looking +X
- `exports/preview_<job>_xy.txt` / `_xz.txt` — the other midplanes
- `exports/preview_<job>_info.txt` — seed, params, block counts

For terrain-generator experiments, keep two full JSON exports with matching seed/dimension/origin/size: `was` is the untouched world region; `expected` is the user's edited target. Compare them first, then replay `was` before changing code to prove the current generator reproduces the original terrain. Only after that baseline is close, edit the generator and iterate toward `expected`:

```powershell
python .agents/skills/region-export-compare/scripts/compare_region.py exports/was.json exports/expected.json
.\gradlew.bat runPreview -Ppreview=world_region '-Pregion=exports/was.json' -Pstage=generated
python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated.json exports/was.json
python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated.json exports/expected.json
.\gradlew.bat runPreview -Ppreview=world_region '-Pregion=exports/was.json' -Pstage=generated_after_modify
python .agents/skills/region-export-compare/scripts/compare_region.py exports/was_generated.json exports/expected.json exports/was_generated_after_modify.json
python .agents/skills/region-export-compare/scripts/compare_terrain.py exports/was_generated_after_modify.json exports/expected.json
```

`world_region` replays `RRChunkGenerator.fillFromNoise` into temporary in-memory chunks using the exact chunk bounds and seed in the export. This runs terrain and arcane plates, not carvers, biome features (including ores/vegetation), or structure placement. Before editing, require the terrain-only `generated` result to be nearly identical to `was`; if not, investigate the replay inputs/version before tuning. Keep `generated` as the baseline; compare its distance to `expected`, then compare `generated_after_modify` to `expected` on each iteration. For a full in-game export versus a terrain-only replay, run the regular `compare_region.py` raw diff and then `compare_terrain.py` to report terrain agreement while masking known post-terrain feature blocks; always report both results because features can replace terrain blocks.

In-game: `/rrpreview list` or `/rrpreview <job> [seed] [name] [k=v]…` (e.g. `/rrpreview boulder 3 radius=10`).

`runPreview` / `runGameTestServer` lock `build/` — do not run them while the client is open. If the lock fails, ask the user to close the game; do not kill Java.

To add a new testable shape: class in `preview/jobs/` implementing `PreviewJob`, then `PreviewCatalog.register(...)`. For write-only features, construct config and call `PreviewJobs.placeFeature`. If the feature scans terrain, `PreviewWorld.fillBox` a floor/ceiling first.

## Quick “where?”

| Want… | Open… |
|-------|--------|
| Vanilla / NeoForge API (this version) | `.mc-sources/` (run `extractMcSources`) |
| Layer Y heights | `Const` |
| Terrain look of a layer | matching `chunkgenerator/*Gen` |
| Which biome at Y | `RRBiomeSource` |
| Biome colors/mobs/features list | `biomes/<layer>/…` |
| New worldgen blob | `features/` → Features → Configured → Placed → biome |
| Dimension registration | `RRDimension` + `DatagenMain` |
| Pillars / arcane plates / runes | `ArcaneStructureGen`, `runes/Runes` |
