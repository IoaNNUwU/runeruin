# Waits until this repo's Gradle game run (runClient / runServer / runGameTestServer) is gone.
# Never kills or starts those processes. Used by agents before `gradlew runData`.
param(
    [switch]$Check,
    [int]$PollSeconds = 5
)

$ErrorActionPreference = 'Continue'
$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Get-BlockingGameProcesses {
    $repo = $RepoRoot
    $repoSlash = $repo.Replace('\', '/')
    try {
        $procs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction Stop
    } catch {
        return @()
    }

    $blocking = foreach ($p in $procs) {
        $cmd = $p.CommandLine
        if ([string]::IsNullOrWhiteSpace($cmd)) { continue }
        if ($cmd.IndexOf($repo, [StringComparison]::OrdinalIgnoreCase) -lt 0 -and
            $cmd.IndexOf($repoSlash, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
            continue
        }

        # Datagen / extract / daemon-only — not a game lock we wait on.
        if ($cmd -match 'runData|DatagenMain|extractMcSources|org\.gradle\.launcher\.daemon\.bootstrap\.GradleDaemon') {
            continue
        }
        if ($cmd -match '--output' -and $cmd -match 'generated[\\/]resources') {
            continue
        }

        $isWrapperGame = ($cmd -match 'GradleWrapperMain|gradlew') -and
            ($cmd -match '(^|[\s"])runClient([\s"]|$)' -or
             $cmd -match '(^|[\s"])runServer([\s"]|$)' -or
             $cmd -match '(^|[\s"])runGameTestServer([\s"]|$)')
        $isClient = $cmd -match 'net\.minecraft\.client\.main\.Main'
        $isServer = $cmd -match 'net\.minecraft\.server\.(Main|MinecraftServer)'
        $isLaunch = ($cmd -match '--launchTarget\s+\S+' -and $cmd -notmatch 'data')
        $isBootstrapGame = ($cmd -match 'cpw\.mods\.bootstraplauncher' -and $cmd -notmatch 'data')

        if ($isWrapperGame -or $isClient -or $isServer -or $isLaunch -or $isBootstrapGame) {
            [PSCustomObject]@{ Pid = $p.ProcessId; Name = $p.Name }
        }
    }
    @($blocking)
}

$blocking = @(Get-BlockingGameProcesses)

if ($Check) {
    if ($blocking.Count -gt 0) {
        $pids = ($blocking | ForEach-Object { $_.Pid }) -join ', '
        [Console]::Error.WriteLine("Game Gradle run is active (PIDs: $pids). Close Minecraft; do not kill it from the agent.")
        exit 1
    }
    exit 0
}

if ($blocking.Count -eq 0) {
    Write-Host "No runClient/runServer process for this repo. Ready for datagen."
    exit 0
}

$pids = ($blocking | ForEach-Object { $_.Pid }) -join ', '
Write-Host "runClient/runServer is locking Gradle (PIDs: $pids)."
Write-Host "Close the Minecraft window. Datagen will start after that. This script will not kill or restart the client."

while ($true) {
    Start-Sleep -Seconds $PollSeconds
    $blocking = @(Get-BlockingGameProcesses)
    if ($blocking.Count -eq 0) {
        Write-Host "Game closed. Ready for datagen."
        exit 0
    }
}
