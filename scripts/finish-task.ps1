[CmdletBinding()]
param(
    # Task worktree to remove, for example ../RuneRuin-giant-goblet.
    [Parameter(Mandatory = $true)]
    [string] $Path
)

$ErrorActionPreference = 'Stop'

function Invoke-Git {
    & git @args
    if ($LASTEXITCODE -ne 0) {
        throw "git $($args -join ' ') failed with exit code $LASTEXITCODE."
    }
}

function Get-NormalizedPath([string] $Value) {
    return [System.IO.Path]::GetFullPath($Value.Replace('/', '\')).TrimEnd('\')
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$worktree = Get-NormalizedPath (Resolve-Path -LiteralPath $Path).Path

$worktrees = @(& git -C $projectRoot worktree list --porcelain |
    Where-Object { $_ -like 'worktree *' } |
    ForEach-Object { Get-NormalizedPath $_.Substring(9) })
if ($worktrees -notcontains $worktree) {
    throw "Not a worktree of this repository: $worktree"
}
if ($worktree -eq $worktrees[0]) {
    throw "Refusing to remove the main checkout: $worktree"
}
if ($worktree -eq (Get-NormalizedPath $projectRoot)) {
    throw 'Run finish-task.ps1 from another checkout, not from the worktree being removed.'
}

$branch = (& git -C $worktree branch --show-current).Trim()

# git deletes ignored directories recursively and follows junctions, so a linked
# .mc-sources or run would wipe the shared cache or game directory. Unlink them first.
Get-ChildItem -LiteralPath $worktree -Force -Directory |
    Where-Object { $_.Attributes -band [System.IO.FileAttributes]::ReparsePoint } |
    ForEach-Object {
        & cmd /c rmdir "$($_.FullName)"
        if ($LASTEXITCODE -ne 0) {
            throw "Could not unlink $($_.FullName)."
        }
        Write-Host "Unlinked $($_.FullName)"
    }

# Without --force git refuses to drop uncommitted or untracked work.
Invoke-Git -C $projectRoot worktree remove $worktree
Write-Host "Removed worktree $worktree"

if ($branch) {
    # -d (not -D) keeps branches that are not merged yet.
    Invoke-Git -C $projectRoot branch -d $branch
}
