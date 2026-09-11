[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

& (Join-Path $PSScriptRoot 'ensure-mc-sources.ps1')
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
