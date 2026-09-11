[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$userHome = [Environment]::GetFolderPath('UserProfile')
if ([string]::IsNullOrWhiteSpace($userHome)) {
    $userHome = $env:USERPROFILE
}
if ([string]::IsNullOrWhiteSpace($userHome)) {
    throw 'Could not determine the current user profile directory.'
}

$gradleUserHome = Join-Path $userHome '.gradle'
$wrapper = Join-Path $projectRoot 'gradlew.bat'
if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
    $wrapper = Join-Path $projectRoot 'gradlew'
}

& (Join-Path $PSScriptRoot 'setup-codex-worktree.ps1')
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Push-Location $projectRoot
try {
    & $wrapper '--gradle-user-home' $gradleUserHome 'runClient'
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $exitCode
