[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

function Get-GradleProperty {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Name
    )

    $pattern = '^' + [regex]::Escape($Name) + '=(.+)$'
    $match = Select-String -LiteralPath $Path -Pattern $pattern | Select-Object -First 1
    if ($null -eq $match) {
        throw "Gradle property '$Name' was not found in '$Path'."
    }

    return $match.Matches[0].Groups[1].Value.Trim()
}

function Read-VersionText {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Directory
    )

    $versionFile = Join-Path $Directory 'VERSION.txt'
    if (-not (Test-Path -LiteralPath $versionFile -PathType Leaf)) {
        return $null
    }

    return ((Get-Content -LiteralPath $versionFile -Raw -Encoding UTF8) -replace "`r`n", "`n").Trim()
}

function New-DirectoryLink {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Link,

        [Parameter(Mandatory = $true)]
        [string] $Target
    )

    if ($env:OS -eq 'Windows_NT') {
        New-Item -ItemType Junction -Path $Link -Target $Target | Out-Null
    }
    else {
        New-Item -ItemType SymbolicLink -Path $Link -Target $Target | Out-Null
    }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$gradleProperties = Join-Path $projectRoot 'gradle.properties'
$minecraftVersion = Get-GradleProperty -Path $gradleProperties -Name 'minecraft_version'
$neoVersion = Get-GradleProperty -Path $gradleProperties -Name 'neo_version'
$expectedVersion = "minecraft=$minecraftVersion`nneoforge=$neoVersion`nmappings=official"

$userHome = [Environment]::GetFolderPath('UserProfile')
if ([string]::IsNullOrWhiteSpace($userHome)) {
    $userHome = $env:USERPROFILE
}
if ([string]::IsNullOrWhiteSpace($userHome)) {
    throw 'Could not determine the current user profile directory.'
}

$cacheRoot = Join-Path $userHome '.runeruin\mc-sources'
$sharedSources = Join-Path $cacheRoot "minecraft-$minecraftVersion-neoforge-$neoVersion-official"
$worktreeSources = Join-Path $projectRoot '.mc-sources'
$sharedGame = Join-Path $userHome '.runeruin\game'
$legacyGame = Join-Path $projectRoot 'run'
$lockPath = Join-Path $cacheRoot '.setup.lock'
$gradleUserHome = Join-Path $userHome '.gradle'
$vanillaTextureVersion = $minecraftVersion -replace '\.0$', ''
$vanillaTextureVersionText = "minecraft=$vanillaTextureVersion`nsource=minecraft_${vanillaTextureVersion}_client.jar"
$vanillaTextures = Join-Path $projectRoot '.vanilla-textures'
$vanillaTextureSentinel = Join-Path $vanillaTextures 'assets\minecraft\textures\block\oak_planks.png'

function Ensure-VanillaTextures {
    if ((Read-VersionText -Directory $vanillaTextures) -eq $vanillaTextureVersionText -and
        (Test-Path -LiteralPath $vanillaTextureSentinel -PathType Leaf)) {
        return
    }

    $wrapper = Join-Path $projectRoot 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
        $wrapper = Join-Path $projectRoot 'gradlew'
    }

    Write-Host "Extracting Minecraft $vanillaTextureVersion textures into .vanilla-textures..."
    Push-Location $projectRoot
    try {
        & $wrapper '--gradle-user-home' $gradleUserHome 'extractVanillaTextures'
        if ($LASTEXITCODE -ne 0) {
            throw "extractVanillaTextures failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }

    if ((Read-VersionText -Directory $vanillaTextures) -ne $vanillaTextureVersionText -or
        -not (Test-Path -LiteralPath $vanillaTextureSentinel -PathType Leaf)) {
        throw "extractVanillaTextures completed without producing the expected versioned texture set: $vanillaTextures"
    }
}

New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null

$lockStream = $null
try {
    for ($attempt = 0; $attempt -lt 120; $attempt++) {
        try {
            $lockStream = [System.IO.File]::Open(
                $lockPath,
                [System.IO.FileMode]::OpenOrCreate,
                [System.IO.FileAccess]::ReadWrite,
                [System.IO.FileShare]::None
            )
            break
        }
        catch [System.IO.IOException] {
            Start-Sleep -Seconds 1
        }
    }

    if ($null -eq $lockStream) {
        throw "Could not acquire the Minecraft sources cache lock: $lockPath"
    }

    $sharedGameExists = Test-Path -LiteralPath $sharedGame
    if ($sharedGameExists -and -not (Test-Path -LiteralPath $sharedGame -PathType Container)) {
        throw "The shared game path is not a directory: $sharedGame"
    }

    if (-not $sharedGameExists) {
        $legacyGameItem = Get-Item -LiteralPath $legacyGame -Force -ErrorAction SilentlyContinue
        if ($null -ne $legacyGameItem -and -not ($legacyGameItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
            New-Item -ItemType Directory -Force -Path (Split-Path -Parent $sharedGame) | Out-Null
            Write-Host "Moving the existing game directory to the shared location..."
            try {
                Move-Item -LiteralPath $legacyGame -Destination $sharedGame -ErrorAction Stop
            }
            catch {
                # Move-Item cannot move a directory between volumes. Copy the
                # existing game data, then remove only the copied legacy dir.
                Copy-Item -LiteralPath $legacyGame -Destination $sharedGame -Recurse -Force
                Remove-Item -LiteralPath $legacyGame -Recurse -Force
            }
            New-DirectoryLink -Link $legacyGame -Target $sharedGame
            Write-Host "Linked the legacy run directory: $legacyGame -> $sharedGame"
        }
        else {
            New-Item -ItemType Directory -Force -Path $sharedGame | Out-Null
        }
    }

    $localSourcesReady = $false
    $existingWorktreeItem = Get-Item -LiteralPath $worktreeSources -Force -ErrorAction SilentlyContinue
    if ($null -ne $existingWorktreeItem) {
        if ($existingWorktreeItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint) {
            $linkedVersion = Read-VersionText -Directory $worktreeSources
            if ($linkedVersion -eq $expectedVersion) {
                Write-Host "Minecraft sources link already exists: $worktreeSources"
                Ensure-VanillaTextures
                return
            }

            Write-Host "Replacing the Minecraft sources link for the new project version."
            Remove-Item -LiteralPath $worktreeSources -Force
            $existingWorktreeItem = $null
        }

        if ($null -ne $existingWorktreeItem) {
            $localVersion = Read-VersionText -Directory $worktreeSources
            if ($localVersion -eq $expectedVersion) {
                $localSourcesReady = $true
            }
            else {
                throw "The worktree already contains '.mc-sources' with a different or missing version. Move it aside before running setup: $worktreeSources"
            }
        }
    }

    $sharedSourcesExists = Test-Path -LiteralPath $sharedSources
    $sharedVersion = Read-VersionText -Directory $sharedSources
    if ($sharedSourcesExists -and -not (Test-Path -LiteralPath $sharedSources -PathType Container)) {
        throw "The shared Minecraft sources cache path is not a directory: $sharedSources"
    }
    if ($sharedSourcesExists -and $sharedVersion -ne $expectedVersion) {
        $hasEntries = Get-ChildItem -LiteralPath $sharedSources -Force | Select-Object -First 1
        if ($null -eq $sharedVersion -and $null -eq $hasEntries) {
            # A prior interrupted setup can leave behind only the cache directory.
            # Remove that empty placeholder so extraction can initialize it cleanly.
            Remove-Item -LiteralPath $sharedSources -Force
            $sharedSourcesExists = $false
            Write-Host "Removed empty incomplete shared Minecraft sources cache directory: $sharedSources"
        }
        else {
            throw "The shared Minecraft sources cache exists with the wrong or missing version: $sharedSources"
        }
    }

    if ($sharedVersion -eq $expectedVersion) {
        if ($localSourcesReady) {
            Write-Host "Shared Minecraft sources cache already exists; keeping this worktree's existing copy: $worktreeSources"
        }
        else {
            New-DirectoryLink -Link $worktreeSources -Target $sharedSources
            Write-Host "Linked Minecraft sources: $worktreeSources -> $sharedSources"
        }
        Ensure-VanillaTextures
        return
    }

    if ($localSourcesReady) {
        try {
            Move-Item -LiteralPath $worktreeSources -Destination $sharedSources -ErrorAction Stop
        }
        catch {
            # Move-Item cannot move a directory between volumes. Copy only the
            # validated extraction, then remove the temporary worktree copy.
            Copy-Item -LiteralPath $worktreeSources -Destination $sharedSources -Recurse -Force
            if ((Read-VersionText -Directory $sharedSources) -ne $expectedVersion) {
                throw "The copied Minecraft sources cache failed validation: $sharedSources"
            }
            Remove-Item -LiteralPath $worktreeSources -Recurse -Force
        }
        New-DirectoryLink -Link $worktreeSources -Target $sharedSources
        Write-Host "Moved existing Minecraft sources to the shared cache and linked this worktree."
        Ensure-VanillaTextures
        return
    }

    $wrapper = Join-Path $projectRoot 'gradlew.bat'
    if (-not (Test-Path -LiteralPath $wrapper -PathType Leaf)) {
        $wrapper = Join-Path $projectRoot 'gradlew'
    }

    Write-Host "Preparing the shared Minecraft sources cache for $minecraftVersion / $neoVersion..."
    Push-Location $projectRoot
    try {
        & $wrapper '--gradle-user-home' $gradleUserHome 'extractMcSources'
        if ($LASTEXITCODE -ne 0) {
            throw "extractMcSources failed with exit code $LASTEXITCODE."
        }
    }
    finally {
        Pop-Location
    }

    if ((Read-VersionText -Directory $worktreeSources) -ne $expectedVersion) {
        throw "extractMcSources completed without producing the expected VERSION.txt."
    }

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $sharedSources) | Out-Null
    try {
        Move-Item -LiteralPath $worktreeSources -Destination $sharedSources -ErrorAction Stop
    }
    catch {
        # Move-Item cannot move a directory between volumes. Copy only the
        # validated extraction, then remove the temporary worktree copy.
        Copy-Item -LiteralPath $worktreeSources -Destination $sharedSources -Recurse -Force
        if ((Read-VersionText -Directory $sharedSources) -ne $expectedVersion) {
            throw "The copied Minecraft sources cache failed validation: $sharedSources"
        }
        Remove-Item -LiteralPath $worktreeSources -Recurse -Force
    }
    New-DirectoryLink -Link $worktreeSources -Target $sharedSources
    Write-Host "Created shared Minecraft sources cache and linked it into the worktree."
    Ensure-VanillaTextures
}
finally {
    if ($null -ne $lockStream) {
        $lockStream.Dispose()
    }
}
