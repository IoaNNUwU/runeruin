#!/bin/sh
# PreToolUse guard for Bash/PowerShell: blocks commands AGENTS.md forbids.
# Matches the raw hook JSON, so backslashes in paths arrive doubled.
# Word boundaries are spelled out as character classes: BSD grep (macOS) has no \b.

input=$(cat)

block() {
    echo "Blocked: $1. See AGENTS.md (Gradle, datagen and the game)." >&2
    exit 2
}

matches() {
    printf '%s' "$input" | grep -qiE "$1"
}

matches 'gradlew[^|;&]*[[:space:]](clean|--rerun-tasks|--refresh-dependencies|--offline|--stop)([^[:alnum:]_-]|$)' \
    && block "clean, --rerun-tasks, --refresh-dependencies, --offline and --stop throw away shared Gradle caches or the user's daemons"
matches '(^|[^[:alnum:]_-])(taskkill|stop-process|pkill|killall|kill)[^[:alnum:]_-][^|;&]*[^[:alnum:]_]java' \
    || matches '[^[:alnum:]_]java(w|\.exe)?[^[:alnum:]_.][^;&]*\|[[:space:]]*stop-process' \
    && block "never kill Java or Gradle: the user's game may be running"
matches '(^|[^[:alnum:]_-])(rm|rmdir|rd|del|remove-item)[^[:alnum:]_-][^|;&]*src[\\/]+generated' \
    && block "never delete src/generated or its .cache; run runData instead"

exit 0
