[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('feature', 'bug', IgnoreCase = $false)]
    [string] $Type,

    # snake_case task name, for example giant_goblet. (?-i): PowerShell matches case-insensitively by default.
    [Parameter(Mandatory = $true)]
    [ValidatePattern('(?-i)^[a-z0-9]+(_[a-z0-9]+)*$')]
    [string] $Name
)

# No 'Stop': Windows PowerShell turns git/Gradle stderr into errors when output is redirected.
# Every native call checks its exit code instead.
$projectRoot = Split-Path -Parent $PSScriptRoot
$branch = "$Type/$Name"
$worktree = Join-Path (Split-Path -Parent $projectRoot) ('RuneRuin-' + $Name.Replace('_', '-'))

if (Test-Path -LiteralPath $worktree) {
    throw "Directory already exists, choose another task name: $worktree"
}

& git -C $projectRoot worktree add -b $branch $worktree HEAD
if ($LASTEXITCODE -ne 0) {
    throw "Could not create $branch at $worktree. If the branch exists, choose another task name."
}

# The worktree's own copy: hooks path, shared .mc-sources link, .vanilla-textures.
& (Join-Path $worktree 'scripts/setup-codex-worktree.ps1')

# src/generated is not in git, so a fresh worktree has no blockstates, models or worldgen.
& (Join-Path $worktree $(if ($env:OS -eq 'Windows_NT') { 'gradlew.bat' } else { 'gradlew' })) -p $worktree runData
if ($LASTEXITCODE -ne 0) {
    throw "runData failed in $worktree."
}

& git -C $worktree status --short --branch
Write-Host "Task worktree ready: $worktree"
