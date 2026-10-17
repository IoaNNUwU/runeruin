# Helpers that the other scripts dot-source.

function Get-UserHome {
    $userHome = [Environment]::GetFolderPath('UserProfile')
    if ([string]::IsNullOrWhiteSpace($userHome)) {
        $userHome = $env:USERPROFILE
    }
    if ([string]::IsNullOrWhiteSpace($userHome)) {
        throw 'Could not determine the current user profile directory.'
    }
    return $userHome
}

# Every checkout has both wrappers, so pick by OS.
function Get-GradleWrapper([string] $ProjectRoot) {
    return Join-Path $ProjectRoot $(if ($env:OS -eq 'Windows_NT') { 'gradlew.bat' } else { 'gradlew' })
}

# Runs a native command and throws on a non-zero exit code. 'Continue' inside: with 'Stop', Windows PowerShell
# turns the command's stderr into a terminating error when the output is redirected.
function Invoke-Native {
    $ErrorActionPreference = 'Continue'
    $command, $arguments = $args
    & $command @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$command $($arguments -join ' ') failed with exit code $LASTEXITCODE."
    }
}
