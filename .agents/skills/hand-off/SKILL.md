---
name: hand-off
description: >-
  Hand the models, mob projects and animations, textures and structure builds an agent drafted to the
  person who finishes them, before the final report. Use whenever a task made or changed such a file.
---

# Hand-off to a person

What an agent makes is a draft; a person finishes it ([AI_POLICY.md](../../../AI_POLICY.md)).

| What | Its file | How a person edits it |
|------|----------|-----------------------|
| model of a block or an item with a geometry of its own | `assets/runeruin/models/{block,item}/*.json` with `"format_version": "1.21.11"` | Blockbench opens the JSON and saves it with Ctrl+S |
| model of a mob | `assets/runeruin/models/entity/<name>.bbmodel`: `BlockbenchModel` builds the model of the game from it, the Java model only plays its animations | Blockbench |
| animations of a mob | the animations of its `.bbmodel`, exported into `client/model/<Model Identifier>Animation.java` | Blockbench, Animate mode, then File → Export → Export Modded Entity Animations (`mob` skill) |
| texture | PNG in `assets/runeruin/textures/` | Blockbench or any editor |
| man-made build: a ship, a hut, a dungeon room | text templates `data/runeruin/structure/<folder>/<name>.snbt` | loads it with `/rrload`, rebuilds it in the game and saves it back with `/rrsave` (`worldgen-structure` skill) |

Data generation builds no geometry. It writes blockstates, item definitions, models on vanilla parents (cube, cross, stairs) and the models derived from a file, such as a plant lifted onto moss (`datagen/models/ModelJson`).

## Steps

Once everything else is done and the checks have passed, before the final report:

1. Stop and send the person, in the chat, a link with the absolute path into the task worktree for every such file, one line per file (a block with several model files gets several lines), with what it shows and what to do with it: "open it in Blockbench" for a model or a mob project, "redraw it in Blockbench or any editor, at the same size" for a texture, the commands of the `worldgen-structure` skill for a build. Say which models are derived from a file and so need `runData` after it is saved. Add the renders of the draft and the single-line `runData` and `runClient` commands for the task worktree.
2. Wait. The pull request stays a draft until the person says that they have finished the files. Do not change those files meanwhile.
3. Run the checks again on the files as the person saved them. Read a file in place; do not rewrite or reformat it.
4. Only now write the final report, with the renders of the finished files. It names every file of step 1 again the same way: the link, what the file shows and what to do with it.
