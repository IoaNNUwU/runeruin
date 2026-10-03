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
    # On Windows GetFullPath also turns git's C:/... into C:\...
    return [System.IO.Path]::GetFullPath($Value).TrimEnd([System.IO.Path]::DirectorySeparatorChar)
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

# Pull requests are merged on GitHub, maybe squashed or rebased, so ask GitHub whether the last commit was merged.
if ($branch) {
    $head = (& git -C $worktree rev-parse HEAD).Trim()
    $merged = & gh pr list --repo IoaNNUwU/runeruin --state merged --search $head --json number --jq length
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not query pull requests with gh.'
    }
    if ($merged -eq '0') {
        throw "No merged pull request contains $branch at $head."
    }
}

# Git for Windows follows junctions while deleting ignored directories, so a linked
# .mc-sources or run would wipe the shared cache or game directory. Unlink them first.
Get-ChildItem -LiteralPath $worktree -Force |
    Where-Object { $_.Attributes -band [System.IO.FileAttributes]::ReparsePoint } |
    ForEach-Object {
        # Non-recursive delete removes a junction or symlink itself, never its target.
        $_.Delete()
        Write-Host "Unlinked $($_.FullName)"
    }

# Without --force git refuses to drop uncommitted or untracked work.
Invoke-Git -C $projectRoot worktree remove $worktree
Write-Host "Removed worktree $worktree"

if ($branch) {
    # -D: -d cannot see a merge made on GitHub; the merged pull request was checked above.
    Invoke-Git -C $projectRoot branch -D $branch
}
