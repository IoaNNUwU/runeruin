# AI usage policy

Rune Ruin is made with **agentic coding**: AI agents write code, textures and documentation under a person's direction. Different models are used, such as Claude, ChatGPT and local models.

> [!IMPORTANT]
> **This project is not vibecoded.** By vibecoding I mean using AI in the style of "generate me a mod, make no mistakes". In this repository AI is a tool, and a human review always follows its work.

I, [@IoaNNUwU](https://github.com/IoaNNUwU), the author of the mod, want to be as open with its players as I can, so I do not hide that AI is used here. Many people work to the same standard today: they use AI, always check its work and finish it themselves, but do not say openly that AI was involved. I would rather say it.

## The process

1. A person decides what to build. A new block or mechanic, or anything that changes existing worlds, starts with a design proposal, and the person approves it before any code is written.
2. An agent does the work in its own branch by the rules in [AGENTS.md](AGENTS.md), runs the checks and reports what it did, how it verified it, what to look at in the game and which decisions it made.
3. A person reviews every feature: they look at it in the game and read the report and the decisions. Reading the code is not required.
4. Only then is the pull request marked ready for review and merged. Whoever opens a pull request answers for it as for their own work.

## What AI may be used for

- Adding blocks and items and the behaviour they need.
- Creating structures and dungeons for world generation.
- Creating models and textures of blocks and items\*.

\* "Creating models and textures" means the first step only: a draft that is then reworked in [Blockbench](https://www.blockbench.net/). A person must study every model that AI generated and bring its textures to a finished state by hand. The work of AI on a model and its textures is never enough: a person finishes it.

The same process covers the rest of the work that agents do here: terrain, biomes and decoration, speed-ups, tests, build scripts, translations and documentation, this file included.

## The only criterion

In the age of AI there is a fair doubt: can these words be trusted? Was this texture really finished by a person? This repository has a single criterion:

> If something **LOOKS LIKE AI slop**, it will not be accepted.

So even when AI takes part in the work, a person answers for it that no model or texture looks cheap and generated. By this criterion it is the person who is responsible for every texture.

If something in the mod looks like AI slop to you, [open an issue](https://github.com/IoaNNUwU/runeruin/issues/new/choose): it is a bug.

## Quality control

- **Benchmarks** for changes to world generation: they show that generation did not get slower.
- **Previews** of features and structures, attached to the pull request.
- **Human review** of every change in the game.
- **Automatic checks** on every pull request: compilation, data generation, an asset lint, GameTests and the build. `main` takes only pull requests that pass them.
- **Rules for agents** in [AGENTS.md](AGENTS.md): for example, agents look Minecraft and NeoForge code up in the sources instead of recalling it, and the git hooks refuse their commits on `main`.

## What AI does not do

- It does not decide what goes into the mod: designs are approved by a person.
- It does not mark its own pull request ready for review and does not merge it before a person has approved its report.
- It does not push to `main`.

## No labels on commits

Commits and pull requests are not marked as made with AI one by one, and the git hooks refuse attribution lines such as `Co-Authored-By`. Assume that an agent took part in any change in this repository: this file is the disclosure.

## Contributors

The same policy applies to everyone who sends a pull request. You may use any AI agent or none; if an agent did the work, review it yourself before you mark the pull request ready, as [CONTRIBUTING.md](CONTRIBUTING.md#pull-requests) says.
