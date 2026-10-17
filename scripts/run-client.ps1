[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

$projectRoot = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot 'setup-worktree.ps1')

Invoke-Native (Get-GradleWrapper $projectRoot) -p $projectRoot '--gradle-user-home' (Join-Path (Get-UserHome) '.gradle') runClient
