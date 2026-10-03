# RuneRuin — agent map

NeoForge mod `runeruin` for Minecraft 26.2.0 + NeoForge 26.2.0.59 (official Mojang mappings): a stacked-cave dimension `runeruin:runeruin_dimension` (`/execute in runeruin:runeruin_dimension …`). `RuneRuinMod` registers the DeferredRegisters; datapack registries come from `DatagenMain`. Helpers: `RR.id` / `RR.resourceKey` / `RR.tagKey`.

Commands below are for Windows PowerShell. On Linux/macOS run the `scripts/*.ps1` with `pwsh` (PowerShell 7), e.g. `pwsh ./scripts/start-task.ps1 feature giant_goblet`, and Gradle as `./gradlew`.

## Issues and pull requests

All work is tracked in the issues of `IoaNNUwU/runeruin` and reaches its `main` only through pull requests. Use `gh` for both. **The maintainer** (`IoaNNUwU`) squash-merges pull requests (the PR title and body become the commit on `main`), decides on stages and closes issues. **The user** is whoever drives the agent: the maintainer or a contributor working in a fork.

Remotes: `upstream` is always `IoaNNUwU/runeruin` (`start-task.ps1` adds it, `gh` targets it); `origin` is where task branches are pushed, the repository itself or the contributor's fork.

Agents may, without asking: create issues and comment on them, push task branches to `origin`, open draft pull requests and edit them, change labels (only the maintainer has the permission). A PR becomes ready for review only after the user's explicit approval. Agents never merge a pull request, push to `main`, merge into `main` locally or close an issue.

| Group         | Labels                                                                                          |
|---------------|-------------------------------------------------------------------------------------------------|
| type          | `bug`, `enhancement`                                                                            |
| bug stage     | `needs triage` → `confirmed`                                                                    |
| feature stage | `idea` → `accepted`                                                                             |
| layer         | `layer: top`, `layer: blooming caves`, `layer: deep caves`, `layer: lost caves`, `layer: void`  |
| biome         | `biome: <name>`, added when first needed                                                        |
| infra         | `infra`: scripts, build, CI, agent docs                                                         |

- An issue has one type, one stage and every layer or biome label that applies. A new stage replaces the old one: `gh issue edit 12 --remove-label idea --add-label accepted`. Labels are defined in `.github/labels.json`; a missing one is added there in the PR that needs it.
- `needs triage` → `confirmed` once the bug is reproduced or the maintainer confirms it; `idea` → `accepted` only when the maintainer agrees. An issue the maintainer asks for directly is created as `confirmed` or `accepted`; a contributor's issue gets its labels from the maintainer.
- After the stage label the pull request tells the rest: an open draft PR with `Fixes #12` means in progress, a PR ready for review means waiting for merge, the merge closes the issue. Rejected issues are closed as "not planned".
- An issue with an open linked PR is taken. If the PR's last commit and comment are older than two weeks, it may be taken over once its author or the maintainer agrees: the maintainer in the chat, anyone else in a PR comment.
- A design proposal (see below) goes into an issue comment as well as the chat, so the choice stays in the issue.

Queries: `gh issue list --search "label:confirmed,accepted -linked:pr"` (free to take), `gh issue list --label "needs triage"`, `gh pr list --draft`.

## Task protocol

Several agents work in this repository at once. Every change, documentation included, goes in its own issue, task branch, worktree and pull request; never edit files in the shared main checkout.

1. Run `git branch --show-current` and `git status --short --branch`.
2. Find the issue (`gh issue view 12`, `gh issue list --search …`) or create it with its labels. Check that it is not taken.
3. Create the task worktree: `feature` or `bug`, a short snake_case name from the request, no `add_` prefix.

   ```powershell
   .\scripts\start-task.ps1 feature giant_goblet
   ```

   It adds and fetches `upstream`, creates `..\RuneRuin-giant-goblet` on `feature/giant_goblet` from `upstream/main`, links the shared Minecraft sources and runs `runData`. If the name is taken, choose another; never take over another agent's branch or worktree. In Claude Code, then call `EnterWorktree` with `path` set to the new worktree (not `name`, not the built-in worktree option).
4. Work only inside the task worktree: edits, searches, Gradle, file links in replies. Do not copy files or uncommitted changes between worktrees; never switch to a branch used by another worktree.
5. After the first commit run `git push -u origin HEAD` and open a draft PR titled like a commit subject, its body in the format of `.github/pull_request_template.md` with `Fixes #12`: `gh pr create --draft --title "…" --body-file <file>`. Push every later commit.
6. Before the final report: `git fetch upstream`, `git merge upstream/main`, resolve conflicts, repeat the checks and push. Give the report in the chat and wait: the user reviews the changes and the Decisions and explicitly approves them. Only then write the report into the PR body (`gh pr edit 34 --body-file <file>`) and mark it ready: `gh pr ready 34`. Requested changes are follow-ups (step 7).
7. Follow-ups stay in the same issue, branch, worktree and PR; while working on them, move a ready PR back to draft (`gh pr ready 34 --undo`). Start a new issue and branch only for an unrelated feature or bug; work after the merge also needs a new branch, since GitHub deletes the merged one.
8. Remove a worktree only when the user asks, after its PR is merged, with `.\scripts\finish-task.ps1 -Path ../RuneRuin-<name>`: it checks that a merged pull request contains the branch and unlinks `.mc-sources` first, whereas `git worktree remove --force` deletes the shared sources cache through the junction.

Git hooks in `.githooks` (enabled by the setup script) reject any commit on `main`, any push to `main`, branch names other than `feature/<snake_case>` / `bug/<snake_case>`, a detached `HEAD`, and attribution lines. Never use `--no-verify`.

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
6. CI on the pushed PR: `gh pr checks 34 --watch`

Lighting, render layers, interaction and biome placement need the game: list them under "Check in game".

**Final report**, also for partial work: first in the chat, in the format of `.github/pull_request_template.md` (Done, Verified, Check in game, Decisions) with the PR link, the branch, the worktree path and the output of `git status --short --branch`; after the user approves it, also in the PR body, so it outlives the chat. End the chat report with single-line PowerShell commands using absolute paths to the task worktree, without `cd`: `runData` first if it is needed, then `& 'C:\path\to\worktree\gradlew.bat' -p 'C:\path\to\worktree' runClient`.

## Commits and code

- Commit subject and PR title in the imperative, no prefix: `Make floating moss sink under creatures that stand still`. No attribution lines (`Co-Authored-By`, "Generated with …").
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

A new noise takes its name into the seed: `LazyNoise.single(name, frequency)` or `Noise.hashString(name + seed)`. A noise built from the bare world seed repeats every other such noise; the lost and blooming cave floors once had the same relief that way.

Procedures for features, biomes and structures: the `worldgen-*` skills.
