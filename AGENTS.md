# RuneRuin — agent map

NeoForge mod `runeruin` for Minecraft 26.2.0 + NeoForge 26.2.0.59 (official Mojang mappings): a stacked-cave dimension `runeruin:runeruin_dimension` (`/execute in runeruin:runeruin_dimension …`). `RuneRuinMod` registers the DeferredRegisters; datapack registries come from `DatagenMain`. Helpers: `RR.id` / `RR.resourceKey` / `RR.tagKey`.

Commands below are for Windows PowerShell. On Linux/macOS run the `scripts/*.ps1` with `pwsh` (PowerShell 7), e.g. `pwsh ./scripts/start-task.ps1 feature giant_goblet`, and Gradle as `./gradlew`.

## Task protocol

Several agents work in this repository at once. Every change, documentation included, goes in its own task branch and worktree; never edit files in the shared main checkout.

1. Run `git branch --show-current` and `git status --short --branch`.
2. A task from a GitHub issue starts only if the issue is free: `gh issue view <n> --json closedByPullRequestsReferences,comments` shows no open pull request and no comment saying that someone is working on it. If it is taken, stop and tell the user.
3. Create the task worktree: `feature` or `bug`, a short snake_case name from the request, no `add_` prefix.

   ```powershell
   .\scripts\start-task.ps1 feature giant_goblet
   ```

   It creates `..\RuneRuin-giant-goblet` on `feature/giant_goblet` from the local `main`, links the shared Minecraft sources and runs `runData`. If the name is taken, choose another; never take over another agent's branch or worktree. In Claude Code, then call `EnterWorktree` with `path` set to the new worktree (not `name`, not the built-in worktree option). In the Claude desktop app, also add the new worktree to the session's folders (the `request_directory` tool with its path): the file pane opens only files from those folders.
4. Claim the issue at once, before any other work: other agents see that it is taken only by its draft pull request. A pull request needs a commit, so start with an empty one:

   ```powershell
   git commit --allow-empty -m "<issue title>"
   git push -u origin HEAD
   gh pr create --draft --title "<issue title>" --body "Fixes #<n>`n`nThe issue has been claimed, but there are no changes yet."
   ```

   Then repeat the check from step 2: if another open pull request links the issue too, the one with the higher number gives way, so close yours (`gh pr close`) and tell the user. If you cannot claim the issue, tell the user before going on.
5. Work only inside the task worktree: edits, searches, Gradle. Links to files in replies are absolute paths into the task worktree: a relative link is resolved from the folder the session was started in, usually the main checkout, where the file is missing or is another version. Do not copy files or uncommitted changes between worktrees; never switch to a branch used by another worktree.
6. Follow-ups after a commit stay in the same branch and worktree. Start a new branch only for an unrelated feature or bug.
7. Before the final report, merge `main` into the task branch, resolve conflicts and repeat the checks.
8. Remove a worktree only when the user asks, with `.\scripts\finish-task.ps1 -Path ../RuneRuin-<name>`: it unlinks `.mc-sources` first, whereas `git worktree remove --force` and `gh pr merge --delete-branch` delete the shared sources cache through the junction, and it keeps the branch unless its commits are pushed or in `main`.

`main` changes only through pull requests on GitHub: never commit on it, merge into it or push to it. Git hooks in `.githooks` (enabled by the setup script) reject commits on `main`, pushes to `main`, branch names other than `feature/<snake_case>` / `bug/<snake_case>`, a detached `HEAD`, and attribution lines. Never use `--no-verify`.

Work with GitHub, in this repository or in the user's fork (issues, pushing, pull requests), only when the user asks for it, in the chat or in their personal agent instructions (a task from an issue is such a request); then follow [CONTRIBUTING.md](CONTRIBUTING.md), and for a maintainer also [MAINTAINING.md](MAINTAINING.md).

## Questions, designs, reports

- A question ("can we…?", "why…?", "how would you…?") gets an answer, not code changes. Edit only after an explicit go-ahead.
- Propose a design (2–3 options and a recommendation) and wait before implementing a new block or mechanic, new blockstate properties, tiling textures, anything that changes existing worlds (see Worldgen) or work touching more than ~5 files.
- Answer numbered follow-up requests point by point.

**Done** means every applicable check passed in the task worktree:

1. `gradlew.bat compileJava`
2. `gradlew.bat runData` — after any change to datagen, blocks/items, models, tags, loot or worldgen bootstrap
3. `gradlew.bat runGameTestServer` — mod and datapack load, GameTests in `preview/RRGameTests`
4. shapes: `runPreview` of the matching job and pictures from `scripts/render_preview.py` that you have looked at (`headless-preview` skill); models, textures, translations: `python scripts/lint_assets.py` after `runData`, and renders from `scripts/render_model.py` / `scripts/render_texture_tile.py`
5. `gradlew.bat build`

Lighting, render layers, interaction and biome placement need the game: list them under "Check in game". Speed benchmarks (`benchmark` skill) are not a check: they run only on the user's command, once the user has closed other programs.

**Final report**, also for partial work: in the chat, in the format of `.github/pull_request_template.md` (Done, Verified, Check in game, Decisions), with the branch, the worktree path and the output of `git status --short --branch`. After a change to the shape of a feature or structure, show its preview pictures in the chat with their seed and parameters, before and after for an existing shape. The user reviews the changes and the Decisions and approves them; only then may a pull request become ready for review. End with single-line PowerShell commands using absolute paths to the task worktree, without `cd`: `runData` first if it is needed, then `& 'C:\path\to\worktree\gradlew.bat' -p 'C:\path\to\worktree' runClient`.

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
| `benchmark` | measuring generation speed before and after a change; only on the user's command |
| `region-export-compare` | `/rrexport` dumps and terrain replay (`world_region`, was → expected) |
| `minecraft-pixel-texture-generation` | creating, recoloring, quantizing and validating textures |
| `minecraft-model-texture-analysis` | model geometry and texture UVs |

## Packages (`src/main/java/ioann/uwu/runeruin/`)

| Path | Role |
|------|------|
| `blocks/`, `items/`, `entities/`, `particles/` | `RRBlocks` (also registers BlockItems), `RRItems`, `RREntityTypes`, `RRParticleTypes`; teleport item `RuneOfSpaceItem` |
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

A new noise takes its name into the seed: `LazyNoise.single(name, frequency)` or `Noise.hashString(name + seed)`. A noise built from the bare world seed repeats every other such noise; the lost and blooming cave floors once had the same relief that way.

Procedures for features, biomes and structures: the `worldgen-*` skills.
