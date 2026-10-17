# Maintaining Rune Ruin

How maintainers and the AI agents working for them handle issues and pull requests. Everyone else follows [CONTRIBUTING.md](CONTRIBUTING.md); this file only adds what maintainers do on top of it. Agents read it only when their personal instructions say that the person they work for is a maintainer.

Maintainers: [@IoaNNUwU](https://github.com/IoaNNUwU).

## Remotes

A maintainer clones the main repository itself, without a fork, so `origin` is the main repository and there is no `upstream`. Wherever [CONTRIBUTING.md](CONTRIBUTING.md) says `upstream`, use `origin`: `git pull --ff-only origin main`, `git merge origin/main`. Never add an `upstream` remote: with two remotes for the same repository, `gh pr create` looks for the pushed branch under `upstream` and refuses to open the pull request.

## Issues and labels

- Agents work with GitHub by default, without asking for each step: they create issues and label them, push task branches, and open and edit draft pull requests. A comment on an issue needs the maintainer's agreement in the chat, as for everyone ([CONTRIBUTING.md](CONTRIBUTING.md#working-with-an-ai-agent)).
- A new label is created on GitHub right away (`gh label create`) and added to [.github/labels.json](.github/labels.json) in the next pull request.
- Only on an explicit request: closing issues and changing repository settings. Nobody pushes to `main`.

## Pull requests

- A pull request becomes ready for review only after the maintainer approves the agent's report.
- A pull request by the maintainer the agent works for, including one the agent opened from their account, is merged right after the maintainer approves its report, once CI is green and the latest `main` is merged in. Pull requests by anyone else are merged only on an explicit request.
- Merge with `gh pr merge <number> --squash`, or with `--auto` while CI still runs: `main` takes a pull request only with a green `build` check and the latest `main` merged in. GitHub deletes merged branches itself. Never pass `--delete-branch` ([AGENTS.md](AGENTS.md#task-protocol), step 8).
- After the approval the agent always watches the pull request until it is merged, then updates `main` in the main checkout (`git pull --ff-only`).
- When the conversation was about this one task, the agent then removes its task folder with `.\scripts\finish-task.ps1` without asking. If the script refuses (uncommitted or untracked files, a running game that locks `build/`), the agent reports it and forces nothing. After a conversation about several tasks, folders are removed when the maintainer asks.

## Personal agent instructions

A maintainer turns this file on in their personal agent instructions: `~/.claude/CLAUDE.md` for Claude Code, `~/.config/opencode/AGENTS.md` for OpenCode (it also reads `~/.claude/CLAUDE.md` when that file is missing), `~/.codex/AGENTS.md` for Codex.

```markdown
## Rune Ruin

I am @<login>, a maintainer of github.com/IoaNNUwU/runeruin. In `RuneRuin` and its task folders `RuneRuin-*`, follow MAINTAINING.md.
```
