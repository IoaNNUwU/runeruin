[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

$projectRoot = Split-Path -Parent $PSScriptRoot

Invoke-Native git -C $projectRoot config core.hooksPath .githooks

& (Join-Path $PSScriptRoot 'ensure-mc-sources.ps1')

# src/generated is not in git, so a fresh checkout has no blockstates, models or worldgen.
Invoke-Native (Get-GradleWrapper $projectRoot) -p $projectRoot runData
