[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

& git config core.hooksPath .githooks
if ($LASTEXITCODE -ne 0) {
    throw 'Could not configure the repository pre-commit hook path.'
}

& (Join-Path $PSScriptRoot 'ensure-mc-sources.ps1')
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
