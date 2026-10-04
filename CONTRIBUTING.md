# Contributing to Rune Ruin

Thanks for wanting to help! This file explains how work moves through GitHub: issues, branches and pull requests. How to build, generate data and check the mod is described in [AGENTS.md](AGENTS.md). It is written for AI coding agents, but its checklists are the same for people.

The maintainer is [@IoaNNUwU](https://github.com/IoaNNUwU): they triage issues, review pull requests and merge them.

## In short

1. Pick an issue that is free to take, or open a new one. A small obvious fix can skip this step.
2. Fork the repository and clone your fork.
3. Make a branch, do the work, run the checks.
4. Open a pull request into `main`, with `Fixes #<issue>` in its description if there is an issue.
5. The maintainer reviews it and squash-merges it.

## Issues

Work starts with an issue, so that everyone can see what is planned, what is taken and what is done. Use the issue forms: a bug report or a feature idea. A small change that needs no discussion (a typo, documentation, an obvious fix, an urgent hotfix) can go straight to a pull request; anything that needs a design proposal (see [AGENTS.md](AGENTS.md)) starts with an issue.

Labels show where an issue stands:

| Group         | Labels                                                                    | Meaning                                       |
|---------------|---------------------------------------------------------------------------|-----------------------------------------------|
| type          | `bug`, `enhancement`                                                      | something broken or something new             |
| bug stage     | `needs triage` → `confirmed`                                              | reported, then reproduced                     |
| feature stage | `idea` → `accepted`                                                       | proposed, then agreed with the maintainer     |
| layer         | `layer: top`, `layer: blooming caves`, `layer: deep caves`, …             | the part of the dimension it touches          |
| biome         | `biome: <name>`                                                           | a specific biome                              |
| infra         | `infra`                                                                   | scripts, build, CI, agent docs                |
| perf          | `perf`                                                                    | speed and memory use                          |

Only the maintainer can change labels. A bug becomes `confirmed` once it is reproduced, a feature becomes `accepted` once the maintainer agrees with it. Issues that will not be done are closed with the reason "not planned".

What happens after `confirmed` or `accepted` is shown by the pull request, not by a label: an open draft pull request means someone is working on it, a pull request ready for review means it waits for the maintainer, and the merge closes the issue.

Labels live in [.github/labels.json](.github/labels.json), and a workflow applies them after every merge. If you need a new one, for example a `biome:` label, add it there in your pull request.

**Free to take** are issues labelled [`confirmed` or `accepted` without a linked pull request](https://github.com/IoaNNUwU/runeruin/issues?q=is%3Aissue+is%3Aopen+label%3Aconfirmed%2Caccepted+-linked%3Apr). An issue with an open pull request is taken. If that pull request has had no commits or comments for two weeks, ask in it whether the work is abandoned, and take it over once its author or the maintainer agrees.

## Setting up

1. Fork the repository on GitHub and clone your fork. Your fork is `origin`. Add the main repository as `upstream`:

   ```powershell
   git remote add upstream https://github.com/IoaNNUwU/runeruin.git
   ```

2. Run the setup script once. It enables the git hooks and links the Minecraft and NeoForge sources:

   ```powershell
   .\scripts\setup-codex-worktree.ps1
   ```

   Gradle needs any installed JDK; the wrapper downloads Java 25 itself. On Linux or macOS run the scripts with `pwsh`.

## Making a change

Never commit to `main`: it only changes through merged pull requests, and the hooks refuse commits on it. Before starting, bring your `main` up to date:

```powershell
git switch main
git pull --ff-only upstream main
```

Branches are named `feature/<snake_case>` or `bug/<snake_case>`, for example `feature/ice_biome`. The easiest way to start is

```powershell
.\scripts\start-task.ps1 feature ice_biome
```

It creates the branch from your `main` in a separate folder (`..\RuneRuin-ice-biome`) with the Minecraft sources linked and data generated, so several tasks can live side by side. A plain `git switch -c feature/ice_biome` works too.

Commit messages are short and in the imperative, without prefixes: `Add an ice biome to the lost caves`. Before asking for a review, run the checks from the "Done" list in [AGENTS.md](AGENTS.md) that apply to your change, and look at anything visual in the game (`gradlew runClient`).

## Pull requests

Push your branch to your fork and open a pull request into `main` of `IoaNNUwU/runeruin`:

```powershell
git push -u origin HEAD
gh pr create --draft
```

- Open it as a **draft** early: it shows that the issue is taken.
- The **title** reads like a commit subject, and the **description** follows the template: `Fixes #<issue>` (drop the line when there is no issue), what was done, how it was verified, what to check in the game and the decisions made along the way. The title and the description become the commit on `main`, so write them for someone reading the history a year from now.
- Before marking it **ready for review**, merge the latest `main` (`git fetch upstream`, then `git merge upstream/main`), run the checks again and make sure CI is green.
- If an AI agent did the work, review its changes and its decisions yourself before marking the pull request ready. By doing so you take responsibility for them, as for your own code.
- The maintainer reviews the pull request and squash-merges it. The branch is deleted after the merge, so further work goes into a new branch.

When you are done, update your `main` again and remove the task folder with `.\scripts\finish-task.ps1 -Path ..\RuneRuin-ice-biome`. Do not delete it with `git worktree remove --force`: that would follow the link and wipe the shared Minecraft sources.

## Working with an AI agent

Agents read [AGENTS.md](AGENTS.md) and by default only work on your computer: they create a branch in a separate folder, commit, run the checks and report in the chat. They touch GitHub only when you ask them to, and then they follow this file:

- They may create issues and comment on them, push the task branch to `origin`, and open and edit draft pull requests. Right after the first commit they push it and open a draft pull request with `Fixes #<issue>`, so the issue shows as taken. Without an issue they work only on a change that needs no design proposal.
- A design proposal goes into the issue as a comment as well as into the chat, so the decision stays with the issue.
- The final report goes into the chat first. Only after you approve it does it go into the pull request description, and only then is the pull request marked ready (`gh pr ready`).
- They never merge pull requests, push to `main` or close issues.

`gh` uses `upstream` as the main repository automatically, so `gh issue list` and `gh pr create` work from a fork without extra setup.
