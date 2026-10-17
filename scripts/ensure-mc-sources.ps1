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

# True for a complete extraction of this version: an interrupted one may have VERSION.txt without the files.
function Test-Extraction {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Directory
    )

    if ((Read-VersionText -Directory $Directory) -ne $expectedVersion) {
        return $false
    }
    foreach ($sentinel in 'code/net/minecraft/core/BlockPos.java', 'textures/assets/minecraft/models/block/cross.json') {
        if (-not (Test-Path -LiteralPath (Join-Path $Directory $sentinel) -PathType Leaf)) {
            return $false
        }
    }
    return $true
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

# Removes a link without its target, or a directory of this checkout with its contents.
function Remove-LinkOrDirectory([System.IO.FileSystemInfo] $Item) {
    if ($Item.Attributes -band [System.IO.FileAttributes]::ReparsePoint) {
        # Remove-Item on a junction can delete the shared cache behind it.
        [System.IO.Directory]::Delete($Item.FullName)
    }
    else {
        Remove-Item -LiteralPath $Item.FullName -Recurse -Force
    }
}

$projectRoot = Split-Path -Parent $PSScriptRoot
# Paths below use '/', which works everywhere.
$wrapper = Get-GradleWrapper $projectRoot
$gradleProperties = Join-Path $projectRoot 'gradle.properties'
$minecraftVersion = Get-GradleProperty -Path $gradleProperties -Name 'minecraft_version'
$neoVersion = Get-GradleProperty -Path $gradleProperties -Name 'neo_version'
# As in build.gradle: code/ holds the Java sources, textures/ the vanilla textures and models.
$expectedVersion = "minecraft=$minecraftVersion`nneoforge=$neoVersion`nmappings=official`ncontent=code,textures"

$userHome = Get-UserHome
$cacheRoot = Join-Path $userHome '.runeruin/mc-sources'
# The minecraft-*-official caches hold the sources without code/ and textures/, for checkouts that have not switched yet.
$sharedSources = Join-Path $cacheRoot "minecraft-$minecraftVersion-neoforge-$neoVersion"
$worktreeSources = Join-Path $projectRoot '.mc-sources'
# Checkouts set up before the textures moved into .mc-sources/textures have a copy of their own here.
$oldVanillaTextures = Join-Path $projectRoot '.vanilla-textures'
# The runClient game directory, as in build.gradle.
$sharedGame = if ($env:RUNERUIN_GAME_DIR) { $env:RUNERUIN_GAME_DIR } else { Join-Path $userHome '.runeruin/game' }
$lockPath = Join-Path $cacheRoot '.setup.lock'
$gradleUserHome = Join-Path $userHome '.gradle'

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

    $oldItem = Get-Item -LiteralPath $oldVanillaTextures -Force -ErrorAction SilentlyContinue
    if ($null -ne $oldItem) {
        Remove-LinkOrDirectory $oldItem
        Write-Host "Removed the old .vanilla-textures, now in .mc-sources/textures: $oldVanillaTextures"
    }

    $existingWorktreeItem = Get-Item -LiteralPath $worktreeSources -Force -ErrorAction SilentlyContinue
    if ($null -ne $existingWorktreeItem -and ($existingWorktreeItem.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
        if (Test-Extraction -Directory $worktreeSources) {
            Write-Host "Minecraft sources link already exists: $worktreeSources"
            return
        }

        Write-Host "Replacing the Minecraft sources link for the new project version."
        Remove-LinkOrDirectory $existingWorktreeItem
        $existingWorktreeItem = $null
    }

    $localSourcesReady = $null -ne $existingWorktreeItem
    if ($localSourcesReady -and -not (Test-Extraction -Directory $worktreeSources)) {
        throw "The worktree already contains '.mc-sources' with a different or missing version. Move it aside before running setup: $worktreeSources"
    }

    if (Test-Extraction -Directory $sharedSources) {
        if ($localSourcesReady) {
            Remove-LinkOrDirectory $existingWorktreeItem
            Write-Host "Removed this worktree's own copy of the Minecraft sources in favour of the shared cache."
        }
        New-DirectoryLink -Link $worktreeSources -Target $sharedSources
        Write-Host "Linked Minecraft sources: $worktreeSources -> $sharedSources"
        return
    }

    if (Test-Path -LiteralPath $sharedSources) {
        if (-not (Test-Path -LiteralPath $sharedSources -PathType Container)) {
            throw "The shared Minecraft sources cache path is not a directory: $sharedSources"
        }
        if ($null -ne (Get-ChildItem -LiteralPath $sharedSources -Force | Select-Object -First 1)) {
            throw "The shared Minecraft sources cache exists with the wrong or missing version: $sharedSources"
        }
        # A prior interrupted setup can leave behind only the cache directory.
        # Remove that empty placeholder so extraction can initialize it cleanly.
        Remove-Item -LiteralPath $sharedSources -Force
        Write-Host "Removed empty incomplete shared Minecraft sources cache directory: $sharedSources"
    }

    if (-not $localSourcesReady) {
        Write-Host "Preparing the shared Minecraft sources cache for $minecraftVersion / $neoVersion..."
        Invoke-Native $wrapper -p $projectRoot '--gradle-user-home' $gradleUserHome 'extractMcSources'
        if (-not (Test-Extraction -Directory $worktreeSources)) {
            throw "extractMcSources completed without producing the expected sources, textures and VERSION.txt: $worktreeSources"
        }
    }

    try {
        Move-Item -LiteralPath $worktreeSources -Destination $sharedSources -ErrorAction Stop
    }
    catch {
        # Move-Item cannot move a directory between volumes. Copy only the
        # validated extraction, then remove the temporary worktree copy.
        Copy-Item -LiteralPath $worktreeSources -Destination $sharedSources -Recurse -Force
        if (-not (Test-Extraction -Directory $sharedSources)) {
            throw "The copied Minecraft sources cache failed validation: $sharedSources"
        }
        Remove-Item -LiteralPath $worktreeSources -Recurse -Force
    }
    New-DirectoryLink -Link $worktreeSources -Target $sharedSources
    Write-Host "Moved this worktree's Minecraft sources to the shared cache and linked them: $worktreeSources -> $sharedSources"
}
finally {
    if ($null -ne $lockStream) {
        $lockStream.Dispose()
    }
}
