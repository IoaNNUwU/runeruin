[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')

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
# Paths below use '/', which works everywhere.
$wrapper = Get-GradleWrapper $projectRoot
$gradleProperties = Join-Path $projectRoot 'gradle.properties'
$minecraftVersion = Get-GradleProperty -Path $gradleProperties -Name 'minecraft_version'
$neoVersion = Get-GradleProperty -Path $gradleProperties -Name 'neo_version'
$expectedVersion = "minecraft=$minecraftVersion`nneoforge=$neoVersion`nmappings=official"

$userHome = Get-UserHome
$cacheRoot = Join-Path $userHome '.runeruin/mc-sources'
$sharedSources = Join-Path $cacheRoot "minecraft-$minecraftVersion-neoforge-$neoVersion-official"
$worktreeSources = Join-Path $projectRoot '.mc-sources'
# The runClient game directory, as in build.gradle.
$sharedGame = if ($env:RUNERUIN_GAME_DIR) { $env:RUNERUIN_GAME_DIR } else { Join-Path $userHome '.runeruin/game' }
$lockPath = Join-Path $cacheRoot '.setup.lock'
$gradleUserHome = Join-Path $userHome '.gradle'
$vanillaTextureVersion = $minecraftVersion -replace '\.0$', ''
$vanillaTextureVersionText = "minecraft=$vanillaTextureVersion`nsource=minecraft_${vanillaTextureVersion}_client.jar`ncontent=textures,models"
$vanillaTextures = Join-Path $projectRoot '.vanilla-textures'
$vanillaTextureSentinel = Join-Path $vanillaTextures 'assets/minecraft/models/block/cross.json'

function Ensure-VanillaTextures {
    if ((Read-VersionText -Directory $vanillaTextures) -eq $vanillaTextureVersionText -and
        (Test-Path -LiteralPath $vanillaTextureSentinel -PathType Leaf)) {
        return
    }

    Write-Host "Extracting Minecraft $vanillaTextureVersion textures and models into .vanilla-textures..."
    Invoke-Native $wrapper -p $projectRoot '--gradle-user-home' $gradleUserHome 'extractVanillaTextures'

    if ((Read-VersionText -Directory $vanillaTextures) -ne $vanillaTextureVersionText -or
        -not (Test-Path -LiteralPath $vanillaTextureSentinel -PathType Leaf)) {
        throw "extractVanillaTextures completed without producing the expected versioned texture set: $vanillaTextures"
    }
}

# Moves the validated sources of this worktree into the shared cache and links them back.
function Move-ToSharedCache {
    New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null
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
}

New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null

$lockStream = $null
try {
    # A first extractMcSources decompiles Minecraft and can take several minutes in another worktree.
    for ($attempt = 0; $attempt -lt 1800; $attempt++) {
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
            if ($attempt -eq 0) {
                Write-Host "Waiting for another checkout to finish setting up the Minecraft sources cache..."
            }
            Start-Sleep -Seconds 1
        }
    }

    if ($null -eq $lockStream) {
        throw "Could not acquire the Minecraft sources cache lock in 30 minutes: $lockPath"
    }

    # runClient uses this shared directory; every other run keeps its own <checkout>/run.
    New-Item -ItemType Directory -Force -Path $sharedGame | Out-Null

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
            # Removes only the link. Remove-Item on a junction can delete the shared cache behind it.
            [System.IO.Directory]::Delete($worktreeSources)
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

    if (-not $localSourcesReady) {
        Write-Host "Preparing the shared Minecraft sources cache for $minecraftVersion / $neoVersion..."
        Invoke-Native $wrapper -p $projectRoot '--gradle-user-home' $gradleUserHome 'extractMcSources'
        if ((Read-VersionText -Directory $worktreeSources) -ne $expectedVersion) {
            throw "extractMcSources completed without producing the expected VERSION.txt."
        }
    }

    Move-ToSharedCache
    Write-Host "Moved this worktree's Minecraft sources to the shared cache and linked them: $worktreeSources -> $sharedSources"
    Ensure-VanillaTextures
}
finally {
    if ($null -ne $lockStream) {
        $lockStream.Dispose()
    }
}
