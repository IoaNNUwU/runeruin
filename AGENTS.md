# RuneRuin — agent map

NeoForge mod `runeruin` for Minecraft 26.2.0 + NeoForge 26.2.0.59 (official Mojang mappings): a dimension `runeruin:runeruin_dimension` of stacked cave layers, bottom to top the Void, the Lost, Deep and Blooming caves and the Top layer (`/execute in runeruin:runeruin_dimension …`). `RuneRuinMod` registers the DeferredRegisters; datapack registries come from `DatagenMain`. Helpers: `RR.id` / `RR.resourceKey` / `RR.tagKey`.

Commands below are for Windows PowerShell. On Linux/macOS run the `scripts/*.ps1` with `pwsh` (PowerShell 7) and Gradle as `./gradlew`.

## Task protocol

Several agents work in this repository at once. Every change, documentation included, goes in its own task branch and worktree; never edit files in the shared main checkout.

1. Run `git branch --show-current` and `git status --short --branch`.
2. A task from a GitHub issue starts only if the issue is free: `gh issue view <n> --json closedByPullRequestsReferences,comments` shows no open pull request and no comment saying that someone is working on it. If it is taken, stop and tell the user.
3. Create the task worktree: `feature` or `bug`, a short snake_case name from the request, no `add_` prefix.

   ```powershell
   .\scripts\start-task.ps1 feature giant_goblet
   ```

   It creates `..\RuneRuin-giant-goblet` on `feature/giant_goblet` from the local `main`, links the shared Minecraft sources and runs `runData`. If the name is taken, choose another; never take over another agent's branch or worktree. In Claude Code, then call `EnterWorktree` with `path` set to the new worktree (not `name`, not the built-in worktree option). In the Claude desktop app, also add the new worktree to the session's folders (the `request_directory` tool with its path): the file pane opens only files from those folders.
4. Claim the issue at once, before any other work, with a draft pull request on an empty commit, as [CONTRIBUTING.md](CONTRIBUTING.md#making-a-change) shows: other agents see that the issue is taken only by that pull request. Then repeat the check from step 2: if another open pull request links the issue too, the one with the higher number gives way, so close yours (`gh pr close`) and tell the user. If you cannot claim the issue, tell the user before going on.
5. Work only inside the task worktree: edits, searches, Gradle. Links to files in replies are absolute paths into the task worktree: a relative link is resolved from the folder the session was started in, usually the main checkout, where the file is missing or is another version. Do not copy files or uncommitted changes between worktrees; never switch to a branch used by another worktree.
6. Follow-ups after a commit stay in the same branch and worktree until its pull request is merged. Start a new branch for an unrelated feature or bug, and for any work after the merge.
7. Before the final report, merge `main` into the task branch, resolve conflicts and repeat the checks.
8. Remove a worktree only when the user asks (a maintainer's agent also after the merge, [MAINTAINING.md](MAINTAINING.md#pull-requests)), and only with `.\scripts\finish-task.ps1 -Path ../RuneRuin-<name-with-dashes>`: `git worktree remove --force` and `gh pr merge --delete-branch` delete the shared sources through the `.mc-sources` junction. The script keeps a branch whose commits are neither pushed nor in `main`; after a squash merge, delete it with `git branch -D`.

`main` changes only through pull requests on GitHub: never commit on it, merge into it or push to it, and never use `--no-verify`. The hooks in `.githooks` enforce this and the branch names.

Work with GitHub, in this repository or in the user's fork (issues, pushing, pull requests), only when the user asks for it, in the chat or in their personal agent instructions (a task from an issue is such a request); then follow [CONTRIBUTING.md](CONTRIBUTING.md), and for a maintainer also [MAINTAINING.md](MAINTAINING.md).

## Questions, designs, reports

- A question ("can we…?", "why…?", "how would you…?") gets an answer, not code changes. Edit only after an explicit go-ahead.
- Propose a design (2–3 options and a recommendation) and wait before implementing a new block or mechanic, new blockstate properties, tiling textures, work touching more than ~5 files, or a change to existing worlds: adding, removing or reordering a biome list entry, renaming a noise seed string, changing `RRTerrainSurfaces` or a structure algorithm.
- Answer numbered follow-up requests point by point.

**Done** means every applicable check passed in the task worktree:

1. `gradlew.bat compileJava`
2. `gradlew.bat runData` — after any change to datagen, blocks/items, models, tags, loot, worldgen bootstrap or a structure template
3. `gradlew.bat runGameTestServer` — mod and datapack load, GameTests in `preview/RRGameTests`
4. a shape, model or texture: the renders its skill names, each looked at; models, textures, translations: `python scripts/lint_assets.py` after `runData`
5. `gradlew.bat build`

Lighting, render layers, interaction and biome placement need the game: list them under "Check in game".

**Final report**, also for partial work: in the chat, in the format of `.github/pull_request_template.md`, with the branch, the worktree path and the output of `git status --short --branch`. Pictures of a visible change follow [CONTRIBUTING.md](CONTRIBUTING.md#pictures): each view with its seed and parameters, before and after for something that already exists. Only after the user approves the changes and the Decisions does the report go into the pull request description, with the same pictures, and may the pull request become ready for review. End with single-line PowerShell commands using absolute paths to the task worktree, without `cd`: `runData` first if it is needed, then `& 'C:\path\to\worktree\gradlew.bat' -p 'C:\path\to\worktree' runClient`.

## What a person finishes

What an agent makes is a draft ([AI_POLICY.md](AI_POLICY.md)) that a person reworks in Blockbench or in the game. So a shape lives in a file a person can open (a model JSON, a mob's `.bbmodel`, a texture, a structure template), never in code that only an agent can change; only shapes of nature that differ with every seed (trees, mushrooms, spikes, the terrain) stay code. If you are not sure which it is, ask. Once such a file exists, change it in place: read it, change what the task is about and keep the rest; never write it anew from a script or from memory, which erases what the person changed. A task that made or changed such a file hands it to the person before the final report (`hand-off` skill).

## Commits and code

- Commit subject in the imperative, no prefix: `Make floating moss sink under creatures that stand still`. No attribution lines (`Co-Authored-By`, "Generated with …").
- Reduce code rather than grow it: fix existing code instead of adding more, change an algorithm instead of adding another condition, comment only important points. Keep the surrounding style; packing code onto one line is not less code.
- No mixin where the same can be done without one: a class of our own, an overridden method, a NeoForge event or hook, even when that takes more code. A mixin is only for vanilla behaviour that leaves no other way in, and its class comment says why.
- Shared files (registries, datagen providers, the creative tab, lang files) merge cleanly between parallel branches when a new entry goes next to related ones, not at the end, and imports are one per line, no wildcards (`import static …Const.*` included), sorted as plain text in one block, static imports in a block after it.

## Gradle, datagen and the game

| Task | Purpose |
|------|---------|
| `runClient` | game with the mod, started only by the user; game dir `~/.runeruin/game` (or `RUNERUIN_GAME_DIR`), shared by all checkouts |
| `runGameTestServer` | run GameTests, exit non-zero on failure |
| `runPreview` | headless shape export into `exports/` (`headless-preview` skill) |
| `runData` | datagen into `src/generated/resources` |
| `build` | mod jar |

- Never `clean`, `--rerun-tasks`, `--refresh-dependencies`, `--offline` or `--stop`; never kill Java or Gradle; never start, restart or wait for the game. A Claude Code hook blocks these, `runClient` included.
- `src/generated` is not in git. Never delete it or `src/generated/resources/.cache`, never hand-write `src/generated/**`: change the Java bootstrap and run `runData` yourself.
- A running game locks `build/` of the checkout it was started from. If Gradle fails on a locked file, tell the user the game is locking `build/`, ask them to close it, and give the single-line absolute `runData` and `runClient` commands for the task worktree.
- `build.gradle` pins NeoForm Runtime to `%USERPROFILE%\.gradle\caches\neoformruntime`. If `downloadAssets` starts downloading thousands of files, stop: the pin failed.
- Every run except `runClient` uses its own `<checkout>/run`, never the game directory.

## Minecraft and NeoForge sources

Do not use Minecraft or NeoForge APIs from memory. Search `.mc-sources/`, a junction to the shared cache `~/.runeruin/mc-sources/<version>`: Java in `code/` (`net/minecraft`, `com/mojang`, `net/neoforged`), vanilla textures and models in `textures/assets/minecraft/{textures,models}/`. Never edit it or copy it into `src/`.

- No `.mc-sources/VERSION.txt`: run `.\scripts\setup-worktree.ps1`. If it fails or the file is still missing, report the cause from the script's output to the user and stop the task.
- Ripgrep does not follow links while it walks the checkout, so a search from the root never reaches `.mc-sources`: pass it as the path (Grep with `path` `.mc-sources/code` or `.mc-sources/textures`, `rg <pattern> .mc-sources/code`) or use `rg -L`. Do not search `~/.gradle`.

## Skills

Procedures for kinds of work (blocks and items, mobs, textures, terrain, biomes, features, structures, previews, benchmarks, the hand-off to a person) are skills in `.agents/skills/`; load the matching one before such work. Claude Code reads stubs in `.claude/skills/` with the same `description`, kept in sync.

## Packages (`src/main/java/ioann/uwu/runeruin/`)

| Path | Role |
|------|------|
| `RuneRuinMod`, `RR`, `Config` | registration, id helpers, mod config |
| `blocks/`, `items/`, `entities/`, `particles/` | `RRBlocks` (also registers BlockItems), `RRItems`, `RREntityTypes`, `RRParticleTypes`; teleport item `RuneOfSpaceItem` |
| `creativetab/` | creative tab; its item list is manual |
| `datagen/` | blockstates, item definitions, models on vanilla parents and derived models, tags, loot, recipes, worldgen bootstrap (`DatagenMain`) |
| `client/` | renderers, mob models and their loader `BlockbenchModel`, tints, clouds (`RuneRuinClient`) |
| `portal/`, `loottables/`, `mixin/` | dimension portal and its `RRPoiTypes`, `RRLootTables`, vanilla tweaks |
| `region/` | `/rrpos1` `/rrpos2` `/rrexport` `/rrsave` `/rrload` `/rrgenerate` `/rrclear` |
| `preview/` | headless previews, `/rrpreview` and GameTests |
| `dimension/` | worldgen registries (`RRFeatures`, `RRConfiguredFeatures`, `RRPlacedFeatures`, `RRBiomes`, `RRBiomeSource`, `RRChunkGenerator`, `RRStructure*`, …), tags `RRTags`, `RRBiomeTags`, layer heights `Const`; below it `biomes/<layer>/`, `features/`, `placements/`, `chunkgenerator/`, `structures/`, `noise/`, `runes/` |

Translations are hand-written in both `assets/runeruin/lang/en_us.json` and `ru_ru.json`.
