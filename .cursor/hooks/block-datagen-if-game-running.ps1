# Deny `gradlew runData` while this repo's game run is still locking build/.
# Combined commands that already wait (wait-until-game-closed) are allowed.
$ErrorActionPreference = 'Continue'
try {
    $raw = [Console]::In.ReadToEnd()
    $payload = $raw | ConvertFrom-Json
} catch {
    Write-Output '{ "permission": "allow" }'
    exit 0
}

$command = [string]$payload.command
if ($command -notmatch 'runData') {
    Write-Output '{ "permission": "allow" }'
    exit 0
}
if ($command -match 'wait-until-game-closed') {
    Write-Output '{ "permission": "allow" }'
    exit 0
}

$check = Join-Path $PWD 'scripts\wait-until-game-closed.ps1'
if (-not (Test-Path $check)) {
    Write-Output '{ "permission": "allow" }'
    exit 0
}

& $check -Check *>$null
if ($LASTEXITCODE -eq 0) {
    Write-Output '{ "permission": "allow" }'
    exit 0
}

$msg = 'runClient/runServer is locking build/. Tell the user to close Minecraft. Run .\scripts\wait-until-game-closed.ps1 (long block_until_ms / AwaitShell until it exits 0), then .\gradlew runData. Do not kill Java/Gradle, do not gradlew --stop, do not start runClient afterwards.'
$json = @{
    permission    = 'deny'
    agent_message = $msg
    user_message  = 'Datagen is waiting: close Minecraft (runClient), then the agent will run datagen. It will not restart the client.'
} | ConvertTo-Json -Compress
Write-Output $json
exit 0
