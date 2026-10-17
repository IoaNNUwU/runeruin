[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('feature', 'bug', IgnoreCase = $false)]
    [string] $Type,

    # snake_case task name without an add_ prefix, for example giant_goblet. (?-i): PowerShell matches case-insensitively by default.
    [Parameter(Mandatory = $true)]
    [ValidatePattern('(?-i)^(?!add_)[a-z0-9]+(_[a-z0-9]+)*$')]
    [string] $Name
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

$projectRoot = Split-Path -Parent $PSScriptRoot
$branch = "$Type/$Name"
$worktree = Join-Path (Split-Path -Parent $projectRoot) ('RuneRuin-' + $Name.Replace('_', '-'))

if (Test-Path -LiteralPath $worktree) {
    throw "Directory already exists, choose another task name: $worktree"
}

try {
    Invoke-Native git -C $projectRoot worktree add -b $branch $worktree main
}
catch {
    throw "Could not create $branch at $worktree. If the branch exists, choose another task name."
}

# The worktree's own setup: hooks path, link to the shared .mc-sources, src/generated.
& (Join-Path $worktree 'scripts/setup-worktree.ps1')

& git -C $worktree status --short --branch
Write-Host "Task worktree ready: $worktree"
