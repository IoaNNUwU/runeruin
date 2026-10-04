---
name: benchmark
description: >-
  Measure RuneRuin generation speed before and after a change: terrain_bench and its before / after /
  gain table, the terrain checksum, the CPU profile, and /jfr in game. Benchmarks run only on the
  user's command. Use for any speed-up or performance task.
---

# Benchmark

Speed is measured as a table: before, after and gain. A speed-up is done only when the table shows it.

## Only on the user's command

Other programs on the computer (the game, other agents' Gradle runs, a browser) skew the numbers, so the user closes them first. Never run a benchmark on your own or as a check from the "Done" list. Prepare both checkouts, give the user the single-line commands, and run them yourself only when the user says the computer is ready.

## Before and after

`terrain_bench` times `RRChunkGenerator.fillFromNoise` on 289 chunks without the game (radius 8 around chunk 0 0, 40 rounds after 2 warm-up rounds); a run takes about a minute. It starts like a preview job, through `runPreview`. Every result is saved as `~/.runeruin/bench/<name>.properties`, outside the checkout, so a task folder can compare with a result measured in another one.

- **Before**: code without the change, that is `main` or the task folder before its first edit.
- **After**: the task branch.

Same computer, same parameters, one run right after the other:

```powershell
& 'C:\path\to\before\gradlew.bat' -p 'C:\path\to\before' runPreview -Ppreview=terrain_bench -PpreviewName=<task>_before
& 'C:\path\to\task\gradlew.bat' -p 'C:\path\to\task' runPreview -Ppreview=terrain_bench -PpreviewName=<task>_after '-Parg.before=<task>_before'
```

The second run prints the table and writes it to `exports/<task>_after.md` in its checkout:

| Terrain fill, seed 1, 289 chunks | Before: main | After: main_again | Gain |
|---|---:|---:|---:|
| One chunk at a time, ms per chunk | 3.01 | 3.16 | -5% |
| All chunks at once, ms | 138.03 | 137.73 | +0% |

- One chunk at a time: the work for one chunk. All chunks at once: all 289 chunks on the generator's executor, closer to the game.
- Gain = before / after − 1, how much faster it got; a slowdown is negative. Up to 5% (one at a time) and 10% (all at once) either way is noise: the example above is the same code measured twice.
- `Terrain: identical` compares a checksum of all blocks and heightmaps with the before run. A pure speed-up must keep it; `DIFFERENT` means the change altered worlds. The run also fails when parallel generation gives other blocks than sequential.
- Parameters: `-Pseed`, `-Pradius`, `'-Parg.rounds=<n>'`, `'-Parg.warmup=<n>'`. Before and after need the same seed and radius.

Put the table into the "Verified" section of the report and the pull request.

## Where the time goes

Below the table: medians, p90, the checksum and a command for the CPU profile `exports/<name>.jfr`:

```powershell
& '<jdk>\bin\jfr.exe' view hot-methods 'C:\path\to\exports\<name>.jfr'
```

It lists methods by CPU samples: noise (`FastNoise`), block writes (`setBlockState`, `PalettedContainer`), heightmaps (`Heightmap`), noise lookups (`LazyNoise`). Generation runs on the pool threads; samples on `Server thread` are chunk creation. For time by generator, open the file in JDK Mission Control.

## In game

`terrain_bench` times only terrain fill. Carvers, features and the scheduling of a running server need the game, so the user measures them: in a new world with the same seed each time (generated chunks are skipped),

```
/jfr start
/execute in runeruin:runeruin_dimension run neoforge generate start 0 300 0 16 false
/jfr stop
```

`/jfr stop` after the generation has finished. The report `~/.runeruin/game/debug/jfr-report-*.json` lists generation time by chunk status under `chunkGen`: `noise` is `fillFromNoise`, `carvers` is `applyCarvers`, `features` the decoration.
