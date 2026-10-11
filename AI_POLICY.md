# AI usage policy

Rune Ruin is made with **agentic coding**: AI agents write code and help create the models and textures of blocks and items under a person's direction. Different models are used, such as Claude, ChatGPT and local models.

> [!IMPORTANT]
> **This project is not vibecoded.** By vibecoding I mean using AI in the style of "generate me a mod, make no mistakes". In this repository AI is a tool, and a human review always follows its work.

I, [@IoaNNUwU](https://github.com/IoaNNUwU), the author of the mod, want to be as open with its players as I can. AI is an incredible tool that lets people bring their dreams to life. I understand that there are questions about its ethics, but I hope that I can use it to make something good for people. I also understand that AI is a tool and not the main driver of creativity, so I use it where it fits and speeds the work up, but I do not let it take creative control away from me.

## The process

1. A person decides what to build. A new block or mechanic, or anything that changes existing worlds, starts with a design, and the person controls it.
2. An agent does the work as a tool in the person's hands: the person is responsible for everything it produces.
3. A person reviews every feature in the game. Reading the code is not required.
4. Only after this review is the change merged. Whoever opens a pull request answers for it as for their own work.

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

If something in the mod looks like AI slop to you, [open an issue](https://github.com/IoaNNUwU/runeruin/issues/new/choose): it is considered a bug.

## Quality control

- **Benchmarks**, run when a change may affect the speed of world generation: they show that generation did not get slower.
- **Previews** of features and structures, attached to the pull request.
- **Real human review** of every change in the game, in survival and creative mode.
- **Automatic checks** on every pull request: compilation, data generation, an asset lint, GameTests and the build. `main` takes only pull requests that pass them.
- **Rules for agents** in [AGENTS.md](AGENTS.md), written to the same standard: an agent knows that its pull request stays a draft until a person has reviewed the change. The human review is a required step.

## What AI does not do

All design decisions are made by a person. AI has no control over design.

## Open questions

Questions that may be controversial and are subject to change:

- If local AI models become able to reach the results that frontier models reach today, a full move to them can be considered, because local models are more ethical than frontier models.
- **Slop textures are acceptable during development.** There is an opinion on the internet that a picture drawn in Paint in a minute is better than an AI-generated texture. I do not agree: slop textures show what I have in mind more exactly and get me to the goal faster.
- Does an AI agent have a soul?

## No labels on commits

Commits and pull requests are not marked as made with AI one by one, and the git hooks refuse attribution lines such as `Co-Authored-By`. This means that every commit was made by a person, and that person is responsible for it. AI is a tool that can be replaced at any moment. The person who made the commit or the pull request is responsible for the work of AI, so we do not allow co-authorship with AI.

## Contributors

The same policy applies to everyone who sends a pull request. You may use any AI agent or none; if an agent did the work, review it yourself before you mark the pull request ready, as [CONTRIBUTING.md](CONTRIBUTING.md#pull-requests) says.
