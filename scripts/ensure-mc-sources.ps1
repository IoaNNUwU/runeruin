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
        [string] $Directory,

        [Parameter(Mandatory = $true)]
        [string] $Version,

        [Parameter(Mandatory = $true)]
        [string] $Sentinel
    )

    return (Read-VersionText -Directory $Directory) -eq $Version -and
        (Test-Path -LiteralPath (Join-Path $Directory $Sentinel) -PathType Leaf)
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

# Links $Link in this checkout to the shared cache $Shared of this version. The first checkout extracts it
# with $Task into $Link and moves it to $Shared; a copy of its own from before the cache existed is replaced by the link.
function Ensure-SharedCache {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Label,

        [Parameter(Mandatory = $true)]
        [string] $Link,

        [Parameter(Mandatory = $true)]
        [string] $Shared,

        [Parameter(Mandatory = $true)]
        [string] $Version,

        [Parameter(Mandatory = $true)]
        [string] $Sentinel,

        [Parameter(Mandatory = $true)]
        [string] $Task
    )

    $item = Get-Item -LiteralPath $Link -Force -ErrorAction SilentlyContinue
    if ($null -ne $item -and ($item.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) {
        if (Test-Extraction -Directory $Link -Version $Version -Sentinel $Sentinel) {
            Write-Host "Link to the shared $Label already exists: $Link"
            return
        }

        Write-Host "Replacing the link to the shared $Label for the new project version."
        # Removes only the link. Remove-Item on a junction can delete the shared cache behind it.
        [System.IO.Directory]::Delete($Link)
        $item = $null
    }

    $localReady = $null -ne $item
    if ($localReady -and -not (Test-Extraction -Directory $Link -Version $Version -Sentinel $Sentinel)) {
        throw "The worktree already contains '$($item.Name)' with a different or missing version. Move it aside before running setup: $Link"
    }

    if (Test-Extraction -Directory $Shared -Version $Version -Sentinel $Sentinel) {
        if ($localReady) {
            Remove-Item -LiteralPath $Link -Recurse -Force
            Write-Host "Removed this worktree's own copy of the $Label in favour of the shared one: $Link"
        }
        New-DirectoryLink -Link $Link -Target $Shared
        Write-Host "Linked the shared ${Label}: $Link -> $Shared"
        return
    }

    if (Test-Path -LiteralPath $Shared) {
        if (-not (Test-Path -LiteralPath $Shared -PathType Container)) {
            throw "The shared $Label cache path is not a directory: $Shared"
        }
        if ($null -ne (Get-ChildItem -LiteralPath $Shared -Force | Select-Object -First 1)) {
            throw "The shared $Label cache exists with the wrong or missing version: $Shared"
        }
        # A prior interrupted setup can leave behind only the cache directory.
        # Remove that empty placeholder so extraction can initialize it cleanly.
        Remove-Item -LiteralPath $Shared -Force
        Write-Host "Removed empty incomplete shared $Label cache directory: $Shared"
    }

    if (-not $localReady) {
        Write-Host "Preparing the shared $Label cache with $Task..."
        Invoke-Native $wrapper -p $projectRoot '--gradle-user-home' $gradleUserHome $Task
        if (-not (Test-Extraction -Directory $Link -Version $Version -Sentinel $Sentinel)) {
            throw "$Task completed without producing the expected $Label with VERSION.txt: $Link"
        }
    }

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Shared) | Out-Null
    try {
        Move-Item -LiteralPath $Link -Destination $Shared -ErrorAction Stop
    }
    catch {
        # Move-Item cannot move a directory between volumes. Copy only the
        # validated extraction, then remove the temporary worktree copy.
        Copy-Item -LiteralPath $Link -Destination $Shared -Recurse -Force
        if (-not (Test-Extraction -Directory $Shared -Version $Version -Sentinel $Sentinel)) {
            throw "The copied shared $Label cache failed validation: $Shared"
        }
        Remove-Item -LiteralPath $Link -Recurse -Force
    }
    New-DirectoryLink -Link $Link -Target $Shared
    Write-Host "Moved this worktree's $Label to the shared cache and linked them: $Link -> $Shared"
}

$projectRoot = Split-Path -Parent $PSScriptRoot
# Paths below use '/', which works everywhere.
$wrapper = Get-GradleWrapper $projectRoot
$gradleProperties = Join-Path $projectRoot 'gradle.properties'
$minecraftVersion = Get-GradleProperty -Path $gradleProperties -Name 'minecraft_version'
$neoVersion = Get-GradleProperty -Path $gradleProperties -Name 'neo_version'
# The client archive of Minecraft 26.2.0 has the release id 26.2, as in build.gradle.
$minecraftClientVersion = $minecraftVersion -replace '\.0$', ''

$userHome = Get-UserHome
$cacheRoot = Join-Path $userHome '.runeruin/mc-sources'
# The runClient game directory, as in build.gradle.
$sharedGame = if ($env:RUNERUIN_GAME_DIR) { $env:RUNERUIN_GAME_DIR } else { Join-Path $userHome '.runeruin/game' }
# One lock for both caches, where older checkouts of this script look for it.
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
                Write-Host "Waiting for another checkout to finish setting up the shared Minecraft caches..."
            }
            Start-Sleep -Seconds 1
        }
    }

    if ($null -eq $lockStream) {
        throw "Could not acquire the shared Minecraft caches lock in 30 minutes: $lockPath"
    }

    # runClient uses this shared directory; every other run keeps its own <checkout>/run.
    New-Item -ItemType Directory -Force -Path $sharedGame | Out-Null

    Ensure-SharedCache -Label 'Minecraft sources' `
        -Link (Join-Path $projectRoot '.mc-sources') `
        -Shared (Join-Path $cacheRoot "minecraft-$minecraftVersion-neoforge-$neoVersion-official") `
        -Version "minecraft=$minecraftVersion`nneoforge=$neoVersion`nmappings=official" `
        -Sentinel 'net/minecraft/core/BlockPos.java' `
        -Task 'extractMcSources'

    Ensure-SharedCache -Label 'vanilla textures and models' `
        -Link (Join-Path $projectRoot '.vanilla-textures') `
        -Shared (Join-Path $userHome ".runeruin/vanilla-textures/minecraft-$minecraftClientVersion") `
        -Version "minecraft=$minecraftClientVersion`nsource=minecraft_${minecraftClientVersion}_client.jar`ncontent=textures,models" `
        -Sentinel 'assets/minecraft/models/block/cross.json' `
        -Task 'extractVanillaTextures'
}
finally {
    if ($null -ne $lockStream) {
        $lockStream.Dispose()
    }
}
